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

public class RemovePbrLightBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RemovePbrLightBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_remove_pbr_light_id_value);
    }

    public RemovePbrLightBrick(String lightId) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(lightId));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_remove_pbr_light;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRemovePbrLightAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1)
                ));
    }
}