package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetParticleTransformBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetParticleTransformBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_particle_transform_instance_id);
        addAllowedBrickField(BrickField.X, R.id.brick_particle_transform_x);
        addAllowedBrickField(BrickField.Y, R.id.brick_particle_transform_y);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_particle_transform_scale_x);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_particle_transform_scale_y);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_particle_transform_rotation);
    }

    public SetParticleTransformBrick(String instanceId, double x, double y, double scaleX, double scaleY, double rotation) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(instanceId));
        setFormulaWithBrickField(BrickField.X, new Formula(x));
        setFormulaWithBrickField(BrickField.Y, new Formula(y));
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(scaleX));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(scaleY));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(rotation));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_particle_transform;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetParticleTransformAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2),
                getFormulaWithBrickField(BrickField.VALUE_3)
        ));
    }
}
