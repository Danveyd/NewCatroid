package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class MouseEventBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public MouseEventBrick() {
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_mouse_x_edit);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_mouse_y_edit);
        addAllowedBrickField(BrickField.VALUE, R.id.brick_mouse_state_edit);
    }

    public MouseEventBrick(String x, String y, int state) {
        this(new Formula(x), new Formula(y), new Formula(state));
    }

    public MouseEventBrick(Formula x, Formula y, Formula state) {
        this();
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
        setFormulaWithBrickField(BrickField.VALUE, state);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_mouse_event;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createMouseEventAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.VALUE)
        ));
    }
}