package org.catrobat.catroid.utils

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.danvexteam.lunoscript_annotations.LunoClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.R
import java.io.File

@LunoClass
class SimpleTextEditorActivity : AppCompatActivity() {

    private lateinit var currentFile: File
    private lateinit var editText: EditText
    private val MAX_FILE_SIZE = 1024 * 1024

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_text_editor)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editText = findViewById(R.id.editorEditText)

        editText.setHorizontallyScrolling(true)

        val filePath = intent.getStringExtra("FILE_PATH")
        if (filePath == null) {
            Toast.makeText(this, getString(R.string.ste_path_not_passed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentFile = File(filePath)
        supportActionBar?.title = currentFile.name

        loadFileContent()
    }

    private fun loadFileContent() {
        if (!currentFile.exists()) return


        if (currentFile.length() > MAX_FILE_SIZE) {
            editText.setText(getString(R.string.ste_file_too_large, currentFile.length() / 1024))
            editText.isEnabled = false
            return
        }


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val text = currentFile.readText()


                withContext(Dispatchers.Main) {
                    editText.setText(text)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SimpleTextEditorActivity, getString(R.string.ste_read_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, getString(R.string.save))
            ?.setIcon(R.drawable.baseline_save_alt_24_w)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            1 -> {
                saveFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveFile() {
        if (!this::currentFile.isInitialized) return


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = editText.text.toString()
                currentFile.writeText(content)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SimpleTextEditorActivity, getString(R.string.ste_saved), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SimpleTextEditorActivity, getString(R.string.common_error_with_message, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}