package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class KeyEventBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public KeyEventBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_key_char_edit);
        addAllowedBrickField(BrickField.VM_KEY_DOWN, R.id.brick_key_down_edit);
    }

    public KeyEventBrick(String character, int isDown) {
        this(new Formula(character), new Formula(isDown));
    }

    public KeyEventBrick(Formula character, Formula isDown) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, character);
        setFormulaWithBrickField(BrickField.VM_KEY_DOWN, isDown);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_key_event;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createKeyEventAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE),
                getFormulaWithBrickField(BrickField.VM_KEY_DOWN)
        ));
    }
}