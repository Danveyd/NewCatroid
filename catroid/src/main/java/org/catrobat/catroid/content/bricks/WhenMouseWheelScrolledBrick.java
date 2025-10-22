package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenMouseWheelScrolledScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenMouseWheelScrolledBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private WhenMouseWheelScrolledScript script;

    public WhenMouseWheelScrolledBrick() {
        this(new WhenMouseWheelScrolledScript());
    }

    public WhenMouseWheelScrolledBrick(WhenMouseWheelScrolledScript script) {
        this.script = script;
        this.script.setScriptBrick(this);
    }

    @Override public int getViewResource() { return R.layout.brick_when_mouse_wheel_scrolled; }
    @Override public Script getScript() { return script; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {}
    @Override public Brick clone() throws CloneNotSupportedException {
        WhenMouseWheelScrolledBrick clone = (WhenMouseWheelScrolledBrick) super.clone();
        clone.script = (WhenMouseWheelScrolledScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }
}