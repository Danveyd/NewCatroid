package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateSphereBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateSphereBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_create_sphere_id);
    }

    public CreateSphereBrick(String objectId) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_sphere;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreateSphereAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1)
                ));
    }
}