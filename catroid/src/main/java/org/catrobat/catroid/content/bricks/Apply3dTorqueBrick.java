package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Apply3dTorqueBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Apply3dTorqueBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_apply_3d_torque_edit_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_apply_3d_torque_edit_x);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_apply_3d_torque_edit_y);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_apply_3d_torque_edit_z);
    }

    public Apply3dTorqueBrick(String objectId, double x, double y, double z) {
        this(new Formula(objectId), new Formula(x), new Formula(y), new Formula(z));
    }

    public Apply3dTorqueBrick(Formula objectId, Formula x, Formula y, Formula z) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, objectId);
        setFormulaWithBrickField(BrickField.VALUE_2, x);
        setFormulaWithBrickField(BrickField.VALUE_3, y);
        setFormulaWithBrickField(BrickField.VALUE_4, z);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_apply_3d_torque;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createApply3dTorqueAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4)));
    }
}
