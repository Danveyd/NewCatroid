package org.catrobat.catroid.libraryeditor.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.R

class FormulaAdapter(
    private val onItemClick: (EditableFormula) -> Unit,
    private val onItemLongClick: (EditableFormula) -> Unit
) : ListAdapter<EditableFormula, FormulaAdapter.ViewHolder>(FormulaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_editable_formula, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val formula = getItem(position)
        holder.bind(formula)
        holder.itemView.setOnClickListener { onItemClick(formula) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(formula)
            true
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val displayName: TextView = itemView.findViewById(R.id.formula_display_name)
        private val functionName: TextView = itemView.findViewById(R.id.formula_function_name)
        private val id: TextView = itemView.findViewById(R.id.formula_id)

        fun bind(formula: EditableFormula) {
            displayName.text = formula.displayName
            functionName.text = "${formula.functionName}(...)"
            id.text = "ID: ${formula.id}"
        }
    }
}

class FormulaDiffCallback : DiffUtil.ItemCallback<EditableFormula>() {
    override fun areItemsTheSame(oldItem: EditableFormula, newItem: EditableFormula): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: EditableFormula, newItem: EditableFormula): Boolean {
        return oldItem == newItem
    }
}