package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import java.util.List;

public class StopVMBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public StopVMBrick() {
        // No fields
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_stop_vm;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createStopVMAction(sprite, sequence));
    }
}