package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class SetRestitutionAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula restitution;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.stageListener.getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            float value = restitution.interpretFloat(scope);

            if (!id.isEmpty()) {
                threeDManager.setRestitution(id, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
