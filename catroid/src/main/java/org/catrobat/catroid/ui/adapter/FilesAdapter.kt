// FilesAdapter.kt
package org.catrobat.catroid.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Project
import java.io.File

@LunoClass
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
        holder.fileSize.text = "0 Б" // Значение по умолчанию
        Log.d("ProjectFile", "Binding item at position $position: ${fileName}")

        project?.let {
            // --- НАЧАЛО ИЗМЕНЕНИЙ ---

            // 1. Определяем, в какой папке мы находимся, по контексту (например, по фрагменту)
            // Но проще просто проверить обе папки и обработать null.

            // 2. Делаем переменную nullable: File?
            var file: File? = it.getFile(fileName) ?: it.getLib(fileName)

            // 3. Добавляем проверку на null
            if (file != null && file.exists()) {
                holder.fileSize.text = formatFileSize(file.length())
            } else {
                file = it.getLib(fileName)
                if (file != null && file.exists()) {
                    holder.fileSize.text = formatFileSize(file.length())
                } else {
                    // Если файл почему-то не найден (хотя должен быть),
                    // показываем ошибку вместо падения.
                    holder.fileSize.text = "Файл не найден"
                    Log.e("FilesAdapter", "File not found: $fileName")
                }
            }

            // --- КОНЕЦ ИЗМЕНЕНИЙ ---
        }

        holder.deleteButton.setOnClickListener {
            onDelete(fileName)
        }
        holder.fileName.setOnClickListener {
            onCopy(fileName)
        }
        holder.openButton.setOnClickListener {
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
