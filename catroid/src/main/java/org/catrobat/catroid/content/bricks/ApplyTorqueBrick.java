package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyTorqueBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ApplyTorqueBrick() {
        addAllowedBrickField(BrickField.TORQUE, R.id.brick_torque_edit_text);
    }
    public ApplyTorqueBrick(int torque) { this(new Formula(torque)); }
    public ApplyTorqueBrick(Formula torque) {
        this();
        setFormulaWithBrickField(BrickField.TORQUE, torque);
    }

    @Override
    public int getViewResource() { return R.layout.brick_apply_torque; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createApplyTorqueAction(sprite, sequence, getFormulaWithBrickField(BrickField.TORQUE)));
    }
}