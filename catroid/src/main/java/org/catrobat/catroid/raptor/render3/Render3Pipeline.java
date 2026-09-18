package org.catrobat.catroid.raptor.render3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

import net.mgsx.gltf.scene3d.scene.SceneManager;

import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.raptor.lod.LODMeshGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Render3Pipeline implements Disposable {

    private final ThreeDManager engine;

    private final UniversalProbeVolume probeVolume = new UniversalProbeVolume();
    private CinematicBloom cinematicBloom;

    private final Map<String, LODMeshGroup> lodGroups = new HashMap<>();
    private final Map<String, BoundingBox> rawBounds = new HashMap<>();
    private final Vector3 tmpPos = new Vector3();
    private final Vector3 tmpScale = new Vector3();
    private final Matrix4 tmpInvProj = new Matrix4();

    private final float[] ssaoKernel = new float[16 * 3];

    private FrameBuffer hdrBaseFbo;
    private FrameBuffer indirectHalfFbo;
    private Texture noiseTexture;

    private ShaderProgram indirectShader;
    private ShaderProgram uberShader;

    private SpriteBatch screenBatch;
    private OrthographicCamera screenCam;

    private boolean isInitialized = false;

    public float exposure = 1.0f;
    public float bloomIntensity = 0.7f;

    public float aoRadius = 1.0f;
    public float aoIntensity = 0.5f;
    public float aoBias = 0.02f;

    public float giRadius = 3.5f;
    public float giIntensity = 0.02f;

    public float ssrReflectivity = 0.5f;

    public Render3Pipeline(ThreeDManager engine) {
        this.engine = engine;
        generateKernel();
    }

    private void generateKernel() {
        for (int i = 0; i < 16; i++) {
            Vector3 v = new Vector3(MathUtils.random() * 2 - 1, MathUtils.random() * 2 - 1, MathUtils.random()).nor();
            float scale = (float) i / 16f;
            scale = MathUtils.lerp(0.1f, 1.0f, scale * scale);
            v.scl(scale);
            ssaoKernel[i * 3] = v.x;
            ssaoKernel[i * 3 + 1] = v.y;
            ssaoKernel[i * 3 + 2] = v.z;
        }
    }

    public void init(int width, int height) {
        if (isInitialized) dispose();

        int w = Math.max(2, width);
        int h = Math.max(2, height);

        screenBatch = new SpriteBatch();
        screenCam = new OrthographicCamera();
        screenCam.setToOrtho(false, 1, 1);

        cinematicBloom = new CinematicBloom();
        cinematicBloom.resize(w, h);

        hdrBaseFbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, true);
        hdrBaseFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        indirectHalfFbo = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(2, w / 2), Math.max(2, h / 2), false);
        indirectHalfFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        createNoiseTexture();
        loadShaders();

        isInitialized = true;
    }

    public void resize(int width, int height) {
        init(width, height);
    }

    private void loadShaders() {
        ShaderProgram.pedantic = false;
        String vsh = "attribute vec4 a_position;\n" +
                "attribute vec4 a_color;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    v_texCoords = a_texCoord0;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}";

        indirectShader = new ShaderProgram(vsh, Gdx.files.internal("shaders/render3_indirect_halfres.frag").readString());
        uberShader = new ShaderProgram(vsh, Gdx.files.internal("shaders/render3_uber.frag").readString());

        if (!indirectShader.isCompiled()) Gdx.app.error("Render3", "Indirect Shader Error:\n" + indirectShader.getLog());
        if (!uberShader.isCompiled()) Gdx.app.error("Render3", "Uber Shader Error:\n" + uberShader.getLog());
    }

    private void createNoiseTexture() {
        Pixmap pix = new Pixmap(4, 4, Pixmap.Format.RGB888);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                pix.setColor(MathUtils.random(), MathUtils.random(), 0, 1);
                pix.drawPixel(x, y);
            }
        }
        noiseTexture = new Texture(pix);
        noiseTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        noiseTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pix.dispose();
    }

    public void registerObject(String id, ModelInstance inst) {
        if (inst == null || lodGroups.containsKey(id)) return;
        try {
            LODMeshGroup lg = new LODMeshGroup(inst);
            lodGroups.put(id, lg);

            BoundingBox bb = new BoundingBox();
            inst.calculateBoundingBox(bb);
            rawBounds.put(id, bb);
        } catch (Exception e) {
            Gdx.app.error("Render3", "Error registering LOD for " + id, e);
        }
    }

    public void unregisterObject(String id, ModelInstance inst) {
        LODMeshGroup lg = lodGroups.remove(id);
        if (lg != null) {
            if (inst != null) lg.restoreOriginalMeshes(inst);
            lg.dispose();
        }
        rawBounds.remove(id);
    }

    public void restoreAllOriginalMeshes(Map<String, ModelInstance> sceneObjects) {
        for (Map.Entry<String, LODMeshGroup> entry : lodGroups.entrySet()) {
            ModelInstance inst = sceneObjects.get(entry.getKey());
            if (inst != null) entry.getValue().restoreOriginalMeshes(inst);
            entry.getValue().dispose();
        }
        lodGroups.clear();
        rawBounds.clear();
    }

    public void render(Camera camera,
                       SceneManager sceneManager,
                       Map<String, ModelInstance> sceneObjects,
                       Set<String> inactiveObjects,
                       FrameBuffer depthFbo,
                       FrameBuffer materialFbo) {

        if (!isInitialized) {
            init(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        float delta = Gdx.graphics.getDeltaTime();

        probeVolume.updateBake(
                sceneObjects,
                rawBounds,
                sceneManager,
                engine.skyColor,
                engine.getSunLightDirection(),
                engine.getSunLightColor(),
                engine.getSunLightIntensity()
        );

        updateLODs(camera, sceneObjects, inactiveObjects);

        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_FRONT);
        sceneManager.renderShadows();
        Gdx.gl.glCullFace(GL20.GL_BACK);

        hdrBaseFbo.begin();
        Gdx.gl.glViewport(0, 0, hdrBaseFbo.getWidth(), hdrBaseFbo.getHeight());
        Gdx.gl.glClearColor(engine.skyColor.r, engine.skyColor.g, engine.skyColor.b, engine.skyColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        sceneManager.update(delta);
        sceneManager.renderMirror();
        sceneManager.renderTransmission();
        sceneManager.renderColors();
        hdrBaseFbo.end();

        indirectHalfFbo.begin();
        Gdx.gl.glViewport(0, 0, indirectHalfFbo.getWidth(), indirectHalfFbo.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        screenBatch.setProjectionMatrix(screenCam.combined);
        screenBatch.setShader(indirectShader);
        screenBatch.begin();

        hdrBaseFbo.getColorBufferTexture().bind(0);
        depthFbo.getColorBufferTexture().bind(1);
        materialFbo.getColorBufferTexture().bind(2);
        noiseTexture.bind(3);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        indirectShader.setUniformi("u_colorTexture", 0);
        indirectShader.setUniformi("u_depthTexture", 1);
        indirectShader.setUniformi("u_materialTexture", 2);
        indirectShader.setUniformi("u_noiseTexture", 3);

        indirectShader.setUniformMatrix("u_projectionMatrix", camera.projection);
        tmpInvProj.set(camera.projection).inv();
        indirectShader.setUniformMatrix("u_invProjectionMatrix", tmpInvProj);
        indirectShader.setUniformMatrix("u_viewMatrix", camera.view);
        indirectShader.setUniformf("u_farPlane", camera.far);

        for (int i = 0; i < 16; i++) {
            indirectShader.setUniformf("u_kernel[" + i + "]", ssaoKernel[i * 3], ssaoKernel[i * 3 + 1], ssaoKernel[i * 3 + 2]);
        }

        indirectShader.setUniformf("u_noiseScale", (float) Gdx.graphics.getWidth() / 4.0f, (float) Gdx.graphics.getHeight() / 4.0f);
        indirectShader.setUniformf("u_aoRadius", aoRadius);
        indirectShader.setUniformf("u_aoIntensity", aoIntensity);
        indirectShader.setUniformf("u_aoBias", aoBias);

        indirectShader.setUniformf("u_giIntensity", giIntensity);
        indirectShader.setUniformf("u_giRadius", giRadius);

        indirectShader.setUniformf("u_reflectivityMulti", ssrReflectivity);
        indirectShader.setUniformf("u_edgeFade", 0.15f);
        indirectShader.setUniformf("u_thickness", 0.5f);
        indirectShader.setUniformf("u_maxDistance", 50.0f);
        indirectShader.setUniformf("u_stride", 0.35f);
        indirectShader.setUniformi("u_maxSteps", 30);

        Texture baseTex = hdrBaseFbo.getColorBufferTexture();
        screenBatch.draw(baseTex, 0, 0, 1, 1, 0, 0, baseTex.getWidth(), baseTex.getHeight(), false, true);
        screenBatch.end();
        indirectHalfFbo.end();

        Texture bloomTex = cinematicBloom.process(hdrBaseFbo.getColorBufferTexture());

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        Gdx.gl.glViewport(0, 0, screenW, screenH);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        screenBatch.setShader(uberShader);
        screenBatch.begin();

        hdrBaseFbo.getColorBufferTexture().bind(0);
        bloomTex.bind(1);
        indirectHalfFbo.getColorBufferTexture().bind(2);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        uberShader.setUniformi("u_sceneTexture", 0);
        uberShader.setUniformi("u_bloomTexture", 1);
        uberShader.setUniformi("u_indirectTexture", 2);
        uberShader.setUniformf("u_bloomIntensity", bloomIntensity);
        uberShader.setUniformf("u_exposure", exposure);

        screenBatch.draw(baseTex, 0, 0, 1, 1, 0, 0, baseTex.getWidth(), baseTex.getHeight(), false, true);
        screenBatch.end();
        screenBatch.setShader(null);
    }

    private void updateLODs(Camera camera, Map<String, ModelInstance> sceneObjects, Set<String> inactiveObjects) {
        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
            String id = entry.getKey();
            if (inactiveObjects.contains(id)) continue;

            ModelInstance inst = entry.getValue();
            BoundingBox localBox = rawBounds.get(id);
            if (localBox == null) {
                registerObject(id, inst);
                localBox = rawBounds.get(id);
                if (localBox == null) continue;
            }

            LODMeshGroup lg = lodGroups.get(id);
            if (lg != null) {
                inst.transform.getTranslation(tmpPos);
                inst.transform.getScale(tmpScale);

                float maxScale = Math.max(Math.abs(tmpScale.x), Math.max(Math.abs(tmpScale.y), Math.abs(tmpScale.z)));
                float localRadius = Math.max(localBox.getWidth(), Math.max(localBox.getHeight(), localBox.getDepth())) * 0.5f;
                float worldRadius = localRadius * maxScale;

                float dist = camera.position.dst(tmpPos);
                if (dist > 0.001f) {
                    float coverage = (worldRadius * 2.0f) / dist;
                    lg.update(inst, coverage);
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (hdrBaseFbo != null) hdrBaseFbo.dispose();
        if (indirectHalfFbo != null) indirectHalfFbo.dispose();
        if (cinematicBloom != null) cinematicBloom.dispose();
        if (indirectShader != null) indirectShader.dispose();
        if (uberShader != null) uberShader.dispose();
        if (noiseTexture != null) noiseTexture.dispose();
        if (screenBatch != null) screenBatch.dispose();

        for (LODMeshGroup group : lodGroups.values()) group.dispose();
        lodGroups.clear();
        rawBounds.clear();
        isInitialized = false;
    }
}
