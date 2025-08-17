package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class Set3dPositionAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula xValue;
    public Formula yValue;
    public Formula zValue;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.stageListener.getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            if (id.isEmpty()) return;

            float x = xValue.interpretFloat(scope);
            float y = yValue.interpretFloat(scope);
            float z = zValue.interpretFloat(scope);

            threeDManager.setPosition(id, x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}