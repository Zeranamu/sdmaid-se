package eu.darken.sdmse.systemcleaner.ui.customfilter.editor

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.sdmse.R
import eu.darken.sdmse.common.areas.DataArea
import eu.darken.sdmse.common.files.FileType
import eu.darken.sdmse.common.uix.Fragment3
import eu.darken.sdmse.common.viewbinding.viewBinding
import eu.darken.sdmse.databinding.SystemcleanerCustomfilterEditorFragmentBinding
import java.io.File


@AndroidEntryPoint
class CustomFilterEditorFragment : Fragment3(R.layout.systemcleaner_customfilter_editor_fragment) {

    override val vm: CustomFilterEditorViewModel by viewModels()
    override val ui: SystemcleanerCustomfilterEditorFragmentBinding by viewBinding()

    private val onBackPressedcallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            vm.cancel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedcallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.toolbar.apply {
            setupWithNavController(findNavController())
            setNavigationOnClickListener { vm.cancel() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_action_remove_exclusion -> {
                        vm.remove()
                        true
                    }

                    R.id.menu_action_save_exclusion -> {
                        vm.save()
                        true
                    }

                    else -> false
                }
            }
        }

        ui.labelInput.addTextChangedListener { text: Editable? ->
            vm.updateLabel(text?.toString() ?: "")
        }

        ui.pathContainsInput.apply {
            onUserAddedTag = { tag -> vm.addPath(tag) }
            onUserRemovedTag = { tag -> vm.removePath(tag) }
        }

        val nonPathInputFilter = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (source[i] == File.separatorChar) return@InputFilter ""
            }
            null
        }

        ui.nameContainInput.apply {
            inputFilter = nonPathInputFilter
            onUserAddedTag = { tag -> vm.addNameContains(tag) }
            onUserRemovedTag = { tag -> vm.removeNameContains(tag) }
        }

        ui.nameEndsWithInput.apply {
            inputFilter = nonPathInputFilter
            onUserAddedTag = { tag -> vm.addNameEndsWith(tag) }
            onUserRemovedTag = { tag -> vm.removeNameEndsWith(tag) }
        }

        ui.exclusionsInput.apply {
            onUserAddedTag = { tag -> vm.addExclusion(tag) }
            onUserRemovedTag = { tag -> vm.removeExclusion(tag) }
        }

        val areaChips = mutableMapOf<DataArea.Type, Chip>()
        setOf(
            DataArea.Type.SDCARD,
            DataArea.Type.PUBLIC_DATA,
            DataArea.Type.PUBLIC_MEDIA,
            DataArea.Type.PUBLIC_OBB,
            DataArea.Type.PRIVATE_DATA,
        ).forEach { type ->
            val chip = Chip(
                context,
                null,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter_Elevated
            ).apply {
                id = ViewCompat.generateViewId()
                this.text = type.raw
                isClickable = true
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked -> vm.toggleArea(type, isChecked) }
                areaChips[type] = this
            }
            ui.dataAreasContainer.addView(chip)
        }

        ui.apply {
            filetypesOptionFiles.setOnClickListener { vm.toggleFileType(FileType.FILE) }
            filetypesOptionDirectories.setOnClickListener { vm.toggleFileType(FileType.DIRECTORY) }
        }

        vm.state.observe2(ui) { state ->
            val config = state.current
            toolbar.menu?.apply {
                findItem(R.id.menu_action_save_exclusion)?.isVisible = state.canSave
                findItem(R.id.menu_action_remove_exclusion)?.isVisible = state.canRemove
            }
            toolbar.subtitle = config.label
            if (labelInput.text.isNullOrEmpty()) labelInput.setText(config.label)

            pathContainsInput.setTags(config.pathContains?.map { vm.pathToTag(it) } ?: emptyList())
            nameContainInput.setTags(config.nameContains?.map { vm.nameToTag(it) } ?: emptyList())
            nameEndsWithInput.setTags(config.nameEndsWith?.map { vm.nameToTag(it) } ?: emptyList())
            exclusionsInput.setTags(config.exclusion?.map { vm.pathToTag(it) } ?: emptyList())

            areaChips.entries.forEach { (type, chip) ->
                chip.isChecked = config.areas?.contains(type) == true
            }

            filetypesOptionFiles.isChecked = config.fileTypes?.contains(FileType.FILE) == true
            filetypesOptionDirectories.isChecked = config.fileTypes?.contains(FileType.DIRECTORY) == true
        }

        vm.events.observe2 {
            when (it) {
                is CustomFilterEditorEvents.RemoveConfirmation -> MaterialAlertDialogBuilder(requireContext()).apply {
                    setMessage(R.string.systemcleaner_editor_remove_confirmation_message)
                    setPositiveButton(eu.darken.sdmse.common.R.string.general_remove_action) { _, _ ->
                        vm.remove(confirmed = true)
                    }
                    setNegativeButton(eu.darken.sdmse.common.R.string.general_cancel_action) { _, _ ->
                    }
                }.show()

                is CustomFilterEditorEvents.UnsavedChangesConfirmation -> MaterialAlertDialogBuilder(requireContext()).apply {
                    setMessage(R.string.systemcleaner_editor_unsaved_confirmation_message)
                    setPositiveButton(eu.darken.sdmse.common.R.string.general_discard_action) { _, _ ->
                        vm.cancel(confirmed = true)
                    }
                    setNegativeButton(eu.darken.sdmse.common.R.string.general_cancel_action) { _, _ ->
                    }
                }.show()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }
}