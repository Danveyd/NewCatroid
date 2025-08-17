package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LoadNativeModuleBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public LoadNativeModuleBrick() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_python_module_file_name_text);
    }

    public LoadNativeModuleBrick(String fileName) {
        this(new Formula(fileName));
    }

    public LoadNativeModuleBrick(Formula fileName) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, fileName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_python_load_native_module;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createLoadNativeModuleAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FILE_NAME)));
    }
}