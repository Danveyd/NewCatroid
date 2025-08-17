package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class ClearPythonEnvironmentBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ClearPythonEnvironmentBrick() {
        // Этот блок не имеет полей для формул
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_python_clear_environment;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createClearPythonEnvironmentAction(sprite, sequence));
    }
}