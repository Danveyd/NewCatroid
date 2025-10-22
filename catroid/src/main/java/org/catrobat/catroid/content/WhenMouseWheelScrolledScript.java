package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenMouseWheelScrolledBrick;
import org.catrobat.catroid.content.eventids.EventId;

public class WhenMouseWheelScrolledScript extends Script {
    private static final long serialVersionUID = 1L;

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new WhenMouseWheelScrolledBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new EventId(EventId.MOUSE_WHEEL_SCROLLED);
    }
}