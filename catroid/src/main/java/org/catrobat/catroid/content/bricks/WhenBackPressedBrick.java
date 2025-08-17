// В пакете: org.catrobat.catroid.content.bricks
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.BackPressedScript;
import org.catrobat.catroid.content.ExitProjectScript; // <-- Наш новый скрипт
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenBackPressedBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private BackPressedScript script;

    public WhenBackPressedBrick() {
        this(new BackPressedScript());
    }

    public WhenBackPressedBrick(BackPressedScript script) {
        script.setScriptBrick(this);
        this.commentedOut = script.isCommentedOut();
        this.script = script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_back_pressed; // <-- Наш новый layout
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenBackPressedBrick clone = (WhenBackPressedBrick) super.clone();
        clone.script = (BackPressedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        // "Шапки" не добавляют действий, они только запускают скрипт
    }
}