// В пакете: org.catrobat.catroid.content.bricks
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class GetZipFileNamesBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public GetZipFileNamesBrick() {
        // Связываем поле для имени файла с ID из layout
        addAllowedBrickField(BrickField.NAME, R.id.brick_get_zip_files_edit_text);
    }

    // Конструктор для строк
    public GetZipFileNamesBrick(String zipFileName) {
        this(new Formula(zipFileName));
    }

    // Основной конструктор
    public GetZipFileNamesBrick(Formula zipFileName, UserVariable userVariable) {
        this();
        setFormulaWithBrickField(BrickField.NAME, zipFileName);
        this.userVariable = userVariable;
    }

    public GetZipFileNamesBrick(Formula zipFileName) {
        this();
        setFormulaWithBrickField(BrickField.NAME, zipFileName);
        //this.userVariable = userVariable;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_get_zip_file_names;
    }

    @Override
    protected int getSpinnerId() {
        // Указываем ID нашего спиннера
        return R.id.brick_get_zip_files_variable_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createGetZipFileNamesAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME), // Получаем формулу для имени файла
                userVariable // Получаем переменную из спиннера
        ));
    }
}