package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PlayPreparedSoundBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PlayPreparedSoundBrick() {
        addAllowedBrickField(BrickField.CACHE_NAME, R.id.brick_play_prepared_sound_cache_name);
    }

    public PlayPreparedSoundBrick(String cacheName) {
        this(new Formula(cacheName));
    }

    public PlayPreparedSoundBrick(Formula cacheName) {
        this();
        setFormulaWithBrickField(BrickField.CACHE_NAME, cacheName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_play_prepared_sound;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPlayPreparedSoundAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.CACHE_NAME)));
    }
}