package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class CreateCubeAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            if (!id.isEmpty()) {
                threeDManager.createCube(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}