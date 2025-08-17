package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetShaderCodeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetShaderCodeBrick() {
        addAllowedBrickField(BrickField.THREED_VERTEX_SHADER_CODE, R.id.brick_shader_vertex_code);
        addAllowedBrickField(BrickField.THREED_FRAGMENT_SHADER_CODE, R.id.brick_shader_fragment_code);
    }

    public SetShaderCodeBrick(String vertexCode, String fragmentCode) {
        this(new Formula(vertexCode), new Formula(fragmentCode));
    }

    public SetShaderCodeBrick(Formula vertexFormula, Formula fragmentFormula) {
        this();
        setFormulaWithBrickField(BrickField.THREED_VERTEX_SHADER_CODE, vertexFormula);
        setFormulaWithBrickField(BrickField.THREED_FRAGMENT_SHADER_CODE, fragmentFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_shader_code;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetShaderCodeAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.THREED_VERTEX_SHADER_CODE),
                getFormulaWithBrickField(BrickField.THREED_FRAGMENT_SHADER_CODE)
        ));
    }
}