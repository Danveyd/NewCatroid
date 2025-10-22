package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LoadSceneBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public LoadSceneBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_load_scene_edit_text);
    }

    public LoadSceneBrick(String fileName) {
        this(new Formula(fileName));
    }

    public LoadSceneBrick(Formula fileNameFormula) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, fileNameFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_load_scene;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createLoadSceneAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE)));
    }
}