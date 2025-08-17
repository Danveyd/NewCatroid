package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateCubeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateCubeBrick() {
        // У вас могут быть другие имена в BrickField, я использую стандартные
        addAllowedBrickField(BrickField.VALUE, R.id.brick_create_cube_id);
    }

    public CreateCubeBrick(String objectId) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, new Formula(objectId));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_cube;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCube(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE)
                ));
    }
}