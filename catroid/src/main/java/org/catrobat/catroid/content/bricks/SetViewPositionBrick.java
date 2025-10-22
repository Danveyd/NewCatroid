package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetViewPositionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetViewPositionBrick() {
        addAllowedBrickField(BrickField.VIEW_ID, R.id.brick_set_view_position_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_set_view_position_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_set_view_position_y);
    }

    public SetViewPositionBrick(String viewId, int x, int y) {
        this(new Formula(viewId), new Formula(x), new Formula(y));
    }

    public SetViewPositionBrick(Formula viewId, Formula x, Formula y) {
        this();
        setFormulaWithBrickField(BrickField.VIEW_ID, viewId);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_view_position;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetViewPositionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VIEW_ID),
                        getFormulaWithBrickField(BrickField.X_POSITION),
                        getFormulaWithBrickField(BrickField.Y_POSITION)
                ));
    }
}