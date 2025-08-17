package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class Set3dRotationAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula yaw;   // Рыскание (вокруг Y)
    public Formula pitch; // Тангаж (вокруг X)
    public Formula roll;  // Крен (вокруг Z)

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.stageListener.getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);

            float yAngle = yaw.interpretFloat(scope);
            float pAngle = pitch.interpretFloat(scope);
            float rAngle = roll.interpretFloat(scope);

            threeDManager.setRotation(id, yAngle, pAngle, rAngle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}