package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.List;

public class CreateGLViewBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateGLViewBrick() {
        addAllowedBrickField(BrickField.GL_VIEW_NAME, R.id.brick_gl_name);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_gl_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_gl_y);
        addAllowedBrickField(BrickField.WIDTH, R.id.brick_gl_width);
        addAllowedBrickField(BrickField.HEIGHT, R.id.brick_gl_height);
    }

    public CreateGLViewBrick(String name, int x, int y, int width, int height) {
        this(new Formula(name), new Formula(x), new Formula(y), new Formula(width), new Formula(height));
    }

    public CreateGLViewBrick(Formula name, Formula x, Formula y, Formula width, Formula height) {
        this();
        setFormulaWithBrickField(BrickField.GL_VIEW_NAME, name);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
        setFormulaWithBrickField(BrickField.WIDTH, width);
        setFormulaWithBrickField(BrickField.HEIGHT, height);
    }

    @Override
    public int getViewResource() { return R.layout.brick_gl_create_view; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createGLViewAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.GL_VIEW_NAME),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.WIDTH),
                getFormulaWithBrickField(BrickField.HEIGHT)
        ));
    }
}