package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SpawnParticleInstanceBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SpawnParticleInstanceBrick() {
        addAllowedBrickField(BrickField.FILE, R.id.brick_spawn_particle_effect_file);
        addAllowedBrickField(BrickField.NAME, R.id.brick_spawn_particle_instance_id);
        addAllowedBrickField(BrickField.X, R.id.brick_spawn_particle_x);
        addAllowedBrickField(BrickField.Y, R.id.brick_spawn_particle_y);
    }

    public SpawnParticleInstanceBrick(String effectFile, String instanceId, double x, double y) {
        this();
        setFormulaWithBrickField(BrickField.FILE, new Formula(effectFile));
        setFormulaWithBrickField(BrickField.NAME, new Formula(instanceId));
        setFormulaWithBrickField(BrickField.X, new Formula(x));
        setFormulaWithBrickField(BrickField.Y, new Formula(y));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_spawn_particle_instance;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSpawnParticleInstanceAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.FILE),
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y)
        ));
    }
}
