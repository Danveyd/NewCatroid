package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCameraZoomBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetCameraZoomBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_set_camera_zoom_value);
    }

    public SetCameraZoomBrick(double zoom) {
        this(new Formula(zoom));
    }

    public SetCameraZoomBrick(Formula zoom) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, zoom);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_camera_zoom;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetCameraZoomAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE)));
    }
}