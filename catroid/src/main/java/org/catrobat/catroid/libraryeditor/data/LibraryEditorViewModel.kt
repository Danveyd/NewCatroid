package org.catrobat.catroid.libraryeditor.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.utils.lunoscript.Lexer
import org.catrobat.catroid.utils.lunoscript.LunoSyntaxError
import org.catrobat.catroid.utils.lunoscript.Token
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LibraryEditorViewModel : ViewModel() {

    private val _libraryDraft = MutableLiveData<LibraryDraft>()
    val libraryDraft: LiveData<LibraryDraft> = _libraryDraft

    private val _lexedTokens = MutableLiveData<List<Token>>()
    val lexedTokens: LiveData<List<Token>> = _lexedTokens

    private val _syntaxError = MutableLiveData<LunoSyntaxError?>()
    val syntaxError: LiveData<LunoSyntaxError?> = _syntaxError

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>> = _toastMessage

    // --- Session Management ---
    fun loadLastSession(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = LibraryStorageManager.loadLastSession(context)
            _libraryDraft.postValue(draft)
            analyzeCode(draft.code)
        }
    }

    fun saveCurrentSession(context: Context) {
        libraryDraft.value?.let { draft ->
            viewModelScope.launch(Dispatchers.IO) {
                LibraryStorageManager.saveSession(context, draft)
            }
        }
    }

    fun clearCurrentLibrary(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            LibraryStorageManager.clearSession(context)
            // Загружаем новый пустой проект
            loadLastSession(context)
            _toastMessage.postValue(Event(CatroidApplication.getAppContext().getString(R.string.libs_cleaned)))
        }
    }

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val importedDraft = NewlibImporter.import(context, uri)
            if (importedDraft != null) {
                _libraryDraft.postValue(importedDraft!!) // Post new draft to UI
                saveCurrentSession(context) // Save it as the new session
                analyzeCode(importedDraft.code) // Analyze new code
                _toastMessage.postValue(Event(CatroidApplication.getAppContext().getString(R.string.libs_imported)))
            } else {
                _toastMessage.postValue(Event(CatroidApplication.getAppContext().getString(R.string.libs_importede)))
            }
        }
    }

    // --- Code Editing ---
    fun updateCode(newCode: String) {
        _libraryDraft.value?.let {
            if (it.code != newCode) {
                it.code = newCode
                _libraryDraft.postValue(it)
                analyzeCode(newCode)
            }
        }
    }

    private fun analyzeCode(code: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val lexer = Lexer(code)
                _lexedTokens.postValue(lexer.scanTokens())
                _syntaxError.postValue(null)
            } catch (e: LunoSyntaxError) {
                _syntaxError.postValue(e)
            }
        }
    }

    fun isFunctionDefined(functionName: String): Boolean {
        val code = _libraryDraft.value?.code ?: return false
        return code.contains("fun $functionName")
    }

    // --- Formula & Brick Management ---
    private fun <T> updateList(list: MutableList<T>, item: T, findPredicate: (T) -> Boolean) {
        val index = list.indexOfFirst(findPredicate)
        if (index != -1) list[index] = item else list.add(item)
        _libraryDraft.postValue(_libraryDraft.value)
    }

    fun saveFormula(formula: EditableFormula) {
        _libraryDraft.value?.formulas?.let { list -> updateList(list, formula) { it.id == formula.id } }
    }

    fun deleteFormula(formulaId: String) {
        _libraryDraft.value?.formulas?.removeAll { it.id == formulaId }
        _libraryDraft.postValue(_libraryDraft.value)
    }

    fun saveBrick(brick: EditableBrick) {
        _libraryDraft.value?.bricks?.let { list -> updateList(list, brick) { it.id == brick.id } }
    }

    fun deleteBrick(brickId: String) {
        _libraryDraft.value?.bricks?.removeAll { it.id == brickId }
        _libraryDraft.postValue(_libraryDraft.value)
    }

    // --- Export ---
    fun exportToUri(context: Context, destinationUri: Uri) {
        val draft = _libraryDraft.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                        zos.putNextEntry(ZipEntry("code.txt"))
                        zos.write(draft.code.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                        zos.putNextEntry(ZipEntry("formulas.xml"))
                        zos.write(XmlGenerator.generateFormulasXml(draft.formulas).toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                        zos.putNextEntry(ZipEntry("bricks.xml"))
                        zos.write(XmlGenerator.generateBricksXml(draft.bricks).toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
                _toastMessage.postValue(Event(CatroidApplication.getAppContext().getString(R.string.libs_exported)))
            } catch (e: Exception) {
                Log.e("LibraryExport", "Failed to export library", e)
                _toastMessage.postValue(Event(CatroidApplication.getAppContext().getString(R.string.libs_exportede)))
            }
        }
    }
}