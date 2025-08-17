package org.catrobat.catroid.content.actions;

// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

public class SetPhysicsStateAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public int stateSelection; // 0: None, 1: Static, 2: Dynamic
    public Formula mass;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.stageListener.getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            if (id.isEmpty()) return;

            float m = mass.interpretFloat(scope);

            ThreeDManager.PhysicsState state;
            switch (stateSelection) {
                case 0: state = ThreeDManager.PhysicsState.NONE; break;
                case 1: state = ThreeDManager.PhysicsState.STATIC; break;
                case 2: state = ThreeDManager.PhysicsState.DYNAMIC; break;
                case 3: state = ThreeDManager.PhysicsState.MESH_STATIC; break; // Новая опция
                default: return;
            }

            threeDManager.setPhysicsState(id, state, m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}