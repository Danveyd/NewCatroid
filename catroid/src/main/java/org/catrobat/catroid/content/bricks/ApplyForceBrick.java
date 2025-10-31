package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyForceBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ApplyForceBrick() {
        addAllowedBrickField(BrickField.FORCE_X, R.id.brick_force_x_edit_text);
        addAllowedBrickField(BrickField.FORCE_Y, R.id.brick_force_y_edit_text);
    }

    public ApplyForceBrick(int forceX, int forceY) {
        this(new Formula(forceX), new Formula(forceY));
    }

    public ApplyForceBrick(Formula forceX, Formula forceY) {
        this();
        setFormulaWithBrickField(BrickField.FORCE_X, forceX);
        setFormulaWithBrickField(BrickField.FORCE_Y, forceY);
    }

    @Override
    public int getViewResource() { return R.layout.brick_apply_force; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createApplyForceAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FORCE_X),
                        getFormulaWithBrickField(BrickField.FORCE_Y)
                ));
    }
}