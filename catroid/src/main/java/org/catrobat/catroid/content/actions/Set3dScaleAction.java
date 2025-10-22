package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class Set3dScaleAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula scaleX;
    public Formula scaleY;
    public Formula scaleZ;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            if (id.isEmpty()) return;

            float x = scaleX.interpretFloat(scope);
            float y = scaleY.interpretFloat(scope);
            float z = scaleZ.interpretFloat(scope);

            threeDManager.setScale(id, x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}