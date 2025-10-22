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

public class SetAnisotropicFilterBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetAnisotropicFilterBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_anisotropic_filter_object_id_value);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_anisotropic_filter_level_value);
    }

    public SetAnisotropicFilterBrick(String objectId, double level) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(level));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_anisotropic_filter;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetAnisotropicFilterAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}