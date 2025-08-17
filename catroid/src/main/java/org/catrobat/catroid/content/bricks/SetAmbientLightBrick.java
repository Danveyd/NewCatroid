package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetAmbientLightBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetAmbientLightBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_ambient_light_r);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_ambient_light_g);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_ambient_light_b);
    }

    public SetAmbientLightBrick(double r, double g, double b) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(r));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(g));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(b));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_ambient_light;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetAmbientLightAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3)
                ));
    }
}