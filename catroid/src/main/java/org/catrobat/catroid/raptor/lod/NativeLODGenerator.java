package org.catrobat.catroid.raptor.lod;

public class NativeLODGenerator {
    static {
        System.loadLibrary("catroid");
    }

    public static native short[] nativeSimplifyMesh(
            float[] vertices, int vertexStrideFloats, int normalOffsetFloats,
            short[] indices, float targetRatio
    );
}
