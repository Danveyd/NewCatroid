// В пакете: org.catrobat.catroid.content
package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenProjectExitsBrick; // <-- Создадим его следующим
import org.catrobat.catroid.content.eventids.EventId;

public class ExitProjectScript extends Script {
    private static final long serialVersionUID = 1L;

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new WhenProjectExitsBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new EventId(EventId.PROJECT_EXIT); // <-- Связываем с нашим новым ID
    }
}