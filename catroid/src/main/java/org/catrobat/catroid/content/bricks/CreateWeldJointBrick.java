package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateWeldJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateWeldJointBrick() {
        addAllowedBrickField(BrickField.JOINT_ID, R.id.brick_joint_id_edit_text);
        addAllowedBrickField(BrickField.SPRITE, R.id.brick_sprite_b_edit_text);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_anchor_x_edit_text);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_anchor_y_edit_text);
    }

    public CreateWeldJointBrick(String jointId, String spriteBName, int anchorX, int anchorY) {
        this(new Formula(jointId), new Formula(spriteBName), new Formula(anchorX), new Formula(anchorY));
    }

    public CreateWeldJointBrick(Formula jointId, Formula spriteB, Formula anchorX, Formula anchorY) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_ID, jointId);
        setFormulaWithBrickField(BrickField.SPRITE, spriteB);
        setFormulaWithBrickField(BrickField.X_POSITION, anchorX);
        setFormulaWithBrickField(BrickField.Y_POSITION, anchorY);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_weld_joint;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createWeldJointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.JOINT_ID),
                        getFormulaWithBrickField(BrickField.SPRITE),
                        getFormulaWithBrickField(BrickField.X_POSITION),
                        getFormulaWithBrickField(BrickField.Y_POSITION)
                ));
    }
}