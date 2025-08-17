package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCameraPositionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetCameraPositionBrick() {
        // Убедитесь, что BrickField поддерживает эти поля, или используйте стандартные
        addAllowedBrickField(BrickField.VALUE, R.id.brick_set_camera_position_x);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_camera_position_y);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_camera_position_z);
    }

    public SetCameraPositionBrick(double x, double y, double z) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, new Formula(x));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(y));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(z));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_camera_position;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetCameraPositionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3)
                ));
    }
}