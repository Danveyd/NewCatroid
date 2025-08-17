package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetShaderUniformVec3Brick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetShaderUniformVec3Brick() {
        addAllowedBrickField(BrickField.THREED_UNIFORM_NAME, R.id.brick_uniform_name_edit);
        addAllowedBrickField(BrickField.VALUE_X, R.id.brick_uniform_value_x_edit);
        addAllowedBrickField(BrickField.VALUE_Y, R.id.brick_uniform_value_y_edit);
        addAllowedBrickField(BrickField.VALUE_Z, R.id.brick_uniform_value_z_edit);
    }

    public SetShaderUniformVec3Brick(String name, double x, double y, double z) {
        this(new Formula(name), new Formula(x), new Formula(y), new Formula(z));
    }

    public SetShaderUniformVec3Brick(Formula name, Formula x, Formula y, Formula z) {
        this();
        setFormulaWithBrickField(BrickField.THREED_UNIFORM_NAME, name);
        setFormulaWithBrickField(BrickField.VALUE_X, x);
        setFormulaWithBrickField(BrickField.VALUE_Y, y);
        setFormulaWithBrickField(BrickField.VALUE_Z, z);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_shader_uniform_vec3;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetShaderUniformAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.THREED_UNIFORM_NAME),
                getFormulaWithBrickField(BrickField.VALUE_X),
                getFormulaWithBrickField(BrickField.VALUE_Y),
                getFormulaWithBrickField(BrickField.VALUE_Z)
        ));
    }
}