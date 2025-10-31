package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class SetFreeCameraBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetFreeCameraBrick() {

    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_free_camera;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetFreeCameraAction(sprite, sequence));
    }
}