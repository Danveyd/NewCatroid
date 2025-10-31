package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetDampingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetDampingBrick() {
        addAllowedBrickField(BrickField.LINEAR_DAMPING, R.id.brick_linear_damping_edit_text);
        addAllowedBrickField(BrickField.ANGULAR_DAMPING, R.id.brick_angular_damping_edit_text);
    }

    public SetDampingBrick(float linear, float angular) {
        this(new Formula(linear), new Formula(angular));
    }

    public SetDampingBrick(Formula linear, Formula angular) {
        this();
        setFormulaWithBrickField(BrickField.LINEAR_DAMPING, linear);
        setFormulaWithBrickField(BrickField.ANGULAR_DAMPING, angular);
    }

    @Override
    public int getViewResource() { return R.layout.brick_set_damping; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetDampingAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.LINEAR_DAMPING),
                        getFormulaWithBrickField(BrickField.ANGULAR_DAMPING)
                ));
    }
}