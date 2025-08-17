package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LoadPythonLibraryBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public LoadPythonLibraryBrick() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_python_library_file_name_text);
    }

    public LoadPythonLibraryBrick(String fileName) {
        this(new Formula(fileName));
    }

    public LoadPythonLibraryBrick(Formula fileName) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, fileName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_python_load_library;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createLoadPythonLibraryAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FILE_NAME)));
    }
}