package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateGearJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateGearJointBrick() {
        addAllowedBrickField(BrickField.JOINT_ID, R.id.brick_joint_id_edit_text);
        addAllowedBrickField(BrickField.JOINT_A_ID, R.id.brick_joint_a_id_edit_text);
        addAllowedBrickField(BrickField.JOINT_B_ID, R.id.brick_joint_b_id_edit_text);
        addAllowedBrickField(BrickField.RATIO, R.id.brick_ratio_edit_text);
    }

    public CreateGearJointBrick(String jointId, String jointA, String jointB, float ratio) {
        this(new Formula(jointId), new Formula(jointA), new Formula(jointB), new Formula(ratio));
    }

    public CreateGearJointBrick(Formula jId, Formula jAId, Formula jBId, Formula ratio) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_ID, jId);
        setFormulaWithBrickField(BrickField.JOINT_A_ID, jAId);
        setFormulaWithBrickField(BrickField.JOINT_B_ID, jBId);
        setFormulaWithBrickField(BrickField.RATIO, ratio);
    }

    @Override
    public int getViewResource() { return R.layout.brick_create_gear_joint; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createGearJointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.JOINT_ID),
                        getFormulaWithBrickField(BrickField.JOINT_A_ID),
                        getFormulaWithBrickField(BrickField.JOINT_B_ID),
                        getFormulaWithBrickField(BrickField.RATIO)
                ));
    }
}