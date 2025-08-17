package org.catrobat.catroid.libraryeditor.ui

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
import org.catrobat.catroid.libraryeditor.data.EditableFormula
import org.catrobat.catroid.libraryeditor.data.EditableParam
import org.catrobat.catroid.libraryeditor.data.LibraryEditorViewModel

class FormulaEditDialogFragment : DialogFragment() {

    private val viewModel: LibraryEditorViewModel by activityViewModels()
    private var formulaToEdit: EditableFormula? = null
    private lateinit var paramsContainer: LinearLayout
    private lateinit var inflater: LayoutInflater

    companion object {
        private const val ARG_FORMULA_ID = "formula_id"
        fun newInstance(formulaId: String?): FormulaEditDialogFragment {
            return FormulaEditDialogFragment().apply {
                arguments = bundleOf(ARG_FORMULA_ID to formulaId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val formulaId = arguments?.getString(ARG_FORMULA_ID)
        formulaToEdit = viewModel.libraryDraft.value?.formulas?.find { it.id == formulaId }
        inflater = requireActivity().layoutInflater

        val view = inflater.inflate(R.layout.dialog_edit_formula, null)
        val idEdit: EditText = view.findViewById(R.id.formula_id_edit)
        val displayNameEdit: EditText = view.findViewById(R.id.formula_display_name_edit)
        val functionNameEdit: EditText = view.findViewById(R.id.formula_function_name_edit)
        val addParamButton: Button = view.findViewById(R.id.add_param_button)
        paramsContainer = view.findViewById(R.id.params_container)

        // Заполнение полей
        formulaToEdit?.let {
            idEdit.setText(it.id)
            displayNameEdit.setText(it.displayName)
            functionNameEdit.setText(it.functionName)
            it.params.forEach { param -> addParamView(param) }
        }
        idEdit.isEnabled = (formulaToEdit == null)

        addParamButton.setOnClickListener { addParamView() }

        return AlertDialog.Builder(requireContext())
            .setTitle(if (formulaToEdit == null) getString(R.string.libs_new_form) else getString(R.string.libs_edit_form))
            .setView(view)
            .setPositiveButton(getString(R.string.libs_save), null)
            .setNegativeButton(getString(R.string.libs_cancel), null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        onSaveClicked(idEdit, displayNameEdit, functionNameEdit)
                    }
                }
            }
    }

    private fun addParamView(param: EditableParam? = null) {
        val paramView = inflater.inflate(R.layout.list_item_editable_param, paramsContainer, false)

        // --- ИСПРАВЛЕНИЕ: Получаем ссылки на TextInputLayout ---
        val typeLayout: TextInputLayout = paramView.findViewById(R.id.param_name_layout)
        val defaultLayout: TextInputLayout = paramView.findViewById(R.id.param_value_layout)

        val typeEdit: EditText = paramView.findViewById(R.id.param_name_edit)
        val defaultEdit: EditText = paramView.findViewById(R.id.param_value_edit)
        val deleteButton: ImageButton = paramView.findViewById(R.id.delete_param_button)

        // --- ИСПРАВЛЕНИЕ: Устанавливаем hint для TextInputLayout, а не для EditText ---
        typeLayout.hint = getString(R.string.libs_type2)
        defaultLayout.hint = getString(R.string.libs_def)

        param?.let {
            typeEdit.setText(it.type)
            defaultEdit.setText(it.defaultValue)
        }

        deleteButton.setOnClickListener {
            paramsContainer.removeView(paramView)
        }
        paramsContainer.addView(paramView)
    }

    private fun onSaveClicked(idEdit: EditText, displayNameEdit: EditText, functionNameEdit: EditText) {
        val id = idEdit.text.toString().trim()
        val displayName = displayNameEdit.text.toString().trim()
        val functionName = functionNameEdit.text.toString().trim()

        if (id.isEmpty() || displayName.isEmpty() || functionName.isEmpty()) {
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
            val defaultValue = paramView.findViewById<EditText>(R.id.param_value_edit).text.toString()
            if (type.isNotEmpty()) {
                paramsList.add(EditableParam(type = type, defaultValue = defaultValue))
            }
        }

        val formula = formulaToEdit?.copy(
            displayName = displayName,
            functionName = functionName,
            params = paramsList
        ) ?: EditableFormula(
            id = id,
            displayName = displayName,
            functionName = functionName,
            params = paramsList
        )
        viewModel.saveFormula(formula)
        dismiss()
    }
}