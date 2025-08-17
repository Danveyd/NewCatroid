package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetRestitutionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetRestitutionBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_restitution_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_restitution_value);
    }

    public SetRestitutionBrick(String objectId, double restitution) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(restitution));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_restitution;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetRestitutionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}