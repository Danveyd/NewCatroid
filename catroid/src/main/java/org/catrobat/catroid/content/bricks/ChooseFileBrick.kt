package org.catrobat.catroid.content.bricks

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import org.catrobat.catroid.R
import org.catrobat.catroid.content.AdapterViewOnItemSelectedListenerImpl
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick.BrickField
import org.catrobat.catroid.content.bricks.Brick.ResourcesSet
import org.catrobat.catroid.formulaeditor.Formula

class ChooseFileBrick : UserVariableBrickWithFormula() {

    private var fileTypeSelection: Int = 0

    override fun getViewResource(): Int = R.layout.brick_choose_file

    override fun getSpinnerId(): Int = R.id.brick_choose_file_var_spinner

    override fun getView(context: Context): View {
        super.getView(context)

        // Spinner для выбора типа файла
        view.findViewById<Spinner>(R.id.brick_choose_file_type_spinner).apply {
            adapter = createFileTypeAdapter(context)
            onItemSelectedListener = AdapterViewOnItemSelectedListenerImpl { position ->
                fileTypeSelection = position
            }
            setSelection(fileTypeSelection)
        }

        // Spinner для выбора переменной (наследуется из UserVariableBrickWithFormula)
        //setSpinner(view, getSpinnerId())

        return view
    }

    private fun createFileTypeAdapter(context: Context): ArrayAdapter<String?> {
        val spinnerValues = arrayOf("Изображение", "Видео", "Аудио", "Документ", "Другое")
        return ArrayAdapter(context, android.R.layout.simple_spinner_item, spinnerValues).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        userVariable?.let { variable ->
            sequence.addAction(
                sprite.actionFactory.createChooseFileAction(sprite, sequence, fileTypeSelection, variable)
            )
        }
    }
}
