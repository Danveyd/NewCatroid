package org.catrobat.catroid.libraryeditor.data

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.catrobat.catroid.R

class LibraryEditorActivity : AppCompatActivity() {

    private val viewModel: LibraryEditorViewModel by viewModels()
    private lateinit var toolbar: Toolbar

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.exportToUri(this, uri) }
        }
    }

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.importFromUri(this, uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_editor)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        viewPager.adapter = LibraryEditorPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.libs_code); 1 -> getString(R.string.libs_formulas); 2 -> getString(R.string.libs_bricks); else -> null
            }
        }.attach()

        viewModel.loadLastSession(this)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.libraryDraft.observe(this) {
            toolbar.title = it.name
        }
        viewModel.toastMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.library_editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_library -> { triggerExport(); true }
            R.id.action_import_library -> { triggerImport(); true }
            R.id.action_clear_library -> { showClearConfirmationDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun triggerExport() {
        val draftName = viewModel.libraryDraft.value?.name ?: "library"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "$draftName.newlib")
        }
        saveFileLauncher.launch(intent)
    }

    private fun triggerImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Разрешаем выбирать любой файл
        }
        importFileLauncher.launch(intent)
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.libs_clearp))
            .setMessage(getString(R.string.libs_clearpd))
            .setPositiveButton(getString(R.string.libs_clear)) { _, _ ->
                viewModel.clearCurrentLibrary(this)
            }
            .setNegativeButton(getString(R.string.libs_cancel), null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveCurrentSession(this)
    }
}