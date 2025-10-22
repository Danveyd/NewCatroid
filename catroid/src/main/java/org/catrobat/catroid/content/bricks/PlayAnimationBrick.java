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

public class PlayAnimationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PlayAnimationBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_play_animation_object_id_value);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_play_animation_name_value);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_play_animation_loops_value);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_play_animation_speed_value);
        addAllowedBrickField(BrickField.VALUE_5, R.id.brick_play_animation_transition_value);
    }

    public PlayAnimationBrick(String objectId, String animationName, int loops, double speed, double transitionTime) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(animationName));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(loops));
        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(speed));
        setFormulaWithBrickField(BrickField.VALUE_5, new Formula(transitionTime));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_play_animation;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPlayAnimationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4),
                        getFormulaWithBrickField(BrickField.VALUE_5)
                ));
    }
}