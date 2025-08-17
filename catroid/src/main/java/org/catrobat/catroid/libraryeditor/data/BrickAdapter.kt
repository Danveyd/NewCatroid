package org.catrobat.catroid.libraryeditor.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.R

class BrickAdapter(
    private val onItemClick: (EditableBrick) -> Unit,
    private val onItemLongClick: (EditableBrick) -> Unit
) : ListAdapter<EditableBrick, BrickAdapter.ViewHolder>(BrickDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_editable_brick, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val brick = getItem(position)
        holder.bind(brick)
        holder.itemView.setOnClickListener { onItemClick(brick) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(brick)
            true
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.brick_header_text)
        private val functionName: TextView = itemView.findViewById(R.id.brick_function_name)
        private val id: TextView = itemView.findViewById(R.id.brick_id)

        fun bind(brick: EditableBrick) {
            headerText.text = brick.headerText
            functionName.text = "${brick.functionName}(...)"
            id.text = "ID: ${brick.id}"
        }
    }
}

class BrickDiffCallback : DiffUtil.ItemCallback<EditableBrick>() {
    override fun areItemsTheSame(oldItem: EditableBrick, newItem: EditableBrick): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: EditableBrick, newItem: EditableBrick): Boolean {
        return oldItem == newItem
    }
}