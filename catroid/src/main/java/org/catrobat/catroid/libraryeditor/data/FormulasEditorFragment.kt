package org.catrobat.catroid.libraryeditor.data

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.catrobat.catroid.R
import org.catrobat.catroid.libraryeditor.ui.FormulaEditDialogFragment

class FormulasEditorFragment : Fragment() {

    private val viewModel: LibraryEditorViewModel by activityViewModels()
    private lateinit var adapter: FormulaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_formulas_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.formulas_recycler_view)
        val fab: FloatingActionButton = view.findViewById(R.id.fab_add_formula)

        setupAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.libraryDraft.observe(viewLifecycleOwner) { draft ->
            adapter.submitList(draft.formulas.toList()) // Отправляем копию списка
        }

        fab.setOnClickListener {
            FormulaEditDialogFragment.newInstance(null).show(childFragmentManager, "EDIT_FORMULA_DIALOG")
        }
    }

    private fun setupAdapter() {
        adapter = FormulaAdapter(
            onItemClick = { formula ->
                FormulaEditDialogFragment.newInstance(formula.id).show(childFragmentManager, "EDIT_FORMULA_DIALOG")
            },
            onItemLongClick = { formula ->
                showDeleteConfirmationDialog(formula.id, formula.displayName)
            }
        )
    }

    private fun showDeleteConfirmationDialog(formulaId: String, name: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.libs_delete_wf))
            .setMessage(getString(R.string.libs_delete_w2) + " '$name'?")
            .setPositiveButton(getString(R.string.libs_delete)) { _, _ ->
                viewModel.deleteFormula(formulaId)
            }
            .setNegativeButton(getString(R.string.libs_cancel), null)
            .show()
    }
}