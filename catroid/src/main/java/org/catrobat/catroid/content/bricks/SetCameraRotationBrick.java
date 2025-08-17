package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCameraRotationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetCameraRotationBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_camera_rotation_yaw);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_camera_rotation_pitch);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_camera_rotation_roll);
    }

    public SetCameraRotationBrick(double y, double p, double r) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(y));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(p));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(r));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_camera_rotation;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetCameraRotationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3)
                ));
    }
}