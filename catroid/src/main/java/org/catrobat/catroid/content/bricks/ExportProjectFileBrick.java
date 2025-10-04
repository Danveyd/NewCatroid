// В пакете: org.catrobat.catroid.content.bricks
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ExportProjectFileBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ExportProjectFileBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_export_file_source_edit_text);
    }

    public ExportProjectFileBrick(String fileName) {
        this(new Formula(fileName));
    }

    public ExportProjectFileBrick(Formula fileName) {
        this();
        setFormulaWithBrickField(BrickField.NAME, fileName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_export_project_file;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createExportProjectFileAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME)
        ));
    }
}