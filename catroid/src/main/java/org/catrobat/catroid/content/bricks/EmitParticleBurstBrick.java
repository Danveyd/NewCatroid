package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class EmitParticleBurstBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public EmitParticleBurstBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_particle_burst_instance_id);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_particle_burst_count_edit_text);
    }

    public EmitParticleBurstBrick(String instanceId, double count) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(instanceId));
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(count));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_emit_particle_burst;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createEmitParticleBurstAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.VALUE_1)
        ));
    }
}
