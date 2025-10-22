package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCameraRotation2Brick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetCameraRotation2Brick() {
        addAllowedBrickField(BrickField.DEGREES, R.id.brick_set_camera_rotation_value);
    }

    public SetCameraRotation2Brick(double degrees) {
        this(new Formula(degrees));
    }

    public SetCameraRotation2Brick(Formula degrees) {
        this();
        setFormulaWithBrickField(BrickField.DEGREES, degrees);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_camera_rotation2;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetCameraRotationAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.DEGREES)));
    }
}