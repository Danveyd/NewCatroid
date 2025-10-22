package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.ExitProjectScript;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenProjectExitsBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private ExitProjectScript script;

    public WhenProjectExitsBrick() {
        this(new ExitProjectScript());
    }

    public WhenProjectExitsBrick(ExitProjectScript script) {
        script.setScriptBrick(this);
        this.commentedOut = script.isCommentedOut();
        this.script = script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_project_exits;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenProjectExitsBrick clone = (WhenProjectExitsBrick) super.clone();
        clone.script = (ExitProjectScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {

    }
}