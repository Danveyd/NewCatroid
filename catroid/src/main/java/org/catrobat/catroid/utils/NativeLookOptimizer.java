package org.catrobat.catroid.utils;

import com.badlogic.gdx.math.Rectangle;

public class NativeLookOptimizer {
    static {
        // Имя библиотеки должно совпадать с project() в CMakeLists.txt
        System.loadLibrary("catroid");
    }

    public static native float[] transformPolygon(
            float[] vertices,
            float x, float y,
            float scaleX, float scaleY,
            float rotation,
            float originX, float originY
    );

    // В NativeLookOptimizer.java
    public static native float[] getTransformedBoundingBox(
            float x, float y,
            float width, float height,
            float scaleX, float scaleY,
            float rotation,
            float originX, float originY
    );

    public static native int[] checkAllCollisions(float[][][] allSpritesPolygons);

    public static native boolean checkSingleCollision(float[][] firstLookPolygons, float[][] secondLookPolygons);
}