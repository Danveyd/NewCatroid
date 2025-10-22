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

public class StopAnimationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public StopAnimationBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_stop_animation_object_id_value);
    }

    public StopAnimationBrick(String objectId) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_stop_animation;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createStopAnimationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1)
                ));
    }
}