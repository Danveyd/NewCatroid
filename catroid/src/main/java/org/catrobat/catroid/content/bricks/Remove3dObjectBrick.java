package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Remove3dObjectBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Remove3dObjectBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_remove_3d_object_id);
    }

    public Remove3dObjectBrick(String objectId) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, new Formula(objectId));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_remove_3d_object;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRemove3dObjectAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE)
                ));
    }
}