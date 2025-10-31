package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.physics.content.ActionPhysicsFactory;

public class DestroyJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public DestroyJointBrick() {
        addAllowedBrickField(BrickField.JOINT_ID, R.id.brick_joint_id_edit_text);
    }

    public DestroyJointBrick(String jointId) {
        this(new Formula(jointId));
    }

    public DestroyJointBrick(Formula jointId) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_ID, jointId);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_destroy_joint;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction((sprite.getActionFactory())
                .createDestroyJointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.JOINT_ID)
                ));
    }
}