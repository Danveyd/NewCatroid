package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCameraRangeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetCameraRangeBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_camera_range_near_value);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_camera_range_far_value);
    }

    public SetCameraRangeBrick(double near, double far) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(near));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(far));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_camera_range;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetCameraRangeAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}