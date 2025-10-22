package org.catrobat.catroid.editor;

public class EditorStateManager {

    private static String cachedSceneJson = null;

    public static void cacheScene(String sceneJson) {
        cachedSceneJson = sceneJson;
    }

    public static String retrieveScene() {
        return cachedSceneJson;
    }

    public static boolean hasCachedScene() {
        return cachedSceneJson != null && !cachedSceneJson.isEmpty();
    }

    public static void clearCache() {
        cachedSceneJson = null;
    }
}