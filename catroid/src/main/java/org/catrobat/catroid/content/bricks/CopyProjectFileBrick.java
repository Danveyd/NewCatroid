// Находится в пакете: org.catrobat.catroid.content.bricks
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.List;

public class CopyProjectFileBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CopyProjectFileBrick() {
        // Связываем поля из layout с типами формул
        addAllowedBrickField(BrickField.NAME, R.id.brick_copy_file_source_edit_text);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_copy_file_destination_edit_text);
    }

    // Конструктор для создания блока со строками
    public CopyProjectFileBrick(String sourceFileName, String newFileName) {
        this(new Formula(sourceFileName), new Formula(newFileName));
    }

    // Основной конструктор с формулами
    public CopyProjectFileBrick(Formula sourceFileName, Formula newFileName) {
        this();
        setFormulaWithBrickField(BrickField.NAME, sourceFileName);
        setFormulaWithBrickField(BrickField.TEXT, newFileName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_copy_project_file;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCopyProjectFileAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.TEXT)
                )
        );
    }
}