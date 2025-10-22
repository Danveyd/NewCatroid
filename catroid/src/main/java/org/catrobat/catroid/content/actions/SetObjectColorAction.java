package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class SetObjectColorAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula red, green, blue;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            if (id.isEmpty()) return;

            float r = red.interpretFloat(scope);
            float g = green.interpretFloat(scope);
            float b = blue.interpretFloat(scope);

            threeDManager.setObjectColor(id, r, g, b);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}