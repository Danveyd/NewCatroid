package org.catrobat.catroid.raptor;

import static com.crashinvaders.vfx.effects.util.MixEffect.Method.MIX;

import android.util.Log;
import androidx.annotation.NonNull;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier;
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.BillboardRenderer;
import com.badlogic.gdx.graphics.g3d.particles.values.PointSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btGeneric6DofSpringConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btPoint2PointConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.crashinvaders.vfx.effects.ChromaticAberrationEffect;
import com.crashinvaders.vfx.effects.CrtEffect;
import com.crashinvaders.vfx.effects.FilmGrainEffect;
import com.crashinvaders.vfx.effects.FisheyeEffect;
import com.crashinvaders.vfx.effects.FxaaEffect;
import com.crashinvaders.vfx.effects.GaussianBlurEffect;
import com.crashinvaders.vfx.effects.LevelsEffect;
import com.crashinvaders.vfx.effects.MotionBlurEffect;
import com.crashinvaders.vfx.effects.OldTvEffect;
import com.crashinvaders.vfx.effects.RadialBlurEffect;
import com.crashinvaders.vfx.effects.VignettingEffect;
import com.crashinvaders.vfx.effects.WaterDistortionEffect;
import com.crashinvaders.vfx.effects.ZoomEffect;
import com.crashinvaders.vfx.effects.util.MixEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.danvexteam.lunoscript_annotations.LunoClass;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.raptor.particles.ParticleSystem3DRuntime;
import org.catrobat.catroid.raptor.postprocessing.AutoLensFlareEffect;
import org.catrobat.catroid.raptor.postprocessing.CustomShaderAttribute;
import org.catrobat.catroid.raptor.postprocessing.DepthOfFieldEffect;
import org.catrobat.catroid.raptor.postprocessing.ExposureEffect;
import org.catrobat.catroid.raptor.postprocessing.EyeAdaptationManager;
import org.catrobat.catroid.raptor.postprocessing.GodRaysEffect;
import org.catrobat.catroid.raptor.postprocessing.HeightFogEffect;
import org.catrobat.catroid.raptor.postprocessing.LinearizeEffect;
import org.catrobat.catroid.raptor.postprocessing.SsaoEffect;
import org.catrobat.catroid.raptor.postprocessing.SsgiEffect;
import org.catrobat.catroid.raptor.postprocessing.SsrRayTracingEffect;
import org.catrobat.catroid.raptor.postprocessing.TonemappingEffect;
import org.catrobat.catroid.raptor.postprocessing.UniversalPBRShaderProvider;
import org.catrobat.catroid.raptor.postprocessing.VolumetricFogEffect;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.utils.ModelPathProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class OptimizedBloomEffect extends BloomEffect {
    private final float scaleFactor;

    public OptimizedBloomEffect(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void resize(int width, int height) {
        super.resize((int)(width / scaleFactor), (int)(height / scaleFactor));
    }
}

class OptimizedGaussianBlurEffect extends GaussianBlurEffect {
    private final float scaleFactor;

    public OptimizedGaussianBlurEffect(float scaleFactor) {
        super(BlurType.Gaussian5x5);
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void resize(int width, int height) {

        super.resize((int)(width / scaleFactor), (int)(height / scaleFactor));
    }
}

class OptimizedRadialBlurEffect extends RadialBlurEffect {
    private final float scaleFactor;

    public OptimizedRadialBlurEffect(int passes, float scaleFactor) {
        super(passes);
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void resize(int width, int height) {
        super.resize((int)(width / scaleFactor), (int)(height / scaleFactor));
    }
}

@LunoClass
public class ThreeDManager implements Disposable {

    public ModelBatch getWireframeBatch() { return wireframeBatch; }

    public void resize(int width, int height) {
        this.lastScreenWidth = width;
        this.lastScreenHeight = height;
        if (!initialized) return;

        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        if (renderScale < 1.0f || aspectMode != 0) {
            renderHeight = (int)(height * renderScale);
            if (aspectMode == 1) { // 4:3
                renderWidth = (int)(renderHeight * (4.0f / 3.0f));
                if (renderWidth > (int)(width * renderScale)) {
                    renderWidth = (int)(width * renderScale);
                    renderHeight = (int)(renderWidth * (3.0f / 4.0f));
                }
            } else if (aspectMode == 2) { // 16:9
                renderWidth = (int)(renderHeight * (16.0f / 9.0f));
                if (renderWidth > (int)(width * renderScale)) {
                    renderWidth = (int)(width * renderScale);
                    renderHeight = (int)(renderWidth * (9.0f / 16.0f));
                }
            } else if (aspectMode == 3) { // 1:1
                renderWidth = renderHeight;
                if (renderWidth > (int)(width * renderScale)) {
                    renderWidth = (int)(width * renderScale);
                    renderHeight = renderWidth;
                }
            } else {
                renderWidth = (int)(width * renderScale);
                renderHeight = (int)(height * renderScale);
            }

            camera.viewportWidth = renderWidth;
            camera.viewportHeight = renderHeight;
            camera.update();
        } else {
            renderWidth = width;
            renderHeight = height;
        }

        if (vfxManager != null) {
            vfxManager.resize(renderWidth, renderHeight);
        }
        if (blitCamera != null) {
            blitCamera.setToOrtho(false, width, height);
            blitCamera.update();
        }

        if (sceneFbo2 != null) sceneFbo2.dispose();
        sceneFbo2 = new FrameBuffer(Pixmap.Format.RGBA8888, renderWidth, renderHeight, true);

        sceneFboRegion.setRegion(sceneFbo2.getColorBufferTexture());
        sceneFboRegion.flip(false, true);

        if (depthFbo != null) depthFbo.dispose();
        depthFbo = new FrameBuffer(Pixmap.Format.RGBA8888, renderWidth, renderHeight, true);
        depthFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        if (materialFbo != null) materialFbo.dispose();
        materialFbo = new FrameBuffer(Pixmap.Format.RGBA8888, renderWidth / 2, renderHeight / 2, true);

        if (currentConfig != null) {
            updatePostProcessing(currentConfig);
        }

        if (realisticMode && sceneManager != null) {
            sceneManager.updateViewport(renderWidth, renderHeight);
        }
    }

    public enum PhysicsState {
        NONE,
        STATIC,
        DYNAMIC,
        MESH_STATIC,
        KINEMATIC
    }

    public enum PhysicsShape {
        BOX,
        SPHERE,
        CAPSULE
    }

    private static class AudioAsset {
        public Sound sound = null;
        public Music music = null;
        public final String filePath;

        public AudioAsset(FileHandle fileHandle, boolean asMusic) {
            this.filePath = fileHandle.path();
            if (asMusic) {
                music = Gdx.audio.newMusic(fileHandle);
            } else {
                sound = Gdx.audio.newSound(fileHandle);
            }
        }
        public void dispose() {
            if (sound != null) sound.dispose();
            if (music != null) music.dispose();
        }
    }

    private static class RaySensor {
        String rayName;
        String attachedObjectId;
        Vector3 localOffset = new Vector3();
        Vector3 localDirection = new Vector3();
        float distance;
    }

    private final Array<RaySensor> continuousRaySensors = new Array<>();

    private final Map<String, AudioAsset> loadedAudioAssets = new HashMap<>();
    private final List<PlayableAudio> active3DSounds = new ArrayList<>();

    private float globalSoundVolume = 1.0f;
    private static final float MAX_HEARING_DISTANCE = 250f;

    private final Vector3 cameraRight = new Vector3();
    private final Vector3 soundToListener = new Vector3();

    private final boolean debugEnabled = false;
    private static final boolean LOG_THREED_MANAGER_DEBUG = false;

    private FrameBuffer depthFbo;
    private ModelBatch depthBatch;
    private ShaderProgram depthShader;

    private static class RayCastResult {
        public boolean hasHit = false;
        public String hitObjectId = "";
        public float hitDistance = -1.0f;

        public final Vector3 hitPoint = new Vector3();
        public final Vector3 hitNormal = new Vector3();
    }

    public com.badlogic.gdx.graphics.Color skyColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0);

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    private boolean editorMode = false;

    private Map<String, Model> loadedModels = new HashMap<>();

    private Map<String, Texture> loadedTextures = new HashMap<>();
    private IBLBuilderCompat iblBuilderCompat;

    private Map<String, ModelInstance> sceneObjects = new HashMap<>();

    private Map<String, DirectionalLight> directionalLights = new HashMap<>();

    private final BoundingBox bounds1 = new BoundingBox();
    private final BoundingBox bounds2 = new BoundingBox();

    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    private btBroadphaseInterface broadphase;
    private btConstraintSolver solver;
    private btDiscreteDynamicsWorld dynamicsWorld;
    private com.badlogic.gdx.graphics.g3d.utils.ModelBuilder modelBuilder;
    private CollisionCallback collisionCallback;
    private Map<String, btRigidBody> physicsBodies = new HashMap<>();
    private Map<String, RayCastResult> rayCastResults = new HashMap<>();
    private ShaderProvider defaultShaderProvider;
    private ShaderProvider customShaderProvider;

    private final Map<String, Object> customUniforms = new HashMap<>();
    private float time = 0f;

    private boolean realisticMode = false;
    private net.mgsx.gltf.scene3d.scene.SceneManager sceneManager;
    private com.badlogic.gdx.graphics.Texture brdfLUT;
    private com.badlogic.gdx.graphics.Cubemap diffuseCubemap;
    private com.badlogic.gdx.graphics.Cubemap specularCubemap;
    private net.mgsx.gltf.scene3d.lights.DirectionalLightEx pbrLight;

    private DebugDrawer debugDrawer;

    private final Map<String, net.mgsx.gltf.scene3d.lights.PointLightEx> pointLights = new HashMap<>();
    private final Map<String, net.mgsx.gltf.scene3d.lights.SpotLightEx> spotLights = new HashMap<>();
    private final java.util.Set<String> gltfObjectIds = new java.util.HashSet<>();
    private final Map<String, AnimationController> animationControllers = new HashMap<>();
    private net.mgsx.gltf.scene3d.scene.SceneSkybox skybox;
    private Cubemap skyboxCubemap;
    private PanoramicConverter panoramicConverter;
    private ModelInstance gridInstance;
    private final Map<String, ModelInstance> editorProxies = new HashMap<>();
    private Model lightProxyModel;
    private ModelBatch wireframeBatch;
    private Model wireframeBoxModel;
    private Model wireframeSphereModel;
    private Model wireframeCylinderModel;
    private Model cameraProxyModel;
    public String cameraTargetId = null;
    private final Vector3 cameraOffset = new Vector3();
    private float cameraDistance = 10.0f;
    private float cameraPitch = 20.0f;
    private float cameraYaw = 0.0f;

    private Map<String, btTypedConstraint> physicsConstraints = new HashMap<>();

    private final Set<String> inactiveRenderObjects = new HashSet<>();
    private final Map<String, btRigidBody> inactivePhysicsBodies = new HashMap<>();
    private final Map<String, net.mgsx.gltf.scene3d.scene.Scene> inactivePbrScenes = new HashMap<>();

    private SceneSettings currentSettings;

    private final Vector3 tmpPos = new Vector3();
    private final Quaternion tmpRot = new Quaternion();
    private final Vector3 tmpScale = new Vector3();

    private VfxManager vfxManager;
    private FrameBuffer sceneFbo2;

    public static class SceneSettings {
        public int numPointLights = 5;
        public int numSpotLights = 2;
        public int numDirectionalLights = 1;
        public int numBones = 110;
    }

    private SceneManager manager;

    public boolean postprocessingEnabled = false;

    private ParticleSystem particleSystem;

    public final Map<String, ParticleEffect> activeParticleEffects = new HashMap<>();
    private Texture defaultParticleTexture;

    private final List<ParticleEffect> effectsNormal = new ArrayList<>();
    private final List<ParticleEffect> effectsAdditive = new ArrayList<>();

    private ModelBatch particleModelBatch;
    private Model particleProxyModel;
    private com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch batchNormal;
    private com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch batchAdditive;

    private final Map<String, BillboardParticleBatch> managedBatches = new HashMap<>();
    private EyeAdaptationManager eyeAdaptationManager;
    private ExposureEffect exposureEffect;
    private TonemappingEffect tonemappingEffect;
    private LinearizeEffect linearizeEffect;
    private final List<com.crashinvaders.vfx.effects.VfxEffect> userEffects = new ArrayList<>();
    private final java.util.Set<String> hiddenSpawnIds = new java.util.HashSet<>();

    private SsrRayTracingEffect rayTracingEffect;
    private SsaoEffect ssaoEffect;
    private HeightFogEffect heightFogEffect;
    private DepthOfFieldEffect dofEffect;
    private GodRaysEffect godRaysEffect;
    private VolumetricFogEffect volumetricFogEffect;
    private SsgiEffect ssgiEffect;

    private ShaderProgram fsrShader;
    private PostProcessingData.Upscaler currentUpscaler;

    private DepthShader depthShaderWrapper;

    private FrameBuffer materialFbo;
    private ModelBatch materialBatch;

    private final Map<String, AIComponent> activeAIs = new HashMap<>();
    private final Map<String, Vector3> aiLastKnownPositions = new HashMap<>();
    private final Map<String, Boolean> aiHasLostTarget = new HashMap<>();
    private final Map<String, Vector3> aiSmoothedDirection = new HashMap<>();

    private float shakeIntensity = 0f;
    private float shakeDuration = 0f;
    private float shakeTimer = 0f;
    private final Vector3 currentShakeOffset = new Vector3();


    private boolean touchRotationEnabled = false;
    private float sensitivity = 0.2f;
    private boolean blockByUI = true;
    private float rotationAreaX = 0, rotationAreaY = 0, rotationAreaW = 100, rotationAreaH = 100;

    private int lastTouchX, lastTouchY;
    private int activePointerId = -1;

    private final java.nio.ByteBuffer dofPixelsBuffer = com.badlogic.gdx.utils.BufferUtils.newByteBuffer(16);

    private final Vector3 tpTargetPos = new Vector3();
    private final Vector3 tpLookAt = new Vector3();
    private final Quaternion tpRot = new Quaternion();
    private final Quaternion tpPitchRot = new Quaternion();
    private final Vector3 tpPosOffset = new Vector3();

    private final Vector3 slCenter = new Vector3();
    private final Vector3 slForward = new Vector3();

    private int maxActivePointLights = 15;
    private final List<net.mgsx.gltf.scene3d.lights.PointLightEx> sortedPointLights = new ArrayList<>();

    public String cameraTrackTargetId = null;
    public int cameraTrackMode = 0; // 0: Off, 1: Pos, 2: Rot, 3: Both
    public final Vector3 cameraTrackPosOffset = new Vector3();
    public final Quaternion cameraTrackRotOffset = new Quaternion();

    private float renderScale = 1.0f;
    private int aspectMode = 0; // 0: Auto, 1: 4:3, 2: 16:9, 3: 1:1
    private int lastScreenWidth;
    private int lastScreenHeight;
    private int renderWidth;
    private int renderHeight;

    private final Map<String, net.mgsx.gltf.scene3d.scene.SceneAsset> loadedGltfAssets = new HashMap<>();

    private net.mgsx.gltf.scene3d.scene.Scene gridScene;

    private boolean disposed = false;

    public boolean isDisposed() {
        return disposed;
    }

    private boolean initialized = false;

    public boolean isInitialized() {
        return initialized;
    }

    public void ensureInitialized() {
        if (!initialized && !disposed) {
            init();
        }
    }

    public void init() {
        init(new SceneSettings());
    }

    public void init(SceneSettings settings) {
        this.currentSettings = settings;

        modelBuilder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
        defaultShaderProvider = new DefaultShaderProvider();
        modelBatch = new ModelBatch(defaultShaderProvider);
        environment = new Environment();
        setAmbientLight(0.4f, 0.4f, 0.4f);

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setCameraPosition(100f, 100f, 100f);
        cameraLookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 2500f;

        Bullet.init();
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -9.81f, 0));
        dynamicsWorld.getSolverInfo().setNumIterations(20);

        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
        dynamicsWorld.setDebugDrawer(debugDrawer);
        collisionCallback = new CollisionCallback();

        net.mgsx.gltf.scene3d.shaders.PBRShaderConfig pbrConfig = net.mgsx.gltf.scene3d.shaders.PBRShaderProvider.createDefaultConfig();
        pbrConfig.numBones = settings.numBones;
        pbrConfig.numPointLights = settings.numPointLights;
        pbrConfig.numSpotLights = settings.numSpotLights;
        pbrConfig.numDirectionalLights = settings.numDirectionalLights;

        PBRShaderProvider prov = new UniversalPBRShaderProvider(pbrConfig);

        DefaultShader.Config config = prov.config;
        config.numPointLights = settings.numPointLights;
        config.numSpotLights = settings.numSpotLights;
        config.numDirectionalLights = settings.numDirectionalLights;
        config.numBones = settings.numBones;

        sceneManager = new net.mgsx.gltf.scene3d.scene.SceneManager(prov, PBRShaderProvider.createDefaultDepth(config.numBones));
        sceneManager.setCamera(camera);
        net.mgsx.gltf.scene3d.lights.DirectionalShadowLight shadowLight = new net.mgsx.gltf.scene3d.lights.DirectionalShadowLight(2048, 2048);
        shadowLight.direction.set(1, -1.5f, 1).nor();
        shadowLight.color.set(com.badlogic.gdx.graphics.Color.WHITE);
        shadowLight.intensity = 5.0f;
        shadowLight.getCamera().far = 1000f;
        sceneManager.environment.add(shadowLight);
        pbrLight = shadowLight;
        net.mgsx.gltf.scene3d.utils.IBLBuilder iblBuilder = net.mgsx.gltf.scene3d.utils.IBLBuilder.createOutdoor(pbrLight);

        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();
        brdfLUT = new com.badlogic.gdx.graphics.Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute(net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute.DiffuseEnv, diffuseCubemap));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute(net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute.SpecularEnv, specularCubemap));

        lightProxyModel = modelBuilder.createSphere(0.25f, 0.25f, 0.25f, 8, 8,
                new com.badlogic.gdx.graphics.g3d.Material(ColorAttribute.createDiffuse(com.badlogic.gdx.graphics.Color.YELLOW)),
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal);
        particleProxyModel = modelBuilder.createSphere(0.25f, 0.25f, 0.25f, 8, 8,
                new com.badlogic.gdx.graphics.g3d.Material(ColorAttribute.createDiffuse(com.badlogic.gdx.graphics.Color.MAGENTA)),
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal);

        Material camMat = new Material(ColorAttribute.createDiffuse(Color.CYAN));
        Model box = modelBuilder.createBox(0.4f, 0.4f, 0.4f, camMat, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        Model cone = modelBuilder.createCone(0.4f, 0.5f, 0.4f, 4, camMat, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("box", box.meshes.get(0), GL20.GL_TRIANGLES, camMat);
        Node coneNode = mb.node();
        coneNode.id = "cone";
        coneNode.rotation.set(Vector3.X, 90);
        coneNode.translation.set(0, 0, -0.2f);
        mb.part("cone", cone.meshes.get(0), GL20.GL_TRIANGLES, camMat);
        cameraProxyModel = mb.end();

        wireframeBatch = new ModelBatch();
        Material wireframeMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        long usage = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        wireframeBoxModel = modelBuilder.createBox(1f, 1f, 1f, GL20.GL_LINES, wireframeMaterial, usage);
        wireframeSphereModel = modelBuilder.createSphere(1f, 1f, 1f, 16, 12, GL20.GL_LINES, wireframeMaterial, usage);
        wireframeCylinderModel = modelBuilder.createCylinder(1f, 2f, 1f, 16, GL20.GL_LINES, wireframeMaterial, usage);

        vfxManager = new VfxManager(Pixmap.Format.RGBA8888);
        sceneFboRegion = new TextureRegion();
        blitBatch = new SpriteBatch();
        blitCamera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        String depthVSH = "attribute vec3 a_position;\n" +
                "attribute vec3 a_normal;\n" +
                "#ifdef boneWeight0Flag\nattribute vec2 a_boneWeight0;\n#endif\n" +
                "#ifdef boneWeight1Flag\nattribute vec2 a_boneWeight1;\n#endif\n" +
                "#ifdef boneWeight2Flag\nattribute vec2 a_boneWeight2;\n#endif\n" +
                "#ifdef boneWeight3Flag\nattribute vec2 a_boneWeight3;\n#endif\n" +
                "#ifdef numBones\nuniform mat4 u_bones[numBones];\n#endif\n" +
                "uniform mat4 u_projViewTrans;\n" +
                "uniform mat4 u_worldTrans;\n" +
                "uniform mat4 u_viewTrans;\n" +
                "varying vec3 v_normal;\n" +
                "varying float v_viewZ;\n" +
                "void main() {\n" +
                "    mat4 skinning = mat4(1.0);\n" +
                "    #ifdef numBones\n" +
                "    skinning = (a_boneWeight0.y * u_bones[int(a_boneWeight0.x)])\n" +
                "               + (a_boneWeight1.y * u_bones[int(a_boneWeight1.x)])\n" +
                "               + (a_boneWeight2.y * u_bones[int(a_boneWeight2.x)])\n" +
                "               + (a_boneWeight3.y * u_bones[int(a_boneWeight3.x)]);\n" +
                "    #endif\n" +
                "    vec4 wpos = u_worldTrans * skinning * vec4(a_position, 1.0);\n" +
                "    vec4 vpos = u_viewTrans * wpos;\n" +
                "    v_viewZ = -vpos.z;\n" +
                "    v_normal = normalize(mat3(u_worldTrans) * a_normal);\n" +
                "    gl_Position = u_projViewTrans * wpos;\n" +
                "}";

        String depthFSH = "#ifdef GL_ES\nprecision highp float;\n#endif\n" +
                "varying vec3 v_normal;\n" +
                "varying float v_viewZ;\n" +
                "uniform float u_farPlane;\n" +
                "uniform float u_objReflectivity;\n" +
                "vec2 packNormal(vec3 v) {\n" +
                "    vec2 p = v.xy * (1.0 / (abs(v.x) + abs(v.y) + abs(v.z)));\n" +
                "    if (v.z <= 0.0) p = (1.0 - abs(p.yx)) * vec2(v.x >= 0.0 ? 1.0 : -1.0, v.y >= 0.0 ? 1.0 : -1.0);\n" +
                "    return p * 0.5 + 0.5;\n" +
                "}\n" +
                "vec2 packDepth(float d) {\n" +
                "    float n = clamp(d / u_farPlane, 0.0, 1.0);\n" +
                "    vec2 enc = vec2(1.0, 255.0) * n;\n" +
                "    enc = fract(enc);\n" +
                "    enc.x -= enc.y * (1.0/255.0);\n" +
                "    return enc;\n" +
                "}\n" +
                "void main() {\n" +
                "    vec2 d = packDepth(v_viewZ);\n" +
                "    gl_FragColor = vec4(d.x, d.y, packNormal(normalize(v_normal)).x, u_objReflectivity * 0.00001 + packNormal(normalize(v_normal)).y);\n" +
                "}";

        depthShader = new ShaderProgram(depthVSH, depthFSH);
        if (!depthShader.isCompiled()) {
            Log.e("3DManager", "Depth Shader Error: " + depthShader.getLog());
        }

        depthShaderWrapper = new DepthShader(depthShader);

        ShaderProvider depthShaderProvider = new DefaultShaderProvider() {
            @Override
            protected Shader createShader(Renderable renderable) {
                return depthShaderWrapper;
            }
        };

        depthBatch = new ModelBatch(depthShaderProvider);

        materialBatch = new ModelBatch(new MaterialShaderProvider());


        String fsrVsh = "attribute vec4 a_position;\n" +
                "attribute vec4 a_color;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    v_color = a_color;\n" +
                "    v_texCoords = a_texCoord0;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}";

        String fsrFsh = "#ifdef GL_ES\nprecision highp float;\n#endif\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform vec2 u_texelSize;\n" +
                "uniform float u_sharpness;\n" +
                "void main() {\n" +


                "    vec3 e = texture2D(u_texture, v_texCoords).rgb;\n" +
                "    vec3 b = texture2D(u_texture, v_texCoords + vec2(0.0, -u_texelSize.y)).rgb;\n" +
                "    vec3 d = texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x, 0.0)).rgb;\n" +
                "    vec3 f = texture2D(u_texture, v_texCoords + vec2(u_texelSize.x, 0.0)).rgb;\n" +
                "    vec3 h = texture2D(u_texture, v_texCoords + vec2(0.0, u_texelSize.y)).rgb;\n" +
                "    \n" +
                "    vec3 minRGB = min(min(min(b, d), min(e, f)), h);\n" +
                "    vec3 maxRGB = max(max(max(b, d), max(e, f)), h);\n" +
                "    vec3 contrast = maxRGB - minRGB;\n" +
                "    \n" +
                "    vec3 w = u_sharpness * (1.0 - contrast);\n" +
                "    w = clamp(w, 0.0, 1.0);\n" +
                "    \n" +
                "    vec3 finalColor = (b + d + f + h) * w + e;\n" +
                "    finalColor = finalColor / (4.0 * w + 1.0);\n" +
                "    gl_FragColor = vec4(finalColor, 1.0) * v_color;\n" +
                "}";

        fsrShader = new ShaderProgram(fsrVsh, fsrFsh);
        if (!fsrShader.isCompiled()) {
            Gdx.app.error("FSR Shader", fsrShader.getLog());
        }

        eyeAdaptationManager = new EyeAdaptationManager();

        currentConfig.isActive = false;
        currentConfig.effects.clear();
        currentConfig.qualityScale = 1.0f;
        updatePostProcessing(currentConfig);

        panoramicConverter = new PanoramicConverter();
        iblBuilderCompat = new IBLBuilderCompat();

        createDefaultParticleTexture();

        contactListCallback = new NameAccumulatingContactCallback();

        initialized = true;
    }

    public void setObjectCustomShader(String objectId, String vertexCode, String fragmentCode) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            try {
                ShaderProgram program = new ShaderProgram(vertexCode, fragmentCode);
                if (!program.isCompiled()) {
                    throw new Exception("Shader compile error: " + program.getLog());
                }

                CustomShaderAttribute shaderAttr = new CustomShaderAttribute(program);
                for (com.badlogic.gdx.graphics.g3d.Material mat : instance.materials) {
                    mat.set(shaderAttr);
                }
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Failed to compile object shader: " + e.getMessage());
            }
        });
    }

    public void setObjectShaderUniform(String objectId, String name, float v1, float v2, float v3, int paramCount) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        CustomShaderAttribute attr = null;
        for (com.badlogic.gdx.graphics.g3d.Material mat : instance.materials) {
            if (mat.has(CustomShaderAttribute.Type)) {
                attr = (CustomShaderAttribute) mat.get(CustomShaderAttribute.Type);
                break;
            }
        }

        if (attr == null) return;

        final CustomShaderAttribute finalAttr = attr;
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            if (paramCount == 1) {
                finalAttr.uniforms.put("u_" + name, v1);
            } else if (paramCount == 2) {
                finalAttr.uniforms.put("u_" + name, new com.badlogic.gdx.math.Vector2(v1, v2));
            } else if (paramCount == 3) {
                finalAttr.uniforms.put("u_" + name, new com.badlogic.gdx.math.Vector3(v1, v2, v3));
            }
        });
    }

    public void setFreeCamera() {
        this.cameraTargetId = null;
        this.cameraTrackMode = 0;
        this.cameraTrackTargetId = null;
    }

    public void setThirdPersonCamera(String targetObjectId, float distance, float height, float pitch) {
        ModelInstance target = sceneObjects.get(targetObjectId);
        if (target == null) {
            Gdx.app.error("3DManager", "Camera target object not found: " + targetObjectId);
            this.cameraTargetId = null;
            return;
        }

        this.cameraTrackMode = 0;

        this.cameraTargetId = targetObjectId;
        this.cameraDistance = distance;
        this.cameraOffset.set(0, height, 0);
        this.cameraPitch = pitch;
        this.cameraYaw = target.transform.getRotation(new Quaternion()).getYaw();
    }

    public void setCameraTracking(String targetId, int mode, float px, float py, float pz, float yaw, float pitch, float roll) {
        this.cameraTrackMode = mode;
        if (mode == 0) {
            this.cameraTrackTargetId = null;
        } else {
            this.cameraTargetId = null;

            this.cameraTrackTargetId = targetId;
            this.cameraTrackPosOffset.set(px, py, pz);
            this.cameraTrackRotOffset.setEulerAngles(yaw, pitch, roll);
        }
    }

    private void updateCameraTracking() {
        if (cameraTrackTargetId == null || cameraTrackMode == 0) return;

        ModelInstance target = sceneObjects.get(cameraTrackTargetId);
        if (target == null) target = editorProxies.get(cameraTrackTargetId);
        if (target == null) return;

        if (cameraTrackMode == 1 || cameraTrackMode == 3) {
            target.transform.getTranslation(tmpPos);
            tmpPos.add(cameraTrackPosOffset);
            camera.position.set(tmpPos);
        }

        if (cameraTrackMode == 2 || cameraTrackMode == 3) {
            target.transform.getRotation(tmpRot, true);
            tmpRot.mul(cameraTrackRotOffset);
            camera.direction.set(0, 0, -1);
            tmpRot.transform(camera.direction);
            camera.up.set(0, 1, 0);
            tmpRot.transform(camera.up);
        }

        camera.update();
    }

    public Map<String, net.mgsx.gltf.scene3d.lights.PointLightEx> getPointLights() { return pointLights; }
    public Map<String, net.mgsx.gltf.scene3d.lights.SpotLightEx> getSpotLights() { return spotLights; }

    public static class CameraAttachment {
        public String objectId;
        public Matrix4 localOffsetMat = new Matrix4();

        public CameraAttachment(String id, Matrix4 offset) {
            this.objectId = id;
            this.localOffsetMat.set(offset);
        }
    }
    public final List<CameraAttachment> cameraAttachments = new ArrayList<>();

    public void attachObjectToCamera(String objectId) {
        detachObjectFromCamera(objectId);

        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) {
            instance = editorProxies.get(objectId);
        }
        if (instance == null) return;

        Matrix4 camWorld = new Matrix4(camera.view).inv();
        Matrix4 offset = new Matrix4(camWorld).inv().mul(instance.transform);
        cameraAttachments.add(new CameraAttachment(objectId, offset));
    }

    public void detachObjectFromCamera(String objectId) {
        cameraAttachments.removeIf(a -> a.objectId.equals(objectId));
    }

    private void updateCameraAttachments() {
        if (cameraAttachments.isEmpty()) return;

        Matrix4 camWorld = new Matrix4(camera.view).inv();
        for (CameraAttachment att : cameraAttachments) {
            ModelInstance instance = sceneObjects.get(att.objectId);
            if (instance == null) {
                instance = editorProxies.get(att.objectId);
            }
            if (instance == null) continue;

            tmpMat1.set(camWorld).mul(att.localOffsetMat);
            instance.transform.set(tmpMat1);
            setWorldTransform(att.objectId, tmpMat1);
        }
    }

    public void setMaxActivePointLights(int maxLights) {
        this.maxActivePointLights = Math.max(0, maxLights);
    }

    private final Matrix4 tmpMat1 = new Matrix4();

    private void updateActivePointLights() {
        if (sceneManager == null || pointLights.isEmpty()) return;

        for (net.mgsx.gltf.scene3d.lights.PointLightEx pl : pointLights.values()) {
            sceneManager.environment.remove(pl);
        }

        if (maxActivePointLights <= 0) return;

        sortedPointLights.clear();
        sortedPointLights.addAll(pointLights.values());

        final Vector3 camPos = camera.position;
        sortedPointLights.sort((l1, l2) -> {
            float d1 = camPos.dst2(l1.position);
            float d2 = camPos.dst2(l2.position);
            return Float.compare(d1, d2);
        });

        int lightsToEnable = Math.min(sortedPointLights.size(), maxActivePointLights);
        for (int i = 0; i < lightsToEnable; i++) {
            sceneManager.environment.add(sortedPointLights.get(i));
        }
    }

    private final Map<String, ParticleSystem3DRuntime> activeParticleRuntimes3D = new HashMap<>();

    public void updateParticleEffect3D(String objectId, ParticleSystem3DComponent data, Matrix4 transform) {
        ParticleSystem3DRuntime existing = activeParticleRuntimes3D.get(objectId);

        if (existing != null) {
            existing.reconfigure(data);
            existing.setTransform(transform);


            Texture tex = defaultParticleTexture;
            if (data.renderer.texturePath != null && !data.renderer.texturePath.isEmpty()) {
                Texture custom = loadTexture(data.renderer.texturePath);
                if (custom != null) tex = custom;
            }
            existing.setTexture(tex);
            return;
        }


        Texture tex = defaultParticleTexture;
        if (data.renderer.texturePath != null && !data.renderer.texturePath.isEmpty()) {
            Texture custom = loadTexture(data.renderer.texturePath);
            if (custom != null) tex = custom;
        }

        ParticleSystem3DRuntime runtime = new ParticleSystem3DRuntime(data, objectId, camera, this);
        runtime.setTexture(tex);
        runtime.setTransform(transform);

        runtime.setSubEmitterCallback((subEmitterObjectId, position, velocity) -> {
            ParticleSystem3DRuntime subRuntime = activeParticleRuntimes3D.get(subEmitterObjectId);
            if (subRuntime == null) return;


            int count = 0;
            for (ParticleSystem3DComponent.SubEmitterEntry entry : data.subEmitters.entries) {
                if (entry.subEmitterObjectId.equals(subEmitterObjectId)) {
                    count = entry.emitCount;
                    break;
                }
            }


            if (count <= 0) {
                ParticleSystem3DComponent subConfig = null;

                for (Map.Entry<String, ParticleSystem3DRuntime> e : activeParticleRuntimes3D.entrySet()) {
                    if (e.getKey().equals(subEmitterObjectId)) {
                        subConfig = e.getValue().getConfig();
                        break;
                    }
                }
                if (subConfig != null && subConfig.emission.enabled) {
                    count = (int) subConfig.emission.rateOverTime.evaluate(0);
                    if (count <= 0) count = 10;
                } else {
                    count = 10;
                }
            }

            subRuntime.emitBurstAt(position, count);
        });

        runtime.play();
        activeParticleRuntimes3D.put(objectId, runtime);
    }

    public void removeParticleEffect3D(String objectId) {
        ParticleSystem3DRuntime runtime = activeParticleRuntimes3D.remove(objectId);
        if (runtime != null) {
            runtime.dispose();
        }
    }

    public void updateParticleTransform3D(String objectId, Matrix4 transform) {
        ParticleSystem3DRuntime runtime = activeParticleRuntimes3D.get(objectId);
        if (runtime != null) {
            runtime.setTransform(transform);
        }
    }


    private void updateParticles3D(float delta) {
        for (ParticleSystem3DRuntime runtime : activeParticleRuntimes3D.values()) {
            runtime.setCamera(camera);
            runtime.update(delta);
        }
    }


    private void renderParticles3D() {
        if (activeParticleRuntimes3D.isEmpty()) return;

        for (ParticleSystem3DRuntime runtime : activeParticleRuntimes3D.values()) {
            runtime.render();
        }
    }

    private boolean useCSM = false;
    private net.mgsx.gltf.scene3d.scene.CascadeShadowMap csm;
    private float csmSplitFactor = 4f;

    public float getCsmSplitFactor() { return csmSplitFactor; }

    public boolean isCSMEnabled() { return useCSM; }

    public void attachRaySensor(String rayName, String objectId, float offX, float offY, float offZ, float dirX, float dirY, float dirZ, float distance) {
        continuousRaySensors.removeValue(getSensorByName(rayName), false);

        RaySensor sensor = new RaySensor();
        sensor.rayName = rayName;
        sensor.attachedObjectId = objectId;
        sensor.localOffset.set(offX, offY, offZ);
        sensor.localDirection.set(dirX, dirY, dirZ).nor();
        sensor.distance = distance;

        continuousRaySensors.add(sensor);
    }

    private RaySensor getSensorByName(String name) {
        for (RaySensor s : continuousRaySensors) if (s.rayName.equals(name)) return s;
        return null;
    }

    private final Vector3 tmpSensorPos = new Vector3();
    private final Vector3 tmpSensorDir = new Vector3();
    private final Vector3 tmpSensorEnd = new Vector3();

    private void updateContinuousSensors() {
        for (RaySensor sensor : continuousRaySensors) {
            ModelInstance instance = sceneObjects.get(sensor.attachedObjectId);
            if (instance == null) continue;

            Matrix4 transform = instance.transform;

            tmpSensorPos.set(sensor.localOffset).mul(transform);

            tmpSensorDir.set(sensor.localDirection).rot(transform).nor();

            tmpSensorEnd.set(tmpSensorPos).add(tmpSensorDir.scl(sensor.distance));

            com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback callback =
                    new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(tmpSensorPos, tmpSensorEnd);

            dynamicsWorld.rayTest(tmpSensorPos, tmpSensorEnd, callback);

            RayCastResult result = rayCastResults.get(sensor.rayName);
            if (result == null) {
                result = new RayCastResult();
                rayCastResults.put(sensor.rayName, result);
            }

            if (callback.hasHit()) {
                result.hasHit = true;
                callback.getHitPointWorld(result.hitPoint);
                callback.getHitNormalWorld(result.hitNormal);
                result.hitDistance = tmpSensorPos.dst(result.hitPoint);

                com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = callback.getCollisionObject();
                result.hitObjectId = "";
                for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                    if (entry.getValue().equals(hitObject)) {
                        result.hitObjectId = entry.getKey();
                        break;
                    }
                }
            } else {
                result.hasHit = false;
                result.hitObjectId = "";
                result.hitDistance = -1.0f;
            }

            callback.dispose();
        }
    }

    public void setSoundInstanceVolume(String instanceName, float volume) {
        for (PlayableAudio audio : active3DSounds) {
            if (instanceName.equals(audio.getInstanceName())) {
                audio.setBaseVolume(volume / 100f);
            }
        }
    }

    public void setSoundInstancePitch(String instanceName, float pitch) {
        for (PlayableAudio audio : active3DSounds) {
            if (instanceName.equals(audio.getInstanceName())) {
                audio.setPitch(pitch);
            }
        }
    }

    public void setAnimationSpeed(String objectId, float speed) {
        com.badlogic.gdx.graphics.g3d.utils.AnimationController controller = animationControllers.get(objectId);
        if (controller != null && controller.current != null) {
            controller.current.speed = speed;
        }
    }

    public void setParticleEmissionRate(String objectId, float rate) {
        com.badlogic.gdx.graphics.g3d.particles.ParticleEffect effect = activeParticleEffects.get(objectId);
        if (effect != null) {
            for (com.badlogic.gdx.graphics.g3d.particles.ParticleController controller : effect.getControllers()) {
                if (controller.emitter instanceof com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter) {
                    com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter emitter =
                            (com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter) controller.emitter;
                    emitter.getEmission().setHigh(rate);
                    if (rate <= 0.01f) emitter.getEmission().setLow(0f);
                }
            }
        }
    }

    public void rotateCameraRelative(float pitch, float yaw, float roll) {
        if (yaw != 0) camera.rotate(Vector3.Y, yaw);

        if (pitch != 0) {
            tmpVec.set(camera.direction).crs(camera.up).nor();
            camera.rotate(tmpVec, pitch);
        }

        if (roll != 0) camera.rotate(camera.direction, roll);

        camera.update();
    }

    public void configureTouchRotation(boolean enabled, float sensitivity, boolean blockByUI,
                                       float x, float y, float w, float h) {
        this.touchRotationEnabled = enabled;
        this.sensitivity = sensitivity;
        this.blockByUI = blockByUI;
        this.rotationAreaX = x;
        this.rotationAreaY = y;
        this.rotationAreaW = w;
        this.rotationAreaH = h;
    }

    public boolean handleTouchDown(int screenX, int screenY, int pointer, Stage uiStage, Stage stage2d) {
        if (!initialized) return false;
        if (!touchRotationEnabled || activePointerId != -1) return false;

        float px = (rotationAreaX / 100f) * Gdx.graphics.getWidth();
        float py = (rotationAreaY / 100f) * Gdx.graphics.getHeight();
        float pw = (rotationAreaW / 100f) * Gdx.graphics.getWidth();
        float ph = (rotationAreaH / 100f) * Gdx.graphics.getHeight();
        float invY = Gdx.graphics.getHeight() - screenY;

        if (screenX < px || screenX > px + pw || invY < py || invY > py + ph) return false;

        if (blockByUI) {
            com.badlogic.gdx.math.Vector2 uiCoords = uiStage.screenToStageCoordinates(new com.badlogic.gdx.math.Vector2(screenX, screenY));
            Actor hitUI = uiStage.hit(uiCoords.x, uiCoords.y, true);

            if (hitUI != null && hitUI != uiStage.getRoot()) {
                if (hitUI.getListeners().size > 0) return false;
            }

            com.badlogic.gdx.math.Vector2 s2dCoords = stage2d.screenToStageCoordinates(new com.badlogic.gdx.math.Vector2(screenX, screenY));
            Actor hit2d = stage2d.hit(s2dCoords.x, s2dCoords.y, true);

            if (hit2d != null && hit2d != stage2d.getRoot()) {
                if (hit2d.getListeners().size > 0) return false;
            }
        }


        activePointerId = pointer;
        lastTouchX = screenX;
        lastTouchY = screenY;

        return false;
    }

    public void handleTouchDragged(int screenX, int screenY, int pointer) {
        if (activePointerId == pointer) {
            float deltaX = screenX - lastTouchX;
            float deltaY = screenY - lastTouchY;

            addCameraRotation(-deltaX * sensitivity, -deltaY * sensitivity);

            lastTouchX = screenX;
            lastTouchY = screenY;
        }
    }

    public void handleTouchUp(int pointer) {
        if (activePointerId == pointer) {
            activePointerId = -1;
        }
    }

    public void setCameraFov(float fov) {
        camera.fieldOfView = fov;
        camera.update();
    }

    public void startCameraShake(float intensity, float duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = duration;
        this.shakeTimer = duration;
    }

    private void applyCameraEffects(float delta) {
        if (shakeTimer > 0) {
            float currentPower = shakeIntensity * (shakeTimer / shakeDuration);

            currentShakeOffset.set(
                    (float)(Math.random() - 0.5f) * 2 * currentPower,
                    (float)(Math.random() - 0.5f) * 2 * currentPower,
                    (float)(Math.random() - 0.5f) * 2 * currentPower
            );

            camera.position.add(currentShakeOffset);
            shakeTimer -= delta;
            camera.update();
        }
    }

    private final Map<String, Quaternion> modelRotationOffsets = new HashMap<>();

    private final Map<String, Array<Vector3>> targetTrails = new HashMap<>();
    private float trailTimer = 0;

    public void setAI(String objectId, AIComponent settings) {
        if (settings == null || settings.mode == AIComponent.Mode.OFF) {
            activeAIs.remove(objectId);
            modelRotationOffsets.remove(objectId);
            return;
        }
        activeAIs.put(objectId, settings);
    }

    private final Vector3 aiCurrentPos = new Vector3();
    private final Vector3 aiFinalMoveTarget = new Vector3();
    private final Vector3 aiRealTargetPos = new Vector3();
    private final Vector3 aiDesiredDir = new Vector3();
    private final Vector3 aiBestMoveDir = new Vector3();
    private final Vector3 aiRayDir = new Vector3();
    private final Vector3 aiStepPos = new Vector3();

    private static class DeadEndZone {
        public final Vector3 position;
        public final float radius;

        public DeadEndZone(Vector3 position, float radius) {
            this.position = position;
            this.radius = radius;
        }
    }

    private final Map<String, Array<Vector3>> aiPathHistory = new HashMap<>();
    private final Map<String, Array<DeadEndZone>> aiDeadEnds = new HashMap<>();
    private final Map<String, Vector3> aiLastTargetPos = new HashMap<>();
    private final Map<String, Vector3> aiBacktrackTarget = new HashMap<>();
    private final Map<String, Integer> aiSlideDirs = new HashMap<>();

    private final Map<String, Float> aiCurrentYaws = new HashMap<>();

    private void updateAIProcessing(float delta) {
        if (editorMode) return;

        trailTimer += delta;
        if (trailTimer >= 0.5f) {
            trailTimer = 0;
            java.util.Set<String> trackedIds = new java.util.HashSet<>();
            for (AIComponent ai : activeAIs.values()) {
                if (ai.mode == AIComponent.Mode.FOLLOW && !ai.targetId.isEmpty()) trackedIds.add(ai.targetId);
            }
            for (String tId : trackedIds) {
                ModelInstance tInst = sceneObjects.get(tId);
                if (tInst != null) {
                    Array<Vector3> trail = targetTrails.computeIfAbsent(tId, k -> new Array<>());
                    tInst.transform.getTranslation(tmpPos);
                    if (trail.size == 0 || trail.peek().dst(tmpPos) > 2.0f) {
                        trail.add(new Vector3(tmpPos));
                        if (trail.size > 40) trail.removeIndex(0);
                    }
                }
            }
        }

        for (Map.Entry<String, AIComponent> entry : activeAIs.entrySet()) {
            String id = entry.getKey();
            AIComponent ai = entry.getValue();
            ModelInstance instance = sceneObjects.get(id);
            if (instance == null) continue;

            instance.transform.getTranslation(aiCurrentPos);

            if (ai.mode == AIComponent.Mode.FOLLOW && !ai.targetId.isEmpty()) {
                ModelInstance tInst = sceneObjects.get(ai.targetId);
                if (tInst == null) tInst = editorProxies.get(ai.targetId);
                if (tInst == null) continue;
                tInst.transform.getTranslation(aiRealTargetPos);

                float distToRealTarget = aiCurrentPos.dst(aiRealTargetPos);
                boolean withinVision = distToRealTarget <= ai.detectionRange;
                boolean hasLOS = false;

                if (withinVision && ai.detectionRange > 0.1f) {
                    aiDesiredDir.set(aiRealTargetPos).sub(aiCurrentPos).nor();

                    Vector3 rayEnd = new Vector3(aiCurrentPos).add(aiDesiredDir.x * distToRealTarget, aiDesiredDir.y * distToRealTarget, aiDesiredDir.z * distToRealTarget);
                    com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback losCallback =
                            new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(aiCurrentPos, rayEnd);

                    dynamicsWorld.rayTest(aiCurrentPos, rayEnd, losCallback);

                    if (losCallback.hasHit()) {
                        com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = losCallback.getCollisionObject();
                        String hitId = "";
                        for (Map.Entry<String, btRigidBody> pbEntry : physicsBodies.entrySet()) {
                            if (pbEntry.getValue().equals(hitObject)) {
                                hitId = pbEntry.getKey();
                                break;
                            }
                        }
                        if (hitId.equals(ai.targetId)) {
                            hasLOS = true;
                        } else {
                            Vector3 hitPoint = new Vector3();
                            losCallback.getHitPointWorld(hitPoint);
                            if (aiCurrentPos.dst(hitPoint) > distToRealTarget - 0.5f) {
                                hasLOS = true;
                            }
                        }
                    } else {
                        hasLOS = true;
                    }
                    losCallback.dispose();
                }

                if (hasLOS) {
                    aiFinalMoveTarget.set(aiRealTargetPos);
                    aiLastKnownPositions.put(id, new Vector3(aiRealTargetPos));
                    aiHasLostTarget.put(id, false);
                } else {
                    Vector3 lastKnown = aiLastKnownPositions.get(id);
                    boolean headingToLastKnown = false;

                    if (lastKnown != null) {
                        float distToLastKnown = aiCurrentPos.dst(lastKnown);
                        if (distToLastKnown > ai.stopDistance + 0.5f) {
                            aiFinalMoveTarget.set(lastKnown);
                            headingToLastKnown = true;
                        } else {
                            aiHasLostTarget.put(id, true);
                        }
                    }

                    if (!headingToLastKnown || aiHasLostTarget.getOrDefault(id, false)) {
                        Array<Vector3> trail = targetTrails.get(ai.targetId);
                        boolean pointFound = false;
                        if (trail != null) {
                            for (int i = trail.size - 1; i >= 0; i--) {
                                Vector3 breadcrumb = trail.get(i);
                                float distToBreadcrumb = aiCurrentPos.dst(breadcrumb);
                                if (distToBreadcrumb <= ai.detectionRange) {
                                    aiDesiredDir.set(breadcrumb).sub(aiCurrentPos).nor();

                                    Vector3 rayEnd = new Vector3(aiCurrentPos).add(aiDesiredDir.x * distToBreadcrumb, aiDesiredDir.y * distToBreadcrumb, aiDesiredDir.z * distToBreadcrumb);
                                    com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback breadcrumbCallback =
                                            new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(aiCurrentPos, rayEnd);

                                    dynamicsWorld.rayTest(aiCurrentPos, rayEnd, breadcrumbCallback);

                                    boolean pathClear = true;
                                    if (breadcrumbCallback.hasHit()) {
                                        Vector3 hitPoint = new Vector3();
                                        breadcrumbCallback.getHitPointWorld(hitPoint);
                                        if (aiCurrentPos.dst(hitPoint) < distToBreadcrumb - 0.5f) {
                                            pathClear = false;
                                        }
                                    }
                                    breadcrumbCallback.dispose();

                                    if (pathClear) {
                                        aiFinalMoveTarget.set(breadcrumb);
                                        pointFound = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!pointFound) {
                            ai.currentVelocity.lerp(Vector3.Zero, delta * 5f);
                            stopPhysicsVelocity(id);
                            continue;
                        }
                    }
                }
            } else {
                if (!ai.targetId.isEmpty()) {
                    ModelInstance tInst = sceneObjects.get(ai.targetId);
                    if (tInst == null) tInst = editorProxies.get(ai.targetId);

                    if (tInst != null) {
                        tInst.transform.getTranslation(ai.targetPos);
                    } else {
                        String trimmed = ai.targetId.trim();
                        String[] parts = trimmed.split("[,;\\s]+");
                        if (parts.length >= 3) {
                            try {
                                float x = Float.parseFloat(parts[0]);
                                float y = Float.parseFloat(parts[1]);
                                float z = Float.parseFloat(parts[2]);
                                ai.targetPos.set(x, y, z);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                aiFinalMoveTarget.set(ai.targetPos);
            }

            Vector3 lastTarget = aiLastTargetPos.get(id);
            if (lastTarget == null) {
                aiLastTargetPos.put(id, new Vector3(aiFinalMoveTarget));
            } else if (lastTarget.dst(aiFinalMoveTarget) > 15.0f) {
                aiDeadEnds.remove(id);
                aiPathHistory.remove(id);
                aiBacktrackTarget.remove(id);
                aiLastTargetPos.put(id, new Vector3(aiFinalMoveTarget));
            }

            if (aiCurrentPos.dst(ai.lastPosition) < 0.02f) {
                ai.stuckTimer += delta;
            } else {
                ai.stuckTimer = 0;
            }
            ai.lastPosition.set(aiCurrentPos);

            if (ai.stuckTimer > 0.8f) {
                Array<Vector3> history = aiPathHistory.get(id);
                Vector3 backtrackPoint = null;
                int backtrackIndex = -1;

                if (history != null && history.size > 1) {
                    for (int i = history.size - 1; i >= 0; i--) {
                        Vector3 pt = history.get(i);
                        if (pt.dst(aiCurrentPos) >= 6.0f) {
                            backtrackIndex = i;
                            backtrackPoint = pt;
                            break;
                        }
                    }

                    if (backtrackIndex != -1) {
                        Array<DeadEndZone> deadEnds = aiDeadEnds.computeIfAbsent(id, k -> new Array<>());

                        for (int j = backtrackIndex + 1; j < history.size; j++) {
                            Vector3 deadPt = history.get(j);
                            boolean duplicate = false;
                            for (DeadEndZone zone : deadEnds) {
                                if (zone.position.dst(deadPt) < 1.5f) {
                                    duplicate = true;
                                    break;
                                }
                            }
                            if (!duplicate) {
                                deadEnds.add(new DeadEndZone(new Vector3(deadPt), 3.0f));
                            }
                        }

                        if (deadEnds.size > 30) {
                            deadEnds.removeRange(0, deadEnds.size - 30);
                        }

                        history.truncate(backtrackIndex + 1);
                        aiBacktrackTarget.put(id, new Vector3(backtrackPoint));
                    }
                }

                if (backtrackPoint == null) {
                    Array<DeadEndZone> deadEnds = aiDeadEnds.computeIfAbsent(id, k -> new Array<>());
                    deadEnds.add(new DeadEndZone(new Vector3(aiCurrentPos), 4.5f));
                    aiDesiredDir.scl(-1f);
                }

                ai.stuckTimer = 0;
            }

            Vector3 btTarget = aiBacktrackTarget.get(id);
            if (btTarget != null) {
                if (aiCurrentPos.dst(btTarget) < 1.2f) {
                    aiBacktrackTarget.remove(id);
                } else {
                    aiFinalMoveTarget.set(btTarget);
                }
            }

            float distToTarget = aiCurrentPos.dst(aiFinalMoveTarget);
            if (distToTarget <= ai.stopDistance) {
                ai.currentVelocity.lerp(Vector3.Zero, delta * 5f);
                stopPhysicsVelocity(id);
                continue;
            }

            aiDesiredDir.set(aiFinalMoveTarget).sub(aiCurrentPos);
            float horizontalDist = (float) Math.sqrt(aiDesiredDir.x * aiDesiredDir.x + aiDesiredDir.z * aiDesiredDir.z);

            if (horizontalDist < 1.3f && Math.abs(aiFinalMoveTarget.y - aiCurrentPos.y) > 1.5f) {
                if (!ai.currentVelocity.isZero(0.01f)) {
                    aiDesiredDir.set(ai.currentVelocity).nor();
                } else {
                    aiDesiredDir.y = 0;
                    aiDesiredDir.nor();
                }
            } else {
                aiDesiredDir.y = 0;
                aiDesiredDir.nor();
            }

            Vector3 currentHeading = new Vector3(ai.currentVelocity).nor();
            if (currentHeading.isZero()) {
                currentHeading.set(aiDesiredDir);
            }

            float safeRange = Math.min(3.0f, Math.max(1.2f, ai.detectionRange));

            boolean pathBlocked = false;
            if (ai.avoidObstacles) {
                Vector3 forwardRayEnd = new Vector3(aiCurrentPos).add(aiDesiredDir.x * safeRange, 0, aiDesiredDir.z * safeRange);
                com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback forwardCallback =
                        new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(aiCurrentPos, forwardRayEnd);

                dynamicsWorld.rayTest(aiCurrentPos, forwardRayEnd, forwardCallback);

                if (forwardCallback.hasHit()) {
                    com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = forwardCallback.getCollisionObject();
                    String hitId = "";
                    for (Map.Entry<String, btRigidBody> pbEntry : physicsBodies.entrySet()) {
                        if (pbEntry.getValue().equals(hitObject)) {
                            hitId = pbEntry.getKey();
                            break;
                        }
                    }
                    if (!hitId.equals(ai.targetId) && !hitId.equals(id)) {
                        Vector3 hitPoint = new Vector3();
                        forwardCallback.getHitPointWorld(hitPoint);
                        if (aiCurrentPos.dst(hitPoint) < safeRange * 0.85f) {
                            pathBlocked = true;
                        }
                    }
                }
                forwardCallback.dispose();
            }

            aiBestMoveDir.set(aiDesiredDir);

            if (!pathBlocked) {
                aiBestMoveDir.set(aiDesiredDir);
                aiSlideDirs.put(id, 0);
            } else {
                int currentSlide = aiSlideDirs.getOrDefault(id, 0);

                float bestScore = -1000f;
                int chosenRayIndex = 5;

                for (int i = 0; i < 11; i++) {
                    if (currentSlide == -1 && i > 5) continue;
                    if (currentSlide == 1 && i < 5) continue;

                    float angle = (i - 5) * 15f;
                    aiRayDir.set(aiDesiredDir).rotate(Vector3.Y, angle);

                    float obstacleDist = safeRange;

                    if (ai.avoidObstacles) {
                        Vector3 rayEnd = new Vector3(aiCurrentPos).add(aiRayDir.x * safeRange, 0, aiRayDir.z * safeRange);
                        com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback callback =
                                new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(aiCurrentPos, rayEnd);

                        dynamicsWorld.rayTest(aiCurrentPos, rayEnd, callback);

                        if (callback.hasHit()) {
                            com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = callback.getCollisionObject();
                            String hitId = "";
                            for (Map.Entry<String, btRigidBody> pbEntry : physicsBodies.entrySet()) {
                                if (pbEntry.getValue().equals(hitObject)) {
                                    hitId = pbEntry.getKey();
                                    break;
                                }
                            }
                            if (!hitId.equals(ai.targetId) && !hitId.equals(id)) {
                                Vector3 hitPoint = new Vector3();
                                callback.getHitPointWorld(hitPoint);
                                obstacleDist = aiCurrentPos.dst(hitPoint);
                            }
                        }
                        callback.dispose();
                    }

                    Array<DeadEndZone> deadEnds = aiDeadEnds.get(id);
                    if (deadEnds != null) {
                        for (DeadEndZone zone : deadEnds) {
                            float distToZone = aiCurrentPos.dst(zone.position);
                            if (distToZone < safeRange * 1.5f) {
                                tmpVec.set(zone.position).sub(aiCurrentPos);
                                float projection = tmpVec.dot(aiRayDir);

                                if (projection > 0) {
                                    float distSq = tmpVec.len2() - (projection * projection);
                                    float radiusSq = zone.radius * zone.radius;

                                    if (distSq < radiusSq) {
                                        float intersectionDist = projection - (float) Math.sqrt(radiusSq - distSq);
                                        if (intersectionDist > 0 && intersectionDist < obstacleDist) {
                                            obstacleDist = intersectionDist;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    float targetDot = aiRayDir.dot(aiDesiredDir);
                    float obstacleWeight = (obstacleDist / safeRange);
                    float headingBonus = aiRayDir.dot(currentHeading) * 0.4f;

                    float score = targetDot + (obstacleWeight * 2.5f) + headingBonus;

                    if (score > bestScore) {
                        bestScore = score;
                        aiBestMoveDir.set(aiRayDir);
                        chosenRayIndex = i;
                    }
                }

                if (currentSlide == 0) {
                    if (chosenRayIndex < 5) {
                        aiSlideDirs.put(id, -1);
                    } else if (chosenRayIndex > 5) {
                        aiSlideDirs.put(id, 1);
                    }
                }
            }

            Vector3 smoothedDir = aiSmoothedDirection.computeIfAbsent(id, k -> new Vector3(aiBestMoveDir));
            smoothedDir.lerp(aiBestMoveDir, delta * 3.0f).nor();

            ai.currentVelocity.lerp(new Vector3(smoothedDir).scl(ai.speed), delta * 4f);

            aiStepPos.set(ai.currentVelocity).nor().scl(0.6f);
            aiStepPos.add(aiCurrentPos).add(0, ai.stepHeight, 0);

            float rayLength = ai.stepHeight * 2.5f;
            Vector3 rayEnd = new Vector3(aiStepPos).add(0, -rayLength, 0);

            com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback floorCallback =
                    new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(aiStepPos, rayEnd);

            dynamicsWorld.rayTest(aiStepPos, rayEnd, floorCallback);

            float jumpVel = 0;
            if (floorCallback.hasHit()) {
                Vector3 hitPoint = new Vector3();
                floorCallback.getHitPointWorld(hitPoint);
                float floorY = hitPoint.y;
                if (floorY > aiCurrentPos.y + 0.1f) {
                    jumpVel = 5f;
                } else if (floorY < aiCurrentPos.y - 0.2f) {
                    jumpVel = -5f;
                }
            }
            floorCallback.dispose();

            float speedSq = ai.currentVelocity.len2();
            Quaternion finalRot = new Quaternion();
            if (speedSq > 0.01f) {
                float targetYaw = (float) Math.toDegrees(Math.atan2(ai.currentVelocity.x, ai.currentVelocity.z));
                float currentYaw = aiCurrentYaws.getOrDefault(id, targetYaw);
                float lerpYaw = MathUtils.lerpAngleDeg(currentYaw, targetYaw, delta * 3.5f);
                aiCurrentYaws.put(id, lerpYaw);
                finalRot.setEulerAngles(lerpYaw, 0, 0);
            } else {
                float lastYaw = aiCurrentYaws.getOrDefault(id, 0f);
                finalRot.setEulerAngles(lastYaw, 0, 0);
            }

            btRigidBody body = physicsBodies.get(id);
            if (body != null && body.getInvMass() > 0) {
                body.activate();
                body.setLinearVelocity(new Vector3(ai.currentVelocity.x, jumpVel != 0 ? jumpVel : body.getLinearVelocity().y, ai.currentVelocity.z));

                Matrix4 m = body.getWorldTransform();
                Vector3 currentPhysicsPos = new Vector3();
                m.getTranslation(currentPhysicsPos);
                m.set(currentPhysicsPos, finalRot);
                body.setWorldTransform(m);

                if (instance != null) {
                    Quaternion visualRot = new Quaternion(finalRot);
                    Quaternion offset = modelRotationOffsets.get(id);
                    if (offset != null) {
                        visualRot.mul(offset);
                    }
                    instance.transform.getScale(tmpScale);
                    instance.transform.set(currentPhysicsPos, visualRot, tmpScale);
                }
            } else {
                aiCurrentPos.add(ai.currentVelocity.x * delta, jumpVel * delta, ai.currentVelocity.z * delta);

                Quaternion visualRot = new Quaternion(finalRot);
                Quaternion offset = modelRotationOffsets.get(id);
                if (offset != null) {
                    visualRot.mul(offset);
                }
                instance.transform.set(aiCurrentPos, visualRot, instance.transform.getScale(tmpScale));
            }

            if (!aiBacktrackTarget.containsKey(id)) {
                Array<Vector3> history = aiPathHistory.computeIfAbsent(id, k -> new Array<>());
                if (history.size == 0 || history.peek().dst(aiCurrentPos) > 1.5f) {
                    history.add(new Vector3(aiCurrentPos));
                    if (history.size > 50) history.removeIndex(0);
                }
            }
        }
    }

    public boolean hasActiveAI(String objectId) {
        return activeAIs.containsKey(objectId);
    }


    private Vector3 calculateRayAvoidance(String ownerId, Vector3 origin, Vector3 rayDir, String targetId, float weight) {
        Vector3 force = new Vector3();
        String rayKey = "ai_ray_" + ownerId + rayDir.hashCode();

        castRay(rayKey, origin, rayDir);

        if (getRayDidHit(rayKey)) {
            String hitId = getRaycastHitObjectId(rayKey);

            if (!hitId.equals(targetId) && !hitId.equals(ownerId)) {
                Vector3 hitNormal = new Vector3(getRayHitNormalX(rayKey), 0, getRayHitNormalZ(rayKey));
                float hitDist = getRaycastDistance(rayKey);


                float multiplier = (1.0f - (hitDist / rayDir.len())) * weight;
                force.set(hitNormal).scl(multiplier * 5.0f);
            }
        }
        return force;
    }

    public void setRenderResolution(float scale, int mode) {
        this.renderScale = Math.max(0.05f, Math.min(1.0f, scale));
        this.aspectMode = mode;
        com.badlogic.gdx.Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (disposed) return;
                resize(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
            }
        });
    }

    private void stopPhysicsVelocity(String id) {
        btRigidBody body = physicsBodies.get(id);
        if (body != null) body.setLinearVelocity(new Vector3(0, body.getLinearVelocity().y, 0));
    }

    public void flagObjectForHiddenSpawn(String objectId) {
        hiddenSpawnIds.add(objectId);
    }

    public void createParticleProxy(String ownerId) {
        if (editorProxies.containsKey(ownerId) || particleProxyModel == null) return;
        ModelInstance proxyInstance = new ModelInstance(particleProxyModel);
        editorProxies.put(ownerId, proxyInstance);
    }

    private BillboardParticleBatch getBatchFor(Texture texture, boolean isAdditive) {
        ensureParticleSystemInitialized();
        String key = texture.getTextureObjectHandle() + (isAdditive ? "_ADD" : "_NRM");

        BillboardParticleBatch batch = managedBatches.get(key);

        if (batch == null) {
            Gdx.app.log("ParticleBatchManager", "Creating new batch for key: " + key);

            BlendingAttribute blendingAttribute;
            DepthTestAttribute depthTestAttribute;

            if (isAdditive) {
                blendingAttribute = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 1f);
                depthTestAttribute = new DepthTestAttribute(GL20.GL_LEQUAL, false);
            } else {
                blendingAttribute = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f);
                depthTestAttribute = new DepthTestAttribute(GL20.GL_LEQUAL, true);
            }

            batch = new BillboardParticleBatch(ParticleShader.AlignMode.Screen, true, 1000, blendingAttribute, depthTestAttribute);
            batch.setCamera(camera);
            batch.setTexture(texture);

            particleSystem.add(batch);
            managedBatches.put(key, batch);
        }


        batch.setTexture(texture);
        return batch;
    }

    private void updateParticles(float delta) {
        if (!particleSystemInitialized || activeParticleEffects.isEmpty()) return;


        particleSystem.begin();


        for (ParticleEffect effect : activeParticleEffects.values()) {
            effect.update(delta);
            effect.draw();
        }


        particleSystem.end();
    }

    private void createDefaultParticleTexture() {
        Gdx.app.log("PARTICLE_DEBUG", "Creating default white gradient texture...");
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float centerX = size / 2f;
        float centerY = size / 2f;
        float maxRadius = size / 2f;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float dx = x - centerX;
                float dy = y - centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);


                float alpha = 1.0f - (distance / maxRadius);
                alpha = Math.max(0, alpha);


                alpha = 1.0f - (1.0f - alpha) * (1.0f - alpha);

                pixmap.setColor(1, 1, 1, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        defaultParticleTexture = new Texture(pixmap);
        pixmap.dispose();
        Gdx.app.log("PARTICLE_DEBUG", "Default texture created. Handle: " + defaultParticleTexture.getTextureObjectHandle());
    }

    private boolean particleSystemInitialized = false;

    private void ensureParticleSystemInitialized() {
        if (particleSystemInitialized) return;

        Gdx.app.log("ParticleSystem", "First particle effect created. Initializing system...");
        particleModelBatch = new ModelBatch(new DefaultShaderProvider() {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new ParticleShader(renderable, new ParticleShader.Config());
            }
        });

        particleSystem = new ParticleSystem();


        batchNormal = new com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch();
        batchNormal.setCamera(camera);
        particleSystem.add(batchNormal);

        batchAdditive = new com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch();
        batchAdditive.setCamera(camera);
        particleSystem.add(batchAdditive);

        particleSystemInitialized = true;
    }

    public void updateParticleEffect(String objectId, ParticleComponent data, Matrix4 transform) {
        removeParticleEffect(objectId);
        data.migrateOldDataIfNeeded();
        data.sortGraphs();

        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            Texture texture = defaultParticleTexture;
            if (data.texturePath != null && !data.texturePath.isEmpty()) {
                Texture customTex = loadTexture(data.texturePath);
                if (customTex != null) texture = customTex;
            }
            if (texture == null) return;

            BillboardParticleBatch targetBatch = getBatchFor(texture, data.isAdditive);
            RegularEmitter emitter = new RegularEmitter();
            BillboardRenderer renderer = new BillboardRenderer(targetBatch);
            com.badlogic.gdx.graphics.g3d.particles.ParticleController controller =
                    new com.badlogic.gdx.graphics.g3d.particles.ParticleController("PC_" + objectId, emitter, renderer);

            emitter.setContinuous(data.looping);
            emitter.getDuration().setLow(data.duration * 1000f);
            emitter.getEmission().setHigh(data.emissionRate);
            emitter.getLife().setHigh(data.startLifetime * 1000f);
            emitter.setMaxParticleCount(data.maxParticles);

            PrimitiveSpawnShapeValue shapeValue = getPrimitiveSpawnShapeValue(data);

            controller.influencers.add(new com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer(shapeValue));

            DynamicsInfluencer dynamics = new DynamicsInfluencer();

            if (!data.speedGraph.isEmpty()) {
                DynamicsModifier.PolarAcceleration velocity = new DynamicsModifier.PolarAcceleration();

                velocity.phiValue.setHigh(0, 360);
                velocity.thetaValue.setHigh(0, data.coneAngle);

                applyFloatGraph(velocity.strengthValue, data.speedGraph);
                dynamics.velocities.add(velocity);
            }

            if (!data.gravityGraph.isEmpty()) {
                DynamicsModifier.PolarAcceleration gravity = new DynamicsModifier.PolarAcceleration();
                gravity.thetaValue.setHigh(180);
                applyFloatGraph(gravity.strengthValue, data.gravityGraph);
                dynamics.velocities.add(gravity);
            }

            if (!data.vortexGraph.isEmpty()) {
                DynamicsModifier.TangentialAcceleration vortex = new DynamicsModifier.TangentialAcceleration();
                vortex.phiValue.setHigh(0);
                vortex.thetaValue.setHigh(90);
                applyFloatGraph(vortex.strengthValue, data.vortexGraph);
                dynamics.velocities.add(vortex);
            }

            if (!data.turbulenceGraph.isEmpty()) {
                DynamicsModifier.BrownianAcceleration noise = new DynamicsModifier.BrownianAcceleration();
                applyFloatGraph(noise.strengthValue, data.turbulenceGraph);
                dynamics.velocities.add(noise);
            }

            if (!data.rotationGraph.isEmpty()) {
                DynamicsModifier.Rotational2D rot = new DynamicsModifier.Rotational2D();
                applyFloatGraph(rot.strengthValue, data.rotationGraph);
                dynamics.velocities.add(rot);
            }

            controller.influencers.add(dynamics);

            ColorInfluencer.Single colorInf = new ColorInfluencer.Single();
            applyColorGraph(colorInf, data.colorGraph);
            controller.influencers.add(colorInf);

            ScaleInfluencer scaleInf = new ScaleInfluencer();
            applyFloatGraph(scaleInf.value, data.sizeGraph);
            scaleInf.value.setHigh(data.baseSize);
            controller.influencers.add(scaleInf);

            controller.influencers.add(new RegionInfluencer.Single(new TextureRegion(texture)));

            ParticleEffect effect = new ParticleEffect();
            effect.getControllers().add(controller);
            effect.init();
            effect.start();
            effect.setTransform(transform);

            particleSystem.add(effect);
            activeParticleEffects.put(objectId, effect);
        });
    }

    @NonNull
    private static PrimitiveSpawnShapeValue getPrimitiveSpawnShapeValue(ParticleComponent data) {
        PrimitiveSpawnShapeValue shapeValue;

        switch (data.spawnShape) {
            case POINT:
                shapeValue = new PointSpawnShapeValue();
                break;
            case BOX:
                com.badlogic.gdx.graphics.g3d.particles.values.RectangleSpawnShapeValue rect =
                        new com.badlogic.gdx.graphics.g3d.particles.values.RectangleSpawnShapeValue();
                rect.setDimensions(data.spawnSize.x * 2, data.spawnSize.y * 2, data.spawnSize.z * 2);
                shapeValue = rect;
                break;
            case SPHERE:
                com.badlogic.gdx.graphics.g3d.particles.values.EllipseSpawnShapeValue ellipse =
                        new com.badlogic.gdx.graphics.g3d.particles.values.EllipseSpawnShapeValue();
                ellipse.setDimensions(data.spawnSize.x * 2, data.spawnSize.y * 2, data.spawnSize.z * 2);
                shapeValue = ellipse;
                break;
            case CYLINDER:
            default:
                com.badlogic.gdx.graphics.g3d.particles.values.CylinderSpawnShapeValue cyl =
                        new com.badlogic.gdx.graphics.g3d.particles.values.CylinderSpawnShapeValue();
                cyl.setDimensions(data.spawnSize.x * 2, data.spawnSize.y, data.spawnSize.z * 2);
                shapeValue = cyl;
                break;
        }

        shapeValue.setEdges(data.spawnOnSurface);
        return shapeValue;
    }

    private void applyFloatGraph(com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue libgdxVal,
                                 List<ParticleCurvePoint<Float>> graph) {
        if (graph.isEmpty()) return;

        float[] timeline = new float[graph.size()];
        float[] values = new float[graph.size()];

        for (int i = 0; i < graph.size(); i++) {
            timeline[i] = graph.get(i).time;
            values[i] = graph.get(i).value;
        }

        libgdxVal.setTimeline(timeline);
        libgdxVal.setScaling(values);
        libgdxVal.setHigh(1f);
    }

    private void applyColorGraph(ColorInfluencer.Single colorInf, List<ParticleCurvePoint<Color>> graph) {
        if (graph.isEmpty()) return;

        float[] timeline = new float[graph.size()];
        float[] colors = new float[graph.size() * 3];
        float[] alphas = new float[graph.size()];

        for (int i = 0; i < graph.size(); i++) {
            ParticleCurvePoint<Color> p = graph.get(i);
            timeline[i] = p.time;

            colors[i*3] = p.value.r;
            colors[i*3+1] = p.value.g;
            colors[i*3+2] = p.value.b;

            alphas[i] = p.value.a;
        }

        colorInf.colorValue.setTimeline(timeline);
        colorInf.colorValue.setColors(colors);

        colorInf.alphaValue.setTimeline(timeline);
        colorInf.alphaValue.setScaling(alphas);
        colorInf.alphaValue.setHigh(1f);
    }


    public void removeParticleEffect(String objectId) {
        ParticleEffect effect = activeParticleEffects.remove(objectId);
        if (effect != null) {
            Gdx.app.log("ParticleLifecycle", "Removing effect: " + objectId);
            particleSystem.remove(effect);
            effect.dispose();
        }
    }


    public void updateParticleTransform(String objectId, Matrix4 transform) {
        ParticleEffect effect = activeParticleEffects.get(objectId);
        if (effect != null) {
            effect.setTransform(transform);
        }
    }

    public class BufferPipeline implements Disposable {
        public com.crashinvaders.vfx.VfxManager vfxManager;
        public FrameBuffer depthFbo;
        public FrameBuffer materialFbo;

        public ExposureEffect exposureEffect;
        public TonemappingEffect tonemappingEffect;
        public LinearizeEffect linearizeEffect;
        public SsaoEffect ssaoEffect;
        public SsrRayTracingEffect rayTracingEffect;
        public SsgiEffect ssgiEffect;
        public GodRaysEffect godRaysEffect;
        public HeightFogEffect heightFogEffect;
        public VolumetricFogEffect volumetricFogEffect;
        public DepthOfFieldEffect dofEffect;
        public OptimizedBloomEffect bloomEffect;
        public VignettingEffect vignetteEffect;
        public FxaaEffect fxaaEffect;
        public MotionBlurEffect motionBlurEffect;
        public GaussianBlurEffect gaussianBlurEffect;
        public LevelsEffect levelsEffect;

        public BufferPipeline(int width, int height) {
            vfxManager = new com.crashinvaders.vfx.VfxManager(Pixmap.Format.RGBA8888);
            vfxManager.resize(width, height);
            depthFbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);
            depthFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            materialFbo = new FrameBuffer(Pixmap.Format.RGBA8888, width / 2, height / 2, true);
        }

        @Override
        public void dispose() {
            if (vfxManager != null) vfxManager.dispose();
            if (depthFbo != null) depthFbo.dispose();
            if (materialFbo != null) materialFbo.dispose();
        }
    }

    private final Map<FrameBuffer, BufferPipeline> bufferPipelines = new HashMap<>();

    public void setupBufferPipeline(FrameBuffer targetFbo, int width, int height, boolean enable) {
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            if (!enable) {
                BufferPipeline pipeline = bufferPipelines.remove(targetFbo);
                if (pipeline != null) pipeline.dispose();
                return;
            }

            BufferPipeline pipeline = bufferPipelines.get(targetFbo);
            if (pipeline == null || pipeline.vfxManager.getWidth() != width || pipeline.vfxManager.getHeight() != height) {
                if (pipeline != null) pipeline.dispose();
                pipeline = new BufferPipeline(width, height);
                bufferPipelines.put(targetFbo, pipeline);
            }

            configurePipelineEffects(pipeline, currentConfig);
        });
    }

    public void removeBufferPipeline(FrameBuffer targetFbo) {
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            BufferPipeline pipeline = bufferPipelines.remove(targetFbo);
            if (pipeline != null) pipeline.dispose();
        });
    }

    private void configurePipelineEffects(BufferPipeline p, PostProcessingComponent config) {
        if (p == null || p.vfxManager == null) return;
        p.vfxManager.removeAllEffects();

        if (config == null || !config.isActive) return;

        boolean toneMap = config.hasEffect(PostProcessingData.ACES.class) || config.hasEffect(PostProcessingData.EyeAdaptation.class);
        if (toneMap) {
            if (p.linearizeEffect == null) p.linearizeEffect = new LinearizeEffect();
            p.vfxManager.addEffect(p.linearizeEffect);
        }

        if (config.hasEffect(PostProcessingData.EyeAdaptation.class)) {
            if (p.exposureEffect == null) p.exposureEffect = new ExposureEffect();
            p.vfxManager.addEffect(p.exposureEffect);
        }

        for (PostProcessingData data : config.effects) {
            if (!data.isEnabled || data instanceof PostProcessingData.Upscaler) continue;

            if (data instanceof PostProcessingData.SSAO) {
                PostProcessingData.SSAO ssaoData = (PostProcessingData.SSAO) data;
                if (p.ssaoEffect == null) p.ssaoEffect = new SsaoEffect();
                p.ssaoEffect.setParams(ssaoData.radius, ssaoData.intensity, ssaoData.bias);
                p.vfxManager.addEffect(p.ssaoEffect);
            } else if (data instanceof PostProcessingData.RayTracing) {
                PostProcessingData.RayTracing rt = (PostProcessingData.RayTracing) data;
                if (p.rayTracingEffect == null) p.rayTracingEffect = new SsrRayTracingEffect();
                p.rayTracingEffect.setParams(rt.steps, rt.reflectivity, rt.thickness, rt.maxDistance, rt.stride, rt.edgeFade);
                p.vfxManager.addEffect(p.rayTracingEffect);
            } else if (data instanceof PostProcessingData.SSGI) {
                PostProcessingData.SSGI ssgiData = (PostProcessingData.SSGI) data;
                if (p.ssgiEffect == null) p.ssgiEffect = new SsgiEffect();
                p.ssgiEffect.setParams(ssgiData.radius, ssgiData.intensity, ssgiData.bias, ssgiData.ssaoStrength, new Color(ssgiData.baseAlbedoR, ssgiData.baseAlbedoG, ssgiData.baseAlbedoB, 1.0f), ssgiData.flipDepth);
                p.vfxManager.addEffect(p.ssgiEffect);
            } else if (data instanceof PostProcessingData.GodRays) {
                PostProcessingData.GodRays grData = (PostProcessingData.GodRays) data;
                if (p.godRaysEffect == null) p.godRaysEffect = new GodRaysEffect();
                p.godRaysEffect.exposure = grData.exposure;
                p.godRaysEffect.decay = grData.decay;
                p.godRaysEffect.density = grData.density;
                p.godRaysEffect.weight = grData.weight;
                p.godRaysEffect.setSunDirection(getSunLightDirection());
                p.vfxManager.addEffect(p.godRaysEffect);
            } else if (data instanceof PostProcessingData.HeightFog) {
                PostProcessingData.HeightFog fogData = (PostProcessingData.HeightFog) data;
                if (p.heightFogEffect == null) p.heightFogEffect = new HeightFogEffect();
                p.heightFogEffect.fogDensity = fogData.density;
                p.heightFogEffect.heightFalloff = fogData.falloff;
                p.heightFogEffect.fogHeight = fogData.height;
                p.heightFogEffect.fogColor.set(fogData.color);
                p.vfxManager.addEffect(p.heightFogEffect);
            } else if (data instanceof PostProcessingData.VolumetricFog) {
                PostProcessingData.VolumetricFog vfData = (PostProcessingData.VolumetricFog) data;
                if (p.volumetricFogEffect == null) p.volumetricFogEffect = new VolumetricFogEffect();
                if (pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) {
                    net.mgsx.gltf.scene3d.lights.DirectionalShadowLight sl = (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;
                    p.volumetricFogEffect.setShadowMap(sl.getFrameBuffer().getColorBufferTexture(), sl.getCamera().combined);
                    p.volumetricFogEffect.setLightParams(sl.direction, sl.color);
                }
                p.volumetricFogEffect.setParams(vfData.density, vfData.scattering, vfData.steps, vfData.maxDistance);
                p.vfxManager.addEffect(p.volumetricFogEffect);
            } else if (data instanceof PostProcessingData.DepthOfField) {
                PostProcessingData.DepthOfField dofData = (PostProcessingData.DepthOfField) data;
                if (p.dofEffect == null) p.dofEffect = new DepthOfFieldEffect();
                p.dofEffect.focusDistance = dofData.focusDistance;
                p.dofEffect.focusRange = dofData.focusRange;
                p.dofEffect.blurSize = dofData.blurSize;
                p.dofEffect.transition = dofData.transition;
                p.vfxManager.addEffect(p.dofEffect);
            } else if (data instanceof PostProcessingData.Bloom) {
                PostProcessingData.Bloom b = (PostProcessingData.Bloom) data;
                if (p.bloomEffect == null) p.bloomEffect = new OptimizedBloomEffect(b.size);
                p.bloomEffect.setBlurPasses(b.blurPasses);
                p.bloomEffect.setBlurAmount(b.blurAmount);
                p.bloomEffect.setThreshold(b.threshold);
                p.bloomEffect.setBloomIntensity(b.intensity);
                p.vfxManager.addEffect(p.bloomEffect);
            } else if (data instanceof PostProcessingData.Vignette) {
                PostProcessingData.Vignette v = (PostProcessingData.Vignette) data;
                if (p.vignetteEffect == null) p.vignetteEffect = new VignettingEffect(false);
                p.vignetteEffect.setIntensity(v.intensity);
                p.vignetteEffect.setSaturation(v.saturation);
                p.vfxManager.addEffect(p.vignetteEffect);
            } else if (data instanceof PostProcessingData.Levels) {
                PostProcessingData.Levels l = (PostProcessingData.Levels) data;
                if (p.levelsEffect == null) p.levelsEffect = new LevelsEffect();
                p.levelsEffect.setContrast(l.contrast);
                p.levelsEffect.setSaturation(l.saturation);
                p.levelsEffect.setGamma(l.gamma);
                p.vfxManager.addEffect(p.levelsEffect);
            } else if (data instanceof PostProcessingData.Grain) {
                PostProcessingData.Grain g = (PostProcessingData.Grain) data;
                FilmGrainEffect effect = new FilmGrainEffect();
                effect.setNoiseAmount(g.amount);
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.Chromatic) {
                PostProcessingData.Chromatic c = (PostProcessingData.Chromatic) data;
                ChromaticAberrationEffect effect = new ChromaticAberrationEffect((int) c.maxDistortion);
                effect.setMaxDistortion(c.strength);
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.RadialBlur) {
                PostProcessingData.RadialBlur r = (PostProcessingData.RadialBlur) data;
                RadialBlurEffect effect = new OptimizedRadialBlurEffect(r.blurPasses, r.size);
                effect.setStrength(r.strength);
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.OldTv) {
                PostProcessingData.OldTv tv = (PostProcessingData.OldTv) data;
                OldTvEffect effect = new OldTvEffect();
                effect.setTime(tv.strength);
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.Gaussian) {
                PostProcessingData.Gaussian g = (PostProcessingData.Gaussian) data;
                if (p.gaussianBlurEffect == null) p.gaussianBlurEffect = new OptimizedGaussianBlurEffect(g.size);
                p.gaussianBlurEffect.setPasses(g.passes);
                p.gaussianBlurEffect.setAmount(g.amount);
                p.vfxManager.addEffect(p.gaussianBlurEffect);
            } else if (data instanceof PostProcessingData.Zoom) {
                PostProcessingData.Zoom z = (PostProcessingData.Zoom) data;
                ZoomEffect effect = new ZoomEffect();
                effect.setZoom(z.zoom);
                effect.setOrigin(z.originX, z.originY);
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.Crt) {
                CrtEffect effect = new CrtEffect();
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.Fisheye) {
                FisheyeEffect effect = new FisheyeEffect();
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.Water) {
                PostProcessingData.Water w2 = (PostProcessingData.Water) data;
                WaterDistortionEffect effect = new WaterDistortionEffect(w2.amount, w2.speed);
                p.vfxManager.addEffect(effect);
            } else if (data instanceof PostProcessingData.MotionBlur) {
                PostProcessingData.MotionBlur mb = (PostProcessingData.MotionBlur) data;
                if (p.motionBlurEffect == null) p.motionBlurEffect = new MotionBlurEffect(Pixmap.Format.RGBA8888, MixEffect.Method.MIX, mb.blurOpacity);
                p.vfxManager.addEffect(p.motionBlurEffect);
            } else if (data instanceof PostProcessingData.Fxaa) {
                if (p.fxaaEffect == null) p.fxaaEffect = new FxaaEffect();
                p.vfxManager.addEffect(p.fxaaEffect);
            }
        }

        if (config.hasEffect(PostProcessingData.ACES.class)) {
            if (p.tonemappingEffect == null) p.tonemappingEffect = new TonemappingEffect();
            p.vfxManager.addEffect(p.tonemappingEffect);
        }
    }

    private void renderDepthAndMaterial(Camera targetCamera, FrameBuffer targetDepthFbo, FrameBuffer targetMaterialFbo) {
        targetDepthFbo.begin();
        Gdx.gl.glViewport(0, 0, targetDepthFbo.getWidth(), targetDepthFbo.getHeight());
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        depthBatch.begin(targetCamera);

        if (realisticMode && sceneManager != null) {
            depthBatch.render(sceneManager.getRenderableProviders());
        }
        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
            if (inactiveRenderObjects.contains(entry.getKey())) continue;
            ModelInstance inst = entry.getValue();
            boolean isTransparent = false;
            for (Material mat : inst.materials) {
                if (mat.has(BlendingAttribute.Type)) {
                    BlendingAttribute blend = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                    if (blend.opacity < 0.1f) { isTransparent = true; break; }
                }
            }
            if (!isTransparent) depthBatch.render(inst);
        }
        depthBatch.end();
        targetDepthFbo.end();

        targetMaterialFbo.begin();
        Gdx.gl.glViewport(0, 0, targetMaterialFbo.getWidth(), targetMaterialFbo.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        materialBatch.begin(targetCamera);

        if (realisticMode && sceneManager != null) {
            materialBatch.render(sceneManager.getRenderableProviders());
        }
        materialBatch.render(sceneObjects.values());
        materialBatch.end();
        targetMaterialFbo.end();
    }

    private com.crashinvaders.vfx.effects.BloomEffect cachedBloomEffect;
    private com.crashinvaders.vfx.effects.GaussianBlurEffect cachedGaussianBlurEffect;
    private com.crashinvaders.vfx.effects.MotionBlurEffect cachedMotionBlurEffect;
    private com.crashinvaders.vfx.effects.VignettingEffect cachedVignetteEffect;
    private com.crashinvaders.vfx.effects.LevelsEffect cachedLevelsEffect;
    public PostProcessingComponent currentConfig = new PostProcessingComponent();

    private boolean isVfxCapturing = false;

    public void configureVfx(com.crashinvaders.vfx.VfxManager vfx, PostProcessingComponent config) {
        configureVfx(vfx, config, this.camera);
    }

    public void configureVfx(com.crashinvaders.vfx.VfxManager vfx, PostProcessingComponent config, Camera effectCamera) {
        if (vfx == null) return;

        vfx.removeAllEffects();

        if (!config.isActive) {
            if (customScreenEffect != null) {
                vfx.addEffect(customScreenEffect);
            }
            return;
        }

        boolean tonemappingOrAdaptationActive =
                config.hasEffect(PostProcessingData.ACES.class) ||
                        config.hasEffect(PostProcessingData.EyeAdaptation.class);

        if (tonemappingOrAdaptationActive) {
            if (linearizeEffect == null) linearizeEffect = new LinearizeEffect();
            vfx.addEffect(linearizeEffect);
        }

        if (config.hasEffect(PostProcessingData.EyeAdaptation.class)) {
            if (exposureEffect == null) exposureEffect = new ExposureEffect();
            vfx.addEffect(exposureEffect);
        }

        for (PostProcessingData data : config.effects) {
            if (!data.isEnabled) continue;

            if (data instanceof PostProcessingData.Upscaler) {
                continue;
            }
            else if (data instanceof PostProcessingData.SSAO) {
                setDepthRender(true);
                PostProcessingData.SSAO ssaoData = (PostProcessingData.SSAO) data;
                if (ssaoEffect == null) ssaoEffect = new SsaoEffect();
                ssaoEffect.setCamera(effectCamera);
                ssaoEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                ssaoEffect.setParams(ssaoData.radius, ssaoData.intensity, ssaoData.bias);
                vfx.addEffect(ssaoEffect);
            }
            else if (data instanceof PostProcessingData.RayTracing) {
                setDepthRender(true);
                PostProcessingData.RayTracing rt = (PostProcessingData.RayTracing) data;
                if (rayTracingEffect == null) rayTracingEffect = new SsrRayTracingEffect();
                rayTracingEffect.setCamera(effectCamera);
                rayTracingEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                rayTracingEffect.setParams(rt.steps, rt.reflectivity, rt.thickness, rt.maxDistance, rt.stride, rt.edgeFade);
                vfx.addEffect(rayTracingEffect);
            } else if (data instanceof PostProcessingData.SSGI) {
                setDepthRender(true);
                PostProcessingData.SSGI ssgiData = (PostProcessingData.SSGI) data;
                if (ssgiEffect == null) ssgiEffect = new SsgiEffect();
                ssgiEffect.setCamera(effectCamera);
                ssgiEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                ssgiEffect.setParams(ssgiData.radius, ssgiData.intensity, ssgiData.bias, ssgiData.ssaoStrength, new Color(ssgiData.baseAlbedoR, ssgiData.baseAlbedoG, ssgiData.baseAlbedoB, 1.0f), ssgiData.flipDepth);
                vfx.addEffect(ssgiEffect);
            } else if (data instanceof PostProcessingData.GodRays) {
                setDepthRender(true);
                PostProcessingData.GodRays grData = (PostProcessingData.GodRays) data;
                if (godRaysEffect == null) godRaysEffect = new GodRaysEffect();
                godRaysEffect.setCamera(effectCamera);
                godRaysEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                godRaysEffect.setSunDirection(getSunLightDirection());
                godRaysEffect.exposure = grData.exposure;
                godRaysEffect.decay = grData.decay;
                godRaysEffect.density = grData.density;
                godRaysEffect.weight = grData.weight;
                vfx.addEffect(godRaysEffect);
            } else if (data instanceof PostProcessingData.HeightFog) {
                setDepthRender(true);
                PostProcessingData.HeightFog fogData = (PostProcessingData.HeightFog) data;
                if (heightFogEffect == null) heightFogEffect = new HeightFogEffect();
                heightFogEffect.setCamera(effectCamera);
                heightFogEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                heightFogEffect.fogDensity = fogData.density;
                heightFogEffect.heightFalloff = fogData.falloff;
                heightFogEffect.fogHeight = fogData.height;
                heightFogEffect.fogColor.set(fogData.color);
                vfx.addEffect(heightFogEffect);
            } else if (data instanceof PostProcessingData.VolumetricFog) {
                setDepthRender(true);
                PostProcessingData.VolumetricFog vfData = (PostProcessingData.VolumetricFog) data;
                if (volumetricFogEffect == null) volumetricFogEffect = new VolumetricFogEffect();
                volumetricFogEffect.setCamera(effectCamera);
                volumetricFogEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                if (pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) {
                    net.mgsx.gltf.scene3d.lights.DirectionalShadowLight sl = (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;
                    volumetricFogEffect.setShadowMap(sl.getFrameBuffer().getColorBufferTexture(), sl.getCamera().combined);
                    volumetricFogEffect.setLightParams(sl.direction, sl.color);
                }
                volumetricFogEffect.setParams(vfData.density, vfData.scattering, vfData.steps, vfData.maxDistance);
                vfx.addEffect(volumetricFogEffect);
            } else if (data instanceof PostProcessingData.DepthOfField) {
                setDepthRender(true);
                PostProcessingData.DepthOfField dofData = (PostProcessingData.DepthOfField) data;
                if (dofEffect == null) dofEffect = new DepthOfFieldEffect();
                dofEffect.setCamera(effectCamera);
                dofEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                dofEffect.focusDistance = dofData.focusDistance;
                dofEffect.focusRange = dofData.focusRange;
                dofEffect.blurSize = dofData.blurSize;
                dofEffect.transition = dofData.transition;
                vfx.addEffect(dofEffect);
            } else if (data instanceof PostProcessingData.Bloom) {
                PostProcessingData.Bloom b = (PostProcessingData.Bloom) data;
                if (cachedBloomEffect == null) cachedBloomEffect = new OptimizedBloomEffect(b.size);
                cachedBloomEffect.setBlurPasses(b.blurPasses);
                cachedBloomEffect.setBlurAmount(b.blurAmount);
                cachedBloomEffect.setThreshold(b.threshold);
                cachedBloomEffect.setBloomIntensity(b.intensity);
                vfx.addEffect(cachedBloomEffect);
            } else if (data instanceof PostProcessingData.Vignette) {
                PostProcessingData.Vignette v = (PostProcessingData.Vignette) data;
                if (cachedVignetteEffect == null) cachedVignetteEffect = new VignettingEffect(false);
                cachedVignetteEffect.setIntensity(v.intensity);
                cachedVignetteEffect.setSaturation(v.saturation);
                vfx.addEffect(cachedVignetteEffect);
            } else if (data instanceof PostProcessingData.Levels) {
                PostProcessingData.Levels l = (PostProcessingData.Levels) data;
                if (cachedLevelsEffect == null) cachedLevelsEffect = new LevelsEffect();
                cachedLevelsEffect.setContrast(l.contrast);
                cachedLevelsEffect.setSaturation(l.saturation);
                cachedLevelsEffect.setGamma(l.gamma);
                vfx.addEffect(cachedLevelsEffect);
            } else if (data instanceof PostProcessingData.Grain) {
                PostProcessingData.Grain g = (PostProcessingData.Grain) data;
                FilmGrainEffect effect = new FilmGrainEffect();
                effect.setNoiseAmount(g.amount);
                vfx.addEffect(effect);
            } else if (data instanceof PostProcessingData.Chromatic) {
                PostProcessingData.Chromatic c = (PostProcessingData.Chromatic) data;
                ChromaticAberrationEffect effect = new ChromaticAberrationEffect((int) c.maxDistortion);
                effect.setMaxDistortion(c.strength);
                vfx.addEffect(effect);
            } else if (data instanceof PostProcessingData.Fxaa) {
                vfx.addEffect(new FxaaEffect());
            } else if (data instanceof PostProcessingData.RadialBlur) {
                PostProcessingData.RadialBlur r = (PostProcessingData.RadialBlur) data;
                RadialBlurEffect effect = new OptimizedRadialBlurEffect(r.blurPasses, r.size);
                effect.setStrength(r.strength);
                vfx.addEffect(effect);
            } else if (data instanceof PostProcessingData.OldTv) {
                PostProcessingData.OldTv tv = (PostProcessingData.OldTv) data;
                OldTvEffect effect = new OldTvEffect();
                effect.setTime(tv.strength);
                vfx.addEffect(effect);
            } else if (data instanceof PostProcessingData.Gaussian) {
                PostProcessingData.Gaussian g = (PostProcessingData.Gaussian) data;
                if (cachedGaussianBlurEffect == null) cachedGaussianBlurEffect = new OptimizedGaussianBlurEffect(g.size);
                cachedGaussianBlurEffect.setPasses(g.passes);
                cachedGaussianBlurEffect.setAmount(g.amount);
                vfx.addEffect(cachedGaussianBlurEffect);
            } else if (data instanceof PostProcessingData.Zoom) {
                PostProcessingData.Zoom z = (PostProcessingData.Zoom) data;
                ZoomEffect effect = new ZoomEffect();
                effect.setZoom(z.zoom);
                effect.setOrigin(z.originX, z.originY);
                vfx.addEffect(effect);
            } else if (data instanceof PostProcessingData.Crt) {
                vfx.addEffect(new CrtEffect());
            } else if (data instanceof PostProcessingData.Fisheye) {
                vfx.addEffect(new FisheyeEffect());
            } else if (data instanceof PostProcessingData.Water) {
                PostProcessingData.Water w2 = (PostProcessingData.Water) data;
                WaterDistortionEffect effect = new WaterDistortionEffect(w2.amount, w2.speed);
                vfx.addEffect(effect);
            } else if (data instanceof PostProcessingData.MotionBlur) {
                PostProcessingData.MotionBlur mb = (PostProcessingData.MotionBlur) data;
                if (cachedMotionBlurEffect == null) cachedMotionBlurEffect = new MotionBlurEffect(Pixmap.Format.RGBA8888, MixEffect.Method.MIX, mb.blurOpacity);
                vfx.addEffect(cachedMotionBlurEffect);
            } else if (data instanceof PostProcessingData.LensFlare) {
                PostProcessingData.LensFlare lf = (PostProcessingData.LensFlare) data;
                AutoLensFlareEffect effect = new AutoLensFlareEffect();
                effect.setThreshold(lf.threshold);
                effect.setIntensity(lf.intensity * 2.0f);
                effect.setDispersal(lf.dispersal);
                effect.setSize(lf.size);
                vfx.addEffect(effect);
            }
        }

        if (config.hasEffect(PostProcessingData.ACES.class)) {
            if (tonemappingEffect == null) tonemappingEffect = new TonemappingEffect();
            vfx.addEffect(tonemappingEffect);
        }

        if (customScreenEffect != null) {
            vfx.addEffect(customScreenEffect);
        }
    }

    public void updatePostProcessing(PostProcessingComponent config) {
        this.currentConfig = config;
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            if (vfxManager == null) return;
            this.postprocessingEnabled = config.isActive;

            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();
            float scale = Math.max(0.01f, Math.min(1.0f, config.qualityScale));

            if (vfxManager.getWidth() != (int) (w * scale) || vfxManager.getHeight() != (int) (h * scale)) {
                vfxManager.resize((int) (w * scale), (int) (h * scale));
            }

            configureVfx(vfxManager, config);
        });
    }

    public void setObjectVisibility(String objectId, boolean visible) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        if (visible) {
            btRigidBody body = inactivePhysicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.addRigidBody(body);
                physicsBodies.put(objectId, body);
            }
        } else {
            btRigidBody body = physicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.removeRigidBody(body);
                inactivePhysicsBodies.put(objectId, body);
            }
        }

        if (visible) {
            inactiveRenderObjects.remove(objectId);
            if (realisticMode && inactivePbrScenes.containsKey(objectId)) {
                sceneManager.addScene(inactivePbrScenes.remove(objectId));
            }
        } else {
            inactiveRenderObjects.add(objectId);
            if (realisticMode) {
                net.mgsx.gltf.scene3d.scene.Scene sceneToRemove = null;
                for (RenderableProvider provider : sceneManager.getRenderableProviders()) {
                    if (provider instanceof net.mgsx.gltf.scene3d.scene.Scene) {
                        net.mgsx.gltf.scene3d.scene.Scene scene = (net.mgsx.gltf.scene3d.scene.Scene) provider;
                        if (scene.modelInstance == instance) {
                            sceneToRemove = scene;
                            break;
                        }
                    }
                }
                if (sceneToRemove != null) {
                    sceneManager.removeScene(sceneToRemove);
                    inactivePbrScenes.put(objectId, sceneToRemove);
                }
            }
        }
    }

    public void setSceneManager(SceneManager manager) {
        this.manager = manager;
    }


    @Deprecated
    public void setObjectRenderState(String objectId, boolean active) {
        setObjectVisibility(objectId, active);
    }

    public void setObjectPhysicsState(String objectId, boolean active) {
        if (active) {
            btRigidBody body = inactivePhysicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.addRigidBody(body);
                physicsBodies.put(objectId, body);
            }
        } else {
            btRigidBody body = physicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.removeRigidBody(body);
                inactivePhysicsBodies.put(objectId, body);
            }
        }
    }

    public SceneSettings getSceneSettings() {
        return currentSettings != null ? currentSettings : new SceneSettings();
    }

    public void setCameraFov(float fieldOfView, float near, float far) {
        camera.fieldOfView = fieldOfView;
        camera.near = near;
        camera.far = far;
        camera.update();
    }

    public DebugDrawer getDebugDrawer() { return debugDrawer; }

    public void createCameraProxy(String ownerId) {
        if (editorProxies.containsKey(ownerId) || cameraProxyModel == null) return;
        ModelInstance proxyInstance = new ModelInstance(cameraProxyModel);
        editorProxies.put(ownerId, proxyInstance);
    }

    public boolean createPoint2PointConstraint(String constraintId, String objectIdA, String objectIdB, Vector3 pivotInA, Vector3 pivotInB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) {
            Gdx.app.error("3DManager", "Cannot create constraint: one or both bodies not found.");
            return false;
        }

        btPoint2PointConstraint constraint = new btPoint2PointConstraint(bodyA, bodyB, pivotInA, pivotInB);
        dynamicsWorld.addConstraint(constraint, true);
        physicsConstraints.put(constraintId, constraint);
        return true;
    }


    public void removeConstraint(String constraintId) {
        btTypedConstraint constraint = physicsConstraints.remove(constraintId);
        if (constraint != null) {
            dynamicsWorld.removeConstraint(constraint);
            constraint.dispose();
        }
    }


    private final Vector3 cyanColorVec = new Vector3(Color.CYAN.r, Color.CYAN.g, Color.CYAN.b);



    public void renderCameraFrustum(Matrix4 ownerTransform, float fov, float far) {
        if (debugDrawer == null) return;

        PerspectiveCamera tempCam = new PerspectiveCamera(fov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        tempCam.far = far;
        Quaternion rot = ownerTransform.getRotation(new Quaternion());
        tempCam.position.set(ownerTransform.getTranslation(new Vector3()));
        tempCam.direction.set(0, 0, -1);
        rot.transform(tempCam.direction);
        tempCam.up.set(0, 1, 0);
        rot.transform(tempCam.up);

        tempCam.update();

        Vector3[] points = tempCam.frustum.planePoints;

        debugDrawer.drawLine(points[4], points[5], cyanColorVec);
        debugDrawer.drawLine(points[5], points[6], cyanColorVec);
        debugDrawer.drawLine(points[6], points[7], cyanColorVec);
        debugDrawer.drawLine(points[7], points[4], cyanColorVec);

        debugDrawer.drawLine(points[0], points[1], cyanColorVec);
        debugDrawer.drawLine(points[1], points[2], cyanColorVec);
        debugDrawer.drawLine(points[2], points[3], cyanColorVec);
        debugDrawer.drawLine(points[3], points[0], cyanColorVec);

        debugDrawer.drawLine(points[0], points[4], cyanColorVec);
        debugDrawer.drawLine(points[1], points[5], cyanColorVec);
        debugDrawer.drawLine(points[2], points[6], cyanColorVec);
        debugDrawer.drawLine(points[3], points[7], cyanColorVec);
    }


    public void setCameraRotation(Quaternion rotation) {
        camera.direction.set(0, 0, -1);
        rotation.transform(camera.direction);
        camera.up.set(0, 1, 0);
        rotation.transform(camera.up);
        camera.update();
    }

    public void setEditorMode(boolean enabled) {
        this.editorMode = enabled;
    }

    public void setPhysicsStateFromComponent(String objectId, PhysicsComponent component) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        removePhysicsBody(objectId);

        float bodyMass = (component.state == PhysicsState.DYNAMIC) ? component.mass : 0f;

        if (component.state == PhysicsState.NONE) return;

        if (component.state == PhysicsState.MESH_STATIC) {
            if (gltfObjectIds.contains(objectId)) {
                createGltfMeshPhysicsBody(objectId, instance);
            } else {
                createMeshPhysicsBody(objectId, instance);
            }
            return;
        }

        if (component.colliders.isEmpty()) {
            Gdx.app.error("3DManager", "setPhysicsStateFromComponent called with no colliders for object: " + objectId);
            return;
        }

        createCompoundPhysicsBody(objectId, instance, component, bodyMass);
    }


    private void createCompoundPhysicsBody(String objectId, ModelInstance instance, PhysicsComponent component, float mass) {
        Log.d("PhysicsDebug", "--- Creating Compound Physics Body for '" + objectId + "' ---");
        Log.d("PhysicsDebug", "  - Input Mass: " + mass);
        Log.d("PhysicsDebug", "  - Instance World Transform on entry:\n" + instance.transform);

        btCompoundShape compoundShape = new btCompoundShape();
        Array<Disposable> disposables = new Array<>();
        disposables.add(compoundShape);

        final Matrix4 worldTransform = instance.transform;
        final float[] vals = worldTransform.val;

        boolean isFlippedX = vals[Matrix4.M00] < 0;
        boolean isFlippedY = vals[Matrix4.M11] < 0;
        boolean isFlippedZ = vals[Matrix4.M22] < 0;
        Log.d("PhysicsDebug", "  - Axis flip detection: X=" + isFlippedX + ", Y=" + isFlippedY + ", Z=" + isFlippedZ);

        for (ColliderShapeData shapeData : component.colliders) {
            Vector3 correctedOffset = new Vector3(shapeData.centerOffset);
            if (isFlippedX) correctedOffset.x *= -1;
            if (isFlippedY) correctedOffset.y *= -1;
            if (isFlippedZ) correctedOffset.z *= -1;

            Log.d("PhysicsDebug", "  - Processing Collider #(тут был номер, но я его убрал)" + ": Type=" + shapeData.type + ", Local Offset=" + shapeData.centerOffset + ", Size=" + shapeData.size);
            Log.d("PhysicsDebug", "    - Original Offset: " + shapeData.centerOffset);
            Log.d("PhysicsDebug", "    - Corrected Offset for Physics: " + correctedOffset);

            btCollisionShape shape;

            switch (shapeData.type) {
                case SPHERE:
                    shape = new btSphereShape(shapeData.radius);
                    break;
                case CAPSULE:
                    float height = Math.max(0, shapeData.size.y - (shapeData.radius * 2));
                    shape = new btCapsuleShape(shapeData.radius, height);
                    break;
                case BOX:
                default:
                    shape = new btBoxShape(shapeData.size.cpy().scl(0.5f));
                    break;
            }
            disposables.add(shape);
            Matrix4 localTransform = new Matrix4().setToTranslation(correctedOffset);
            compoundShape.addChildShape(localTransform, shape);
            Log.d("PhysicsDebug", "    - Added child shape with local transform:\n" + localTransform);
        }

        Vector3 absScale = worldTransform.getScale(new Vector3());
        if (absScale.x == 0) absScale.x = 0.0001f;
        if (absScale.y == 0) absScale.y = 0.0001f;
        if (absScale.z == 0) absScale.z = 0.0001f;

        compoundShape.setLocalScaling(absScale);
        Log.d("PhysicsDebug", "  - Compound Shape Local Scaling set to (abs value): " + absScale);

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        Vector3 pos = new Vector3();
        Quaternion rot = new Quaternion();
        worldTransform.getTranslation(pos);
        worldTransform.getRotation(rot, true);

        Matrix4 bodyTransform = new Matrix4(pos, rot, new Vector3(1, 1, 1));

        Log.d("PhysicsDebug", "  - Final RigidBody Transform for creation:\n" + bodyTransform);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        if (mass > 0) {
            body.setAngularFactor(1f);
            body.setDamping(0.5f, 0.5f);
        }

        if (dynamicsWorld != null && !disposed) {
            dynamicsWorld.addRigidBody(body);
        } else {
            Log.w("ThreeDManager", "Aborted adding rigid body: dynamicsWorld is null or disposed.");
        }
        physicsBodies.put(objectId, body);

        body.userData = disposables;
        physicsResources.put(objectId, disposables);
        bodyInfo.dispose();

        Matrix4 finalBodyTransform = body.getWorldTransform();
        Log.d("PhysicsDebug", "  - SUCCESS: Body created. Final transform from physics world:\n" + finalBodyTransform);
        Log.d("PhysicsDebug", "--------------------------------------------------------");
    }

    public void setPhysicsStateFromComponent(String objectId, PhysicsComponent component, Matrix4 worldTransform) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        instance.transform.set(worldTransform);

        removePhysicsBody(objectId);

        float bodyMass = (component.state == PhysicsState.DYNAMIC) ? component.mass : 0f;

        if (component.state == PhysicsState.NONE) return;

        if (component.state == PhysicsState.MESH_STATIC) {
            if (gltfObjectIds.contains(objectId)) {
                createGltfMeshPhysicsBody(objectId, instance);
            } else {
                createMeshPhysicsBody(objectId, instance);
            }
            return;
        }

        if (component.colliders.isEmpty()) {
            Gdx.app.error("3DManager", "setPhysicsStateFromComponent called with no colliders for object: " + objectId);
            return;
        }

        createCompoundPhysicsBody(objectId, instance, component, bodyMass);
    }

    private final Vector3 tmpVec = new Vector3();


    public void renderWireframeShape(ModelBatch batch, ColliderShapeData shapeData, Matrix4 parentTransform, Color color) {
        if (batch == null) return;

        ModelInstance instance;
        Matrix4 finalTransform = new Matrix4(parentTransform);

        switch (shapeData.type) {
            case BOX:
                instance = new ModelInstance(wireframeBoxModel);
                instance.transform.scl(shapeData.size);
                instance.transform.mulLeft(finalTransform.translate(shapeData.centerOffset));
                ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(instance);
                break;

            case SPHERE:
                instance = new ModelInstance(wireframeSphereModel);
                instance.transform.scl(shapeData.radius * 2);
                instance.transform.mulLeft(finalTransform.translate(shapeData.centerOffset));
                ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(instance);
                break;

            case CAPSULE:
                float cylinderHeight = Math.max(0, shapeData.size.y - (shapeData.radius * 2));
                Vector3 capsuleCenter = shapeData.centerOffset;
                instance = new ModelInstance(wireframeCylinderModel);
                instance.transform.scl(shapeData.radius * 2, cylinderHeight, shapeData.radius * 2);
                instance.transform.mulLeft(finalTransform.cpy().translate(capsuleCenter));
                ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(instance);

                ModelInstance topSphere = new ModelInstance(wireframeSphereModel);
                tmpVec.set(capsuleCenter).add(0, cylinderHeight / 2f, 0);
                topSphere.transform.scl(shapeData.radius * 2);
                topSphere.transform.mulLeft(finalTransform.cpy().translate(tmpVec));
                ((ColorAttribute)topSphere.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(topSphere);

                ModelInstance bottomSphere = new ModelInstance(wireframeSphereModel);
                tmpVec.set(capsuleCenter).add(0, -cylinderHeight / 2f, 0);
                bottomSphere.transform.scl(shapeData.radius * 2);
                bottomSphere.transform.mulLeft(finalTransform.cpy().translate(tmpVec));
                ((ColorAttribute)bottomSphere.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(bottomSphere);
                break;
        }
    }



    public void setBackgroundLightIntensity(float intensity) {
        if (sceneManager != null) {
            ColorAttribute ambientLight = (ColorAttribute) sceneManager.environment.get(ColorAttribute.AmbientLight);
            if (ambientLight == null) {
                ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, intensity, intensity, intensity, 1.0f);
                sceneManager.environment.set(ambientLight);
            } else {
                ambientLight.color.set(intensity, intensity, intensity, 1.0f);
            }

            try {
                java.lang.reflect.Method registerMethod = com.badlogic.gdx.graphics.g3d.Attribute.class.getDeclaredMethod("register", String.class);
                registerMethod.setAccessible(true);
                long envIntensityType = (long) registerMethod.invoke(null, "envIntensity");

                sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute(envIntensityType, intensity));
            } catch (Exception e) {
                Gdx.app.error("ThreeDManager", "Failed to register envIntensity via reflection: " + e.getMessage());
            }
        }
    }


    public void setPointLight(String lightId, float x, float y, float z, float r, float g, float b, float intensity, float range) {
        net.mgsx.gltf.scene3d.lights.SpotLightEx oldSpot = spotLights.remove(lightId);

        if (oldSpot != null && sceneManager != null) {
            sceneManager.environment.remove(oldSpot);
        }

        net.mgsx.gltf.scene3d.lights.PointLightEx light = pointLights.get(lightId);
        if (light == null) {
            light = new net.mgsx.gltf.scene3d.lights.PointLightEx();
            pointLights.put(lightId, light);
        }
        light.position.set(x, y, z);
        light.color.set(r, g, b, 1);
        light.intensity = intensity;
        light.range = range > 0 ? range : null;
    }

    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    public void setSpotLight(String lightId, float x, float y, float z, float dirX, float dirY, float dirZ, float r, float g, float b, float intensity, float cutoffAngle, float softness, float range) {
        net.mgsx.gltf.scene3d.lights.PointLightEx oldPoint = pointLights.remove(lightId);
        if (oldPoint != null && sceneManager != null) {
            sceneManager.environment.remove(oldPoint);
        }

        net.mgsx.gltf.scene3d.lights.SpotLightEx light = spotLights.get(lightId);
        if (light == null) {
            light = new net.mgsx.gltf.scene3d.lights.SpotLightEx();
            spotLights.put(lightId, light);
            if (sceneManager != null) sceneManager.environment.add(light);
        }
        light.position.set(x, y, z);
        light.direction.set(dirX, dirY, dirZ).nor();
        light.color.set(r, g, b, 1);
        light.intensity = intensity;

        float s = Math.max(0.0f, Math.min(1.0f, softness));
        float innerAngle = cutoffAngle * (1.0f - s);

        light.setConeDeg(cutoffAngle, innerAngle);

        light.range = range > 0 ? range : null;
    }

    @Deprecated
    public void setSpotLightOld(String lightId, float x, float y, float z, float dirX, float dirY, float dirZ, float r, float g, float b, float intensity, float cutoffAngle, float exponent, float range) {
        net.mgsx.gltf.scene3d.lights.PointLightEx oldPoint = pointLights.remove(lightId);
        if (oldPoint != null && sceneManager != null) {
            sceneManager.environment.remove(oldPoint);
        }

        net.mgsx.gltf.scene3d.lights.SpotLightEx light = spotLights.get(lightId);
        if (light == null) {
            light = new net.mgsx.gltf.scene3d.lights.SpotLightEx();
            spotLights.put(lightId, light);
            sceneManager.environment.add(light);
        }
        light.position.set(x, y, z);
        light.direction.set(dirX, dirY, dirZ).nor();
        light.color.set(r, g, b, 1);
        light.intensity = intensity;
        light.cutoffAngle = map(cutoffAngle, 0, 360, -1000, 1000);
        light.exponent = exponent * 1000;
        light.range = range > 0 ? range : null;
    }


    public boolean removePBRLight(String lightId) {
        boolean removed = false;
        net.mgsx.gltf.scene3d.lights.PointLightEx pointLight = pointLights.remove(lightId);
        if (pointLight != null) {
            if (sceneManager != null) {
                sceneManager.environment.remove(pointLight);
            }
            removed = true;
        }
        net.mgsx.gltf.scene3d.lights.SpotLightEx spotLight = spotLights.remove(lightId);
        if (spotLight != null) {
            if (sceneManager != null) {
                sceneManager.environment.remove(spotLight);
            }
            removed = true;
        }
        return removed;
    }


    public void setObjectAnisotropicFilter(String objectId, float level) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            for (com.badlogic.gdx.graphics.g3d.Attribute attr : material) {
                if (attr instanceof com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) {
                    Texture texture = ((com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) attr).textureDescription.texture;
                    if (texture != null) {
                        texture.setAnisotropicFilter(level);
                    }
                }
            }
        }
    }


    public void setFog(float r, float g, float b, float density) {
        ColorAttribute fogAttr = new ColorAttribute(ColorAttribute.Fog, r, g, b, 1.0f);

        if (density > 0) {
            if (sceneManager != null) {
                sceneManager.environment.set(fogAttr);
            }
            environment.set(fogAttr);
            camera.far = 1f / density;
        } else {
            camera.far = 1000f;
            if (sceneManager != null) sceneManager.environment.remove(ColorAttribute.Fog);
            environment.remove(ColorAttribute.Fog);
        }
        camera.update();
    }

    public void setAngularFactor(String objectId, float x, float y, float z) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {



            body.setAngularFactor(new Vector3(x, y, z));


            body.activate();
        }
    }


    public void setSkyColor(float r, float g, float b) {
        skyColor.set(r, g, b, 1f);
    }


    public Vector3 getCameraPosition() {
        return camera.position;
    }

    public void renameObject(String oldId, String newId) {

        if (sceneObjects.containsKey(oldId)) {
            sceneObjects.put(newId, sceneObjects.remove(oldId));
        }

        if (physicsBodies.containsKey(oldId)) {
            physicsBodies.put(newId, physicsBodies.remove(oldId));
        }

        if (physicsResources.containsKey(oldId)) {
            physicsResources.put(newId, physicsResources.remove(oldId));
        }

        if (animationControllers.containsKey(oldId)) {
            animationControllers.put(newId, animationControllers.remove(oldId));
        }

        if (gltfObjectIds.contains(oldId)) {
            gltfObjectIds.remove(oldId);
            gltfObjectIds.add(newId);
        }

        if (editorProxies.containsKey(oldId)) {
            editorProxies.put(newId, editorProxies.remove(oldId));
        }

        if (pointLights.containsKey(oldId)) {
            pointLights.put(newId, pointLights.remove(oldId));
        }
        if (spotLights.containsKey(oldId)) {
            spotLights.put(newId, spotLights.remove(oldId));
        }
    }


    public Vector3 getCameraDirection() {
        return camera.direction;
    }

    public Matrix4 getWorldTransform(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            return instance.transform;
        }
        return null;
    }

    public void setPhysicsState(String objectId, PhysicsState state, PhysicsShape shape, float mass, Matrix4 worldTransform) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;


        instance.transform.set(worldTransform);


        setPhysicsState(objectId, state, shape, mass);
    }

    public void setShadowSettings(float size, int resolution, boolean enableCSM, float csmFactor) {
        this.currentShadowSize = Math.max(1f, size);
        boolean csmChanged = (this.useCSM != enableCSM);
        this.useCSM = enableCSM;
        this.csmSplitFactor = Math.max(1f, csmFactor);

        if (this.currentShadowResolution != resolution || csmChanged) {
            this.currentShadowResolution = resolution;
            Gdx.app.postRunnable(this::recreateShadowLight);
        }
    }

    public void setShadowSettings(float size, int resolution) {
        setShadowSettings(size, resolution, this.useCSM, 4f);
    }

    public float getShadowSize() { return currentShadowSize; }
    public float getShadowResolution() { return (float) currentShadowResolution; }

    private void recreateShadowLight() {
        if (sceneManager == null) return;

        if (pbrLight != null) {
            sceneManager.environment.remove(pbrLight);
            if (pbrLight instanceof Disposable) {
                ((Disposable) pbrLight).dispose();
            }
        }

        if (csm != null) {
            csm.dispose();
            csm = null;
            sceneManager.setCascadeShadowMap(null);
        }

        net.mgsx.gltf.scene3d.lights.DirectionalShadowLight shadowLight =
                new net.mgsx.gltf.scene3d.lights.DirectionalShadowLight(
                        currentShadowResolution,
                        currentShadowResolution
                );

        shadowLight.direction.set(1, -1.5f, 1).nor();
        shadowLight.color.set(Color.WHITE);
        shadowLight.intensity = 5.0f;

        sceneManager.environment.add(shadowLight);
        pbrLight = shadowLight;

        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute(
                net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.ShadowBias, 0.0005f));

        if (useCSM) {
            csm = new net.mgsx.gltf.scene3d.scene.CascadeShadowMap(3);
            sceneManager.setCascadeShadowMap(csm);
        }

        updateProceduralIBL();
    }


    private void updateShadowLight() {
        if (pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) {
            net.mgsx.gltf.scene3d.lights.DirectionalShadowLight sun =
                    (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;

            if (useCSM && csm != null) {
                csm.setCascades(camera, sun, currentShadowSize, csmSplitFactor);
            } else {
                slCenter.set(camera.position);
                slForward.set(camera.direction).scl(currentShadowSize * 0.4f);
                slForward.y = 0;
                slCenter.add(slForward);
                slCenter.y = 0;
                sun.setCenter(slCenter);

                Camera shadowCam = sun.getCamera();
                shadowCam.viewportWidth = currentShadowSize;
                shadowCam.viewportHeight = currentShadowSize;

                float depthRange = currentShadowSize * 2f;
                shadowCam.near = -depthRange / 2f;
                shadowCam.far = depthRange / 2f;
                shadowCam.update();
            }
        }
    }

    private static class BoneAttachment {
        String childId;
        String parentModelId;
        String boneName;


        Vector3 localPosOffset = new Vector3();
        Quaternion localRotOffset = new Quaternion();

        public BoneAttachment(String child, String parent, String bone) {
            this.childId = child;
            this.parentModelId = parent;
            this.boneName = bone;
        }
    }


    private final List<BoneAttachment> activeAttachments = new ArrayList<>();



    public void attachObjectToBone(String childId, String parentId, String boneName, float offsetX, float offsetY, float offsetZ) {

        detachObject(childId);

        BoneAttachment attachment = new BoneAttachment(childId, parentId, boneName);
        attachment.localPosOffset.set(offsetX, offsetY, offsetZ);

        activeAttachments.add(attachment);
    }


    public void detachObject(String childId) {
        activeAttachments.removeIf(a -> a.childId.equals(childId));
    }

    public void update(float delta) {
        if (!initialized) return;
        if (LOG_THREED_MANAGER_DEBUG) Log.d("TDM_DEBUG", "--- ThreeDManager.update() START (Delta: " + delta + ") ---");
        if (cameraTargetId != null) {
            updateThirdPersonCamera();
        } else if (cameraTrackMode != 0 && cameraTrackTargetId != null) {
            updateCameraTracking();
        }

        updateCameraAttachments();
        updateActivePointLights();

        updateShadowLight();

        for (com.badlogic.gdx.graphics.g3d.utils.AnimationController controller : animationControllers.values()) {
            controller.update(delta);
        }

        if (realisticMode) {
            sceneManager.update(delta);
        }

        time += delta;

        if (customShaderProvider != null) {
            customUniforms.put("u_time", time);
            customUniforms.put("u_resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            customUniforms.put("u_cameraPosition", camera.position);
            customUniforms.put("u_cameraDirection", camera.direction);
            customUniforms.put("u_viewMatrix", camera.view);
            customUniforms.put("u_projectionMatrix", camera.projection);
        }

        if (!editorMode) {
            updateContinuousSensors();
            updateAIProcessing(delta);

            dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                String objectId = entry.getKey();
                btRigidBody body = entry.getValue();

                boolean isManagedBySceneManager = (Objects.requireNonNull(StageActivity.getActiveStageListener()).sceneManager != null && Objects.requireNonNull(StageActivity.getActiveStageListener()).sceneManager.findGameObject(objectId) != null);

                if (!isManagedBySceneManager) {
                    ModelInstance instance = sceneObjects.get(objectId);
                    if (instance != null && !body.isStaticObject() && body.isActive()) {
                        Matrix4 bodyTransform = body.getWorldTransform();
                        bodyTransform.getTranslation(tmpPos);
                        bodyTransform.getRotation(tmpRot, true);

                        Quaternion offset = modelRotationOffsets.get(objectId);
                        if (offset != null) {
                            tmpRot.mul(offset);
                        }

                        instance.transform.getScale(tmpScale);
                        instance.transform.set(tmpPos, tmpRot, tmpScale);
                    }
                }
            }
        }

        if (!editorMode && LOG_THREED_MANAGER_DEBUG) {
            Log.d("TDM_DEBUG", "    [Physics] After stepSimulation:");
            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                Vector3 physBodyPos = new Vector3();
                entry.getValue().getWorldTransform().getTranslation(physBodyPos);
                Log.d("TDM_DEBUG", "      - '" + entry.getKey() + "' Physics Pos: " + physBodyPos);
            }
        }

        Matrix4 tempBoneMat = new Matrix4();
        for (BoneOverride override : boneOverrides) {
            ModelInstance model = sceneObjects.get(override.modelId);
            ModelInstance target = sceneObjects.get(override.targetId);
            if (model != null && target != null) {

                Node bone = model.getNode(override.boneName, true);
                if (bone != null) {

                    tempBoneMat.set(model.transform).inv().mul(target.transform);


                    bone.globalTransform.set(tempBoneMat);
                    model.calculateTransforms();
                } else {
                    Log.e("ThreeDManager", "ОШИБКА IK: Кость '" + override.boneName + "' не найдена!");
                }
            }
        }

        if (!editorMode) {
            update3DAudio();
        }
        applyCameraEffects(delta);
        if (LOG_THREED_MANAGER_DEBUG) Log.d("TDM_DEBUG", "--- ThreeDManager.update() END ---");
    }

    private void update3DAudio() {
        if (active3DSounds.isEmpty()) {
            return;
        }

        Vector3 listenerPos = camera.position;
        cameraRight.set(camera.direction).crs(camera.up).nor();

        Iterator<PlayableAudio> iterator = active3DSounds.iterator();
        while (iterator.hasNext()) {
            PlayableAudio audio = iterator.next();

            if (!audio.isPlaying()) {
                audio.dispose();
                iterator.remove();
                continue;
            }

            String attachedId = audio.getAttachedObjectId();
            if (attachedId != null) {
                ModelInstance attachedObject = sceneObjects.get(attachedId);
                if (attachedObject != null) {
                    attachedObject.transform.getTranslation(tmpPos);
                    audio.setPosition(tmpPos);
                } else {
                    audio.stop();
                    audio.dispose();
                    iterator.remove();
                    continue;
                }
            }

            float distance = listenerPos.dst(audio.getPosition());
            float maxDist = audio.getMaxDistance();

            float finalVolume;
            float finalPan;

            if (distance > maxDist) {
                finalVolume = 0;
                finalPan = 0;
            } else {
                finalVolume = (1.0f - (distance / maxDist)) * globalSoundVolume;

                soundToListener.set(audio.getPosition()).sub(listenerPos).nor();
                finalPan = soundToListener.dot(cameraRight);
            }

            audio.update3D(finalVolume, finalPan);
        }
    }

    public void set3DSoundMaxDistance(String instanceName, float maxDistance) {
        for (PlayableAudio audio : active3DSounds) {
            if (instanceName.equals(audio.getInstanceName())) {
                audio.setMaxDistance(maxDistance);
                break;
            }
        }
    }

    public void updateSoundPosition(String instanceName, float x, float y, float z) {
        if (instanceName == null || instanceName.isEmpty()) return;

        for (PlayableAudio audio : active3DSounds) {
            if (instanceName.equals(audio.getInstanceName())) {
                audio.setAttachedObjectId(null);
                audio.setPosition(new Vector3(x, y, z));
                break;
            }
        }
    }

    public void applyTorque(String objectId, float forceX, float forceY, float forceZ) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) {
            body.activate();
            body.applyTorque(new Vector3(forceX, forceY, forceZ));
        }
    }

    public boolean createConfigurableSpringConstraint(
            String constraintId, String objectIdA, String objectIdB,
            Vector3 pivotA, Vector3 pivotB,
            boolean springX, boolean springY, boolean springZ,
            Vector3 minLimit, Vector3 maxLimit,
            float stiffness, float damping) {

        if (physicsConstraints.containsKey(constraintId)) return false;

        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) return false;

        bodyA.activate();
        bodyB.activate();

        Matrix4 frameInA = new Matrix4().setToTranslation(pivotA);
        Matrix4 frameInB = new Matrix4().setToTranslation(pivotB);

        btGeneric6DofSpringConstraint constraint = new btGeneric6DofSpringConstraint(bodyA, bodyB, frameInA, frameInB, true);

        constraint.setLinearLowerLimit(minLimit);
        constraint.setLinearUpperLimit(maxLimit);

        if (springX) {
            constraint.enableSpring(0, true);
            constraint.setStiffness(0, stiffness);
            constraint.setDamping(0, damping);
        }
        if (springY) {
            constraint.enableSpring(1, true);
            constraint.setStiffness(1, stiffness);
            constraint.setDamping(1, damping);
        }
        if (springZ) {
            constraint.enableSpring(2, true);
            constraint.setStiffness(2, stiffness);
            constraint.setDamping(2, damping);
        }

        dynamicsWorld.addConstraint(constraint, true);
        physicsConstraints.put(constraintId, constraint);
        return true;
    }

    public boolean createSpringConstraint(String constraintId, String objectIdA, String objectIdB, Matrix4 frameInA, Matrix4 frameInB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);

        btRigidBody bodyB = physicsBodies.get(objectIdB);
        if (bodyB == null) {
            Gdx.app.error("3DManager", "Cannot create spring constraint: bodyB not found.");
            return false;
        }

        if (bodyA == null) {
            Gdx.app.error("3DManager", "Cannot create spring constraint: bodyA not found.");
            return false;
        }

        btGeneric6DofSpringConstraint springConstraint = new btGeneric6DofSpringConstraint(bodyA, bodyB, frameInA, frameInB, true);
        dynamicsWorld.addConstraint(springConstraint, true);
        physicsConstraints.put(constraintId, springConstraint);
        return true;
    }

    public boolean createHingeConstraint(String constraintId, String objectIdA, String objectIdB,
                                         Vector3 pivotInA, Vector3 axisInA,
                                         Vector3 pivotInB, Vector3 axisInB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) {
            Gdx.app.error("3DManager", "Cannot create hinge: one or both bodies not found.");
            return false;
        }
        btHingeConstraint hinge = new btHingeConstraint(bodyA, bodyB, pivotInA, pivotInB, axisInA, axisInB, true);
        dynamicsWorld.addConstraint(hinge, true);
        physicsConstraints.put(constraintId, hinge);
        return true;
    }

    public void setHingeMotorTarget(String constraintId, float targetAngleDegrees, float maxMotorImpulse) {
        btTypedConstraint constraint = physicsConstraints.get(constraintId);
        if (constraint instanceof btHingeConstraint) {
            btHingeConstraint hinge = (btHingeConstraint) constraint;
            hinge.enableAngularMotor(true, 0, maxMotorImpulse);
            hinge.setMotorTarget(targetAngleDegrees * MathUtils.degreesToRadians, 0.1f);
        }
    }

    public void setAngularSpringProperties(String constraintId, int axisIndex, float stiffness, float damping) {
        btTypedConstraint constraint = physicsConstraints.get(constraintId);
        if (constraint instanceof btGeneric6DofSpringConstraint) {
            btGeneric6DofSpringConstraint spring = (btGeneric6DofSpringConstraint) constraint;
            int bulletAxisIndex = axisIndex + 3;

            spring.enableSpring(bulletAxisIndex, true);
            spring.setStiffness(bulletAxisIndex, stiffness);
            spring.setDamping(bulletAxisIndex, damping);
        }
    }


    public btRigidBody getPhysicsBody(String objectId) {
        return physicsBodies.get(objectId);
    }

    private void updateThirdPersonCamera() {
        ModelInstance target = sceneObjects.get(cameraTargetId);
        if (target == null) {
            setFreeCamera();
            return;
        }

        target.transform.getTranslation(tpTargetPos);
        tpLookAt.set(tpTargetPos).add(cameraOffset);

        tpRot.set(Vector3.Y, cameraYaw);
        tpPitchRot.set(Vector3.X, -cameraPitch);
        tpRot.mul(tpPitchRot);

        tpPosOffset.set(0, 0, cameraDistance).mul(tpRot);

        camera.position.set(tpLookAt).add(tpPosOffset);
        camera.lookAt(tpLookAt);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    public void addCameraRotation(float yawDelta, float pitchDelta) {
        if (cameraTargetId != null) {
            cameraYaw += yawDelta;
            cameraPitch += pitchDelta;

            if (cameraPitch > 89.0f) cameraPitch = 89.0f;
            if (cameraPitch < -89.0f) cameraPitch = -89.0f;
        }
        else {
            camera.view.getRotation(tmpRot);
            camera.rotate(Vector3.Y, yawDelta);

            tmpVec.set(camera.direction).crs(camera.up).nor();
            camera.rotate(tmpVec, pitchDelta);

            camera.update();
        }
    }

    public void setCameraRotation(float yaw, float pitch) {
        if (cameraTargetId == null) return;

        cameraYaw = yaw;
        cameraPitch = pitch;

        if (cameraPitch > 89.0f) cameraPitch = 89.0f;
        if (cameraPitch < -89.0f) cameraPitch = -89.0f;
    }

    public void playSoundAt(String instanceName, String soundName, float x, float y, float z, float volume, float pitch, boolean loop) {
        playSoundInternal(instanceName, soundName, volume, pitch, loop, new Vector3(x, y, z), null);
    }

    public void playSoundAttached(String instanceName, String soundName, String objectId, float volume, float pitch, boolean loop) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;
        Vector3 initialPos = instance.transform.getTranslation(new Vector3());
        playSoundInternal(instanceName, soundName, volume, pitch, loop, initialPos, objectId);
    }

    public boolean prepareAudio(String soundName, String fileName, boolean asMusic) {
        if (soundName == null || soundName.isEmpty() || loadedAudioAssets.containsKey(soundName)) {
            return false;
        }
        try {
            File soundFile = ProjectManager.getInstance().getCurrentProject().getFile(fileName);
            if (soundFile != null && soundFile.exists()) {
                FileHandle fileHandle = Gdx.files.absolute(soundFile.getAbsolutePath());
                AudioAsset asset = new AudioAsset(fileHandle, asMusic);
                loadedAudioAssets.put(soundName, asset);
                return true;
            }
        } catch (Exception e) {
            Gdx.app.error("ThreeDManager_Audio", "Failed to load audio: " + fileName, e);
        }
        return false;
    }


    private void playSoundInternal(String instanceName, String soundName, float volume, float pitch, boolean loop, Vector3 position, String attachedToId) {
        if (instanceName == null || instanceName.isEmpty()) return;
        stopSound(instanceName);

        AudioAsset asset = loadedAudioAssets.get(soundName);
        if (asset == null) return;

        PlayableAudio playableAudio;
        if (asset.music != null) {

            Music newMusicInstance = Gdx.audio.newMusic(Gdx.files.absolute(asset.filePath));
            playableAudio = new MusicWrapper(newMusicInstance, volume, pitch, loop);
        } else {
            playableAudio = new SoundWrapper(asset.sound, volume, pitch, loop);
        }

        playableAudio.setInstanceName(instanceName);
        playableAudio.setPosition(position);
        playableAudio.setAttachedObjectId(attachedToId);

        playableAudio.play();
        active3DSounds.add(playableAudio);
    }


    public void stopSound(String instanceName) {
        Iterator<PlayableAudio> iterator = active3DSounds.iterator();
        while (iterator.hasNext()) {
            PlayableAudio audio = iterator.next();
            if (audio.getInstanceName().equals(instanceName)) {
                audio.stop();
                audio.dispose();
                iterator.remove();
                break;
            }
        }
    }


    public void setGlobalSoundVolume(float volume) {
        this.globalSoundVolume = Math.max(0, Math.min(1, volume));
    }

    public Array<String> getAnimationNames(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        Array<String> names = new Array<>();
        if (instance != null && instance.animations.size > 0) {
            for (Animation anim : instance.animations) {
                names.add(anim.id);
            }
        }
        return names;
    }

    public void setSkybox(String panoramicTexturePath) {
        if (disposed || sceneManager == null) {
            Gdx.app.error("Skybox", "Aborted setting skybox: ThreeDManager is disposed.");
            return;
        }

        if (!realisticMode) {
            Gdx.app.error("Skybox", "Skybox can only be enabled in realistic rendering mode.");
            return;
        }

        if (panoramicConverter == null) {
            Gdx.app.error("Skybox", "PanoramicConverter is not initialized yet. Deferring task.");

            Gdx.app.postRunnable(() -> setSkybox(panoramicTexturePath));
            return;
        }

        if (panoramicTexturePath == null || panoramicTexturePath.isEmpty()) {
            if (skybox != null) {
                sceneManager.setSkyBox(null);
                skybox.dispose();
                skybox = null;
            }
            if (skyboxCubemap != null) {
                skyboxCubemap.dispose();
                skyboxCubemap = null;
            }
            updateProceduralIBL();
            Gdx.app.log("Skybox", "Skybox cleared. Reverted to procedural IBL.");
            return;
        }

        try {
            FileHandle textureFile = Gdx.files.absolute(panoramicTexturePath);
            if (!textureFile.exists()) {
                Gdx.app.error("Skybox", "Skybox texture not found: " + panoramicTexturePath);
                return;
            }

            Texture panoramicTexture = new Texture(textureFile);

            if (skyboxCubemap != null) skyboxCubemap.dispose();
            if (skybox != null) skybox.dispose();

            skyboxCubemap = panoramicConverter.convert(panoramicTexture, 1024);
            skybox = new net.mgsx.gltf.scene3d.scene.SceneSkybox(skyboxCubemap);
            sceneManager.setSkyBox(skybox);


            updateIBLFromCubemap(skyboxCubemap);

            panoramicTexture.dispose();

            Gdx.app.log("Skybox", "Skybox and its Image-Based Lighting set successfully.");

        } catch (Exception e) {
            Gdx.app.error("Skybox", "Failed to set skybox", e);
        }
    }

    private void updateProceduralIBL() {
        if (disposed || sceneManager == null || pbrLight == null) return;

        if (diffuseCubemap != null) diffuseCubemap.dispose();
        if (specularCubemap != null) specularCubemap.dispose();

        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(pbrLight);

        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);

        iblBuilder.dispose();

        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.DiffuseEnv, diffuseCubemap));
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.SpecularEnv, specularCubemap));
    }



    private void updateIBLFromCubemap(Cubemap sourceCubemap) {
        if (disposed || sceneManager == null) return;

        if (iblBuilderCompat == null) {
            Gdx.app.error("IBL", "IBLBuilderCompat not initialized yet. Deferring IBL update.");

            Gdx.app.postRunnable(() -> updateIBLFromCubemap(sourceCubemap));
            return;
        }

        if (diffuseCubemap != null) diffuseCubemap.dispose();
        if (specularCubemap != null) specularCubemap.dispose();


        diffuseCubemap = iblBuilderCompat.buildIrradianceMap(sourceCubemap, 32);



        specularCubemap = iblBuilderCompat.buildRadianceMap(sourceCubemap, 128, 5);



        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.DiffuseEnv, diffuseCubemap));
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.SpecularEnv, specularCubemap));
        Gdx.app.log("IBL", "Image-Based Lighting recalculated from skybox texture via Compat builder.");
    }





    public void enableRealisticRendering(boolean enabled) {
        this.realisticMode = enabled;
    }


    public void setVelocity(String objectId, float vx, float vy, float vz) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) {
            body.activate();
            body.setLinearVelocity(new Vector3(vx, vy, vz));
        }
    }

    public void setGravity(float x, float y, float z) {
        dynamicsWorld.setGravity(new Vector3(x, y, z));
    }

    public void renderShadowsOnly() {
        camera.update();
        if (realisticMode) {
            Gdx.gl.glEnable(GL20.GL_CULL_FACE);
            Gdx.gl.glCullFace(GL20.GL_FRONT);

            sceneManager.renderShadows();

            Gdx.gl.glCullFace(GL20.GL_BACK);
        }
    }

    public void renderColorsOnly(int vpWidth, int vpHeight) {
        camera.update();
        Gdx.gl.glViewport(0, 0, vpWidth, vpHeight);

        if (realisticMode) {
            if (skyColor.a != 0) {
                Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            }
            sceneManager.renderMirror();
            sceneManager.renderTransmission();
            sceneManager.renderColors();
        } else {
            if (skyColor.a != 0) {
                Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
            }

            modelBatch.begin(camera);

            for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
                if (!inactiveRenderObjects.contains(entry.getKey())) {
                    modelBatch.render(entry.getValue(), environment);
                }
            }
            modelBatch.end();
        }

        if (particleSystemInitialized && !activeParticleEffects.isEmpty()) {

            particleModelBatch.begin(camera);
            particleModelBatch.render(particleSystem);
            particleModelBatch.end();


            if (!effectsNormal.isEmpty()) {
                Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
                Gdx.gl.glDepthMask(true);
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                batchNormal.setCamera(camera);
                batchNormal.begin();
                for (ParticleEffect effect : effectsNormal) effect.draw();
                batchNormal.end();
            }

            if (!effectsAdditive.isEmpty()) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                Gdx.gl.glDepthMask(false);

                batchAdditive.setCamera(camera);
                batchAdditive.begin();
                for (ParticleEffect effect : effectsAdditive) effect.draw();
                batchAdditive.end();

                Gdx.gl.glDepthMask(true);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

        if (debugEnabled) {
            debugDrawer.begin(camera);
            dynamicsWorld.debugDrawWorld();
            debugDrawer.end();
        }
    }

    private void drawFinalTextureToScreen(Texture texture) {
        int width = lastScreenWidth > 0 ? lastScreenWidth : Gdx.graphics.getWidth();
        int height = lastScreenHeight > 0 ? lastScreenHeight : Gdx.graphics.getHeight();

        Gdx.gl.glViewport(0, 0, width, height);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        blitCamera.setToOrtho(false, width, height);
        blitCamera.update();
        blitBatch.setProjectionMatrix(blitCamera.combined);

        float screenAspect = (float) width / height;
        float targetAspect = (float) renderWidth / renderHeight;

        float drawWidth = width;
        float drawHeight = height;

        if (screenAspect > targetAspect) {
            drawWidth = height * targetAspect;
        } else {
            drawHeight = width / targetAspect;
        }

        float drawX = (width - drawWidth) / 2f;
        float drawY = (height - drawHeight) / 2f;

        blitBatch.begin();
        blitBatch.draw(texture, drawX, drawY, drawWidth, drawHeight, 0, 0, texture.getWidth(), texture.getHeight(), false, true);
        blitBatch.end();
    }

    private SpriteBatch blitBatch;
    private OrthographicCamera blitCamera;
    private TextureRegion sceneFboRegion;

    private boolean isDepthRenderEnabled = false;

    private int targetFps = 0;
    private float renderTimeAccumulator = 0f;
    private Texture lastRenderedTexture = null;

    public void setTargetFps(int fps) {
        this.targetFps = fps;
    }

    public void setDepthRender(boolean value) {isDepthRenderEnabled = value; }

    public void render() {
        if (!initialized) return;
        try {
            float delta = Gdx.graphics.getDeltaTime();

            updateParticles(delta);
            updateParticles3D(delta);
            camera.update();

            boolean shouldRender = false;
            if (targetFps > 0) {
                renderTimeAccumulator += delta;
                float targetDelay = 1f / targetFps;
                if (renderTimeAccumulator >= targetDelay || lastRenderedTexture == null) {
                    shouldRender = true;
                    renderTimeAccumulator = Math.max(0, renderTimeAccumulator - targetDelay);
                }
            } else {
                shouldRender = true;
                renderTimeAccumulator = 0;
            }

            if (shouldRender) {
                if (isDepthRenderEnabled) {
                    depthFbo.begin();
                    Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                    depthBatch.begin(camera);

                    if (realisticMode && sceneManager != null) {
                        depthBatch.render(sceneManager.getRenderableProviders());
                    }
                    for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
                        String id = entry.getKey();

                        if (inactiveRenderObjects.contains(id)) {
                            continue;
                        }

                        ModelInstance inst = entry.getValue();
                        boolean isTransparent = false;
                        for (Material mat : inst.materials) {
                            if (mat.has(BlendingAttribute.Type)) {
                                BlendingAttribute blend = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                                if (blend.opacity < 0.1f) {
                                    isTransparent = true;
                                    break;
                                }
                            }
                        }
                        if (isTransparent) {
                            continue;
                        }

                        depthBatch.render(inst);
                    }

                    depthBatch.end();

                    PostProcessingData.DepthOfField dofData = null;
                    if (currentConfig != null && currentConfig.isActive) {
                        for (PostProcessingData e : currentConfig.effects) {
                            if (e instanceof PostProcessingData.DepthOfField && e.isEnabled) {
                                dofData = (PostProcessingData.DepthOfField) e;
                                break;
                            }
                        }
                    }

                    if (dofData != null && dofData.autoFocus) {
                        dofPixelsBuffer.clear();

                        int x = depthFbo.getWidth() / 2;
                        int y = depthFbo.getHeight() / 2;

                        Gdx.gl.glReadPixels(x, y, 2, 2, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, dofPixelsBuffer);
                        dofPixelsBuffer.rewind();

                        float sumDist = 0;
                        int validSamples = 0;

                        for (int i = 0; i < 4; i++) {
                            int r = dofPixelsBuffer.get() & 0xFF;
                            int g = dofPixelsBuffer.get() & 0xFF;
                            int b = dofPixelsBuffer.get() & 0xFF;
                            int a = dofPixelsBuffer.get() & 0xFF;

                            float normDist = (r / 255.0f) + (g / (255.0f * 255.0f));
                            if (r == 128 && b != 128) {
                                normDist = (b / 255.0f) + (a / (255.0f * 255.0f));
                            }

                            if (normDist > 0.0001f) {
                                sumDist += normDist;
                                validSamples++;
                            }
                        }

                        float finalNormDist = (validSamples > 0) ? (sumDist / validSamples) : 1.0f;
                        float targetDist = finalNormDist * camera.far;

                        if (Float.isNaN(dofData.focusDistance) || dofData.focusDistance <= 0) {
                            dofData.focusDistance = targetDist;
                        } else {
                            float lerpSpeed = dofData.autoFocusSpeed * delta;
                            dofData.focusDistance += (targetDist - dofData.focusDistance) * Math.min(lerpSpeed, 1.0f);
                        }

                        if (dofEffect != null) dofEffect.focusDistance = dofData.focusDistance;
                    }

                    depthFbo.end();

                    materialFbo.begin();
                    Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                    materialBatch.begin(camera);

                    if (realisticMode && sceneManager != null) {
                        materialBatch.render(sceneManager.getRenderableProviders());
                    }
                    materialBatch.render(sceneObjects.values());

                    materialBatch.end();
                    materialFbo.end();

                    if (rayTracingEffect != null) {
                        rayTracingEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                        rayTracingEffect.setMaterialTexture(materialFbo.getColorBufferTexture());
                    }
                    if (ssaoEffect != null && currentConfig.hasEffect(PostProcessingData.SSAO.class)) {
                        ssaoEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                        ssaoEffect.setCamera(camera);
                    }
                    if (heightFogEffect != null) {
                        heightFogEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                        heightFogEffect.setCamera(camera);
                    }
                    if (godRaysEffect != null) {
                        godRaysEffect.setDepthTexture(depthFbo.getColorBufferTexture());
                        godRaysEffect.setCamera(camera);
                        godRaysEffect.setSunDirection(getSunLightDirection());
                    }
                    if (volumetricFogEffect != null && currentConfig.hasEffect(PostProcessingData.VolumetricFog.class)) {
                        volumetricFogEffect.setCamera(camera);

                        if (pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) {
                            net.mgsx.gltf.scene3d.lights.DirectionalShadowLight sl = (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;
                            volumetricFogEffect.setShadowMap(sl.getFrameBuffer().getColorBufferTexture(), sl.getCamera().combined);
                            volumetricFogEffect.setLightParams(sl.direction, sl.color);
                        }
                    }
                }
            }

            if (postprocessingEnabled) {
                if (shouldRender) {
                    if (vfxManager != null) {
                        vfxManager.update(delta);
                    }

                    renderShadowsOnly();

                    sceneFbo2.begin();
                    renderColorsOnly(renderWidth, renderHeight);
                    sceneFbo2.end();

                    if (currentConfig.hasEffect(PostProcessingData.EyeAdaptation.class)) {
                        PostProcessingData.EyeAdaptation settings = (PostProcessingData.EyeAdaptation) currentConfig.effects.stream()
                                .filter(e -> e instanceof PostProcessingData.EyeAdaptation).findFirst().orElse(null);
                        if (settings != null) {
                            eyeAdaptationManager.update(delta, sceneFbo2.getColorBufferTexture(), settings);
                            if (exposureEffect != null) {
                                exposureEffect.setExposure(eyeAdaptationManager.getCurrentExposure());
                            }
                        }
                    }

                    vfxManager.cleanUpBuffers();
                    vfxManager.beginInputCapture();
                    isVfxCapturing = true;

                    blitCamera.update();
                    blitBatch.setProjectionMatrix(blitCamera.combined);
                    blitBatch.begin();

                    blitBatch.draw(sceneFboRegion, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    blitBatch.end();

                    vfxManager.endInputCapture();
                    isVfxCapturing = false;
                    vfxManager.applyEffects();

                    applyUniformsToScreenShader();

                    lastRenderedTexture = vfxManager.getResultBuffer().getTexture();
                }

                if (lastRenderedTexture != null) {
                    if (renderScale < 1.0f || aspectMode != 0) {
                        lastRenderedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                        blitCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        blitCamera.update();
                        blitBatch.setProjectionMatrix(blitCamera.combined);

                        float screenWidth = Gdx.graphics.getWidth();
                        float screenHeight = Gdx.graphics.getHeight();
                        float screenAspect = screenWidth / screenHeight;
                        float targetAspect = (float) renderWidth / renderHeight;

                        float drawWidth = screenWidth;
                        float drawHeight = screenHeight;

                        if (screenAspect > targetAspect) {
                            drawWidth = screenHeight * targetAspect;
                        } else {
                            drawHeight = screenWidth / targetAspect;
                        }

                        float drawX = (screenWidth - drawWidth) / 2f;
                        float drawY = (screenHeight - drawHeight) / 2f;

                        if (currentUpscaler != null && currentConfig.qualityScale < 1.0f) {
                            blitBatch.setShader(fsrShader);
                            blitBatch.begin();
                            fsrShader.setUniformf("u_texelSize", 1.0f / lastRenderedTexture.getWidth(), 1.0f / lastRenderedTexture.getHeight());
                            fsrShader.setUniformf("u_sharpness", currentUpscaler.sharpness);
                            blitBatch.draw(lastRenderedTexture, drawX, drawY, drawWidth, drawHeight,
                                    0, 0, lastRenderedTexture.getWidth(), lastRenderedTexture.getHeight(), false, true);
                            blitBatch.end();
                            blitBatch.setShader(null);
                        } else {
                            blitBatch.begin();
                            blitBatch.draw(lastRenderedTexture, drawX, drawY, drawWidth, drawHeight,
                                    0, 0, lastRenderedTexture.getWidth(), lastRenderedTexture.getHeight(), false, true);
                            blitBatch.end();
                        }
                    } else {
                        blitCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        blitCamera.update();
                        blitBatch.setProjectionMatrix(blitCamera.combined);
                        blitBatch.begin();
                        blitBatch.draw(lastRenderedTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                                0, 0, lastRenderedTexture.getWidth(), lastRenderedTexture.getHeight(), false, true);
                        blitBatch.end();
                    }
                }
            } else {
                boolean useFbo = (targetFps > 0) || (renderScale < 1.0f) || (aspectMode != 0);

                if (shouldRender) {
                    if (useFbo) {
                        sceneFbo2.begin();
                    }

                    camera.update();

                    if (realisticMode && sceneManager != null) {
                        if (useFbo) {
                            sceneManager.updateViewport(renderWidth, renderHeight);
                        } else {
                            sceneManager.updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        }
                    }

                    if (realisticMode) {
                        if (skyColor.a != 0) {
                            Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                            Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
                        }
                        sceneManager.render();
                    } else {
                        Gdx.gl.glViewport(0, 0, useFbo ? renderWidth : Gdx.graphics.getWidth(), useFbo ? renderHeight : Gdx.graphics.getHeight());

                        if (skyColor.a != 0) {
                            Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                            Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
                        }

                        modelBatch.begin(camera);

                        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
                            if (!inactiveRenderObjects.contains(entry.getKey())) {
                                modelBatch.render(entry.getValue(), environment);
                            }
                        }
                        modelBatch.end();
                    }

                    if (particleSystemInitialized && !activeParticleEffects.isEmpty()) {
                        particleModelBatch.begin(camera);
                        particleModelBatch.render(particleSystem);
                        particleModelBatch.end();

                        if (!effectsNormal.isEmpty()) {
                            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
                            Gdx.gl.glDepthMask(true);
                            Gdx.gl.glEnable(GL20.GL_BLEND);
                            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                            batchNormal.setCamera(camera);
                            batchNormal.begin();
                            for (ParticleEffect effect : effectsNormal) effect.draw();
                            batchNormal.end();
                        }

                        if (!effectsAdditive.isEmpty()) {
                            Gdx.gl.glEnable(GL20.GL_BLEND);
                            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                            Gdx.gl.glDepthMask(false);

                            batchAdditive.setCamera(camera);
                            batchAdditive.begin();
                            for (ParticleEffect effect : effectsAdditive) effect.draw();
                            batchAdditive.end();

                            Gdx.gl.glDepthMask(true);
                            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                        }
                    }

                    renderParticles3D();

                    if (debugEnabled) {
                        debugDrawer.begin(camera);
                        dynamicsWorld.debugDrawWorld();
                        debugDrawer.end();
                    }

                    if (useFbo) {
                        sceneFbo2.end();
                        lastRenderedTexture = sceneFbo2.getColorBufferTexture();
                    }
                }

                if (useFbo) {
                    if (lastRenderedTexture != null) {
                        drawFinalTextureToScreen(lastRenderedTexture);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ThreeDManager", "FATAL 3D RENDER ERROR", e);
            e.printStackTrace();

            if (isVfxCapturing && vfxManager != null) {
                try {
                    vfxManager.endInputCapture();
                } catch (Exception ignored) {}
                isVfxCapturing = false;
            }

            try { depthFbo.end(); } catch (Exception ignored) {}
            try { depthBatch.end(); } catch (Exception ignored) {}
            try { modelBatch.end(); } catch (Exception ignored) {}
            try { particleModelBatch.end(); } catch (Exception ignored) {}
            try { if (sceneFbo2 != null) sceneFbo2.end(); } catch (Exception ignored) {}
        }
    }

    private <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }


    public void setObjectColor(String objectId, float r, float g, float b) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);
            material.set(ColorAttribute.createDiffuse(r, g, b, 1.0f));
        }
    }


    public void setObjectTexture(String objectId, String texturePath) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null || texturePath == null || texturePath.isEmpty()) return;

        Texture texture = null;

        if (texturePath.startsWith("buffer://")) {
            String bufferName = texturePath.substring(9);
            TextureRegion region = org.catrobat.catroid.content.RenderTextureManager.INSTANCE.getTextureRegion(bufferName);
            if (region != null) {
                texture = region.getTexture();
            }
        } else {
            texture = loadedTextures.get(texturePath);
            if (texture == null) {
                try {
                    FileHandle textureFile = Gdx.files.absolute(texturePath);
                    if (textureFile.exists()) {
                        texture = new com.badlogic.gdx.graphics.Texture(textureFile);
                        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                        loadedTextures.put(texturePath, texture);
                    } else {
                        Gdx.app.error("3DManager", "Texture file not found: " + texturePath);
                        return;
                    }
                } catch (Exception e) {
                    Gdx.app.error("3DManager", "Could not load texture: " + texturePath, e);
                    return;
                }
            }
        }

        if (texture == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(texture));

            material.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(
                    com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
            ));
        }
    }


    private void createMeshPhysicsBody(String objectId, ModelInstance instance) {

        com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray vertexArray =
                new com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray(instance.model.meshParts);

        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);

        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);

        meshShape.setLocalScaling(scale);

        com.badlogic.gdx.math.Matrix4 bodyTransform = new com.badlogic.gdx.math.Matrix4();
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);
        bodyTransform.set(position, rotation);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        float mass = 0f;
        Vector3 localInertia = new Vector3(0, 0, 0);

        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, meshShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        bodyInfo.dispose();
    }




    public boolean checkCollision(String objectId1, String objectId2) {
        btRigidBody body1 = physicsBodies.get(objectId1);
        btRigidBody body2 = physicsBodies.get(objectId2);

        if (body1 == null || body2 == null) {
            return false;
        }

        collisionCallback.collided = false;
        dynamicsWorld.contactPairTest(body1, body2, collisionCallback);

        return collisionCallback.collided;
    }

    public void castRay(Vector3 from, Vector3 direction, RayCastResult outResult) {
        tmpPos.set(from).add(direction.x * camera.far, direction.y * camera.far, direction.z * camera.far);

        com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback callback =
                new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(from, tmpPos);

        callback.setFlags(com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback.EFlags.kF_None);
        dynamicsWorld.rayTest(from, tmpPos, callback);

        if (callback.hasHit()) {
            outResult.hasHit = true;
            callback.getHitPointWorld(outResult.hitPoint);
            callback.getHitNormalWorld(outResult.hitNormal);

            com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = callback.getCollisionObject();
            outResult.hitObjectId = "";
            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                if (entry.getValue().equals(hitObject)) {
                    outResult.hitObjectId = entry.getKey();
                    break;
                }
            }
            outResult.hitDistance = from.dst(outResult.hitPoint);
        } else {
            outResult.hasHit = false;
            outResult.hitObjectId = "";
            outResult.hitDistance = -1.0f;
        }
        callback.dispose();
    }


    public void castRay(String rayName, Vector3 from, Vector3 direction) {
        tmpPos.set(from).add(direction.x * camera.far, direction.y * camera.far, direction.z * camera.far);

        com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback callback =
                new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(from, tmpPos);

        callback.setFlags(com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback.EFlags.kF_None);

        dynamicsWorld.rayTest(from, tmpPos, callback);

        RayCastResult result = rayCastResults.get(rayName);
        if (result == null) {
            result = new RayCastResult();
            rayCastResults.put(rayName, result);
        }
        if (callback.hasHit()) {
            result.hasHit = true;

            callback.getHitPointWorld(result.hitPoint);
            callback.getHitNormalWorld(result.hitNormal);

            com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = callback.getCollisionObject();
            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                if (entry.getValue().equals(hitObject)) {
                    result.hitObjectId = entry.getKey();
                    break;
                }
            }
            result.hitDistance = from.dst(result.hitPoint);
        } else {
            result.hasHit = false;
            result.hitObjectId = "";
            result.hitDistance = -1.0f;
        }

        rayCastResults.put(rayName, result);
        callback.dispose();
    }

    public boolean getRayDidHit(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return result != null && result.hasHit;
    }

    public float getRayHitPointX(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitPoint.x : 0f;
    }

    public float getRayHitPointY(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitPoint.y : 0f;
    }

    public float getRayHitPointZ(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitPoint.z : 0f;
    }

    public float getRayHitNormalX(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitNormal.x : 0f;
    }

    public float getRayHitNormalY(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitNormal.y : 0f;
    }

    public float getRayHitNormalZ(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitNormal.z : 0f;
    }


    public float getRaycastDistance(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null) ? result.hitDistance : -1.0f;
    }

    public void setModelRotationOffset(String objectId, float yaw, float pitch, float roll) {
        modelRotationOffsets.put(objectId, new Quaternion().setEulerAngles(yaw, pitch, roll));

        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 pos = new Vector3();
            Quaternion rot = new Quaternion();
            Vector3 scale = new Vector3();
            instance.transform.getTranslation(pos);
            instance.transform.getScale(scale);

            btRigidBody body = physicsBodies.get(objectId);
            if (body != null) {
                body.getWorldTransform().getRotation(rot, true);
            } else {
                instance.transform.getRotation(rot, true);
            }

            Quaternion offset = modelRotationOffsets.get(objectId);
            if (offset != null) {
                rot.mul(offset);
            }
            instance.transform.set(pos, rot, scale);
            instance.calculateTransforms();
        }
    }

    public void setRotation(String objectId, Quaternion newRotation) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        instance.transform.set(position, newRotation, scale);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 transform = body.getWorldTransform();
            transform.getTranslation(position);
            transform.set(position, newRotation);
            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
            if (body.isStaticObject() || body.isKinematicObject()) {
                dynamicsWorld.updateSingleAabb(body);
            }
        }
    }


    public String getRaycastHitObjectId(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null) ? result.hitObjectId : "";
    }




    public boolean createObject(String objectId, String modelPath) {
        ensureInitialized();
        if (sceneObjects.containsKey(objectId)) return false;

        try {
            FileHandle fileHandle;
            if (modelPath.startsWith("/")) {
                fileHandle = Gdx.files.absolute(modelPath);
            } else {
                fileHandle = Gdx.files.internal("models/" + modelPath);
            }

            if (!fileHandle.exists()) {
                Gdx.app.error("3DManager", "Model file not found: " + fileHandle.path());
                return false;
            }

            String lowerCasePath = modelPath.toLowerCase();

            if (lowerCasePath.endsWith(".glb") || lowerCasePath.endsWith(".gltf")) {
                net.mgsx.gltf.scene3d.scene.SceneAsset sceneAsset = loadedGltfAssets.get(modelPath);

                if (sceneAsset == null) {
                    if (lowerCasePath.endsWith(".glb")) {
                        sceneAsset = new net.mgsx.gltf.loaders.glb.GLBLoader().load(fileHandle, true);
                    } else {
                        sceneAsset = new net.mgsx.gltf.loaders.gltf.GLTFLoader().load(fileHandle, true);
                    }
                    if (sceneAsset != null) {
                        loadedGltfAssets.put(modelPath, sceneAsset);
                    }
                }

                if (sceneAsset != null) {
                    net.mgsx.gltf.scene3d.scene.Scene scene = new net.mgsx.gltf.scene3d.scene.Scene(sceneAsset.scene);
                    sceneManager.addScene(scene);
                    sceneObjects.put(objectId, scene.modelInstance);

                    gltfObjectIds.add(objectId);

                    if (scene.modelInstance.animations.size > 0) {
                        com.badlogic.gdx.graphics.g3d.utils.AnimationController controller = new com.badlogic.gdx.graphics.g3d.utils.AnimationController(scene.modelInstance);
                        animationControllers.put(objectId, controller);
                    }
                    return true;
                }
            } else {
                Model model = loadedModels.get(modelPath);

                if (model == null) {
                    try {
                        FileHandle modelFileHandle;

                        if (modelPath.startsWith("/")) {
                            modelFileHandle = Gdx.files.absolute(modelPath);
                        } else {
                            modelFileHandle = Gdx.files.internal("models/" + modelPath);
                        }

                        if (!modelFileHandle.exists()) {
                            Gdx.app.error("3DManager", "Model file does not exist: " + modelPath);
                            return false;
                        }

                        FileHandle patchedModelHandle = ModelPathProcessor.process(modelFileHandle);

                        Gdx.app.log("3DManager", "--- Verification before loading model ---");
                        FileHandle textureToVerify = Gdx.files.local("Lowpoly_Laptop_Nor_2.jpg");
                        Gdx.app.log("3DManager", "Verifying texture path: " + textureToVerify.path());
                        Gdx.app.log("3DManager", "Does texture exist at path? -> " + textureToVerify.exists());
                        Gdx.app.log("3DManager", "--- Verification finished ---");

                        FileHandleResolver resolver = fileName -> patchedModelHandle.parent().child(fileName);

                        ObjLoader loader = new ObjLoader(resolver);

                        model = loader.loadModel(patchedModelHandle, true);
                        loadedModels.put(modelPath, model);
                    } catch (Exception e) {
                        Gdx.app.error("3DManager", "Could not load model: " + modelPath, e);
                        return false;
                    }
                }

                ModelInstance instance = new ModelInstance(model);
                sceneObjects.put(objectId, instance);

                if (realisticMode) {
                    net.mgsx.gltf.scene3d.attributes.PBRColorAttribute baseColor =
                            net.mgsx.gltf.scene3d.attributes.PBRColorAttribute.createBaseColorFactor(com.badlogic.gdx.graphics.Color.WHITE);

                    for (Material mat : instance.materials) {
                        mat.set(baseColor);
                        mat.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createMetallic(0.0f));
                        mat.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createRoughness(1.0f));
                    }

                    net.mgsx.gltf.scene3d.scene.Scene pbrScene = new net.mgsx.gltf.scene3d.scene.Scene(instance);
                    sceneManager.addScene(pbrScene);
                }

                if (hiddenSpawnIds.contains(objectId)) { hiddenSpawnIds.remove(objectId); setObjectVisibility(objectId, false); }

                return true;
            }



        } catch (Exception e) {
            Gdx.app.error("3DManager_PBR", "Failed to load GLTF model: " + modelPath, e);
            e.printStackTrace();
            return false;
        }

        return false;
    }


    public void playAnimation(String objectId, String animationName, int loops, float speed, float transitionTime) {
        AnimationController controller = animationControllers.get(objectId);
        if (controller != null) {
            controller.animate(animationName, -1, speed, new AnimationController.AnimationListener() {
                private int loopsRun = 0;

                private final int targetLoops = (loops <= 0) ? -1 : loops;

                @Override
                public void onLoop(AnimationController.AnimationDesc animation) {
                    loopsRun++;

                    if (targetLoops != -1 && loopsRun >= targetLoops) {

                        animation.speed = 0f;
                        animation.time = animation.animation.duration;
                    }
                }

                @Override
                public void onEnd(AnimationController.AnimationDesc animation) {

                }
            }, transitionTime);
        }
    }


    public void stopAnimation(String objectId) {
        AnimationController controller = animationControllers.get(objectId);
        if (controller != null) {
            controller.setAnimation(null);
        }
    }

    public void setObjectEmissive(String objectId, com.badlogic.gdx.graphics.Color color, float intensity, String texturePath) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createEmissiveIntensity(intensity));

            applyTextureOrColor(material, texturePath,
                    net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute.EmissiveTexture,
                    net.mgsx.gltf.scene3d.attributes.PBRColorAttribute.Emissive,
                    color);
        }
    }

    private float currentShadowSize = 100f;
    private int currentShadowResolution = 2048;


    private void setupDefaultLighting() {
        if (pbrLight != null) {
            sceneManager.environment.remove(pbrLight);
            pbrLight = null;
        }

        net.mgsx.gltf.scene3d.lights.DirectionalShadowLight shadowLight = new net.mgsx.gltf.scene3d.lights.DirectionalShadowLight(2048, 2048);
        shadowLight.direction.set(1, -1.5f, 1).nor();
        shadowLight.color.set(com.badlogic.gdx.graphics.Color.WHITE);
        shadowLight.intensity = 5.0f;
        shadowLight.getCamera().far = 1000f;

        sceneManager.environment.add(shadowLight);
        pbrLight = shadowLight;

        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        updateProceduralIBL();
    }


    public void setRealisticSunLight(float dirX, float dirY, float dirZ, float intensity) {
        if (pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) {
            net.mgsx.gltf.scene3d.lights.DirectionalShadowLight sun = (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;

            if (dirX == 0f && dirY == 0f && dirZ == 0f) {
                sun.direction.set(0f, -1f, 0f);
            } else {
                sun.direction.set(dirX, dirY, dirZ).nor();
            }

            sun.intensity = intensity;

            if (intensity <= 0.001f) {
                sun.color.set(0f, 0f, 0f, 1f);
            } else {
                sun.color.set(1f, 1f, 1f, 1f);
            }
        } else {
            Gdx.app.error("ThreeDManager", "setRealisticSunLight called, but no DirectionalShadowLight was found!");
        }
    }

    public void setSunLightColor(float r, float g, float b) {
        if (pbrLight != null) {
            pbrLight.color.set(r, g, b, 1);
        }
    }

    public void applyForce(String objectId, float forceX, float forceY, float forceZ) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.activate();
            body.applyCentralForce(new Vector3(forceX, forceY, forceZ));
        }
    }


    public void setPhysicsState(String objectId, PhysicsState state, PhysicsShape shape, float mass) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        removePhysicsBody(objectId);
        if (state == PhysicsState.NONE) return;

        float bodyMass = (state == PhysicsState.DYNAMIC) ? mass : 0f;
        if (state == PhysicsState.DYNAMIC && mass <= 0) {
            Gdx.app.error("3DManager", "Dynamic body must have mass > 0.");
            return;
        }

        if (state == PhysicsState.MESH_STATIC) {
            if (gltfObjectIds.contains(objectId)) {
                createGltfMeshPhysicsBody(objectId, instance);
            } else {
                createMeshPhysicsBody(objectId, instance);
            }
        } else {
            createPrimitivePhysicsBody(objectId, instance, shape, bodyMass, state);
        }
    }


    public void setPhysicsState(String objectId, PhysicsState state, float mass) {
        setPhysicsState(objectId, state, PhysicsShape.BOX, mass);
    }


    private void createPrimitivePhysicsBody(String objectId, ModelInstance instance, PhysicsShape shapeType, float mass, PhysicsState state) {
        BoundingBox bbox = new BoundingBox();
        instance.calculateBoundingBox(bbox);
        Vector3 dimensions = bbox.getDimensions(new Vector3());

        Vector3 center = bbox.getCenter(new Vector3());
        btCollisionShape shape;
        switch (shapeType) {
            case SPHERE:
                float radius = Math.max(dimensions.x, Math.max(dimensions.y, dimensions.z)) / 2f;
                shape = new btSphereShape(radius);
                break;
            case CAPSULE:
                float capsuleRadius = Math.max(dimensions.x, dimensions.z) / 2f;
                float capsuleHeight = dimensions.y - (2 * capsuleRadius);
                shape = new btCapsuleShape(capsuleRadius, Math.max(0, capsuleHeight));
                break;
            case BOX:
            default:
                shape = new btBoxShape(dimensions.cpy().scl(0.5f));
                break;
        }

        btCompoundShape compoundShape = new btCompoundShape();

        Matrix4 shapeOffsetTransform = new Matrix4().setToTranslation(center);
        compoundShape.addChildShape(shapeOffsetTransform, shape);

        Vector3 scale = instance.transform.getScale(new Vector3());
        compoundShape.setLocalScaling(scale);

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        Matrix4 bodyTransform = new Matrix4();
        bodyTransform.set(instance.transform.getRotation(new Quaternion()));
        bodyTransform.setTranslation(instance.transform.getTranslation(new Vector3()));

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        if (state == PhysicsState.KINEMATIC) {
            body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
            body.setActivationState(Collision.DISABLE_DEACTIVATION);
        }

        if (mass > 0) {
            body.setAngularFactor(1f);

            body.setDamping(0.5f, 0.5f);
        }

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        Array<Disposable> disposables = new Array<>();
        disposables.add(shape);
        disposables.add(compoundShape);
        body.userData = disposables;
        physicsResources.put(objectId, disposables);

        bodyInfo.dispose();
    }




    private void createGltfMeshPhysicsBody(String objectId, ModelInstance instance) {
        Gdx.app.log("PhysicsDebug", "============================================================");
        Gdx.app.log("PhysicsDebug", "=== STARTING MESH BODY CREATION (BAKING METHOD) for object: '" + objectId + "'");
        Gdx.app.log("PhysicsDebug", "============================================================");

        instance.calculateTransforms();

        btCompoundShape compoundShape = new btCompoundShape();
        Array<Disposable> disposables = new Array<>();

        if (instance.nodes.size == 0) {
            Gdx.app.error("PhysicsDebug", "FATAL: ModelInstance has NO nodes!");
        } else {
            Gdx.app.log("PhysicsDebug", "Model has " + instance.nodes.size + " root nodes. Starting recursion...");

            addPartsToCompoundShapeRecursive(instance.nodes, instance.transform, compoundShape, disposables, "  ");
        }

        int childCount = compoundShape.getNumChildShapes();
        Gdx.app.log("PhysicsDebug", "RECURSION FINISHED. Total child shapes in CompoundShape: " + childCount);

        if (childCount == 0) {
            Gdx.app.error("PhysicsDebug", "FATAL: CompoundShape is EMPTY!");
            compoundShape.dispose();
            for (Disposable d : disposables) d.dispose();
            Gdx.app.log("PhysicsDebug", "=================== CREATION FAILED ===================");
            return;
        }

        Matrix4 bodyTransform = new Matrix4().idt();

        Gdx.app.log("PhysicsDebug", "RigidBody initial transform is IDENTITY because geometry is pre-transformed.");

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(0f, motionState, compoundShape, new Vector3(0, 0, 0));
        btRigidBody body = new btRigidBody(bodyInfo);

        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_STATIC_OBJECT);
        body.setActivationState(Collision.DISABLE_DEACTIVATION);

        dynamicsWorld.addRigidBody(body);
        dynamicsWorld.updateSingleAabb(body);

        physicsBodies.put(objectId, body);
        disposables.add(compoundShape);
        body.userData = disposables;
        physicsResources.put(objectId, disposables);

        Gdx.app.log("PhysicsDebug", "SUCCESS: RigidBody added to dynamicsWorld. AABB was manually updated.");
        Gdx.app.log("PhysicsDebug", "=================== CREATION FINISHED for: '" + objectId + "' ===================");

        bodyInfo.dispose();
    }


    public void createEditorProxy(String ownerId) {
        if (editorProxies.containsKey(ownerId) || lightProxyModel == null) return;
        ModelInstance proxyInstance = new ModelInstance(lightProxyModel);
        editorProxies.put(ownerId, proxyInstance);
    }


    public void removeEditorProxy(String ownerId) {
        editorProxies.remove(ownerId);
    }


    public void updateEditorProxyPosition(String ownerId, Vector3 position) {
        ModelInstance proxy = editorProxies.get(ownerId);
        if (proxy != null) {
            proxy.transform.setTranslation(position);
        }
    }


    public Map<String, ModelInstance> getEditorProxies() {
        return editorProxies;
    }



    private void addPartsToCompoundShapeRecursive(Iterable<Node> nodes, Matrix4 rootTransform, btCompoundShape compoundShape, Array<Disposable> disposables, String indent) {
        for (Node node : nodes) {
            Gdx.app.log("PhysicsDebug", indent + "-> Processing Node: '" + node.id + "'");

            Matrix4 finalTransform = new Matrix4(rootTransform).mul(node.globalTransform);

            if (node.parts.size > 0) {
                for (NodePart nodePart : node.parts) {
                    MeshPart originalMeshPart = nodePart.meshPart;
                    if (originalMeshPart != null && originalMeshPart.mesh != null && originalMeshPart.mesh.getNumVertices() > 0) {
                        Mesh bakedMesh = originalMeshPart.mesh.copy(true);

                        bakedMesh.transform(finalTransform);
                        disposables.add(bakedMesh);

                        MeshPart bakedMeshPart = new MeshPart("baked_part_" + node.id, bakedMesh, 0, bakedMesh.getNumIndices(), GL20.GL_TRIANGLES);

                        btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(bakedMeshPart);
                        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);
                        meshShape.setMargin(0.04f);

                        disposables.add(vertexArray);
                        disposables.add(meshShape);

                        compoundShape.addChildShape(new Matrix4(), meshShape);
                        Gdx.app.log("PhysicsDebug", indent + "  SUCCESS: Baked and added child shape.");
                    }
                }
            }

            if (node.hasChildren()) {
                addPartsToCompoundShapeRecursive(node.getChildren(), rootTransform, compoundShape, disposables, indent + "  ");
            }
        }
    }

    Boolean objectExists(String id) {
        return sceneObjects.containsKey(id);
    }


    public Vector3 getVelocity(String objectId) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) {
            return body.getLinearVelocity();
        }
        return Vector3.Zero;
    }


    public Vector3 getCameraRotation() {
        Quaternion q = new Quaternion();

        q.setFromMatrix(camera.view);

        q.conjugate();

        return new Vector3(q.getPitch(), q.getYaw(), q.getRoll());
    }

    public ModelInstance getModelInstance(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) {
            instance = editorProxies.get(objectId);
        }
        return instance;
    }

    public boolean createCylinder(String objectId) {
        ensureInitialized();
        if (sceneObjects.containsKey(objectId)) return false;
        final String CYL_MODEL_KEY = "__PRIMITIVE_CYLINDER__";
        Model cylModel = loadedModels.get(CYL_MODEL_KEY);

        if (cylModel == null) {
            Material pbrMaterial = new Material(
                    net.mgsx.gltf.scene3d.attributes.PBRColorAttribute.createBaseColorFactor(Color.WHITE)
            );
            long attributes = VertexAttributes.Usage.Position |
                    VertexAttributes.Usage.Normal |
                    VertexAttributes.Usage.TextureCoordinates;

            com.badlogic.gdx.graphics.g3d.utils.ModelBuilder modelBuilder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
            modelBuilder.begin();

            com.badlogic.gdx.graphics.g3d.model.Node node = modelBuilder.node();
            node.id = "cylinder_node";
            node.rotation.set(Vector3.X, 90);

            com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder mpb = modelBuilder.part("cylinder",
                    GL20.GL_TRIANGLES, attributes, pbrMaterial);
            com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder.build(mpb, 1f, 1f, 1f, 16);

            cylModel = modelBuilder.end();
            loadedModels.put(CYL_MODEL_KEY, cylModel);
        }

        ModelInstance instance = new ModelInstance(cylModel);
        sceneObjects.put(objectId, instance);

        if (realisticMode && sceneManager != null) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(instance));
        }

        if (hiddenSpawnIds.contains(objectId)) { hiddenSpawnIds.remove(objectId); setObjectVisibility(objectId, false); }

        return true;
    }


    public boolean createSphere(String objectId) {
        ensureInitialized();
        if (sceneObjects.containsKey(objectId)) return false;

        final String SPHERE_MODEL_KEY = "__PRIMITIVE_SPHERE__";
        Model sphereModel = loadedModels.get(SPHERE_MODEL_KEY);

        if (sphereModel == null) {
            Material pbrMaterial = new Material(
                    PBRColorAttribute.createBaseColorFactor(Color.WHITE)
            );

            final long attributes = VertexAttributes.Usage.Position |
                    VertexAttributes.Usage.Normal |
                    VertexAttributes.Usage.TextureCoordinates |
                    VertexAttributes.Usage.Tangent;

            sphereModel = modelBuilder.createSphere(50f, 50f, 50f, 16, 16,
                    pbrMaterial,
                    attributes);
            loadedModels.put(SPHERE_MODEL_KEY, sphereModel);
        }

        ModelInstance instance = new ModelInstance(sphereModel);
        sceneObjects.put(objectId, instance);

        if (realisticMode && sceneManager != null) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(instance));
        }

        if (hiddenSpawnIds.contains(objectId)) { hiddenSpawnIds.remove(objectId); setObjectVisibility(objectId, false); }

        return true;
    }

    private void createPhysicsBody(String objectId, ModelInstance instance, float mass) {
        BoundingBox bbox = new BoundingBox();
        instance.model.calculateBoundingBox(bbox);
        Vector3 dimensions = new Vector3();
        bbox.getDimensions(dimensions);
        Vector3 center = new Vector3();
        bbox.getCenter(center);

        btBoxShape boxShape = new btBoxShape(dimensions.scl(0.5f));

        btCompoundShape compoundShape = new btCompoundShape();
        com.badlogic.gdx.math.Matrix4 shapeOffsetTransform = new com.badlogic.gdx.math.Matrix4();
        shapeOffsetTransform.setToTranslation(center);
        compoundShape.addChildShape(shapeOffsetTransform, boxShape);

        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        compoundShape.setLocalScaling(scale);

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        com.badlogic.gdx.math.Matrix4 bodyTransform = new com.badlogic.gdx.math.Matrix4();
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);
        bodyTransform.set(position, rotation);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        bodyInfo.dispose();
    }




    private final Map<String, com.badlogic.gdx.utils.Array<Disposable>> physicsResources = new HashMap<>();








    private void addNodePartsToCompoundShape(com.badlogic.gdx.graphics.g3d.model.Node node, btCompoundShape compoundShape) {
        if (node.parts.size > 0) {
            Gdx.app.log("PhysicsDebug", "Processing Node: " + node.id + " with " + node.parts.size + " parts.");

            com.badlogic.gdx.utils.Array<Disposable> disposables = new com.badlogic.gdx.utils.Array<>();

            for (com.badlogic.gdx.graphics.g3d.model.NodePart nodePart : node.parts) {
                com.badlogic.gdx.graphics.g3d.model.MeshPart meshPart = nodePart.meshPart;
                if (meshPart.size > 0) {
                    btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(meshPart);
                    disposables.add(vertexArray);

                    btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);
                    disposables.add(meshShape);

                    compoundShape.addChildShape(node.globalTransform, meshShape);
                }
            }

            for (Disposable d : disposables) {
                d.dispose();
            }
        }


        for (com.badlogic.gdx.graphics.g3d.model.Node child : node.getChildren()) {
            addNodePartsToCompoundShape(child, compoundShape);
        }
    }


    void removePhysicsBody(String objectId) {
        btRigidBody body = physicsBodies.remove(objectId);
        if (body != null) {
            dynamicsWorld.removeRigidBody(body);

            com.badlogic.gdx.utils.Array<Disposable> resources = physicsResources.remove(objectId);
            if (resources != null) {
                for (Disposable d : resources) {
                    try {
                        d.dispose();
                    } catch (Exception ignored) {
                    }
                }
            }

            if (body.userData instanceof com.badlogic.gdx.utils.Array) {
                com.badlogic.gdx.utils.Array<Disposable> disposables = (com.badlogic.gdx.utils.Array<Disposable>) body.userData;
                for (Disposable d : disposables) {
                    try {
                        d.dispose();
                    } catch (Exception e) {
                        // None
                    }
                }
            }

            if (body.getMotionState() != null) body.getMotionState().dispose();
            if (body.getCollisionShape() != null) body.getCollisionShape().dispose();
            body.dispose();
        }
    }


    public boolean checkIntersection(String objectId1, String objectId2) {
        ModelInstance instance1 = sceneObjects.get(objectId1);
        ModelInstance instance2 = sceneObjects.get(objectId2);

        if (instance1 == null || instance2 == null) {
            return false;
        }

        instance1.calculateBoundingBox(bounds1);

        bounds1.mul(instance1.transform);

        instance2.calculateBoundingBox(bounds2);
        bounds2.mul(instance2.transform);

        return bounds1.intersects(bounds2);
    }

    public void removeObjectsByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return;

        java.util.List<String> toRemove = new java.util.ArrayList<>();
        for (String id : sceneObjects.keySet()) {
            if (id.startsWith(prefix)) {
                toRemove.add(id);
            }
        }

        for (String id : toRemove) {
            removeObject(id);
        }
    }

    public boolean removeObject(String objectId) {
        ModelInstance instance = sceneObjects.remove(objectId);

        if (manager != null) {
            GameObject go = manager.findGameObject(objectId);
            if (go != null) {
                manager.removeGameObject(go);
            }
        }

        removePhysicsBody(objectId);
        animationControllers.remove(objectId);
        removeParticleEffect3D(objectId);
        gltfObjectIds.remove(objectId);
        inactiveRenderObjects.remove(objectId);
        inactivePbrScenes.remove(objectId);
        aiLastKnownPositions.remove(objectId);
        aiHasLostTarget.remove(objectId);
        aiSmoothedDirection.remove(objectId);

        if (instance != null) {
            if (realisticMode && sceneManager != null) {
                com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.RenderableProvider> providers = sceneManager.getRenderableProviders();
                for (com.badlogic.gdx.graphics.g3d.RenderableProvider provider : providers) {
                    if (provider instanceof net.mgsx.gltf.scene3d.scene.Scene) {
                        net.mgsx.gltf.scene3d.scene.Scene scene = (net.mgsx.gltf.scene3d.scene.Scene) provider;
                        if (scene.modelInstance == instance) {
                            sceneManager.removeScene(scene);
                            break;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean removeModelOnly(String objectId) {
        ModelInstance instance = sceneObjects.remove(objectId);
        animationControllers.remove(objectId);
        gltfObjectIds.remove(objectId);
        inactiveRenderObjects.remove(objectId);
        inactivePbrScenes.remove(objectId);

        if (instance != null) {
            if (realisticMode && sceneManager != null) {
                com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.RenderableProvider> providers = sceneManager.getRenderableProviders();
                for (com.badlogic.gdx.graphics.g3d.RenderableProvider provider : providers) {
                    if (provider instanceof net.mgsx.gltf.scene3d.scene.Scene) {
                        net.mgsx.gltf.scene3d.scene.Scene scene = (net.mgsx.gltf.scene3d.scene.Scene) provider;
                        if (scene.modelInstance == instance) {
                            sceneManager.removeScene(scene);
                            break;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public void replaceModel(final String objectId, final String modelFileName) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (disposed) return;
                if (manager != null) {
                    GameObject go = manager.findGameObject(objectId);
                    if (go != null) {
                        RenderComponent render = go.getComponent(RenderComponent.class);
                        if (render == null) {
                            render = new RenderComponent();
                            go.addComponent(render);
                        }
                        render.modelFileName = modelFileName;
                    }
                }

                removeModelOnly(objectId);

                String absolutePath;
                if (modelFileName.startsWith("assets://")) {
                    absolutePath = modelFileName.substring("assets://".length());
                } else {
                    File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelFileName);
                    if (modelFile != null && modelFile.exists()) {
                        absolutePath = modelFile.getAbsolutePath();
                    } else {
                        Gdx.app.error("ThreeDManager", "replaceModel failed: Model file not found: " + modelFileName);
                        return;
                    }
                }

                createObject(objectId, absolutePath);

                if (manager != null) {
                    GameObject go = manager.findGameObject(objectId);
                    if (go != null) {
                        manager.updateWorldTransforms();
                        setWorldTransform(go.id, go.transform.worldTransform);

                        MaterialComponent material = go.getComponent(MaterialComponent.class);
                        if (material != null) {
                            applyPBRMaterial(go.id, material);
                        }

                        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
                        if (physics != null) {
                            setPhysicsStateFromComponent(go.id, physics, go.transform.worldTransform);
                        }
                    }
                }
            }
        });
    }

    public boolean removeLight(String id) {
        DirectionalLight light = directionalLights.remove(id);
        return light != null;
    }

    public void setPosition(String objectId, float x, float y, float z) {
        if (manager != null && manager.findGameObject(objectId) != null) {
            GameObject go = manager.findGameObject(objectId);
            manager.setPosition(go, new Vector3(x, y, z));
        }


        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) {
            return;
        }

        instance.transform.setTranslation(x, y, z);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 worldTransform = body.getWorldTransform();

            Quaternion rotation = worldTransform.getRotation(new Quaternion());
            worldTransform.set(new Vector3(x, y, z), rotation);

            body.setWorldTransform(worldTransform);
            body.getMotionState().setWorldTransform(worldTransform);

            body.activate();
            if (body.isStaticObject() || body.isKinematicObject()) {
                dynamicsWorld.updateSingleAabb(body);
            }
        }
    }

    public void setRotation(String objectId, float yaw, float pitch, float roll) {
        Quaternion newRotation = new Quaternion().setEulerAngles(yaw, pitch, roll);
        if (manager != null && manager.findGameObject(objectId) != null) {
            GameObject go = manager.findGameObject(objectId);
            manager.setRotation(go, newRotation);
        }
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        instance.transform.set(position, newRotation, scale);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();

            transform.getTranslation(position);

            transform.set(position, newRotation);

            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
            if (body.isStaticObject() || body.isKinematicObject()) {
                dynamicsWorld.updateSingleAabb(body);
            }
        }
    }

    public Quaternion getRotationQuaternion(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Quaternion q = new Quaternion();
            instance.transform.getRotation(q, true);
            return q;
        }
        return null;
    }

    public void setCameraPosition(float x, float y, float z) {
        camera.position.set(x, y, z);
        camera.update();
    }

    public void cameraLookAt(float x, float y, float z) {
        camera.lookAt(x, y, z);
        camera.update();
    }

    public void objectLookAtCorrect(String id, float x, float y, float z) {
        ModelInstance instance = sceneObjects.get(id);
        if (instance == null) return;

        GameObject go = null;
        if (manager != null) {
            manager.updateWorldTransforms();
            go = manager.findGameObject(id);
        }

        Vector3 position = new Vector3();
        Vector3 scale = new Vector3(1, 1, 1);

        if (go != null) {
            go.transform.worldTransform.getTranslation(position);
            go.transform.worldTransform.getScale(scale);
        } else {
            instance.transform.getTranslation(position);
            instance.transform.getScale(scale);
        }

        Vector3 target = new Vector3(x, y, z);
        Vector3 forward = new Vector3(target).sub(position).nor();

        if (forward.isZero()) return;

        Vector3 up = new Vector3(Vector3.Y);
        if (Math.abs(forward.y) > 0.999f) {
            up.set(Vector3.Z);
        }

        Vector3 right = new Vector3(up).crs(forward).nor();
        Vector3 newUp = new Vector3(forward).crs(right).nor();

        Matrix4 rotMat = new Matrix4().idt();
        rotMat.val[Matrix4.M00] = right.x;   rotMat.val[Matrix4.M10] = right.y;   rotMat.val[Matrix4.M20] = right.z;
        rotMat.val[Matrix4.M01] = newUp.x;   rotMat.val[Matrix4.M11] = newUp.y;   rotMat.val[Matrix4.M21] = newUp.z;
        rotMat.val[Matrix4.M02] = forward.x; rotMat.val[Matrix4.M12] = forward.y; rotMat.val[Matrix4.M22] = forward.z;

        Quaternion worldRot = new Quaternion();
        rotMat.getRotation(worldRot, true);

        if (go != null) {
            if (go.parentId != null) {
                GameObject parent = manager.findGameObject(go.parentId);
                if (parent != null) {
                    Quaternion parentWorldRot = new Quaternion();
                    parent.transform.worldTransform.getRotation(parentWorldRot, true);

                    Quaternion localRot = parentWorldRot.conjugate().mul(worldRot);
                    manager.setRotation(go, localRot);
                } else {
                    manager.setRotation(go, worldRot);
                }
            } else {
                manager.setRotation(go, worldRot);
            }

            manager.updateWorldTransforms();
            setWorldTransform(id, go.transform.worldTransform);
        } else {
            instance.transform.set(position, worldRot, scale);

            btRigidBody body = physicsBodies.get(id);
            if (body != null) {
                com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();
                transform.getTranslation(position);
                transform.set(position, worldRot);
                body.setWorldTransform(transform);
                if (body.getMotionState() != null) {
                    body.getMotionState().setWorldTransform(transform);
                }
                body.activate();
            }
        }
    }

    public void objectLookAt(String id, float x, float y, float z) {
        ModelInstance instance = sceneObjects.get(id);
        if (instance == null) return;

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);

        Matrix4 tempMat = new Matrix4().setToLookAt(new Vector3(x, y, z), Vector3.Y);
        Quaternion oldViewRot = new Quaternion();
        tempMat.getRotation(oldViewRot);

        if (manager != null && manager.findGameObject(id) != null) {
            GameObject go = manager.findGameObject(id);
            if (go.parentId != null) {
                GameObject parent = manager.findGameObject(go.parentId);
                if (parent != null) {
                    Quaternion parentWorldRot = new Quaternion();
                    parent.transform.worldTransform.getRotation(parentWorldRot, true);
                    Quaternion localRot = parentWorldRot.conjugate().mul(oldViewRot);
                    manager.setRotation(go, localRot);
                } else {
                    manager.setRotation(go, oldViewRot);
                }
            } else {
                manager.setRotation(go, oldViewRot);
            }

            manager.updateWorldTransforms();
            setWorldTransform(id, go.transform.worldTransform);
            return;
        }

        instance.transform.set(position, oldViewRot, scale);

        btRigidBody body = physicsBodies.get(id);
        if (body != null) {
            com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();
            transform.getTranslation(position);
            transform.set(position, oldViewRot);
            body.setWorldTransform(transform);
            if (body.getMotionState() != null) {
                body.getMotionState().setWorldTransform(transform);
            }
            body.activate();
        }
    }

    public void alignObjectToNormal(String objectId, float nx, float ny, float nz) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Vector3 normal = new Vector3(nx, ny, nz).nor();

        Vector3 forward = new Vector3(0, 0, 1);


        Quaternion worldRot = new Quaternion();
        if (forward.isCollinearOpposite(normal, 0.01f)) {
            worldRot.setEulerAngles(180, 0, 0);
        } else {
            worldRot.setFromCross(forward, normal);
        }




        if (manager != null) {
            GameObject go = manager.findGameObject(objectId);
            if (go != null) {

                if (go.parentId != null) {
                    GameObject parent = manager.findGameObject(go.parentId);
                    if (parent != null) {
                        Quaternion parentWorldRot = new Quaternion();
                        parent.transform.worldTransform.getRotation(parentWorldRot, true);

                        Quaternion localRot = parentWorldRot.conjugate().mul(worldRot);
                        manager.setRotation(go, localRot);
                    } else {
                        manager.setRotation(go, worldRot);
                    }
                } else {
                    manager.setRotation(go, worldRot);
                }


                manager.updateWorldTransforms();
                setWorldTransform(objectId, go.transform.worldTransform);
                return;
            }
        }




        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);

        instance.transform.set(position, worldRot, scale);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 transform = body.getWorldTransform();
            transform.set(position, worldRot);
            body.setWorldTransform(transform);
            if (body.getMotionState() != null) body.getMotionState().setWorldTransform(transform);
            body.activate();
        }
    }


    public boolean createFixedConstraintWeld(String constraintId, String objectIdA, String objectIdB) {
        if (physicsConstraints.containsKey(constraintId)) return false;

        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) return false;

        bodyA.activate();
        bodyB.activate();


        Matrix4 frameInA = new Matrix4().idt();



        Matrix4 frameInB = new Matrix4(bodyB.getWorldTransform()).inv().mul(bodyA.getWorldTransform());

        btFixedConstraint constraint = new btFixedConstraint(bodyA, bodyB, frameInA, frameInB);
        dynamicsWorld.addConstraint(constraint, true);
        physicsConstraints.put(constraintId, constraint);
        return true;
    }


    public boolean createFixedConstraintManual(String constraintId, String objectIdA, String objectIdB,
                                               float ax, float ay, float az,
                                               float bx, float by, float bz) {
        if (physicsConstraints.containsKey(constraintId)) return false;

        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) return false;

        bodyA.activate();
        bodyB.activate();


        Matrix4 frameInA = new Matrix4().setToTranslation(ax, ay, az);
        Matrix4 frameInB = new Matrix4().setToTranslation(bx, by, bz);

        btFixedConstraint constraint = new btFixedConstraint(bodyA, bodyB, frameInA, frameInB);
        dynamicsWorld.addConstraint(constraint, true);
        physicsConstraints.put(constraintId, constraint);
        return true;
    }


    public void setCameraRotation(float yaw, float pitch, float roll) {
        Quaternion rotation = new Quaternion();
        rotation.setEulerAngles(yaw, pitch, roll);

        camera.direction.set(0, 0, -1);
        rotation.transform(camera.direction);
        camera.up.set(0, 1, 0);
        rotation.transform(camera.up);

        camera.update();
    }

    public void setAmbientLight(float r, float g, float b) {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, r, g, b, 1f));
    }

    public void setDirectionalLight(String lightId, float r, float g, float b, float dirX, float dirY, float dirZ) {
        DirectionalLight light = directionalLights.get(lightId);
        if (light == null) {
            light = new DirectionalLight();
            directionalLights.put(lightId, light);
            environment.add(light);
        }
        light.set(r, g, b, dirX, dirY, dirZ);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setScale(String objectId, float scaleX, float scaleY, float scaleZ) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Quaternion rotation = new Quaternion();



        instance.transform.getRotation(rotation, true);

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        instance.transform.set(position, rotation, new Vector3(scaleX, scaleY, scaleZ));

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            float mass = (body.getInvMass() > 0f) ? 1f / body.getInvMass() : 0f;
            float friction = body.getFriction();
            float restitution = body.getRestitution();
            Vector3 velocity = body.getLinearVelocity();
            PhysicsState state = (mass > 0f) ? PhysicsState.DYNAMIC : PhysicsState.STATIC;

            setPhysicsState(objectId, state, mass);

            btRigidBody newBody = physicsBodies.get(objectId);
            if (newBody != null) {
                com.badlogic.gdx.math.Matrix4 transform = newBody.getWorldTransform();
                transform.getTranslation(position);
                transform.set(position, rotation);
                newBody.setWorldTransform(transform);
                newBody.getMotionState().setWorldTransform(transform);

                newBody.setFriction(friction);
                newBody.setRestitution(restitution);
                if (state == PhysicsState.DYNAMIC) {
                    newBody.setLinearVelocity(velocity);
                }
            }
        }
    }


    public void createGrid(float size, int divisions) {
        if (gridScene != null) {
            if (sceneManager != null) {
                sceneManager.removeScene(gridScene);
            }
            gridScene = null;
        }
        if (gridInstance != null) {
            gridInstance.model.dispose();
            gridInstance = null;
        }

        com.badlogic.gdx.graphics.g3d.utils.ModelBuilder modelBuilder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
        Model gridModel = modelBuilder.createLineGrid(divisions, divisions, size / divisions, size / divisions,
                new com.badlogic.gdx.graphics.g3d.Material(),
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.ColorUnpacked);

        gridInstance = new ModelInstance(gridModel);
        gridScene = new net.mgsx.gltf.scene3d.scene.Scene(gridInstance);

        if (sceneManager != null) {
            sceneManager.addScene(gridScene);
        }
    }


    public float getDeltaTime() {
        return Gdx.graphics.getDeltaTime();
    }

    public void setShadowsEnabled(boolean enabled) {
        if (sceneManager == null || pbrLight == null) return;

        boolean isShadowLight = pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;

        if (enabled && !isShadowLight) {

            Color oldColor = pbrLight.color.cpy();
            Vector3 oldDir = pbrLight.direction.cpy();
            float oldIntensity = pbrLight.intensity;

            sceneManager.environment.remove(pbrLight);
            recreateShadowLight();


            if (pbrLight != null) {
                pbrLight.direction.set(oldDir);
                pbrLight.color.set(oldColor);
                pbrLight.intensity = oldIntensity;
            }
        }
        else if (!enabled && isShadowLight) {

            net.mgsx.gltf.scene3d.lights.DirectionalShadowLight shadowLight =
                    (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;

            net.mgsx.gltf.scene3d.lights.DirectionalLightEx simpleLight =
                    new net.mgsx.gltf.scene3d.lights.DirectionalLightEx();

            simpleLight.direction.set(shadowLight.direction);
            simpleLight.color.set(shadowLight.color);
            simpleLight.intensity = shadowLight.intensity;

            sceneManager.environment.remove(shadowLight);
            shadowLight.dispose();

            sceneManager.environment.add(simpleLight);
            pbrLight = simpleLight;
        }
    }

    public void bakeObjectsByPrefix(String prefix, String newId) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        int partCounter = 0;
        Array<String> objectsToRemove = new Array<>();

        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
            String id = entry.getKey();

            if (id.startsWith(prefix) && !id.equals("player")) {
                btRigidBody body = physicsBodies.get(id);

                if (body != null && body.getInvMass() == 0) {
                    ModelInstance inst = entry.getValue();

                    for (Node node : inst.nodes) {
                        for (NodePart part : node.parts) {
                            MeshPartBuilder builder = modelBuilder.part("p" + partCounter++,
                                    part.meshPart.primitiveType, part.meshPart.mesh.getVertexAttributes(), part.material);

                            builder.setVertexTransform(inst.transform);
                            builder.addMesh(part.meshPart);
                        }
                    }
                    objectsToRemove.add(id);
                }
            }
        }

        if (partCounter == 0) {
            modelBuilder.end();
            Gdx.app.log("Bake", "No static objects found with prefix: " + prefix);
            return;
        }

        Model bakedModel = modelBuilder.end();
        ModelInstance bakedInstance = new ModelInstance(bakedModel);

        for (String id : objectsToRemove) {
            removeObject(id);
        }

        sceneObjects.put(newId, bakedInstance);
        if (realisticMode) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(bakedInstance));
        }
        createGltfMeshPhysicsBody(newId, bakedInstance);

        Gdx.app.log("Bake", "Optimized " + objectsToRemove.size + " objects into " + newId);
    }

    public void bakeObjectsList(String newId, List<String> idsToBake) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        int partCounter = 0;
        Array<String> actuallyRemoved = new Array<>();

        for (String id : idsToBake) {
            ModelInstance inst = sceneObjects.get(id);
            if (inst == null) continue;

            for (Node node : inst.nodes) {
                for (NodePart part : node.parts) {
                    MeshPartBuilder builder = modelBuilder.part("p" + partCounter++,
                            part.meshPart.primitiveType, part.meshPart.mesh.getVertexAttributes(), part.material);
                    builder.setVertexTransform(inst.transform);
                    builder.addMesh(part.meshPart);
                }
            }
            actuallyRemoved.add(id);
        }

        if (partCounter == 0) {
            modelBuilder.end();
            return;
        }

        Model bakedModel = modelBuilder.end();
        ModelInstance bakedInstance = new ModelInstance(bakedModel);

        for (String id : actuallyRemoved) {
            removeObject(id);
        }

        sceneObjects.put(newId, bakedInstance);
        if (realisticMode) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(bakedInstance));
        }
        createGltfMeshPhysicsBody(newId, bakedInstance);
    }

    public boolean createCube(String objectId) {
        ensureInitialized();
        if (sceneObjects.containsKey(objectId)) return false;

        final String CUBE_MODEL_KEY = "__PRIMITIVE_CUBE__";
        Model cubeModel = loadedModels.get(CUBE_MODEL_KEY);

        if (cubeModel == null) {
            Material pbrMaterial = new Material(
                    PBRColorAttribute.createBaseColorFactor(Color.WHITE)
            );

            final long attributes = VertexAttributes.Usage.Position |
                    VertexAttributes.Usage.Normal |
                    VertexAttributes.Usage.TextureCoordinates |
                    VertexAttributes.Usage.Tangent;

            cubeModel = modelBuilder.createBox(50f, 50f, 50f,
                    pbrMaterial,
                    attributes);
            loadedModels.put(CUBE_MODEL_KEY, cubeModel);
        }

        ModelInstance instance = new ModelInstance(cubeModel);
        sceneObjects.put(objectId, instance);

        if (realisticMode && sceneManager != null) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(instance));
        }

        if (hiddenSpawnIds.contains(objectId)) { hiddenSpawnIds.remove(objectId); setObjectVisibility(objectId, false); }

        return true;
    }

    public void applyPBRMaterial(String objectId, MaterialComponent materialData) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null || materialData == null) return;

        for (Material material : instance.materials) {
            PBRColorAttribute colorAttr = (PBRColorAttribute) material.get(PBRColorAttribute.BaseColorFactor);
            if (colorAttr != null) {
                colorAttr.color.set(materialData.baseColor);
            } else {
                material.set(PBRColorAttribute.createBaseColorFactor(materialData.baseColor));
            }

            if (materialData.baseColor.a < 1.0f) {
                material.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(
                        GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA
                ));
            } else {
                material.remove(com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute.Type);
            }

            if (materialData.baseColor.a < 0.05f) {
                material.set(new DepthTestAttribute(false));
            } else {
                material.set(new DepthTestAttribute(GL20.GL_LEQUAL, true));
            }

            applyTextureOrColor(material, materialData.baseColorTexturePath,
                    PBRTextureAttribute.BaseColorTexture, PBRColorAttribute.BaseColorFactor, materialData.baseColor);

            material.set(PBRFloatAttribute.createMetallic(materialData.metallic));
            material.set(PBRFloatAttribute.createRoughness(materialData.roughness));

            applyTexture(material, materialData.metallicRoughnessTexturePath, PBRTextureAttribute.MetallicRoughnessTexture);

            applyTexture(material, materialData.normalTexturePath, PBRTextureAttribute.NormalTexture);

            applyTexture(material, materialData.occlusionTexturePath, PBRTextureAttribute.OcclusionTexture);

            applyTextureOrColor(material, materialData.emissiveTexturePath,
                    PBRTextureAttribute.EmissiveTexture, PBRColorAttribute.Emissive, materialData.emissiveColor);

            for (com.badlogic.gdx.graphics.g3d.Attribute attr : material) {
                if (attr instanceof com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) {
                    com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute texAttr =
                            (com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) attr;
                    texAttr.scaleU = materialData.uvScaleX;
                    texAttr.scaleV = materialData.uvScaleY;
                }
            }
        }
    }

    public void setTextureTiling(String objectId, float scaleU, float scaleV) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            for (com.badlogic.gdx.graphics.g3d.Attribute attr : material) {
                if (attr instanceof com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) {
                    com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute texAttr =
                            (com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) attr;
                    texAttr.scaleU = scaleU;
                    texAttr.scaleV = scaleV;
                }
            }
        }
    }

    private void applyTextureOrColor(Material material, String texturePath, long textureType, long colorType, Color color) {
        if (texturePath != null && !texturePath.isEmpty()) {
            Texture texture = loadTexture(texturePath);
            if (texture != null) {
                material.remove(colorType);
                material.set(new PBRTextureAttribute(textureType, texture));
            }
        } else {
            material.remove(textureType);
            material.set(new PBRColorAttribute(colorType, color));
        }
    }

    private void applyTexture(Material material, String texturePath, long textureType) {
        if (texturePath != null && !texturePath.isEmpty()) {
            Texture texture = loadTexture(texturePath);
            if (texture != null) {
                material.set(new PBRTextureAttribute(textureType, texture));
            }
        } else {
            material.remove(textureType);
        }
    }

    private Texture loadTexture(String textureFileName) {
        if (textureFileName == null || textureFileName.isEmpty()) return null;

        if (textureFileName.startsWith("buffer://")) {
            String bufferName = textureFileName.substring(9);
            TextureRegion region = org.catrobat.catroid.content.RenderTextureManager.INSTANCE.getTextureRegion(bufferName);
            if (region != null) {
                return region.getTexture();
            }
            return null;
        }

        Texture texture = loadedTextures.get(textureFileName);
        if (texture == null) {
            try {
                File textureFileHandle = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(textureFileName);
                if (textureFileHandle != null && textureFileHandle.exists()) {
                    texture = new Texture(Gdx.files.absolute(textureFileHandle.getAbsolutePath()));
                    texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                    loadedTextures.put(textureFileName, texture);
                } else {
                    Gdx.app.error("3DManager", "Texture file not found in project: " + textureFileName);
                    return null;
                }
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Could not load texture: " + textureFileName, e);
                return null;
            }
        }
        return texture;
    }

    private void disposeManagedBatches() {
        for (BillboardParticleBatch batch : managedBatches.values()) {
            try {
                java.lang.reflect.Field poolField = BillboardParticleBatch.class.getDeclaredField("renderablePool");
                poolField.setAccessible(true);
                com.badlogic.gdx.utils.Pool<Renderable> renderablePool = (com.badlogic.gdx.utils.Pool<Renderable>) poolField.get(batch);
                Array<Renderable> allRenderables = new Array<>();
                if (renderablePool == null) return;
                renderablePool.freeAll(allRenderables);
                for(Renderable r : allRenderables){
                    if(r.meshPart.mesh != null) {
                        r.meshPart.mesh.dispose();
                    }
                }
                renderablePool.clear();

            } catch (Exception e) {
                Gdx.app.error("Dispose", "Failed to dispose meshes in BillboardParticleBatch via reflection", e);
            }
        }
        managedBatches.clear();
    }


    public Vector3 getPosition(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 position = new Vector3();
            instance.transform.getTranslation(position);
            return position;
        }
        return null;
    }


    public Vector3 getRotation(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Quaternion q = new Quaternion();
            instance.transform.getRotation(q);

            return new Vector3(q.getPitch(), q.getYaw(), q.getRoll());
        }
        return null;
    }


    public Vector3 getScale(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 scale = new Vector3();
            instance.transform.getScale(scale);
            return scale;
        }
        return null;
    }

    public Float getDistance(String id1, String id2) {
        if (getPosition(id1) == null || getPosition(id2) == null) return -1f;
        return getPosition(id1).dst(getPosition(id2));
    }





    public void setFriction(String objectId, float friction) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            if (friction > 9999) {
                body.setDamping(friction - 10000, friction - 10000);
            } else {
                body.setFriction(friction);
            }

            body.activate();
        }
    }


    public void setRestitution(String objectId, float restitution) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.setRestitution(restitution);

            body.activate();
        }
    }


    public void setContinuousCollisionDetection(String objectId, boolean enabled) {
        btRigidBody body = physicsBodies.get(objectId);
        ModelInstance instance = sceneObjects.get(objectId);
        if (body == null || instance == null || body.isStaticObject()) {
            return;
        }
        if (enabled) {
            BoundingBox bbox = new BoundingBox();
            instance.model.calculateBoundingBox(bbox);
            Vector3 dimensions = new Vector3();
            bbox.getDimensions(dimensions);
            float minSize = Math.min(Math.min(dimensions.x, dimensions.y), dimensions.z);
            body.setCcdMotionThreshold(minSize * 0.5f);
            body.setCcdSweptSphereRadius(minSize * 0.5f);
        } else {
            body.setCcdMotionThreshold(0);
            body.setCcdSweptSphereRadius(0);
        }
    }

    private org.catrobat.catroid.raptor.postprocessing.CustomScreenShaderEffect customScreenEffect;

    public void setCustomScreenShader(String vertexCode, String fragmentCode) {
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            if (vfxManager != null) {
                if (customScreenEffect != null) {
                    vfxManager.removeEffect(customScreenEffect);
                    customScreenEffect.dispose();
                    customScreenEffect = null;
                }
                if (vertexCode == null || vertexCode.isEmpty() || fragmentCode == null || fragmentCode.isEmpty()) {
                    if (currentConfig == null || !currentConfig.isActive) {
                        postprocessingEnabled = false;
                    }
                    return;
                }

                try {
                    customScreenEffect = new org.catrobat.catroid.raptor.postprocessing.CustomScreenShaderEffect(vertexCode, fragmentCode);
                    vfxManager.addEffect(customScreenEffect);
                    postprocessingEnabled = true;
                } catch (Exception e) {
                    Gdx.app.error("CustomScreenShader", "Failed to compile: " + e.getMessage());
                }
            }
        });
    }

    public void setShaderCode(String vertexCode, String fragmentCode) {
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            Gdx.app.log("ShaderDebug", "--- setShaderCode CALLED ---");
            if (vertexCode == null || vertexCode.isEmpty() || fragmentCode == null || fragmentCode.isEmpty()) {
                resetSceneShader();
                return;
            }

            try {
                DefaultShader.Config config = new DefaultShader.Config(vertexCode, fragmentCode);

                ShaderProgram tempProgram = new ShaderProgram(vertexCode, fragmentCode);
                if (!tempProgram.isCompiled()) {
                    throw new Exception("Shader compilation failed: " + tempProgram.getLog());
                }
                tempProgram.dispose();

                if (customShaderProvider != null) customShaderProvider.dispose();
                if (modelBatch != null) modelBatch.dispose();

                customShaderProvider = new CustomShaderProvider(config, customUniforms);

                modelBatch = new ModelBatch(customShaderProvider);

                Gdx.app.log("ShaderDebug", "--- CustomShaderProvider and new ModelBatch CREATED successfully ---");
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Shader setup failed: " + e.getMessage());
                resetSceneShader();
            }
        });
    }


    public void resetSceneShader() {
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            if (customShaderProvider != null) {
                customShaderProvider.dispose();
                customShaderProvider = null;
            }

            if (modelBatch != null) modelBatch.dispose();

            customUniforms.clear();

            modelBatch = new ModelBatch(defaultShaderProvider);


        });
    }


    public void setShaderUniform(String name, float value) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, value);
        }
    }


    public void setShaderUniform(String name, float x, float y, float z) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector3(x, y, z));
        }
    }

    public void setShaderUniform(String name, int value) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, value);
        }
    }


    public void setShaderUniform(String name, float x, float y) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector2(x, y));
        }
    }


    public void setShaderUniform(String name, float x, float y, float z, float w) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector4(x, y, z, w));
        }
    }

    public Vector3 getSunLightDirection() {
        if (pbrLight != null) {
            return pbrLight.direction;
        }
        return new Vector3(1, -1.5f, 1);
    }

    public Color getSunLightColor() {
        if (pbrLight != null) {
            return pbrLight.color;
        }
        return Color.WHITE;
    }

    public float getSunLightIntensity() {
        if (pbrLight != null) {
            return pbrLight.intensity;
        }
        return 0;
    }

    public void setWorldTransform(String objectId, Matrix4 worldTransform) {
        if (LOG_THREED_MANAGER_DEBUG) {
            Vector3 receivedPos = new Vector3();
            Quaternion receivedRot = new Quaternion();
            worldTransform.getTranslation(receivedPos);
            worldTransform.getRotation(receivedRot, true);
            Log.d("TDM_DEBUG", "  >>> ThreeDManager.setWorldTransform() called for '" + objectId + "' <<<");
            Log.d("TDM_DEBUG", "    [SetWorldTrans] Received World Pos: " + receivedPos + ", Rot: " + receivedRot);
        }

        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) {
            instance = editorProxies.get(objectId);
        }

        if (instance != null) {
            instance.transform.set(worldTransform);
            instance.calculateTransforms();
            if (LOG_THREED_MANAGER_DEBUG) {
                Vector3 modelPos = new Vector3();
                instance.transform.getTranslation(modelPos);
                Log.d("TDM_DEBUG", "    [SetWorldTrans] ModelInstance '" + objectId + "' set to Pos: " + modelPos);
            }
        } else {
            if (LOG_THREED_MANAGER_DEBUG) Log.w("TDM_DEBUG", "    [SetWorldTrans] ModelInstance for '" + objectId + "' not found. Skipping model update.");
        }

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 bodyTransform = body.getWorldTransform();
            Vector3 position = new Vector3();
            Quaternion rotation = new Quaternion();
            worldTransform.getTranslation(position);
            worldTransform.getRotation(rotation, true);

            bodyTransform.set(position, rotation);

            body.setWorldTransform(bodyTransform);
            if (body.getMotionState() != null) {
                body.getMotionState().setWorldTransform(bodyTransform);
            }
            body.activate();
            if (body.isStaticObject() || body.isKinematicObject()) {
                dynamicsWorld.updateSingleAabb(body);
            }
            if (LOG_THREED_MANAGER_DEBUG) {
                Vector3 physicsPos = new Vector3();
                body.getWorldTransform().getTranslation(physicsPos);
                Log.d("TDM_DEBUG", "    [SetWorldTrans] Physics body '" + objectId + "' set to Pos: " + physicsPos);
            }
        } else {
            if (LOG_THREED_MANAGER_DEBUG && body == null) Log.w("TDM_DEBUG", "    [SetWorldTrans] Physics body for '" + objectId + "' not found. Skipping physics update.");
            else if (LOG_THREED_MANAGER_DEBUG && editorMode) Log.d("TDM_DEBUG", "    [SetWorldTrans] In editor mode, skipping physics update for '" + objectId + "'.");
        }
    }

    private void applyUniformsToScreenShader() {
        if (customScreenEffect == null) return;
        try {
            java.lang.reflect.Field programField = com.crashinvaders.vfx.effects.ShaderVfxEffect.class.getDeclaredField("program");
            programField.setAccessible(true);
            ShaderProgram program = (ShaderProgram) programField.get(customScreenEffect);

            if (program != null && program.isCompiled()) {
                program.begin();
                for (Map.Entry<String, Object> entry : customUniforms.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof Float) {
                        program.setUniformf(name, (Float) value);
                    } else if (value instanceof Vector2) {
                        Vector2 v2 = (Vector2) value;
                        program.setUniformf(name, v2.x, v2.y);
                    } else if (value instanceof Vector3) {
                        Vector3 v3 = (Vector3) value;
                        program.setUniformf(name, v3.x, v3.y, v3.z);
                    } else if (value instanceof Integer) {
                        program.setUniformi(name, (Integer) value);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ThreeDManager", "Failed to apply uniforms to custom screen shader", e);
        }
    }

    public void clearScene() {
        Array<String> objectIds = new Array<>();
        for (String id : sceneObjects.keySet()) {
            objectIds.add(id);
        }

        for (String id : objectIds) {
            removeObject(id);
        }
        sceneObjects.clear();

        for (net.mgsx.gltf.scene3d.scene.SceneAsset asset : loadedGltfAssets.values()) {
            if (asset != null) asset.dispose();
        }
        loadedGltfAssets.clear();

        Array<String> bodyIds = new Array<>();
        for (String id : physicsBodies.keySet()) {
            bodyIds.add(id);
        }
        for (String id : bodyIds) {
            removePhysicsBody(id);
        }
        physicsBodies.clear();
        physicsResources.clear();

        for (ParticleSystem3DRuntime rt : activeParticleRuntimes3D.values()) rt.dispose();
        activeParticleRuntimes3D.clear();

        if (skybox != null) {
            skybox.dispose();
            skybox = null;
        }
        if (skyboxCubemap != null) {
            skyboxCubemap.dispose();
            skyboxCubemap = null;
        }
        if (diffuseCubemap != null) {
            diffuseCubemap.dispose();
            diffuseCubemap = null;
        }
        if (specularCubemap != null) {
            specularCubemap.dispose();
            specularCubemap = null;
        }
        if (gridScene != null) {
            if (sceneManager != null) {
                sceneManager.removeScene(gridScene);
            }
            gridScene = null;
        }
        if (gridInstance != null) {
            gridInstance.model.dispose();
            gridInstance = null;
        }
        Array<String> constraintIds = new Array<>();
        for (String id : physicsConstraints.keySet()) {
            constraintIds.add(id);
        }
        for (String id : constraintIds) {
            removeConstraint(id);
        }

        if (particleSystemInitialized) {
            disposeManagedBatches();
            particleSystem = new ParticleSystem();
        }

        pointLights.clear();
        editorProxies.clear();
        spotLights.clear();
        directionalLights.clear();
        if (sceneManager != null) {
            sceneManager.environment.clear();

            try {
                setupDefaultLighting();
            } catch (Exception e) {
                Log.e("ThreeDManager", "Couldn't setup lightning: " + e);
            }
        }

        aiLastKnownPositions.clear();
        aiHasLostTarget.clear();
        aiSmoothedDirection.clear();
        animationControllers.clear();
        gltfObjectIds.clear();
        rayCastResults.clear();
        cameraAttachments.clear();
        aiPathHistory.clear();
        aiDeadEnds.clear();
        aiLastTargetPos.clear();
        aiBacktrackTarget.clear();
        modelRotationOffsets.clear();
        aiSlideDirs.clear();
        aiCurrentYaws.clear();

        cameraTargetId = null;
        cameraYaw = 0f;
        cameraPitch = 0f;
        cameraDistance = 10f;
        cameraOffset.set(0, 0, 0);

        cameraTrackTargetId = null;
        cameraTrackMode = 0;
        cameraTrackPosOffset.set(0, 0, 0);
        cameraTrackRotOffset.idt();

        activePointerId = -1;
        touchRotationEnabled = false;

        shakeTimer = 0f;
        shakeIntensity = 0f;
        currentShakeOffset.set(0, 0, 0);

        if (camera != null) {
            camera.position.set(0, 0, 0);
            camera.direction.set(0, 0, -1);
            camera.up.set(0, 1, 0);
            camera.fieldOfView = 67f;
            camera.update();
        }
    }


    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        clearScene();
        physicsConstraints.clear();

        if (depthFbo != null) {
            depthFbo.dispose();
            depthFbo = null;
        }
        if (materialFbo != null) {
            materialFbo.dispose();
            materialFbo = null;
        }
        if (materialBatch != null) {
            materialBatch.dispose();
            materialBatch = null;
        }

        for (BufferPipeline pipeline : bufferPipelines.values()) {
            if (pipeline != null) pipeline.dispose();
        }
        bufferPipelines.clear();

        if (isVfxCapturing && vfxManager != null) {
            try {
                vfxManager.endInputCapture();
            } catch (Exception ignored) {}
            isVfxCapturing = false;
        }

        if (ssaoEffect != null) { ssaoEffect.dispose(); ssaoEffect = null; }
        if (rayTracingEffect != null) { rayTracingEffect.dispose(); rayTracingEffect = null; }
        if (ssgiEffect != null) { ssgiEffect.dispose(); ssgiEffect = null; }
        if (godRaysEffect != null) { godRaysEffect.dispose(); godRaysEffect = null; }
        if (heightFogEffect != null) { heightFogEffect.dispose(); heightFogEffect = null; }
        if (volumetricFogEffect != null) { volumetricFogEffect.dispose(); volumetricFogEffect = null; }
        if (dofEffect != null) { dofEffect.dispose(); dofEffect = null; }
        if (linearizeEffect != null) { linearizeEffect.dispose(); linearizeEffect = null; }
        if (customScreenEffect != null) { customScreenEffect.dispose(); customScreenEffect = null; }

        if (particleProxyModel != null) particleProxyModel.dispose();
        if (modelBatch != null) modelBatch.dispose();
        if (defaultParticleTexture != null) defaultParticleTexture.dispose();
        if (particleModelBatch != null) particleModelBatch.dispose();
        if (particleSystemInitialized) disposeManagedBatches();
        if (blitBatch != null) blitBatch.dispose();
        if (iblBuilderCompat != null) iblBuilderCompat.dispose();
        if (eyeAdaptationManager != null) eyeAdaptationManager.dispose();
        if (exposureEffect != null) exposureEffect.dispose();
        if (tonemappingEffect != null) tonemappingEffect.dispose();
        if (defaultShaderProvider != null) defaultShaderProvider.dispose();
        if (customShaderProvider != null) customShaderProvider.dispose();
        if (sceneManager != null) sceneManager.dispose();
        if (lightProxyModel != null) lightProxyModel.dispose();
        if (cameraProxyModel != null) cameraProxyModel.dispose();
        if (wireframeBoxModel != null) wireframeBoxModel.dispose();
        if (wireframeSphereModel != null) wireframeSphereModel.dispose();
        if (wireframeCylinderModel != null) wireframeCylinderModel.dispose();
        if (wireframeBatch != null) wireframeBatch.dispose();
        if (brdfLUT != null) brdfLUT.dispose();
        if (panoramicConverter != null) panoramicConverter.dispose();
        if (debugDrawer != null) debugDrawer.dispose();
        if (solver != null) solver.dispose();
        if (broadphase != null) broadphase.dispose();
        if (dispatcher != null) dispatcher.dispose();
        if (collisionConfig != null) collisionConfig.dispose();
        if (collisionCallback != null) collisionCallback.dispose();
        if (sceneFbo2 != null) sceneFbo2.dispose();
        if (vfxManager != null) vfxManager.dispose();
        if (csm != null) csm.dispose();
        if (voxelExecutor != null) voxelExecutor.dispose();
        if (fsrShader != null) fsrShader.dispose();
        if (depthShader != null) depthShader.dispose();
        if (cachedBloomEffect != null) { cachedBloomEffect.dispose(); cachedBloomEffect = null; }
        if (cachedGaussianBlurEffect != null) { cachedGaussianBlurEffect.dispose(); cachedGaussianBlurEffect = null; }
        if (cachedMotionBlurEffect != null) { cachedMotionBlurEffect.dispose(); cachedMotionBlurEffect = null; }
        if (cachedVignetteEffect != null) { cachedVignetteEffect.dispose(); cachedVignetteEffect = null; }
        if (cachedLevelsEffect != null) { cachedLevelsEffect.dispose(); cachedLevelsEffect = null; }
        if (ssgiEffect != null) { ssgiEffect.dispose(); ssgiEffect = null; }

        if (dynamicsWorld != null) {
            dynamicsWorld.dispose();
            dynamicsWorld = null;
        }

        for (ParticleSystem3DRuntime rt : activeParticleRuntimes3D.values()) rt.dispose();
        activeParticleRuntimes3D.clear();
        for (ParticleEffect eff : activeParticleEffects.values()) if (eff != null) eff.dispose();
        for (Model model : loadedModels.values()) if (model != null) model.dispose();
        for (Texture texture : loadedTextures.values()) if (texture != null) texture.dispose();
        for (PlayableAudio audio : active3DSounds) {
            if (audio == null) continue;
            audio.stop();
            audio.dispose();
        }
        for (AudioAsset asset : loadedAudioAssets.values()) {
            if (asset != null) asset.dispose();
        }

        managedBatches.clear();
        activeParticleEffects.clear();
        effectsNormal.clear();
        effectsAdditive.clear();
        loadedModels.clear();
        loadedTextures.clear();
        active3DSounds.clear();
        loadedAudioAssets.clear();
        aiLastKnownPositions.clear();
        aiHasLostTarget.clear();
        aiSmoothedDirection.clear();

        modelBatch = null;
        dynamicsWorld = null;
        sceneManager = null;
    }

    private class CollisionCallback extends com.badlogic.gdx.physics.bullet.collision.ContactResultCallback {
        public boolean collided = false;

        @Override
        public float addSingleResult(com.badlogic.gdx.physics.bullet.collision.btManifoldPoint cp,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {
            collided = true;
            return 0;
        }
    }


    private class NameAccumulatingContactCallback extends com.badlogic.gdx.physics.bullet.collision.ContactResultCallback {
        public StringBuilder namesBuilder = new StringBuilder();
        private String targetId;

        public void setTargetId(String targetId) {
            this.targetId = targetId;
            namesBuilder.setLength(0);
        }

        @Override
        public float addSingleResult(com.badlogic.gdx.physics.bullet.collision.btManifoldPoint cp,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {


            btCollisionObject obj0 = colObj0Wrap.getCollisionObject();
            btCollisionObject obj1 = colObj1Wrap.getCollisionObject();


            String hitName = null;


            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                if (entry.getValue().equals(obj0) && !entry.getKey().equals(targetId)) {
                    hitName = entry.getKey();
                    break;
                }
            }

            if (hitName == null) {
                for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                    if (entry.getValue().equals(obj1) && !entry.getKey().equals(targetId)) {
                        hitName = entry.getKey();
                        break;
                    }
                }
            }


            if (hitName != null && namesBuilder.indexOf(hitName) == -1) {
                if (namesBuilder.length() > 0) namesBuilder.append("\n");
                namesBuilder.append(hitName);
            }

            return 0;
        }
    }

    private NameAccumulatingContactCallback contactListCallback;


    public String getPhysicsCollisionsList(String objectId) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body == null) return "";

        contactListCallback.setTargetId(objectId);

        dynamicsWorld.contactTest(body, contactListCallback);

        return contactListCallback.namesBuilder.toString();
    }

    public String getIntersectionCollisionsList(String objectId) {
        ModelInstance targetInstance = sceneObjects.get(objectId);
        if (targetInstance == null) return "";

        StringBuilder sb = new StringBuilder();


        targetInstance.calculateBoundingBox(bounds1);
        bounds1.mul(targetInstance.transform);

        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
            String otherId = entry.getKey();
            if (otherId.equals(objectId)) continue;

            ModelInstance otherInstance = entry.getValue();


            otherInstance.calculateBoundingBox(bounds2);
            bounds2.mul(otherInstance.transform);

            if (bounds1.intersects(bounds2)) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(otherId);
            }
        }
        return sb.toString();
    }

    private static class BoneOverride {
        String modelId; String boneName; String targetId;
        public BoneOverride(String m, String b, String t) { modelId = m; boneName = b; targetId = t; }
    }
    private final List<BoneOverride> boneOverrides = new ArrayList<>();

    public void bindBoneToObject(String modelId, String boneName, String targetId) {
        boneOverrides.removeIf(b -> b.modelId.equals(modelId) && b.boneName.equals(boneName));
        if (targetId != null && !targetId.isEmpty()) {
            boneOverrides.add(new BoneOverride(modelId, boneName, targetId));
        }
    }

    public void renderSceneForCustomCamera(PerspectiveCamera customCamera, FrameBuffer targetFbo, boolean useVfx) {
        Camera originalCamera = this.camera;
        this.camera = customCamera;
        if (realisticMode && sceneManager != null) {
            sceneManager.setCamera(customCamera);
            sceneManager.updateViewport(targetFbo.getWidth(), targetFbo.getHeight());
            sceneManager.update(0f);
        }
        customCamera.update();

        try {
            updateShadowLight();

            if (realisticMode && sceneManager != null) {
                sceneManager.renderShadows();
            }

            BufferPipeline pipeline = useVfx ? bufferPipelines.get(targetFbo) : null;

            if (pipeline != null && pipeline.vfxManager != null && postprocessingEnabled) {
                renderDepthAndMaterial(customCamera, pipeline.depthFbo, pipeline.materialFbo);

                Texture depthTex = pipeline.depthFbo.getColorBufferTexture();
                Texture matTex = pipeline.materialFbo.getColorBufferTexture();

                if (pipeline.ssaoEffect != null) { pipeline.ssaoEffect.setCamera(customCamera); pipeline.ssaoEffect.setDepthTexture(depthTex); }
                if (pipeline.rayTracingEffect != null) { pipeline.rayTracingEffect.setCamera(customCamera); pipeline.rayTracingEffect.setDepthTexture(depthTex); pipeline.rayTracingEffect.setMaterialTexture(matTex); }
                if (pipeline.ssgiEffect != null) { pipeline.ssgiEffect.setCamera(customCamera); pipeline.ssgiEffect.setDepthTexture(depthTex); }
                if (pipeline.godRaysEffect != null) { pipeline.godRaysEffect.setCamera(customCamera); pipeline.godRaysEffect.setDepthTexture(depthTex); }
                if (pipeline.heightFogEffect != null) { pipeline.heightFogEffect.setCamera(customCamera); pipeline.heightFogEffect.setDepthTexture(depthTex); }
                if (pipeline.volumetricFogEffect != null) { pipeline.volumetricFogEffect.setCamera(customCamera); pipeline.volumetricFogEffect.setDepthTexture(depthTex); }
                if (pipeline.dofEffect != null) { pipeline.dofEffect.setCamera(customCamera); pipeline.dofEffect.setDepthTexture(depthTex); }

                targetFbo.begin();
                Gdx.gl.glViewport(0, 0, targetFbo.getWidth(), targetFbo.getHeight());
                Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                renderColorsOnly(targetFbo.getWidth(), targetFbo.getHeight());
                targetFbo.end();

                pipeline.vfxManager.cleanUpBuffers();
                pipeline.vfxManager.useAsInput(targetFbo.getColorBufferTexture());

                pipeline.vfxManager.update(Gdx.graphics.getDeltaTime());

                pipeline.vfxManager.applyEffects();

                targetFbo.begin();
                Gdx.gl.glViewport(0, 0, targetFbo.getWidth(), targetFbo.getHeight());
                Gdx.gl.glClearColor(0, 0, 0, 0);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                Texture resultTex = pipeline.vfxManager.getResultBuffer().getTexture();
                blitBatch.begin();
                blitBatch.draw(resultTex, 0, 0, targetFbo.getWidth(), targetFbo.getHeight(),
                        0, 0, resultTex.getWidth(), resultTex.getHeight(), false, true);
                blitBatch.end();

                targetFbo.end();

            } else {
                targetFbo.begin();
                Gdx.gl.glViewport(0, 0, targetFbo.getWidth(), targetFbo.getHeight());
                Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                if (realisticMode && sceneManager != null) {
                    sceneManager.renderMirror();
                    sceneManager.renderTransmission();
                    sceneManager.renderColors();
                } else {
                    modelBatch.begin(customCamera);
                    for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
                        if (!inactiveRenderObjects.contains(entry.getKey())) {
                            modelBatch.render(entry.getValue(), environment);
                        }
                    }
                    modelBatch.end();
                }

                if (particleSystemInitialized && !activeParticleEffects.isEmpty()) {
                    particleModelBatch.begin(customCamera);
                    particleModelBatch.render(particleSystem);
                    particleModelBatch.end();
                }

                targetFbo.end();
            }

        } catch (Exception e) {
            Log.e("ThreeDManager", "Critical error in buffer rendering", e);
            try { targetFbo.end(); } catch (Exception ignored) {}
        } finally {
            this.camera = (PerspectiveCamera) originalCamera;
            if (realisticMode && sceneManager != null) {
                sceneManager.setCamera(originalCamera);

                sceneManager.updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                sceneManager.update(0f);
            }

            updateShadowLight();

            if (blitCamera != null) {
                blitCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                blitCamera.update();
            }

            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    private final AsyncExecutor voxelExecutor = new AsyncExecutor(1);

    public void updateVoxelMesh(final String objectId, final VoxelManager.VoxelBuffer buffer, final String texturePath, final int atlasWidth, final int atlasHeight) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        Texture texture = loadTexture(texturePath);
        Material mat = new Material(PBRTextureAttribute.createBaseColorTexture(texture));
        mat.set(FloatAttribute.createAlphaTest(0.5f));
        mat.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createMetallic(0.0f));
        mat.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createRoughness(1.0f));

        MeshPartBuilder builder = mb.part("voxel_part", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates, mat);

        float uStep = 1.0f / atlasWidth;
        float vStep = 1.0f / atlasHeight;

        for (int x = 0; x < buffer.getSizeX(); x++) {
            for (int y = 0; y < buffer.getSizeY(); y++) {
                for (int z = 0; z < buffer.getSizeZ(); z++) {
                    short type = buffer.get(x, y, z);
                    if (type <= 0) continue;

                    VoxelManager.BlockConfig config = VoxelManager.Companion.getConfig(type);

                    float texX = config.getTexX();
                    float texY = config.getTexY();

                    float u = texX * uStep;
                    float v = 1.0f - (texY * vStep) - vStep;

                    float u2 = u + uStep;
                    float v2 = v + vStep;

                    builder.setUVRange(u, v, u2, v2);

                    switch (config.getShape()) {
                        case CUBE:
                            if (buffer.get(x, y + 1, z) == 0) builder.rect(x,y+1,z+1, x+1,y+1,z+1, x+1,y+1,z, x,y+1,z, 0,1,0);
                            if (buffer.get(x, y - 1, z) == 0) builder.rect(x,y,z, x+1,y,z, x+1,y,z+1, x,y,z+1, 0,-1,0);
                            if (buffer.get(x, y, z + 1) == 0) builder.rect(x,y,z+1, x+1,y,z+1, x+1,y+1,z+1, x,y+1,z+1, 0,0,1);
                            if (buffer.get(x, y, z - 1) == 0) builder.rect(x+1,y,z, x,y,z, x,y+1,z, x+1,y+1,z, 0,0,-1);
                            if (buffer.get(x - 1, y, z) == 0) builder.rect(x,y,z, x,y,z+1, x,y+1,z+1, x,y+1,z, -1,0,0);
                            if (buffer.get(x + 1, y, z) == 0) builder.rect(x+1,y,z+1, x+1,y,z, x+1,y+1,z, x+1,y+1,z+1, 1,0,0);
                            break;
                        case CROSS:
                            builder.rect(x,y,z, x+1,y,z+1, x+1,y+1,z+1, x,y+1,z, 1,0,1);
                            builder.rect(x,y+1,z, x+1,y+1,z+1, x+1,y,z+1, x,y,z, -1,0,-1);
                            builder.rect(x+1,y,z, x,y,z+1, x,y+1,z+1, x+1,y+1,z, -1,0,1);
                            builder.rect(x+1,y+1,z, x,y+1,z+1, x,y,z+1, x+1,y,z, 1,0,-1);
                            break;
                        case FLOOR:
                            builder.rect(x, y+0.01f, z+1, x+1, y+0.01f, z+1, x+1, y+0.01f, z, x, y+0.01f, z, 0,1,0);
                            break;
                        case CEILING:
                            builder.rect(x, y+1-0.01f, z, x+1, y+1-0.01f, z, x+1, y+1-0.01f, z+1, x, y+1-0.01f, z+1, 0,-1,0);
                            break;
                        case WALL_NORTH:
                            builder.rect(x+1, y, z+0.01f, x, y, z+0.01f, x, y+1, z+0.01f, x+1, y+1, z+0.01f, 0,0,1);
                            break;
                        case WALL_SOUTH:
                            builder.rect(x, y, z+1-0.01f, x+1, y, z+1-0.01f, x+1, y+1, z+1-0.01f, x, y+1, z+1-0.01f, 0,0,-1);
                            break;
                        case WALL_WEST:
                            builder.rect(x+0.01f, y, z, x+0.01f, y, z+1, x+0.01f, y+1, z+1, x+0.01f, y+1, z, 1,0,0);
                            break;
                        case WALL_EAST:
                            builder.rect(x+1-0.01f, y, z+1, x+1-0.01f, y, z, x+1-0.01f, y+1, z, x+1-0.01f, y+1, z+1, -1,0,0);
                            break;
                    }
                }
            }
        }

        final Model model = mb.end();

        voxelExecutor.submit(() -> {
            btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(model.meshParts);
            final btCollisionShape newShape = new btBvhTriangleMeshShape(vertexArray, false);

            Gdx.app.postRunnable(() -> {
                if (disposed) return;
                ModelInstance oldInstance = sceneObjects.get(objectId);
                ModelInstance newInstance = new ModelInstance(model);

                if (oldInstance != null) {
                    newInstance.transform.set(oldInstance.transform);

                    if (realisticMode && sceneManager != null) {
                        for (com.badlogic.gdx.graphics.g3d.RenderableProvider p : sceneManager.getRenderableProviders()) {
                            if (p instanceof net.mgsx.gltf.scene3d.scene.Scene) {
                                if (((net.mgsx.gltf.scene3d.scene.Scene)p).modelInstance == oldInstance) {
                                    sceneManager.removeScene((net.mgsx.gltf.scene3d.scene.Scene)p);
                                    break;
                                }
                            }
                        }
                    }
                }

                newInstance.calculateTransforms();
                newInstance.calculateBoundingBox(new BoundingBox());

                if (realisticMode) sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(newInstance));
                sceneObjects.put(objectId, newInstance);

                btRigidBody body = physicsBodies.get(objectId);
                if (body != null) {
                    btCollisionShape oldShape = body.getCollisionShape();

                    Vector3 currentScale = new Vector3();
                    newInstance.transform.getScale(currentScale);
                    newShape.setLocalScaling(currentScale);

                    body.setCollisionShape(newShape);
                    dynamicsWorld.updateSingleAabb(body);

                    if (oldShape != null) oldShape.dispose();
                } else {
                    createMeshPhysicsBody(objectId, newInstance);
                    btRigidBody newBody = physicsBodies.get(objectId);
                    if (newBody != null) {
                        newBody.setCollisionFlags(newBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_STATIC_OBJECT);
                        newBody.setCollisionShape(newShape);
                    }
                }
            });
            return null;
        });
    }

    public String getVoxelData(String worldId, float x, float y, float z) {
        return VoxelManager.Companion.getBlockInfo(worldId, (int)x, (int)y, (int)z);
    }

    public void applyShaderToImage(final String filename, final String vertexCode, final String fragmentCode) {
        Gdx.app.postRunnable(() -> {
            if (disposed) return;
            File file = ProjectManager.getInstance().getCurrentProject().getFile(filename);
            if (file == null || !file.exists()) {
                Gdx.app.error("ThreeDManager", "ShaderImage Error: file not found: " + filename);
                return;
            }

            FileHandle handle = Gdx.files.absolute(file.getAbsolutePath());
            Texture originalTexture = null;
            FrameBuffer fbo = null;
            ShaderProgram shader = null;
            SpriteBatch batch = null;
            Pixmap resultPixmap = null;

            try {
                originalTexture = new Texture(handle);
                int width = originalTexture.getWidth();
                int height = originalTexture.getHeight();

                fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

                shader = new ShaderProgram(vertexCode, fragmentCode);
                if (!shader.isCompiled()) {
                    throw new Exception("Shader cannot be compiled:\n" + shader.getLog());
                }

                fbo.begin();
                Gdx.gl.glClearColor(0, 0, 0, 0);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                OrthographicCamera cam = new OrthographicCamera(width, height);
                cam.setToOrtho(true, width, height);

                batch = new SpriteBatch();
                batch.setProjectionMatrix(cam.combined);
                batch.setShader(shader);

                batch.begin();
                batch.draw(originalTexture, 0, 0, width, height);
                batch.end();

                resultPixmap = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0, 0, width, height);
                fbo.end();

                PixmapIO.writePNG(handle, resultPixmap);

                Gdx.app.log("ThreeDManager", "Shader successfully applyed to file: " + filename);

                if (loadedTextures.containsKey(filename)) {
                    Texture cached = loadedTextures.remove(filename);
                    if (cached != null) cached.dispose();
                }

            } catch (Exception e) {
                Gdx.app.error("ThreeDManager", "Error when processing image with shader: " + e.getMessage(), e);
            } finally {
                if (originalTexture != null) originalTexture.dispose();
                if (fbo != null) fbo.dispose();
                if (shader != null) shader.dispose();
                if (batch != null) batch.dispose();
                if (resultPixmap != null) resultPixmap.dispose();
            }
        });
    }
}

class DepthShader implements com.badlogic.gdx.graphics.g3d.Shader {
    ShaderProgram program;
    Camera camera;

    private int u_bones = -1;
    private int u_objReflectivity = -1;


    private static final float[] boneBuffer = new float[110 * 16];

    public DepthShader(ShaderProgram program) {
        this.program = program;
        this.u_bones = program.getUniformLocation("u_bones");
        this.u_objReflectivity = program.getUniformLocation("u_objReflectivity");
    }

    @Override public void init() {}
    @Override public int compareTo(Shader other) { return 0; }
    @Override public boolean canRender(Renderable instance) { return true; }

    @Override
    public void begin(Camera camera, com.badlogic.gdx.graphics.g3d.utils.RenderContext context) {
        this.camera = camera;
        program.begin();
        program.setUniformMatrix("u_projViewTrans", camera.combined);
        program.setUniformMatrix("u_viewTrans", camera.view);
        program.setUniformf("u_farPlane", camera.far);
        context.setDepthTest(GL20.GL_LEQUAL);
        context.setDepthMask(true);
    }

    @Override
    public void render(Renderable renderable) {
        if (renderable.material != null) {
            if (renderable.material.has(ColorAttribute.Diffuse)) {
                ColorAttribute diff = (ColorAttribute) renderable.material.get(ColorAttribute.Diffuse);
                if (diff.color.a <= 0.01f) return;
            }
            if (renderable.material.has(PBRColorAttribute.BaseColorFactor)) {
                PBRColorAttribute pbrColor = (PBRColorAttribute) renderable.material.get(PBRColorAttribute.BaseColorFactor);
                if (pbrColor.color.a <= 0.01f) return;
            }
            if (renderable.material.has(BlendingAttribute.Type)) {
                BlendingAttribute blend = (BlendingAttribute) renderable.material.get(BlendingAttribute.Type);
                if (blend.opacity <= 0.01f) return;
            }
        }

        program.setUniformMatrix("u_worldTrans", renderable.worldTransform);


        if (u_objReflectivity != -1) {
            float refl = 1.0f;
            if (renderable.userData instanceof Float) {
                refl = (Float) renderable.userData;
            }
            program.setUniformf(u_objReflectivity, refl);
        }


        if (renderable.bones != null && u_bones != -1) {
            int n = renderable.bones.length;

            for (int i = 0; i < n && i < 110; i++) {
                System.arraycopy(renderable.bones[i].val, 0, boneBuffer, i * 16, 16);
            }


            program.setUniformMatrix4fv(u_bones, boneBuffer, 0, n * 16);
        }

        renderable.meshPart.render(program);
    }

    @Override public void end() { program.end(); }
    @Override public void dispose() { program.dispose(); }
}


class MaterialShaderProvider extends DefaultShaderProvider {
    @Override
    protected Shader createShader(Renderable renderable) {
        String prefix = "";

        if (renderable.meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates) != null) {
            prefix += "#define HAS_UV\n";
        }

        if (renderable.bones != null) {
            prefix += "#define numBones " + 110 + "\n";
            prefix += "#define boneWeight0Flag\n";
            prefix += "#define boneWeight1Flag\n";
            prefix += "#define boneWeight2Flag\n";
            prefix += "#define boneWeight3Flag\n";
        }

        String vsh = prefix +
                "attribute vec3 a_position;\n" +
                "#ifdef HAS_UV\nattribute vec2 a_texCoord0;\nvarying vec2 v_uv;\n#endif\n" +
                "uniform mat4 u_projViewTrans;\n" +
                "uniform mat4 u_worldTrans;\n" +
                "void main() {\n" +
                "#ifdef HAS_UV\n    v_uv = a_texCoord0;\n#endif\n" +
                "    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);\n" +
                "}";

        String fsh = prefix +
                "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
                "#ifdef HAS_UV\nvarying vec2 v_uv;\n#endif\n" +
                "uniform float u_metallic;\n" +
                "uniform int u_hasTexture;\n" +
                "uniform sampler2D u_texture;\n" +
                "void main() {\n" +
                "    float m = u_metallic;\n" +
                "#ifdef HAS_UV\n" +

                "    if (u_hasTexture == 1) m *= texture2D(u_texture, v_uv).b;\n" +
                "#endif\n" +
                "    gl_FragColor = vec4(m, 0.0, 0.0, 1.0);\n" +
                "}";

        DefaultShader.Config config = new DefaultShader.Config(vsh, fsh);
        config.numBones = 110;

        return new DefaultShader(renderable, config) {
            private final int u_metallic = register("u_metallic");
            private final int u_hasTexture = register("u_hasTexture");
            private final int u_texture = register("u_texture");

            @Override
            public void render(Renderable renderable) {
                float metallic = 0.0f;
                boolean hasTex = false;
                Texture tex = null;


                if (renderable.material.has(PBRFloatAttribute.Metallic)) {
                    metallic = ((PBRFloatAttribute) renderable.material.get(PBRFloatAttribute.Metallic)).value;
                }


                if (renderable.material.has(PBRTextureAttribute.MetallicRoughnessTexture)) {
                    tex = ((PBRTextureAttribute) renderable.material.get(PBRTextureAttribute.MetallicRoughnessTexture)).textureDescription.texture;
                    hasTex = true;
                }

                set(u_metallic, metallic);
                set(u_hasTexture, hasTex ? 1 : 0);
                if (hasTex && tex != null) {
                    set(u_texture, tex);
                }

                super.render(renderable);
            }
        };
    }
}
