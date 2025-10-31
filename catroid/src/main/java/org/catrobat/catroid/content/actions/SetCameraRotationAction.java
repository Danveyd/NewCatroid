package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class SetCameraRotationAction extends TemporalAction {
    public Scope scope;
    public Formula yaw, pitch, roll;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {

            float y = yaw.interpretFloat(scope);
            float p = pitch.interpretFloat(scope);
            float r = roll.interpretFloat(scope);

            if (threeDManager.cameraTargetId != null) {
                threeDManager.setCameraRotation(y, p);
            } else {
                threeDManager.setCameraRotation(y, p, r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}