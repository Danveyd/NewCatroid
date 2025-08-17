package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class Set3dFrictionAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula friction;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.stageListener.getThreeDManager();
        if (threeDManager == null) return;

        try {
            String id = objectId.interpretString(scope);
            float value = friction.interpretFloat(scope);
            if (!id.isEmpty()) {
                Log.d("Friction", "Set friction: " + id + ", " + value);
                threeDManager.setFriction(id, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}