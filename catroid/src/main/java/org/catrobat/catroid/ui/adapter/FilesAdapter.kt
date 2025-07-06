// FilesAdapter.kt
package org.catrobat.catroid.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Project
import java.io.File

class FilesAdapter(
    private val project: Project?,
    private val files: List<String>,
    private val onDelete: (String) -> Unit,
    private val onCopy: (String) -> Unit,
    private val onOpen: (String) -> Unit // <-- ДОБАВЬТЕ ЭТО
) : RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val openButton: Button = view.findViewById(R.id.openButton) // <-- ДОБАВЬТЕ ЭТО
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileName = files[position]
        holder.fileName.text = fileName
        holder.fileSize.text = "0 Б"
        Log.d("ProjectFile", "Binding item at position $position: ${fileName}")

        project?.let {
            val file: File = it.getFile(fileName) ?: it.getLib(fileName)

            holder.fileSize.text = formatFileSize(file.length())
        }

        holder.deleteButton.setOnClickListener {
            onDelete(fileName)
        }
        holder.fileName.setOnClickListener {
            onCopy(fileName)
        }
        holder.openButton.setOnClickListener { // <-- ДОБАВЬТЕ ЭТОТ БЛОК
            onOpen(fileName)
        }
    }

    fun formatFileSize(size: Long): String {
        val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
        var sizeInUnits = size.toDouble()
        var index = 0

        while (sizeInUnits >= 1024 && index < units.size - 1) {
            sizeInUnits /= 1024
            index++
        }

        return String.format("%.1f %s", sizeInUnits, units[index])
    }


    override fun getItemCount() = files.size
}
