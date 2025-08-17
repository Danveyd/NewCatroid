package org.catrobat.catroid.libraryeditor.data

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout
import org.catrobat.catroid.R

class BrickEditDialogFragment : DialogFragment() {

    private val viewModel: LibraryEditorViewModel by activityViewModels()
    private var brickToEdit: EditableBrick? = null
    private lateinit var paramsContainer: LinearLayout
    private lateinit var inflater: LayoutInflater

    companion object {
        private const val ARG_BRICK_ID = "brick_id"
        fun newInstance(brickId: String?): BrickEditDialogFragment {
            return BrickEditDialogFragment().apply {
                arguments = bundleOf(ARG_BRICK_ID to brickId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val brickId = arguments?.getString(ARG_BRICK_ID)
        brickToEdit = viewModel.libraryDraft.value?.bricks?.find { it.id == brickId }
        inflater = requireActivity().layoutInflater

        val view = inflater.inflate(R.layout.dialog_edit_formula, null)
        val idEdit: EditText = view.findViewById(R.id.formula_id_edit)
        val headerEdit: EditText = view.findViewById(R.id.formula_display_name_edit)
        val functionNameEdit: EditText = view.findViewById(R.id.formula_function_name_edit)
        val addParamButton: Button = view.findViewById(R.id.add_param_button)
        paramsContainer = view.findViewById(R.id.params_container)

        val headerLayout: TextInputLayout = view.findViewById(R.id.text_input_layout_display_name)
        headerLayout.hint = getString(R.string.libs_htext)

        brickToEdit?.let {
            idEdit.setText(it.id)
            headerEdit.setText(it.headerText)
            functionNameEdit.setText(it.functionName)
            it.params.forEach { param -> addParamView(param) }
        }
        idEdit.isEnabled = (brickToEdit == null)

        addParamButton.setOnClickListener { addParamView() }

        return AlertDialog.Builder(requireContext())
            .setTitle(if (brickToEdit == null) getString(R.string.libs_new_brick) else getString(R.string.libs_edit_brick))
            .setView(view)
            .setPositiveButton(getString(R.string.libs_save), null)
            .setNegativeButton(getString(R.string.libs_cancel), null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        onSaveClicked(idEdit, headerEdit, functionNameEdit)
                    }
                }
            }
    }

    private fun addParamView(param: EditableParam? = null) {
        val paramView = inflater.inflate(R.layout.list_item_editable_param, paramsContainer, false)

        // --- ИСПРАВЛЕНИЕ: Получаем ссылки на TextInputLayout ---
        val typeLayout: TextInputLayout = paramView.findViewById(R.id.param_name_layout)
        val nameLayout: TextInputLayout = paramView.findViewById(R.id.param_value_layout)

        val typeEdit: EditText = paramView.findViewById(R.id.param_name_edit)
        val nameEdit: EditText = paramView.findViewById(R.id.param_value_edit)
        val deleteButton: ImageButton = paramView.findViewById(R.id.delete_param_button)

        // --- ИСПРАВЛЕНИЕ: Устанавливаем hint для TextInputLayout, а не для EditText ---
        typeLayout.hint = getString(R.string.libs_type1)
        nameLayout.hint = getString(R.string.libs_cname)

        param?.let {
            typeEdit.setText(it.type)
            nameEdit.setText(it.name)
        }

        deleteButton.setOnClickListener {
            paramsContainer.removeView(paramView)
        }
        paramsContainer.addView(paramView)
    }

    private fun onSaveClicked(idEdit: EditText, headerEdit: EditText, functionNameEdit: EditText) {
        val id = idEdit.text.toString().trim()
        val header = headerEdit.text.toString().trim()
        val functionName = functionNameEdit.text.toString().trim()

        if (id.isEmpty() || header.isEmpty() || functionName.isEmpty()) {
            Toast.makeText(context, getString(R.string.libs_error_n), Toast.LENGTH_SHORT).show()
            return
        }
        if (!viewModel.isFunctionDefined(functionName)) {
            functionNameEdit.error = getString(R.string.libs_error_f)
            return
        }

        val paramsList = mutableListOf<EditableParam>()
        for (i in 0 until paramsContainer.childCount) {
            val paramView = paramsContainer.getChildAt(i)
            val type = paramView.findViewById<EditText>(R.id.param_name_edit).text.toString().uppercase()
            val name = paramView.findViewById<EditText>(R.id.param_value_edit).text.toString()
            if (type.isNotEmpty()) {
                paramsList.add(EditableParam(type = type, name = name, defaultValue = null))
            }
        }

        val brick = brickToEdit?.copy(
            headerText = header,
            functionName = functionName,
            params = paramsList
        ) ?: EditableBrick(
            id = id,
            headerText = header,
            functionName = functionName,
            params = paramsList
        )
        viewModel.saveBrick(brick)
        dismiss()
    }
}