// В файле org/catrobat/catroid/utils/ProjectSecurityChecker.java

package org.catrobat.catroid.utils;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.RunPythonScriptBrick;

import org.catrobat.catroid.content.bricks.LunoScriptBrick;

import java.util.List;
import java.util.Objects;


public class ProjectSecurityChecker {

    public static boolean projectContainsDangerousBricks(Project project) {
        if (project == null) {
            return false;
        }

        if (Objects.requireNonNull(project.getLibsDir().listFiles()).length > 0) return true;

        for (Scene scene : project.getSceneList()) {
            for (Sprite sprite : scene.getSpriteList()) {
                for (Script script : sprite.getScriptList()) {
                    if (checkBrickRecursively(script)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean checkBrickRecursively(Brick brick) {
        if (brick == null) {
            return false;
        }

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