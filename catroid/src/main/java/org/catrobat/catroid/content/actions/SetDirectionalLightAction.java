package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class SetDirectionalLightAction extends TemporalAction {
    public Scope scope;
    public Formula lightId;
    public Formula red, green, blue;
    public Formula dirX, dirY, dirZ;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = lightId.interpretString(scope);
            if (id.isEmpty()) return;

            float r = red.interpretFloat(scope);
            float g = green.interpretFloat(scope);
            float b = blue.interpretFloat(scope);
            float dX = dirX.interpretFloat(scope);
            float dY = dirY.interpretFloat(scope);
            float dZ = dirZ.interpretFloat(scope);

            threeDManager.setDirectionalLight(id, r, g, b, dX, dY, dZ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}