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

class BricksEditorFragment : Fragment() {

    private val viewModel: LibraryEditorViewModel by activityViewModels()
    private lateinit var adapter: BrickAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bricks_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.bricks_recycler_view)
        val fab: FloatingActionButton = view.findViewById(R.id.fab_add_brick)

        setupAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.libraryDraft.observe(viewLifecycleOwner) { draft ->
            adapter.submitList(draft.bricks.toList()) // Отправляем копию
        }

        fab.setOnClickListener {
            BrickEditDialogFragment.newInstance(null).show(childFragmentManager, "EDIT_BRICK_DIALOG")
        }
    }

    private fun setupAdapter() {
        adapter = BrickAdapter(
            onItemClick = { brick ->
                BrickEditDialogFragment.newInstance(brick.id).show(childFragmentManager, "EDIT_BRICK_DIALOG")
            },
            onItemLongClick = { brick ->
                showDeleteConfirmationDialog(brick.id, brick.headerText)
            }
        )
    }

    private fun showDeleteConfirmationDialog(brickId: String, name: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.libs_delete_w))
            .setMessage(getString(R.string.libs_delete_w2) + " '$name'?")
            .setPositiveButton(getString(R.string.libs_delete)) { _, _ ->
                viewModel.deleteBrick(brickId)
            }
            .setNegativeButton(getString(R.string.libs_cancel), null)
            .show()
    }
}