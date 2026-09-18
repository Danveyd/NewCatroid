package org.catrobat.catroid.raptor.lod;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

public class LODMeshGroup implements Disposable {

    public static class LODLevel {
        public final short[] indices;
        public Mesh mesh;

        public LODLevel(short[] indices, Mesh mesh) {
            this.indices = indices;
            this.mesh = mesh;
        }
    }

    public static class OriginalPartState {
        public final Mesh mesh;
        public final int offset;
        public final int size;

        public OriginalPartState(MeshPart part) {
            this.mesh = part.mesh;
            this.offset = part.offset;
            this.size = part.size;
        }
    }

    private final Map<String, LODLevel[]> partLods = new HashMap<>();
    private final Map<String, OriginalPartState> originalStates = new HashMap<>();

    private int currentLod = 0;
    private float lastCoverage = 1.0f;

    private static final float LOD0_LIMIT = 0.05f;
    private static final float LOD1_LIMIT = 0.02f;
    private static final float HYSTERESIS = 0.015f;

    public LODMeshGroup(ModelInstance instance) {
        for (Node node : instance.nodes) {
            processNode(node);
        }
    }

    private void processNode(Node node) {
        for (NodePart np : node.parts) {
            MeshPart part = np.meshPart;
            Mesh mesh = part.mesh;
            if (mesh == null) continue;

            originalStates.put(part.id, new OriginalPartState(part));

            boolean hasBones = (mesh.getVertexAttribute(VertexAttributes.Usage.BoneWeight) != null);
            if (hasBones) continue;

            if (part.size < 800) {
                continue;
            }

            int numVertices = mesh.getNumVertices();
            int stride = mesh.getVertexSize() / 4;

            VertexAttribute normAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
            int normalOffset = (normAttr != null) ? normAttr.offset / 4 : -1;

            float[] vertices = new float[numVertices * stride];
            mesh.getVertices(vertices);

            short[] originalIndices = new short[part.size];
            mesh.getIndices(part.offset, part.size, originalIndices, 0);

            short[] lod1 = NativeLODGenerator.nativeSimplifyMesh(vertices, stride, normalOffset, originalIndices, 0.65f);
            short[] lod2 = NativeLODGenerator.nativeSimplifyMesh(vertices, stride, normalOffset, originalIndices, 0.35f);

            LODLevel[] levels = new LODLevel[2];

            Mesh m1 = new Mesh(true, numVertices, lod1.length, mesh.getVertexAttributes());
            m1.setVertices(vertices);
            m1.setIndices(lod1);
            levels[0] = new LODLevel(lod1, m1);

            Mesh m2 = new Mesh(true, numVertices, lod2.length, mesh.getVertexAttributes());
            m2.setVertices(vertices);
            m2.setIndices(lod2);
            levels[1] = new LODLevel(lod2, m2);

            partLods.put(part.id, levels);
        }

        for (Node child : node.getChildren()) {
            processNode(child);
        }
    }

    public void update(ModelInstance instance, float coverage) {
        int targetLod = 0;
        float h = (coverage > lastCoverage) ? -HYSTERESIS : HYSTERESIS;

        if (coverage > (LOD0_LIMIT + h)) {
            targetLod = 0;
        } else if (coverage > (LOD1_LIMIT + h)) {
            targetLod = 1;
        } else {
            targetLod = 2;
        }
        lastCoverage = coverage;

        if (targetLod == currentLod) return;
        currentLod = targetLod;

        applyLOD(instance);
    }

    private void applyLOD(ModelInstance instance) {
        for (Node node : instance.nodes) {
            applyNode(node);
        }
    }

    private void applyNode(Node node) {
        for (NodePart np : node.parts) {
            String id = np.meshPart.id;
            OriginalPartState orig = originalStates.get(id);
            LODLevel[] lvls = partLods.get(id);

            if (orig == null || lvls == null) continue;

            if (currentLod == 0) {
                np.meshPart.mesh = orig.mesh;
                np.meshPart.offset = orig.offset;
                np.meshPart.size = orig.size;
            } else {
                LODLevel target = lvls[currentLod - 1];
                np.meshPart.mesh = target.mesh;
                np.meshPart.offset = 0;
                np.meshPart.size = target.indices.length;
            }
        }

        for (Node child : node.getChildren()) {
            applyNode(child);
        }
    }

    public void restoreOriginalMeshes(ModelInstance instance) {
        currentLod = 0;
        if (instance == null) return;
        for (Node node : instance.nodes) {
            for (NodePart np : node.parts) {
                OriginalPartState orig = originalStates.get(np.meshPart.id);
                if (orig != null) {
                    np.meshPart.mesh = orig.mesh;
                    np.meshPart.offset = orig.offset;
                    np.meshPart.size = orig.size;
                }
            }
        }
    }

    @Override
    public void dispose() {
        for (LODLevel[] lvls : partLods.values()) {
            for (LODLevel l : lvls) {
                if (l.mesh != null) {
                    l.mesh.dispose();
                    l.mesh = null;
                }
            }
        }
        partLods.clear();
        originalStates.clear();
    }
}
