package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PerformRayCastBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PerformRayCastBrick() {
        addAllowedBrickField(BrickField.RAY_ID, R.id.brick_ray_id_edit_text);
        addAllowedBrickField(BrickField.X_START, R.id.brick_start_x_edit_text);
        addAllowedBrickField(BrickField.Y_START, R.id.brick_start_y_edit_text);
        addAllowedBrickField(BrickField.X_END, R.id.brick_end_x_edit_text);
        addAllowedBrickField(BrickField.Y_END, R.id.brick_end_y_edit_text);
    }

    public PerformRayCastBrick(String rayId, int sX, int sY, int eX, int eY) {
        this(new Formula(rayId), new Formula(sX), new Formula(sY), new Formula(eX), new Formula(eY));
    }

    public PerformRayCastBrick(Formula rId, Formula sX, Formula sY, Formula eX, Formula eY) {
        this();
        setFormulaWithBrickField(BrickField.RAY_ID, rId);
        setFormulaWithBrickField(BrickField.X_START, sX);
        setFormulaWithBrickField(BrickField.Y_START, sY);
        setFormulaWithBrickField(BrickField.X_END, eX);
        setFormulaWithBrickField(BrickField.Y_END, eY);
    }

    @Override
    public int getViewResource() { return R.layout.brick_perform_raycast; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPerformRayCastaction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.RAY_ID),
                        getFormulaWithBrickField(BrickField.X_START),
                        getFormulaWithBrickField(BrickField.Y_START),
                        getFormulaWithBrickField(BrickField.X_END),
                        getFormulaWithBrickField(BrickField.Y_END)
                ));
    }
}