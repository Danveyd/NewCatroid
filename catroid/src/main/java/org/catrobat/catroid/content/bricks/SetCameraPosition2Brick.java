package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCameraPosition2Brick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetCameraPosition2Brick() {
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_set_camera_pos_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_set_camera_pos_y);
    }

    public SetCameraPosition2Brick(int x, int y) {
        this(new Formula(x), new Formula(y));
    }

    public SetCameraPosition2Brick(Formula x, Formula y) {
        this();
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_camera_position2;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetCameraPositionAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION)
        ));
    }
}