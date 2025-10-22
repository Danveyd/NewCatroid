package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenMouseButtonClickedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.MouseButtonEventId;

public class WhenMouseButtonClickedScript extends Script {
    private static final long serialVersionUID = 1L;
    private int buttonCode; // LibGDX Input.Buttons.LEFT, RIGHT, etc.

    public WhenMouseButtonClickedScript(int buttonCode) {
        this.buttonCode = buttonCode;
    }

    public int getButtonCode() {
        return buttonCode;
    }

    public void setButtonCode(int buttonCode) { // <-- ДОБАВЬТЕ ЭТОТ МЕТОД
        this.buttonCode = buttonCode;
    }

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new WhenMouseButtonClickedBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new MouseButtonEventId(buttonCode);
    }
}