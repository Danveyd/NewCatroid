// В новом файле, например, org/catrobat/catroid/libraries/LibraryManager.kt
package org.catrobat.catroid.libraries

import android.util.Log
import android.util.Xml
import android.widget.Toast
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.CustomFormula
import org.catrobat.catroid.formulaeditor.CustomFormulaManager
import org.catrobat.catroid.formulaeditor.InternTokenType
import org.catrobat.catroid.ui.MainMenuActivity
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.lunoscript.Interpreter
import org.catrobat.catroid.utils.lunoscript.LunoRuntimeError
import org.catrobat.catroid.utils.lunoscript.Lexer
import org.catrobat.catroid.utils.lunoscript.Parser
import org.koin.ext.scope
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.util.zip.ZipInputStream

// Хранит "контекст" одной загруженной библиотеки
data class LoadedLibrary(val id: String, val interpreter: Interpreter)

object LibraryManager {

    private val loadedLibraries = mutableMapOf<String, LoadedLibrary>()

    fun getLoadedLibrary(id: String): LoadedLibrary? = loadedLibraries[id]

    private var errors: Int = 0

    // Эту функцию ты будешь вызывать из ProjectLibsFragment.onViewCreated или onResume
    fun syncAndLoadLibraries(project: Project) {
        errors = 0
        val libsDir = project.libsDir
        if (!libsDir.exists()) libsDir.mkdirs()

        val currentLibFiles = libsDir.listFiles { _, name -> name.endsWith(".newlib") }?.map { it.name } ?: emptyList()
        val previouslyLoadedIds = loadedLibraries.keys.toSet()

        // 1. Выгружаем библиотеки, которые пользователь удалил
        val libsToUnload = previouslyLoadedIds - currentLibFiles.toSet()
        libsToUnload.forEach { unloadLibrary(it) }

        // 2. Загружаем новые или обновленные библиотеки
        currentLibFiles.forEach { libFileName ->
            // Можно добавить проверку по дате изменения файла, чтобы перезагружать
            if (!loadedLibraries.containsKey(libFileName)) {
                loadLibraryFromFile(project, File(libsDir, libFileName))
            }
        }

        if (errors == 0) {
            if ((libsDir.listFiles()?.size ?: 0) != 0) {
                MainMenuActivity.toast("Синхронизация прошла успешно!", Toast.LENGTH_SHORT)
            }
        } else {
            MainMenuActivity.toast("Синхронизация завершилась со счетчиком ошибок: $errors", Toast.LENGTH_SHORT)
        }
    }

    private fun loadLibraryFromFile(project: Project, libFile: File) {
        val libraryId = libFile.name
        try {
            Log.i("LibraryManager", "Загрузка библиотеки: $libraryId")

            // Распаковываем .newlib из ZIP
            val codeTxt = getZipEntryContent(libFile, "code.txt")
            val formulasXml = getZipEntryContent(libFile, "formulas.xml")
            val bricksXml = getZipEntryContent(libFile, "bricks.xml")

            if (codeTxt == null) {
                throw IllegalStateException("В библиотеке отсутствует code.txt")
            }

            // Создаем и выполняем LunoScript для этой библиотеки
            // ВАЖНО: У каждой библиотеки свой Interpreter!
            val lunoInterpreter = Interpreter(CatroidApplication.getAppContext(), Scope(project, project.defaultScene.spriteList[0], SequenceAction()))
            val tokens = Lexer(codeTxt).scanTokens()
            val ast = Parser(tokens).parse()
            lunoInterpreter.interpret(ast)

            val loadedLib = LoadedLibrary(libraryId, lunoInterpreter)

            // Парсим и регистрируем формулы и блоки
            formulasXml?.let { parseAndRegisterFormulas(it, loadedLib) }
            bricksXml?.let { parseAndRegisterBricks(it, loadedLib) }

            loadedLibraries[libraryId] = loadedLib

        } catch (e: Exception) {
            Log.e("LibraryManager", "Ошибка загрузки библиотеки $libraryId", e)
            MainMenuActivity.toast("Ошибка загрузки '$libraryId'", Toast.LENGTH_SHORT)
            errors += 1
            // Здесь можно показать Toast с ошибкой
        }
    }

    private fun unloadLibrary(libraryId: String) {
        Log.i("LibraryManager", "Выгрузка библиотеки: $libraryId")
        CustomFormulaManager.removeFormulasByOwner(libraryId)
        CustomBrickManager.removeBricksByOwner(libraryId) // Добавим позже
        loadedLibraries.remove(libraryId)
    }

    // Вспомогательная функция для чтения файла из ZIP-архива
    private fun getZipEntryContent(zipFile: File, entryName: String): String? {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    return zis.bufferedReader().readText()
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    private fun parseAndRegisterFormulas(xmlContent: String, library: LoadedLibrary) {
        Log.d("LibraryManager", "Начинаю парсинг formulas.xml для ${library.id}")
        try {
            val formulas = parseFormulasXml(xmlContent, library.id)
            formulas.forEach { formula ->
                CustomFormulaManager.addFormula(formula)
                Log.i("LibraryManager", "Зарегистрирована формула '${formula.displayName}' из библиотеки ${library.id}")
            }
        } catch (e: Exception) {
            Log.e("LibraryManager", "Ошибка парсинга formulas.xml для ${library.id}", e)
            //MainMenuActivity.toast("Ошибка парсинга формул для '${library.id}'", Toast.LENGTH_SHORT)
        }
    }

    // Новая приватная функция для парсинга
    private fun parseFormulasXml(xmlContent: String, libraryId: String): List<CustomFormula> {
        val formulas = mutableListOf<CustomFormula>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType
        var currentFormula: CustomFormula? = null

        // Временные списки для параметров текущей формулы
        var paramDefaults = mutableListOf<String>()
        var paramTypes = mutableListOf<InternTokenType>()

        var currentId: String? = null
        var currentFunction: String? = null
        var currentDisplayName: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "formula" -> {
                            // НАШЛИ <formula>. Сразу же читаем и сохраняем ее атрибуты.
                            currentId = parser.getAttributeValue(null, "id")
                            currentFunction = parser.getAttributeValue(null, "function")
                            currentDisplayName = parser.getAttributeValue(null, "displayName")

                            // Очищаем списки для параметров новой формулы
                            paramDefaults = mutableListOf()
                            paramTypes = mutableListOf()
                        }
                        "param" -> {
                            // Нашли <param>, читаем его атрибуты и добавляем в списки
                            val typeStr = parser.getAttributeValue(null, "type")
                            val default = parser.getAttributeValue(null, "default")

                            paramDefaults.add(default ?: "")
                            paramTypes.add(mapStringToInternTokenType(typeStr ?: "STRING"))
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "formula") {
                        // НАШЛИ </formula>. Теперь у нас есть все данные:
                        // атрибуты (currentId и т.д.) и списки параметров (paramDefaults, paramTypes).
                        // Можно создавать объект CustomFormula.

                        if (currentId != null && currentFunction != null && currentDisplayName != null) {
                            // Создаем объект CustomFormula и добавляем в список
                            Log.d("LibManager", "name: $currentId, display: $currentDisplayName, params: ${paramDefaults.size}, def: $paramDefaults, types: $paramTypes, func: $currentFunction, lib: $libraryId")
                            val formula = CustomFormula(
                                uniqueName = currentId!!,
                                displayName = currentDisplayName!!,
                                paramCount = paramDefaults.size,
                                defaultParamValues = paramDefaults,
                                defaultParamTypes = paramTypes,
                                lunoFunctionName = currentFunction!!,
                                ownerLibraryId = libraryId
                            )
                            formulas.add(formula)
                        } else {
                            Log.d("LibManager", "id: $currentId, func: $currentFunction, name: $currentDisplayName")
                            Log.d("LibManager", "File: $xmlContent")
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return formulas
    }

    // Вспомогательная функция для конвертации строки в тип токена
    private fun mapStringToInternTokenType(typeName: String): InternTokenType {
        return when (typeName.uppercase()) {
            "STRING" -> InternTokenType.STRING
            "NUMBER" -> InternTokenType.NUMBER
            "USER_VARIABLE" -> InternTokenType.USER_VARIABLE
            "USER_LIST" -> InternTokenType.USER_LIST
            else -> InternTokenType.STRING // Тип по умолчанию
        }
    }

    private fun parseAndRegisterBricks(xmlContent: String, library: LoadedLibrary) {
        Log.d("LibraryManager", "Начинаю парсинг bricks.xml для ${library.id}")
        try {
            val bricks = parseBricksXml(xmlContent, library.id)
            bricks.forEach { brickDef ->
                CustomBrickManager.registerBrick(brickDef)
                Log.i("LibraryManager", "Зарегистрирован блок '${brickDef.headerText}' из библиотеки ${library.id}")
            }
        } catch (e: Exception) {
            Log.e("LibraryManager", "Ошибка парсинга bricks.xml для ${library.id}", e)
            //MainMenuActivity.toast("Ошибка парсинга блоков для '${library.id}'", Toast.LENGTH_SHORT)
        }
    }

    // ДОБАВЬ ЭТУ НОВУЮ ФУНКЦИЮ РЯДОМ С parseFormulasXml
    private fun parseBricksXml(xmlContent: String, libraryId: String): List<CustomBrickDefinition> {
        val definitions = mutableListOf<CustomBrickDefinition>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType

        var currentId: String? = null
        var currentFunction: String? = null
        var currentHeader: String? = null
        var currentHeaderText: String = "" // Для сбора текста из тега <header>
        var params = mutableListOf<BrickParameter>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "brick" -> {
                            // Сразу читаем все атрибуты тега <brick>
                            currentId = parser.getAttributeValue(null, "id")
                            currentFunction = parser.getAttributeValue(null, "function")
                            currentHeader = parser.getAttributeValue(null, "header") // <-- ИСПРАВЛЕНИЕ
                            params = mutableListOf()
                        }
                        "param" -> {
                            // Парсим параметры как и раньше
                            val typeStr = parser.getAttributeValue(null, "type")
                            val name = parser.getAttributeValue(null, "name")
                            if (name != null) {
                                val param = BrickParameter(
                                    type = mapStringToParameterType(typeStr ?: "TEXT_FIELD"),
                                    nameInLuno = name
                                )
                                params.add(param)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "brick") {
                        Log.d("LibManager", "id $currentId, ht $currentHeaderText, params $params, name: $currentFunction, owner $libraryId")
                        if (currentId != null && currentFunction != null && currentHeader != null) {
                            val definition = CustomBrickDefinition(
                                id = currentId!!,
                                headerText = currentHeader!!, // Используем значение, прочитанное из атрибута
                                parameters = params,
                                lunoFunctionName = currentFunction!!,
                                ownerLibraryId = libraryId
                            )
                            definitions.add(definition)
                            Log.d("LibManager", "Успешно спарсен блок: ${definition.id}")
                        } else {
                            Log.d("LibManager", "Brick error: id $currentId, func $currentFunction, hc ${currentHeaderText.isNotBlank()}")
                            Log.d("LibManager", xmlContent)
                        }
                        // Сброс
                        currentId = null
                        currentFunction = null
                        currentHeaderText = ""
                    }
                }
            }
            eventType = parser.next()
        }
        return definitions
    }

    fun unloadAllLibraries() {
        Log.i("LibraryManager", "Выгрузка всех загруженных библиотек...")
        // Создаем копию ключей, чтобы избежать ConcurrentModificationException
        val loadedIds = loadedLibraries.keys.toList()
        loadedIds.forEach { unloadLibrary(it) }
        Log.i("LibraryManager", "Все библиотеки выгружены.")
    }

    // ДОБАВЬ ЭТУ ВСПОМОГАТЕЛЬНУЮ ФУНКЦИЮ
    private fun mapStringToParameterType(typeName: String): ParameterType {
        return when (typeName.uppercase()) {
            /*"VARIABLE_DROPDOWN" -> ParameterType.VARIABLE_DROPDOWN
            "LIST_DROPDOWN" -> ParameterType.LIST_DROPDOWN*/
            else -> ParameterType.TEXT_FIELD
        }
    }
}