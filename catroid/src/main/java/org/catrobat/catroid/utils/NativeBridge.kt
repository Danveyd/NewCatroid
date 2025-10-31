package org.catrobat.catroid.utils

import android.view.Surface
import org.catrobat.catroid.ProjectManager

object NativeBridge {
    var isWorking = true

    init {
        if (!NativeLibraryManager.isLoaded(NativeLibraryManager.Feature.CORE)) {
            isWorking = false
        }
    }

    external fun attachSoToView(viewName: String, pathToSo: String)
    external fun onSurfaceCreated(viewName: String, surface: Surface)
    external fun onSurfaceChanged(viewName: String, width: Int, height: Int)
    external fun onSurfaceDestroyed(viewName: String)
    external fun cleanupInstance(viewName: String)
    external fun cleanupAllInstances()
    external fun setCrashLogPath(path: String)
    external fun onTouchEvent(viewName: String, action: Int, x: Float, y: Float, pointerId: Int)
    @JvmStatic
    fun getProjectFilePath(fileName: String): String? {
        val project = ProjectManager.getInstance().currentProject ?: return null
        val projectFile = project.getFile(fileName)
        return projectFile?.absolutePath
    }
}