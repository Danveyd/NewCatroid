package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreatePrismaticJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreatePrismaticJointBrick() {
        addAllowedBrickField(BrickField.JOINT_ID, R.id.brick_joint_id_edit_text);
        addAllowedBrickField(BrickField.SPRITE, R.id.brick_sprite_b_edit_text);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_anchor_x_edit_text);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_anchor_y_edit_text);
        addAllowedBrickField(BrickField.AXIS_X, R.id.brick_axis_x_edit_text);
        addAllowedBrickField(BrickField.AXIS_Y, R.id.brick_axis_y_edit_text);
    }

    public CreatePrismaticJointBrick(String jointId, String spriteB, int aX, int aY, int axisX, int axisY) {
        this(new Formula(jointId), new Formula(spriteB), new Formula(aX), new Formula(aY), new Formula(axisX), new Formula(axisY));
    }

    public CreatePrismaticJointBrick(Formula jId, Formula sB, Formula aX, Formula aY, Formula axisX, Formula axisY) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_ID, jId);
        setFormulaWithBrickField(BrickField.SPRITE, sB);
        setFormulaWithBrickField(BrickField.X_POSITION, aX);
        setFormulaWithBrickField(BrickField.Y_POSITION, aY);
        setFormulaWithBrickField(BrickField.AXIS_X, axisX);
        setFormulaWithBrickField(BrickField.AXIS_Y, axisY);
    }

    @Override
    public int getViewResource() { return R.layout.brick_create_prismatic_joint; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPrismaticJointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.JOINT_ID),
                        getFormulaWithBrickField(BrickField.SPRITE),
                        getFormulaWithBrickField(BrickField.X_POSITION),
                        getFormulaWithBrickField(BrickField.Y_POSITION),
                        getFormulaWithBrickField(BrickField.AXIS_X),
                        getFormulaWithBrickField(BrickField.AXIS_Y)
                ));
    }
}