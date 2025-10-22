package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetBackgroundLightBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetBackgroundLightBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_background_light_intensity_value);
    }

    public SetBackgroundLightBrick(double intensity) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(intensity));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_background_light;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetBackgroundLightAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1)
                ));
    }
}