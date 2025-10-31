package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyImpulseBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ApplyImpulseBrick() {
        addAllowedBrickField(BrickField.IMPULSE_X, R.id.brick_impulse_x_edit_text);
        addAllowedBrickField(BrickField.IMPULSE_Y, R.id.brick_impulse_y_edit_text);
    }

    public ApplyImpulseBrick(int impulseX, int impulseY) {
        this(new Formula(impulseX), new Formula(impulseY));
    }

    public ApplyImpulseBrick(Formula impulseX, Formula impulseY) {
        this();
        setFormulaWithBrickField(BrickField.IMPULSE_X, impulseX);
        setFormulaWithBrickField(BrickField.IMPULSE_Y, impulseY);
    }

    @Override
    public int getViewResource() { return R.layout.brick_apply_impulse; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createApplyImpulseAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.IMPULSE_X),
                        getFormulaWithBrickField(BrickField.IMPULSE_Y)
                ));
    }
}