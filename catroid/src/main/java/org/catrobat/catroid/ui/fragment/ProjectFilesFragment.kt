/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.fragment

import android.Manifest.permission
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.common.ProjectData
import org.catrobat.catroid.common.ScreenModes
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.databinding.FragmentProjectOptionsBinding
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.asynctask.ProjectExportTask
import org.catrobat.catroid.io.asynctask.loadProject
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.io.asynctask.renameProject
import org.catrobat.catroid.io.asynctask.saveProjectSerial
import org.catrobat.catroid.merge.NewProjectNameTextWatcher
import org.catrobat.catroid.ui.BottomBar.hideBottomBar
import org.catrobat.catroid.ui.PROJECT_DIR
import org.catrobat.catroid.ui.ProjectUploadActivity
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.utils.notifications.StatusBarNotificationManager
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException
import android.content.Context
import android.view.ContextThemeWrapper
import org.catrobat.catroid.BuildConfig
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContextWrapper
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.MenuItem
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.cast.CastManager
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.FlavoredConstants.CATROBAT_HELP_URL
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.common.Survey
import org.catrobat.catroid.databinding.ActivityMainMenuBinding
import org.catrobat.catroid.databinding.ActivityMainMenuSplashscreenBinding
import org.catrobat.catroid.databinding.DeclinedTermsOfUseAndServiceAlertViewBinding
import org.catrobat.catroid.databinding.PrivacyPolicyViewBinding
import org.catrobat.catroid.databinding.ProgressBarBinding
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.io.asynctask.ProjectLoader
import org.catrobat.catroid.io.asynctask.ProjectLoader.ProjectLoadListener
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.dialogs.TermsOfUseDialogFragment
import org.catrobat.catroid.ui.recyclerview.dialog.AboutDialogFragment
import org.catrobat.catroid.ui.recyclerview.fragment.MainMenuFragment
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import org.catrobat.catroid.utils.FileMetaDataExtractor
import org.catrobat.catroid.utils.ScreenValueHandler
import org.catrobat.catroid.utils.setVisibleOrGone
import org.koin.android.ext.android.inject
import java.io.OutputStream
import com.android.apksig.ApkSigner
import com.android.apksig.internal.x509.Certificate
import org.catrobat.catroid.databinding.FragmentProjectFilesBinding
import org.catrobat.catroid.ui.adapter.FilesAdapter
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.ArrayList
import kotlin.random.Random

class ProjectFilesFragment : Fragment() {

    private val projectManager: ProjectManager by inject()
    private var _binding: FragmentProjectFilesBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private var sceneName: String? = null
    private var projectInZip: File? = null
    private var buildFilename: String? = null
    private var zipTempDir: File? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var filesAdapter: FilesAdapter
    private var filesList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectFilesBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerViewFiles
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.project_files)

        project = projectManager.currentProject
        sceneName = projectManager.currentlyEditedScene.name

        setupAdd()
        setupRecyclerView()

        project?.let { proj ->
            updateFilesList(File(proj.directory, "files").absoluteFile)
        }

        hideBottomBar(requireActivity())
    }

    private fun setupAdd() {
        binding.projectFilesAdd.setOnClickListener {
            //handleText()
            handleAdd()
        }
    }

    private fun setupRecyclerView() {
        filesAdapter = FilesAdapter(project, filesList,
            { fileName -> deleteFile(fileName) }, // Обработчик для удаления файла
            { fileName -> copyFile(fileName) } // Обработчик для копирования файла
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = filesAdapter
    }

    private fun copyToClipboard(text: String) {
        val clipboard = CatroidApplication.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Response", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun copyFile(fileName: String) {
        copyToClipboard(fileName)
        Toast.makeText(requireContext(), "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFile(fileName: String) {
        project?.let {
            val dir = File(it.directory, "files")
            val file = File(dir.absolutePath, fileName)
            if (file.exists() && file.delete()) {
                // Удаляем файл и обновляем список
                updateFilesList(dir) // Предположим, что updateFilesList обновляет список файлов
                Toast.makeText(requireContext(), "Файл удален", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Ошибка при удалении файла", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun updateFilesList(directory: File) {
        val newFiles = directory.listFiles()?.map { it.name } ?: emptyList()
        val oldFiles = filesList.toList() // Создаем копию текущего списка

        Log.d("ProjectFile", "Number of files: ${directory.listFiles()?.size}")
        // Добавляем новые файлы
        newFiles.forEach { fileName ->
            if (!oldFiles.contains(fileName)) {
                filesList.add(fileName)
                filesAdapter.notifyItemInserted(filesList.size - 1)
            }
        }

        // Удаляем старые файлы
        oldFiles.forEach { fileName ->
            if (!newFiles.contains(fileName)) {
                val position = filesList.indexOf(fileName)
                if (position != -1) {
                    filesList.removeAt(position)
                    filesAdapter.notifyItemRemoved(position)
                }
            }
        }

        Log.d("ProjectFile", "Files: $filesList")

        // Дополнительно можно вызвать notifyDataSetChanged, если лучшее решение не подходит
        // filesAdapter.notifyDataSetChanged()
    }


    override fun onPause() {
        saveProject()
        super.onPause()
    }

    private fun saveProject() {
        project ?: return
        saveProjectSerial(project, requireContext())
    }

    private fun handleAdd() {
        // Открываем меню выбора файла
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // Позволяет выбрать файл с любым расширением
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        // Проверяем, есть ли такая возможность
        val chooser = Intent.createChooser(intent, "Выберите файл")
        startActivityForResult(chooser, ADD_FILE_REQUEST) // REQUEST_CODE - это константа, которую нужно определить
    }

    override fun onResume() {
        super.onResume()

        projectManager.currentProject = project
        hideBottomBar(requireActivity())
    }

    private fun handleText() {
        showToast(getRandomError())
    }

    private fun onSaveProjectComplete() {
        val currentProject = projectManager.currentProject

        if (Utils.isDefaultProject(currentProject, activity)) {
            binding.root.apply {
                Snackbar.make(binding.root, R.string.error_upload_default_project, Snackbar.LENGTH_LONG).show()
            }
            return
        }

        val intent = Intent(requireContext(), ProjectUploadActivity::class.java)
        intent.putExtra(PROJECT_DIR, currentProject.directory)

        startActivity(intent)
    }

    fun showToast(toast: String) {
        if (StageActivity.messageHandler != null) {
            val params = ArrayList<Any>(listOf(toast))
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
        } else {
            // Обработка ситуации, когда messageHandler равно null
            Log.e("ShowToast", "messageHandler is null!")
        }
    }

    fun getRandomMessage(): String {
        val messages = listOf(
            "Готово!",
            "Сделано!",
            "Успех!",
            "Завершено!",
            "Готово к использованию!",
            "Задача выполнена!",
            "Отличная работа!",
            "Все готово!",
            "Яйцо или курица..?",
            "Готово! Проверяй!",
            "Поехали!",
            "Вроде сделано..",
            "Проверяй, начальник э!",
            "Готово. Удачи с проектом!",
            "Работа завершена, как кофе на утро!",
            "Готово! Как будто я маг, а не программист!",
            "Все сделано! Как раз вовремя перед обедом.",
            "Все завершено! Можно идти за пирожками!",
            "Задача выполнена! Теперь можно отдохнуть и посмотреть котиков.",
            "Готово! Даже не успел заметить, как это произошло.",
            "Сделано! Осталось только отпраздновать с танцами.",
            "Готово! Минутка успокоения перед новыми приключениями.",
            "Отличная работа! Ты как супергерой, только без плаща.",
            "Готово! Наконец-то смогу отвлечься на онлайн-шопинг.",
            "Как сказать: «Сделай это» и получить: «Сделано!»? Вот так!",
            "Все готово! Теперь можем заниматься более важными делами.",
            "Задача выполнена! Как хорошая книга – не отпускает до последней страницы.",
            "Готово! Можно отдыхать, как будто мы все это сделали за пятюню.",
            "Сделано! Готовы к новым подвигам?"
        )

        // Генерируем случайный индекс
        val randomIndex = Random.nextInt(messages.size)
        // Возвращаем случайное сообщение
        return messages[randomIndex]
    }

    fun getRandomError(): String {
        val errorMessages = listOf(
            "Произошла ошибка! Кажется, я не тот алгоритм заказывал.",
            "Упс! Что-то пошло не так. Как будто кошка пробежала по клавиатуре.",
            "Произошла ошибка! Может, система решила немного отдохнуть?",
            "Ой! Похоже, произошла ошибка. Возможно, это программистская шутка?",
            "Произошла ошибка! Да кто придумал обновлять программу перед дедлайном?",
            "Упс! Ошибка. Наверное, мой код тоже решил поспать.",
            "Произошла ошибка! Как бы я ни старался, выводы не совпали.",
            "Ой-ой! Ошибка! Это как раз то, что нам нужно было избежать.",
            "Произошла ошибка! По всей видимости, сервер тоже устал.",
            "Упс! Ошибка. Это как забыть о важной встрече.",
            "Произошла ошибка! Может, стоит заказывать пиццу вместо кода?",
            "Ой! Ошибка. Обычно говорят, что все дороги ведут к Риму, но не сегодня.",
            "Произошла ошибка! Это не то, что я хотел об этом напомнить.",
            "Упс! Ошибка! Возможно, машина решила, что у нее выходной.",
            "Произошла ошибка! Я попытался угостить код печеньками и вот что вышло!",
            "Ой-ой! Ошибка. Наверное, в коде слишком много любопытных переменных.",
            "Произошла ошибка! Извините, не я такой - жизнь такая!",
            "Упс! Произошла ошибка. Код сам по себе иногда делает капризы.",
            "Ой! Произошла ошибка! Как будто интернет пошел на пикник без меня.",
            "Произошла ошибка! И тут, конечно, глюк всегда оказывается виноват.",
            "Упс! Ошибка. Вы знаете, прощать - это тоже искусство."
        )

        val randomIndex = Random.nextInt(errorMessages.size)
        // Возвращаем случайное сообщение
        return errorMessages[randomIndex]
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        /*if (requestCode == REQUEST_EXPORT_PROJECT && resultCode == Activity.RESULT_OK) {
            val projectDestination = data.data ?: return
            startAsyncProjectExport(projectDestination)
        }*/
        if (requestCode == ADD_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Получаем директорию проекта
                val directory: File? = project?.directory
                val filesDir = File(directory, "files")


                // Создаем папку, если она не существует
                if (!filesDir.exists()) {
                    filesDir.mkdirs()
                }

                // Загрузка файла в папку
                copyFileToDir(uri, filesDir)
            }
        }
    }

    private fun copyFileToDir(uri: Uri, dir: File) {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val outputFileName = getFileName(uri) // Получаем имя файла
        val outputFile = File(dir, outputFileName)

        // Копируем содержимое
        inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input?.copyTo(output)
            }
        }

        updateFilesList(dir)
    }

    // Функция для получения имени файла из Uri
    private fun getFileName(uri: Uri): String {
        var fileName = ""
        if (uri.scheme == "content") {
            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        } else if (uri.scheme == "file") {
            fileName = File(uri.path).name
        }
        return fileName.ifEmpty { "неизвестный_файл" } // Возврат значения по умолчанию
    }


    companion object {
        val TAG: String = ProjectOptionsFragment::class.java.simpleName

        private const val ADD_FILE_REQUEST = 15
        //private const val PERMISSIONS_REQUEST_EXPORT_TO_EXTERNAL_STORAGE = 802
    }
}
