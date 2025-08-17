/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */
package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class SetFogAction extends TemporalAction {
    public Scope scope;
    public Formula red;
    public Formula green;
    public Formula blue;
    public Formula density;

    @Override
    protected void update(float percent) {
        if (StageActivity.stageListener == null || StageActivity.stageListener.getThreeDManager() == null) {
            return;
        }

        try {

            float r = red.interpretFloat(scope);
            float g = green.interpretFloat(scope);
            float b = blue.interpretFloat(scope);
            float d = density.interpretFloat(scope);

            StageActivity.stageListener.getThreeDManager().setFog(r, g, b, d);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}