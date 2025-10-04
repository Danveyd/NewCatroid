package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.List;

public class AttachSOBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AttachSOBrick() {
        addAllowedBrickField(BrickField.GL_VIEW_NAME, R.id.brick_gl_view_name);
        addAllowedBrickField(BrickField.FILE, R.id.brick_gl_so_name);
    }

    public AttachSOBrick(String name, String file) {
        this(new Formula(name), new Formula(file));
    }

    public AttachSOBrick(Formula name, Formula file) {
        this();
        setFormulaWithBrickField(BrickField.GL_VIEW_NAME, name);
        setFormulaWithBrickField(BrickField.FILE, file);
    }

    @Override
    public int getViewResource() { return R.layout.brick_gl_attach_so; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createAttachSOAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.GL_VIEW_NAME),
                getFormulaWithBrickField(BrickField.FILE)
        ));
    }
}