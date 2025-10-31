package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyAngularImpulseBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ApplyAngularImpulseBrick() {
        addAllowedBrickField(BrickField.ANGULAR_IMPULSE, R.id.brick_angular_impulse_edit_text);
    }
    public ApplyAngularImpulseBrick(int impulse) { this(new Formula(impulse)); }
    public ApplyAngularImpulseBrick(Formula impulse) {
        this();
        setFormulaWithBrickField(BrickField.ANGULAR_IMPULSE, impulse);
    }

    @Override
    public int getViewResource() { return R.layout.brick_apply_angular_impulse; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createApplyAngularImpulseAction(sprite, sequence, getFormulaWithBrickField(BrickField.ANGULAR_IMPULSE)));
    }
}