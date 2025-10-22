package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PrepareSoundBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PrepareSoundBrick() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_prepare_sound_file_name);
        addAllowedBrickField(BrickField.CACHE_NAME, R.id.brick_prepare_sound_cache_name);
    }

    public PrepareSoundBrick(String fileName, String cacheName) {
        this(new Formula(fileName), new Formula(cacheName));
    }

    public PrepareSoundBrick(Formula fileName, Formula cacheName) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, fileName);
        setFormulaWithBrickField(BrickField.CACHE_NAME, cacheName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_prepare_sound;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPrepareSoundAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FILE_NAME),
                        getFormulaWithBrickField(BrickField.CACHE_NAME)));
    }
}