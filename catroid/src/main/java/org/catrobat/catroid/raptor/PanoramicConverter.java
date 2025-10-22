package org.catrobat.catroid.raptor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBufferCubemap;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

public class PanoramicConverter implements Disposable {

    private final ShaderProgram shader;
    private final Mesh skyboxMesh;
    private final PerspectiveCamera camera;

    public PanoramicConverter() {
        String vertexShader = "attribute vec3 a_position;\n" +
                "varying vec3 v_position;\n" +
                "uniform mat4 u_projViewTrans;\n" +
                "void main() {\n" +
                "  v_position = a_position;\n" +
                "  gl_Position = u_projViewTrans * vec4(a_position, 1.0);\n" +
                "}";

        String fragmentShader = "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "varying vec3 v_position;\n" +
                "uniform sampler2D u_equirectangularMap;\n" +
                "const vec2 invAtan = vec2(0.1591, 0.3183);\n" +
                "vec2 sampleSphericalMap(vec3 v) {\n" +
                "    vec2 uv = vec2(atan(v.z, v.x), asin(v.y));\n" +
                "    uv *= invAtan;\n" +
                "    uv += 0.5;\n" +
                "    return uv;\n" +
                "}\n" +
                "void main() {\n" +
                "  vec2 uv = sampleSphericalMap(normalize(v_position));\n" +
                "  gl_FragColor = texture2D(u_equirectangularMap, uv);\n" +
                "}";

        Gdx.app.log("PanoramicConverter", "Compiling shader...");
        shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            Gdx.app.error("PanoramicConverter", "FATAL: Shader compilation failed: " + shader.getLog());
            throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        }
        Gdx.app.log("PanoramicConverter", "Shader compiled successfully.");

        skyboxMesh = createSkyboxMesh();
        camera = new PerspectiveCamera(90, 1, 1);
        camera.near = 0.1f;
        camera.far = 10f;
    }

    public Cubemap convert(Texture equirectangularTexture, int cubemapSize) {
        Gdx.app.log("PanoramicConverter", "Starting conversion to " + cubemapSize + "x" + cubemapSize + " Cubemap.");

        Pixmap.Format format = equirectangularTexture.getTextureData().getFormat();
        if (format != Pixmap.Format.RGB888 && format != Pixmap.Format.RGBA8888) {
            Gdx.app.log("PanoramicConverter", "Unsupported source format " + format + ", falling back to RGBA8888.");
            format = Pixmap.Format.RGBA8888;
        }

        FrameBufferCubemap fbo = new FrameBufferCubemap(format, cubemapSize, cubemapSize, true);

        equirectangularTexture.bind(0);
        shader.bind();
        shader.setUniformi("u_equirectangularMap", 0);

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        fbo.begin();
        while (fbo.nextSide()) {
            camera.direction.set(fbo.getSide().direction);
            camera.up.set(fbo.getSide().up);
            camera.update();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            shader.setUniformMatrix("u_projViewTrans", camera.combined);
            skyboxMesh.render(shader, GL20.GL_TRIANGLES);
        }
        fbo.end();

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        Cubemap result = fbo.getColorBufferTexture();
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        fbo.dispose();
        Gdx.app.log("PanoramicConverter", "Conversion finished.");
        return result;
    }

    private Mesh createSkyboxMesh() {
        float[] vertices = {
                -1f, -1f, -1f, 1f, -1f, -1f, 1f, 1f, -1f, -1f, 1f, -1f, // Back face
                -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f,   // Front face
                -1f, 1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, -1f, 1f, 1f,     // Top face
                -1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, 1f, // Bottom face
                1f, -1f, -1f, 1f, -1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f,     // Right face
                -1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f  // Left face
        };
        short[] indices = {
                0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11,
                12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23
        };
        Mesh mesh = new Mesh(true, 24, 36, new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    @Override
    public void dispose() {
        shader.dispose();
        skyboxMesh.dispose();
    }
}