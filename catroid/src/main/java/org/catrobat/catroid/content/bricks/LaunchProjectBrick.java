package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.formulaeditor.Formula;

public class LaunchProjectBrick extends FormulaBrick {
    public LaunchProjectBrick() {
        addAllowedBrickField(BrickField.PROJECT_NAME, R.id.brick_launch_project_name);
    }
    public LaunchProjectBrick(String name) { this(new Formula(name)); }
    public LaunchProjectBrick(Formula name) {
        this();
        setFormulaWithBrickField(BrickField.PROJECT_NAME, name);
    }
    @Override public int getViewResource() { return R.layout.brick_launch_project; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createLaunchProjectAction(sprite, sequence, getFormulaWithBrickField(BrickField.PROJECT_NAME)));
    }
}