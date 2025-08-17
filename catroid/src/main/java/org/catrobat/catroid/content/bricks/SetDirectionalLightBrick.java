package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetDirectionalLightBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetDirectionalLightBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_directional_light_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_directional_light_r);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_directional_light_g);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_set_directional_light_b);
        addAllowedBrickField(BrickField.VALUE_5, R.id.brick_set_directional_light_x);
        addAllowedBrickField(BrickField.VALUE_6, R.id.brick_set_directional_light_y);
        addAllowedBrickField(BrickField.VALUE_7, R.id.brick_set_directional_light_z);
    }

    public SetDirectionalLightBrick(String id, double r, double g, double b, double dx, double dy, double dz) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(id));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(r));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(g));
        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(b));
        setFormulaWithBrickField(BrickField.VALUE_5, new Formula(dx));
        setFormulaWithBrickField(BrickField.VALUE_6, new Formula(dy));
        setFormulaWithBrickField(BrickField.VALUE_7, new Formula(dz));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_directional_light;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetDirectionalLightAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4),
                        getFormulaWithBrickField(BrickField.VALUE_5),
                        getFormulaWithBrickField(BrickField.VALUE_6),
                        getFormulaWithBrickField(BrickField.VALUE_7)
                ));
    }
}