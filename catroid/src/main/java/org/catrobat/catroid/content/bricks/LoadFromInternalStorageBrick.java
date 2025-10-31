package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LoadFromInternalStorageBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public LoadFromInternalStorageBrick() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_load_from_internal_path);
    }

    public LoadFromInternalStorageBrick(String internalPath) {
        this(new Formula(internalPath));
    }

    public LoadFromInternalStorageBrick(Formula internalPath) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, internalPath);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_load_from_internal;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createLoadFromInternalStorageAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FILE_NAME)
                ));
    }
}