package org.catrobat.catroid.content;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

public class CreateSphereAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;

    @Override
    protected void update(float percent) {
        ThreeDManager manager = StageActivity.stageListener.getThreeDManager();
        if (manager == null) return;

        try {

            String id = objectId.interpretString(scope);
            if (!id.isEmpty()) {
                manager.createSphere(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}