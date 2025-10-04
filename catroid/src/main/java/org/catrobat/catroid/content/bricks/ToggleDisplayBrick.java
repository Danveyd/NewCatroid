package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ToggleDisplayBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ToggleDisplayBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_visible_edit);
    }

    public ToggleDisplayBrick(int visible) {
        this(new Formula(visible));
    }

    public ToggleDisplayBrick(Formula visible) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, visible);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_toggle_display;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createToggleDisplayAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE)
        ));
    }
}