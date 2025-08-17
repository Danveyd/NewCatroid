// В файле org/catrobat/catroid/utils/ProjectSecurityChecker.java

package org.catrobat.catroid.utils;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.RunPythonScriptBrick;

// ЗАМЕНИТЕ ЭТИ ИМЕНА НА ВАШИ РЕАЛЬНЫЕ КЛАССЫ БЛОКОВ
import org.catrobat.catroid.content.bricks.LunoScriptBrick;

import java.util.List;
import java.util.Objects;


public class ProjectSecurityChecker {

    /**
     * Проверяет, содержит ли проект потенциально опасные блоки.
     *
     * @param project Проект для проверки.
     * @return true, если найден хотя бы один опасный блок, иначе false.
     */
    public static boolean projectContainsDangerousBricks(Project project) {
        if (project == null) {
            return false;
        }

        if (Objects.requireNonNull(project.getLibsDir().listFiles()).length > 0) return true;

        // Перебираем все спрайты в проекте
        for (Scene scene : project.getSceneList()) {
            for (Sprite sprite : scene.getSpriteList()) {
                // Перебираем все скрипты в спрайте
                for (Script script : sprite.getScriptList()) {
                    // Запускаем рекурсивную проверку для каждого скрипта
                    if (checkBrickRecursively(script)) {
                        return true; // Нашли опасный блок, дальше можно не искать
                    }
                }
            }
        }

        return false; // Не нашли ничего опасного
    }

    /**
     * Рекурсивно обходит все блоки, начиная с указанного, и ищет опасные.
     *
     * @param brick Стартовый блок для проверки.
     * @return true, если в этой "ветке" блоков найден опасный, иначе false.
     */
    private static boolean checkBrickRecursively(Brick brick) {
        if (brick == null) {
            return false;
        }

        // 1. Проверяем сам текущий блок
        // ЗАМЕНИТЕ ЭТИ КЛАССЫ НА ВАШИ РЕАЛЬНЫЕ
        return brick instanceof LunoScriptBrick || brick instanceof RunPythonScriptBrick;
    }

    private static boolean checkBrickRecursively(Script brick) {
        if (brick == null) {
            return false;
        }

        List<Brick> brickList = ((Script) brick).getBrickList();
        if (brickList != null) {
            for (Brick childBrick : brickList) {
                if (checkBrickRecursively(childBrick)) {
                    return true;
                }
            }
        }

        return false;
    }
}