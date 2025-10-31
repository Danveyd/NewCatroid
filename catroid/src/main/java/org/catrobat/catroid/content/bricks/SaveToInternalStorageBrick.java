package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SaveToInternalStorageBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SaveToInternalStorageBrick() {
        addAllowedBrickField(BrickField.PROJECT_FILE_NAME, R.id.brick_save_to_internal_project_file);
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_save_to_internal_path);
    }

    public SaveToInternalStorageBrick(String projectFileName, String internalPath) {
        this(new Formula(projectFileName), new Formula(internalPath));
    }

    public SaveToInternalStorageBrick(Formula projectFileName, Formula internalPath) {
        this();
        setFormulaWithBrickField(BrickField.PROJECT_FILE_NAME, projectFileName);
        setFormulaWithBrickField(BrickField.FILE_NAME, internalPath);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_save_to_internal;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSaveToInternalStorageAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.PROJECT_FILE_NAME),
                        getFormulaWithBrickField(BrickField.FILE_NAME)
                ));
    }
}