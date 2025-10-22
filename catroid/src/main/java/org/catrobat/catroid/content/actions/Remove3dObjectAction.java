package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.stage.StageActivity;

public class Remove3dObjectAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) {
            return;
        }

        String id = null;
        try {
            id = objectId.interpretString(scope);
            threeDManager.removeObject(id);
        } catch (InterpretationException e) {
           e.printStackTrace();
        }
    }
}