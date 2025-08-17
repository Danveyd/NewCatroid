package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity


class Create3dObjectAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var modelPath: Formula? = null // Имя файла, например "car.obj"

    override fun update(percent: Float) {
        val threeDManager = StageActivity.stageListener.threeDManager ?: return

        val id = objectId!!.interpretString(scope)
        val modelFileName = modelPath!!.interpretString(scope)

        if (id.isEmpty() || modelFileName.isEmpty()) {
            return  // Не пытаемся создать объект без ID или модели
        }

        // Получаем файл из хранилища проекта
        val modelFile = scope!!.project!!.getFile(modelFileName)

        if (modelFile == null || !modelFile.exists()) {
            // Можно добавить вывод ошибки в лог, если нужно
            return
        }

        // Получаем абсолютный путь и передаем его в менеджер
        Log.d("3D", modelFile.absolutePath)
        threeDManager.createObject(id, modelFile.absolutePath)
    }
}