package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

public class SetSkyColorAction extends TemporalAction {
    public Scope scope;
    public Formula red, green, blue;

    @Override
    protected void update(float percent) {
        ThreeDManager manager = StageActivity.getActiveStageListener().getThreeDManager();
        if (manager == null) return;

        try {

            manager.setSkyColor(
                    red.interpretFloat(scope),
                    green.interpretFloat(scope),
                    blue.interpretFloat(scope)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}