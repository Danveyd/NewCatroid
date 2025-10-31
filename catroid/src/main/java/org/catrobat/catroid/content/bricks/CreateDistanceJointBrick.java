package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateDistanceJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateDistanceJointBrick() {
        addAllowedBrickField(BrickField.JOINT_ID, R.id.brick_joint_id_edit_text);
        addAllowedBrickField(BrickField.SPRITE, R.id.brick_sprite_b_edit_text);
        addAllowedBrickField(BrickField.JOINT_LENGTH, R.id.brick_length_edit_text);
        addAllowedBrickField(BrickField.JOINT_FREQUENCY, R.id.brick_frequency_edit_text);
        addAllowedBrickField(BrickField.JOINT_DAMPING, R.id.brick_damping_edit_text);
    }

    public CreateDistanceJointBrick(String jointId, String spriteB, String length, String frequency, String damping) {
        this(new Formula(jointId), new Formula(spriteB), new Formula(length), new Formula(frequency), new Formula(damping));
    }

    public CreateDistanceJointBrick(Formula jointId, Formula spriteB, Formula length, Formula frequency, Formula damping) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_ID, jointId);
        setFormulaWithBrickField(BrickField.SPRITE, spriteB);
        setFormulaWithBrickField(BrickField.JOINT_LENGTH, length);
        setFormulaWithBrickField(BrickField.JOINT_FREQUENCY, frequency);
        setFormulaWithBrickField(BrickField.JOINT_DAMPING, damping);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_distance_joint;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createDistanceJointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.JOINT_ID),
                        getFormulaWithBrickField(BrickField.SPRITE),
                        getFormulaWithBrickField(BrickField.JOINT_LENGTH),
                        getFormulaWithBrickField(BrickField.JOINT_FREQUENCY),
                        getFormulaWithBrickField(BrickField.JOINT_DAMPING)
                ));
    }
}