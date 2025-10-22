package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class UnlockMouseBrick extends BrickBaseType {
    @Override public int getViewResource() { return R.layout.brick_unlock_mouse; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createUnlockMouseAction());
    }
}