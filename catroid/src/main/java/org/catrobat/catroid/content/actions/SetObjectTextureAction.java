package org.catrobat.catroid.content.actions;
// package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;
import java.io.File;

public class SetObjectTextureAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public Formula textureName;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.stageListener.getThreeDManager();
        if (threeDManager == null) return;

        try {

            String id = objectId.interpretString(scope);
            String texFileName = textureName.interpretString(scope);
            if (id.isEmpty() || texFileName.isEmpty()) return;

            // Получаем файл из хранилища проекта
            File textureFile = scope.getProject().getFile(texFileName);
            if (textureFile == null || !textureFile.exists()) return;

            threeDManager.setObjectTexture(id, textureFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}