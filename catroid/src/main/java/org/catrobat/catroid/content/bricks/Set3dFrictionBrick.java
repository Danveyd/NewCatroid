package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Set3dFrictionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Set3dFrictionBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_friction_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_friction_value);
    }

    public Set3dFrictionBrick(String objectId, double friction) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(friction));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_friction;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSet3dFrictionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}