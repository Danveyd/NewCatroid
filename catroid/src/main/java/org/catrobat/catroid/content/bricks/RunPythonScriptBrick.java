package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RunPythonScriptBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RunPythonScriptBrick() {
        addAllowedBrickField(BrickField.SCRIPT, R.id.brick_python_script_text);
        // ДОБАВЛЕНО: Регистрация нового поля
        addAllowedBrickField(BrickField.VARIABLE_NAME, R.id.brick_python_script_variable_name);
    }

    public RunPythonScriptBrick(String script, String variableName) {
        this(new Formula(script), new Formula(variableName));
    }

    public RunPythonScriptBrick(Formula script, Formula variableName) {
        this();
        setFormulaWithBrickField(BrickField.SCRIPT, script);
        setFormulaWithBrickField(BrickField.VARIABLE_NAME, variableName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_python_run_script;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRunPythonScriptAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.SCRIPT),
                        getFormulaWithBrickField(BrickField.VARIABLE_NAME) // Передаем новую формулу
                ));
    }
}