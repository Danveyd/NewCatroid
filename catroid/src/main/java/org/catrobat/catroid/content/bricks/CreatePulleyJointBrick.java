package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreatePulleyJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreatePulleyJointBrick() {
        addAllowedBrickField(BrickField.JOINT_ID, R.id.brick_joint_id_edit_text);
        addAllowedBrickField(BrickField.SPRITE_A, R.id.brick_sprite_a_edit_text);
        addAllowedBrickField(BrickField.SPRITE_B, R.id.brick_sprite_b_edit_text);
        addAllowedBrickField(BrickField.GROUND_ANCHOR_A_X, R.id.brick_ground_anchor_a_x_edit_text);
        addAllowedBrickField(BrickField.GROUND_ANCHOR_A_Y, R.id.brick_ground_anchor_a_y_edit_text);
        addAllowedBrickField(BrickField.GROUND_ANCHOR_B_X, R.id.brick_ground_anchor_b_x_edit_text);
        addAllowedBrickField(BrickField.GROUND_ANCHOR_B_Y, R.id.brick_ground_anchor_b_y_edit_text);
        addAllowedBrickField(BrickField.RATIO, R.id.brick_ratio_edit_text);
    }

    public CreatePulleyJointBrick(String jId, String sA, String sB, int gAX, int gAY, int gBX, int gBY, float ratio) {
        this(new Formula(jId), new Formula(sA), new Formula(sB), new Formula(gAX), new Formula(gAY), new Formula(gBX), new Formula(gBY), new Formula(ratio));
    }

    public CreatePulleyJointBrick(Formula jId, Formula sA, Formula sB, Formula gAX, Formula gAY, Formula gBX, Formula gBY, Formula ratio) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_ID, jId);
        setFormulaWithBrickField(BrickField.SPRITE_A, sA);
        setFormulaWithBrickField(BrickField.SPRITE_B, sB);
        setFormulaWithBrickField(BrickField.GROUND_ANCHOR_A_X, gAX);
        setFormulaWithBrickField(BrickField.GROUND_ANCHOR_A_Y, gAY);
        setFormulaWithBrickField(BrickField.GROUND_ANCHOR_B_X, gBX);
        setFormulaWithBrickField(BrickField.GROUND_ANCHOR_B_Y, gBY);
        setFormulaWithBrickField(BrickField.RATIO, ratio);
    }

    @Override
    public int getViewResource() { return R.layout.brick_create_pulley_joint; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPulleyJointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.JOINT_ID),
                        getFormulaWithBrickField(BrickField.SPRITE_A),
                        getFormulaWithBrickField(BrickField.SPRITE_B),
                        getFormulaWithBrickField(BrickField.GROUND_ANCHOR_A_X),
                        getFormulaWithBrickField(BrickField.GROUND_ANCHOR_A_Y),
                        getFormulaWithBrickField(BrickField.GROUND_ANCHOR_B_X),
                        getFormulaWithBrickField(BrickField.GROUND_ANCHOR_B_Y),
                        getFormulaWithBrickField(BrickField.RATIO)
                ));
    }
}