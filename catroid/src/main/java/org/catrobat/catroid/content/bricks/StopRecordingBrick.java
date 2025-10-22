package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class StopRecordingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public StopRecordingBrick() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_recording_filename);
    }

    public StopRecordingBrick(String fileName) {
        this(new Formula(fileName));
    }

    public StopRecordingBrick(Formula fileNameFormula) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, fileNameFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_stop_recording_and_save;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createStopRecordingAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FILE_NAME)));
    }
}