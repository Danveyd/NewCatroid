package org.catrobat.catroid.stage.event;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;

public class EventManager {

    /**
     * Проверяет, содержит ли проект хотя бы один скрипт указанного типа.
     *
     * @param project     Проект для проверки.
     * @param scriptClass Класс скрипта для поиска (например, BackPressedScript.class).
     * @return true, если найден хотя бы один экземпляр, иначе false.
     */
    public static boolean projectHasScriptOfType(Project project, Class<? extends Script> scriptClass) {
        if (project == null || scriptClass == null) {
            return false;
        }
        for (Scene scene : project.getSceneList()) {
            for (Sprite sprite : scene.getSpriteList()) {
                for (Script script : sprite.getScriptList()) {
                    // isInstance() - это безопасный способ проверить, является ли объект
                    // экземпляром указанного класса.
                    if (scriptClass.isInstance(script)) {
                        return true; // Нашли, дальше можно не искать
                    }
                }
            }
        }
        return false; // Не нашли во всем проекте
    }
}