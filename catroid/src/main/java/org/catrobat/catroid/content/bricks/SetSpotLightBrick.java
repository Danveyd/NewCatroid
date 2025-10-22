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

public class SetSpotLightBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetSpotLightBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_spot_light_id_value);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_spot_light_pos_x_value);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_spot_light_pos_y_value);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_set_spot_light_pos_z_value);
        addAllowedBrickField(BrickField.VALUE_5, R.id.brick_set_spot_light_dir_x_value);
        addAllowedBrickField(BrickField.VALUE_6, R.id.brick_set_spot_light_dir_y_value);
        addAllowedBrickField(BrickField.VALUE_7, R.id.brick_set_spot_light_dir_z_value);
        addAllowedBrickField(BrickField.VALUE_8, R.id.brick_set_spot_light_color_r_value);
        addAllowedBrickField(BrickField.VALUE_9, R.id.brick_set_spot_light_color_g_value);
        addAllowedBrickField(BrickField.VALUE_10, R.id.brick_set_spot_light_color_b_value);
        addAllowedBrickField(BrickField.VALUE_11, R.id.brick_set_spot_light_intensity_value);
        addAllowedBrickField(BrickField.VALUE_12, R.id.brick_set_spot_light_angle_value);
        addAllowedBrickField(BrickField.VALUE_13, R.id.brick_set_spot_light_exponent_value);
        addAllowedBrickField(BrickField.VALUE_14, R.id.brick_set_spot_light_range_value);
    }

    public SetSpotLightBrick(String lightId, double x, double y, double z,
                             double dx, double dy, double dz,
                             int r, int g, int b,
                             double intensity, double angle, double exponent, double range) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(lightId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(x));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(y));
        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(z));
        setFormulaWithBrickField(BrickField.VALUE_5, new Formula(dx));
        setFormulaWithBrickField(BrickField.VALUE_6, new Formula(dy));
        setFormulaWithBrickField(BrickField.VALUE_7, new Formula(dz));
        setFormulaWithBrickField(BrickField.VALUE_8, new Formula(r));
        setFormulaWithBrickField(BrickField.VALUE_9, new Formula(g));
        setFormulaWithBrickField(BrickField.VALUE_10, new Formula(b));
        setFormulaWithBrickField(BrickField.VALUE_11, new Formula(intensity));
        setFormulaWithBrickField(BrickField.VALUE_12, new Formula(angle));
        setFormulaWithBrickField(BrickField.VALUE_13, new Formula(exponent));
        setFormulaWithBrickField(BrickField.VALUE_14, new Formula(range));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_spot_light;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetSpotLightAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4),
                        getFormulaWithBrickField(BrickField.VALUE_5),
                        getFormulaWithBrickField(BrickField.VALUE_6),
                        getFormulaWithBrickField(BrickField.VALUE_7),
                        getFormulaWithBrickField(BrickField.VALUE_8),
                        getFormulaWithBrickField(BrickField.VALUE_9),
                        getFormulaWithBrickField(BrickField.VALUE_10),
                        getFormulaWithBrickField(BrickField.VALUE_11),
                        getFormulaWithBrickField(BrickField.VALUE_12),
                        getFormulaWithBrickField(BrickField.VALUE_13),
                        getFormulaWithBrickField(BrickField.VALUE_14)
                ));
    }
}