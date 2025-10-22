package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class CameraLookAtAction extends TemporalAction {
    public Scope scope;
    public Formula xValue;
    public Formula yValue;
    public Formula zValue;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) {
            return;
        }

        try {
            float x = xValue.interpretFloat(scope);
            float y = yValue.interpretFloat(scope);
            float z = zValue.interpretFloat(scope);

            threeDManager.cameraLookAt(x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}