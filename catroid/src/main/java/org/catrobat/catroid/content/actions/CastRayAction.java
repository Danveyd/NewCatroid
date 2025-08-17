package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

public class CastRayAction extends TemporalAction {
    public Scope scope;
    public Formula rayName;
    public Formula fromX, fromY, fromZ;
    public Formula dirX, dirY, dirZ;

    @Override
    protected void update(float percent) {
        ThreeDManager manager = StageActivity.stageListener.getThreeDManager();
        if (manager == null) return;

        try {

            String name = rayName.interpretString(scope);
            if (name.isEmpty()) return;

            Vector3 from = new Vector3(
                    fromX.interpretFloat(scope),
                    fromY.interpretFloat(scope),
                    fromZ.interpretFloat(scope)
            );

            Vector3 direction = new Vector3(
                    dirX.interpretFloat(scope),
                    dirY.interpretFloat(scope),
                    dirZ.interpretFloat(scope)
            ).nor(); // nor() - нормализует вектор (делает его длину равной 1)

            manager.castRay(name, from, direction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}