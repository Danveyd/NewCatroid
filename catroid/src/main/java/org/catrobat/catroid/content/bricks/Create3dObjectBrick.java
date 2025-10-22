package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Create3dObjectBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Create3dObjectBrick() {
        // У вас могут быть другие имена в BrickField, я использую стандартные
        addAllowedBrickField(BrickField.VALUE, R.id.brick_create_3d_object_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_create_3d_object_model_path);
    }

    public Create3dObjectBrick(String objectId, String modelPath) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(modelPath));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_3d_object;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreate3dObjectAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE),
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}