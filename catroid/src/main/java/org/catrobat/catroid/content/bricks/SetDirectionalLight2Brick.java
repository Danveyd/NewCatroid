/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */

package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetDirectionalLight2Brick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetDirectionalLight2Brick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_directional_light_dir_x_value);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_directional_light_dir_y_value);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_directional_light_dir_z_value);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_set_directional_light_intensity_value);
    }

    public SetDirectionalLight2Brick(double dirX, double dirY, double dirZ, double intensity) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(dirX));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(dirY));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(dirZ));
        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(intensity));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_directional_light2;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetDirectionalLight2Action(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4)
                ));
    }
}