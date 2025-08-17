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

public class SetFogBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetFogBrick() {
        // Замените FORMULA_* на реальные значения из вашего BrickField enum
        addAllowedBrickField(BrickField.FORMULA_1, R.id.brick_set_fog_r_edit);
        addAllowedBrickField(BrickField.FORMULA_2, R.id.brick_set_fog_g_edit);
        addAllowedBrickField(BrickField.FORMULA_3, R.id.brick_set_fog_b_edit);
        addAllowedBrickField(BrickField.FORMULA_4, R.id.brick_set_fog_density_edit);
    }

    public SetFogBrick(float r, float g, float b, float density) {
        this(new Formula(r), new Formula(g), new Formula(b), new Formula(density));
    }

    public SetFogBrick(Formula r, Formula g, Formula b, Formula density) {
        this();
        setFormulaWithBrickField(BrickField.FORMULA_1, r);
        setFormulaWithBrickField(BrickField.FORMULA_2, g);
        setFormulaWithBrickField(BrickField.FORMULA_3, b);
        setFormulaWithBrickField(BrickField.FORMULA_4, density);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_fog;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetFogAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FORMULA_1),
                        getFormulaWithBrickField(BrickField.FORMULA_2),
                        getFormulaWithBrickField(BrickField.FORMULA_3),
                        getFormulaWithBrickField(BrickField.FORMULA_4)
                ));
    }
}