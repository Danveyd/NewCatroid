package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetShaderUniformFloatBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetShaderUniformFloatBrick() {
        addAllowedBrickField(BrickField.THREED_UNIFORM_NAME, R.id.brick_uniform_name_edit);
        addAllowedBrickField(BrickField.VALUE_X, R.id.brick_uniform_value_edit);
    }

    public SetShaderUniformFloatBrick(String name, double value) {
        this(new Formula(name), new Formula(value));
    }

    public SetShaderUniformFloatBrick(Formula name, Formula value) {
        this();
        setFormulaWithBrickField(BrickField.THREED_UNIFORM_NAME, name);
        setFormulaWithBrickField(BrickField.VALUE_X, value);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_shader_uniform_float;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetShaderUniformAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.THREED_UNIFORM_NAME),
                getFormulaWithBrickField(BrickField.VALUE_X),
                null, // Y
                null  // Z
        ));
    }
}