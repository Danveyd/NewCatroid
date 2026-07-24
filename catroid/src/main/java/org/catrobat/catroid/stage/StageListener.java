/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.FxaaEffect;
import com.danvexteam.lunoscript_annotations.LunoClass;
import com.gaurav.avnc.vnc.VncClient;
import com.google.common.collect.Multimap;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.camera.CameraManager;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ScreenModes;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.common.ThreadScheduler;
import org.catrobat.catroid.content.AfterUpdateScript;
import org.catrobat.catroid.content.BeforeUpdateScript;
import org.catrobat.catroid.content.EventWrapper;
import org.catrobat.catroid.content.ExitProjectScript;
import org.catrobat.catroid.content.GlobalManager;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.SoundBackup;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.SquareActor;
import org.catrobat.catroid.content.SquareController;
import org.catrobat.catroid.content.VmMonitorActor;
import org.catrobat.catroid.content.WhenNotificationActionTriggeredScript;
import org.catrobat.catroid.content.WhenNotificationClickedScript;
import org.catrobat.catroid.content.XmlHeader;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.WhenNotificationActionTriggeredBrick;
import org.catrobat.catroid.content.bricks.WhenNotificationClickedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.GamepadEventId;
import org.catrobat.catroid.content.eventids.MouseButtonEventId;
import org.catrobat.catroid.embroidery.DSTPatternManager;
import org.catrobat.catroid.embroidery.EmbroideryPatternManager;
import org.catrobat.catroid.fast2d.FastTwoDManager;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.formulaeditor.UserDataWrapper;
import org.catrobat.catroid.io.SoundCacheManager;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.ml.MLBridge;
import org.catrobat.catroid.physics.PhysicsDebugSettings;
import org.catrobat.catroid.physics.PhysicsLook;
import org.catrobat.catroid.physics.PhysicsObject;
import org.catrobat.catroid.physics.PhysicsWorld;
import org.catrobat.catroid.physics.shapebuilder.PhysicsShapeBuilder;
import org.catrobat.catroid.pocketmusic.mididriver.MidiSoundManager;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.sensing.CollisionDetection;
import org.catrobat.catroid.sensing.ColorAtXYDetection;
import org.catrobat.catroid.ui.MainMenuActivity;
import org.catrobat.catroid.ui.dialogs.DebugMenuManager;
import org.catrobat.catroid.ui.dialogs.StageDialog;
import org.catrobat.catroid.ui.recyclerview.controller.SpriteController;
import org.catrobat.catroid.utils.GlobalShaderManager;
import org.catrobat.catroid.utils.ModelPathProcessor;
import org.catrobat.catroid.utils.NewCatroidNotificationManager;
import org.catrobat.catroid.utils.PerformanceTracker;
import org.catrobat.catroid.utils.Resolution;
import org.catrobat.catroid.utils.TouchUtil;
import org.catrobat.catroid.utils.VibrationManager;
import org.catrobat.catroid.utils.lunoscript.RenderManager;
import org.catrobat.catroid.virtualmachine.VirtualMachineManager;
import org.catrobat.catroid.web.WebConnectionHolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import kotlinx.coroutines.GlobalScope;

import static org.catrobat.catroid.common.Constants.SCREENSHOT_AUTOMATIC_FILE_NAME;
import static org.catrobat.catroid.common.Constants.SCREENSHOT_MANUAL_FILE_NAME;
import static org.koin.java.KoinJavaComponent.get;

@LunoClass
public class StageListener implements ApplicationListener {

	private double accumulator = 0.0;
	private final double TIME_STEP = 1.0 / 60.0;
	private final double MAX_ACCUMULATOR = 0.25;

	private static final int AXIS_WIDTH = 4;
	private static final float DELTA_ACTIONS_DIVIDER_MAXIMUM = 10f;
	private static final int ACTIONS_COMPUTATION_TIME_MAXIMUM = 8;
	private static final float AXIS_FONT_SIZE_SCALE_FACTOR = 0.025f;

	private float deltaActionTimeDivisor = 10f;

	private Stage stage = null;
	private Stage uiStage = null;
	private boolean paused = true;
	private boolean finished = false;
	private boolean reloadProject = false;
	public boolean firstFrameDrawn = false;

	private boolean makeScreenshot = false;
	private int screenshotWidth;
	private int screenshotHeight;
	private int screenshotX;
	private int screenshotY;

	private Project project;
	private Scene scene;

	private PhysicsWorld physicsWorld;

	private OrthographicCamera camera;
	private OrthographicCamera uiCamera;
	private Batch batch = null;
	private BitmapFont font;
	private Passepartout passepartout;
	private Viewport viewPort;
	public ShapeRenderer shapeRenderer;
	private PenActor penActor;
	private PlotActor plotActor;
	public EmbroideryPatternManager embroideryPatternManager;
	public WebConnectionHolder webConnectionHolder;

	private List<Sprite> sprites;
	public CameraPositioner cameraPositioner;

	private float virtualWidthHalf;
	private float virtualHeightHalf;
	private float virtualWidth;
	private float virtualHeight;

	private Mesh fullscreenQuad;

	private float time = 0f;

	private Texture axes;

	private boolean makeTestPixels = false;
	private SpriteBatch postProcessBatch;
	private byte[] testPixels;
	private int testX = 0;
	private int testY = 0;
	private int testWidth = 0;
	private int testHeight = 0;

	private StageDialog stageDialog;

	private Resolution maxViewPort = null;
	private Viewport uiViewPort;

	private float cameraRotation = 0f;

    public FastTwoDManager fastTwoDManager;

	public void setMaxViewPort(Resolution maxViewPort) {
		this.maxViewPort = maxViewPort;
	}

	public boolean axesOn = false;
	private final GlyphLayout axesLabelLayout = new GlyphLayout();
	private static final Color AXIS_COLOR = new Color(0xff000cff);

	private static final int Z_LAYER_PEN_ACTOR = 1;
	private static final int Z_LAYER_EMBROIDERY_ACTOR = 2;

	private ShaderProgram postProcessShader;
	private String lastFragmentShaderCode = null;

	private final String POST_PROCESS_VERTEX_SHADER = ""
			+ "attribute vec4 a_position;\n"
			+ "attribute vec2 a_texCoord0;\n"
			+ "varying vec2 v_texCoords;\n"
			+ "\n"
			+ "void main()\n"
			+ "{\n"
			+ "    v_texCoords = a_texCoord0;\n"
			+ "    gl_Position = a_position;\n"
			+ "}\n";

	private Map<String, StageBackup> stageBackupMap = new HashMap<>();

	private InputListener inputListener = null;

	private Map<Sprite, ShowBubbleActor> bubbleActorMap = new HashMap<>();
	private String screenshotName;
	private ScreenshotSaverCallback screenshotSaverCallback = null;
	private ScreenshotSaver screenshotSaver;


	private ThreeDManager threeDManager;

	public SceneManager sceneManager;


	public ThreeDManager getThreeDManager() {
		if (threeDManager != null) {
			threeDManager.ensureInitialized();
		}
		return threeDManager;
	}

	public SceneManager getSceneManager() {
		return sceneManager;
	}

	private com.badlogic.gdx.graphics.g3d.Environment environment;

	private FrameBuffer sceneFbo;
	private FrameBuffer postProcessFbo;
	private TextureRegion fboRegion;

	private InputMultiplexer inputMultiplexer;

	private Mesh vmScreenMesh;
	private float vmX, vmY, vmWidth, vmHeight;

	private Look.BrightnessContrastHueShader brightnessContrastHueShader;

    private final com.badlogic.gdx.utils.StringBuilder debugTextBuilder = new com.badlogic.gdx.utils.StringBuilder();

	public StageListener() {
		webConnectionHolder = new WebConnectionHolder();
	}

	private ShaderProgram vncSwizzleShader;

    private final List<ScriptSequenceAction> beforeUpdateActions = new ArrayList<>();
    private boolean hasBeforeUpdateScripts = false;

    private final List<ScriptSequenceAction> afterUpdateActions = new ArrayList<>();
    private boolean hasAfterUpdateScripts = false;

    private boolean isFirstResize = true;

    public static class PinnedState {
        public float rx;
        public float ry;
        public float originalRotation;
        public float originalScaleX;
        public float originalScaleY;
    }

    private final java.util.Map<Sprite, PinnedState> pinnedSprites = new java.util.HashMap<>();

    public void create() {
        isFirstResize = true;
        org.catrobat.catroid.utils.ActionThreadRegistry.clear();

        deltaActionTimeDivisor = 10f;

        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (uiStage != null) {
            uiStage.dispose();
            uiStage = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (brightnessContrastHueShader != null) {
            brightnessContrastHueShader.dispose();
            brightnessContrastHueShader = null;
        }
        if (threeDManager != null) {
            threeDManager.dispose();
            threeDManager = null;
        }
        if (sceneManager != null) {
            sceneManager.clearScene();
        }
        if (axes != null) {
            axes.dispose();
            axes = null;
        }

        SquareController.getInstance().clearSquares();
        SquareActor.disposeShared();

        brightnessContrastHueShader = new Look.BrightnessContrastHueShader();
        shapeRenderer = new ShapeRenderer();

		project = ProjectManager.getInstance().getCurrentProject();
		scene = ProjectManager.getInstance().getCurrentlyPlayingScene();

		threeDManager = new ThreeDManager();

        MLBridge.nativeResetEngine();

		sceneManager = new SceneManager(threeDManager);

		if (stage == null) {
			createNewStage();
			Gdx.input.setInputProcessor(inputMultiplexer);
		} else {
			stage.getRoot().clear();
			uiStage.getRoot().clear();
		}
		initScreenMode();
		initStageInputListener();
		screenshotSaver = new ScreenshotSaver(Gdx.files, getScreenshotPath(), screenshotWidth,
				screenshotHeight);

		font = getLabelFont(project);

		physicsWorld = scene.resetPhysicsWorld();
		sprites = new ArrayList<>(scene.getSpriteList());

		resetConditionScriptTriggers();

		embroideryPatternManager = new DSTPatternManager();
		initActors(sprites);

		SoundCacheManager.getInstance().initialize();

		RenderManager.INSTANCE.initialize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		final List<Sprite> spritesToWarm = new ArrayList<>(sprites);
		Thread collisionWarmThread = new Thread(() -> {
			Gdx.app.log("CacheWarming", "Starting asset pre-loading...");
			for (Sprite sprite : spritesToWarm) {
				if (sprite.getLookList() != null) {
					for (LookData lookData : sprite.getLookList()) {
						if (lookData != null) {
							lookData.getCollisionInformation().loadCollisionPolygon();
						}
					}
				}
			}
			Gdx.app.log("CacheWarming", "Pre-loading finished.");
		}, "CollisionCacheWarming");
		collisionWarmThread.setPriority(Thread.MIN_PRIORITY);
		collisionWarmThread.setDaemon(true);
		collisionWarmThread.start();

		passepartout = new Passepartout(
				ScreenValues.currentScreenResolution.getWidth(),
				ScreenValues.currentScreenResolution.getHeight(),
				maxViewPort.getWidth(),
				maxViewPort.getHeight(),
				virtualWidth,
				virtualHeight);


		axes = new Texture(Gdx.files.internal("stage/red_pixel.bmp"));

		if (fullscreenQuad == null) {
			fullscreenQuad = new Mesh(true, 4, 6,
					new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

			float[] vertices = {
					-1.0f, -1.0f,
					0.0f,  0.0f,
					1.0f, -1.0f,
					1.0f,  0.0f,
					1.0f,  1.0f,
					1.0f,  1.0f,
					-1.0f,  1.0f,
					0.0f,  1.0f
			};

			short[] indices = { 0, 1, 2, 2, 3, 0 };

			fullscreenQuad.setVertices(vertices);
			fullscreenQuad.setIndices(indices);

			vmWidth = virtualWidth;
			vmHeight = virtualHeight;
			vmX = -virtualWidthHalf;
			vmY = -virtualHeightHalf;
		}

		if (vncSwizzleShader == null || !vncSwizzleShader.isCompiled()) try {


			String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
					+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
					+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
					+ "uniform mat4 u_projTrans;\n"
					+ "varying vec4 v_color;\n"
					+ "varying vec2 v_texCoords;\n"
					+ "\n"
					+ "void main()\n"
					+ "{\n"
					+ "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
					+ "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
					+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
					+ "}\n";


			String fragmentShader = "#ifdef GL_ES\n"
					+ "precision mediump float;\n"
					+ "#endif\n"
					+ "varying vec4 v_color;\n"
					+ "varying vec2 v_texCoords;\n"
					+ "uniform sampler2D u_texture;\n"
					+ "void main()\n"
					+ "{\n"
					+ "  vec4 tex = texture2D(u_texture, v_texCoords);\n"
					+ "  gl_FragColor = v_color * vec4(tex.b, tex.g, tex.r, tex.a);\n"
					+ "}";

			vncSwizzleShader = new ShaderProgram(vertexShader, fragmentShader);

			if (!vncSwizzleShader.isCompiled()) {
				Log.e("SHADER_ERROR", "Error compiling shader: " + vncSwizzleShader.getLog());
			}

			if (!vncSwizzleShader.isCompiled()) {
				Log.e("SHADER_ERROR", "VNC Swizzle Shader failed to compile: " + vncSwizzleShader.getLog());
			} else {
				Log.i("SHADER_INFO", "VNC Swizzle Shader compiled successfully.");
			}
		} catch (Exception e) {
			Log.e("SHADER_ERROR", "Could not load VNC Swizzle Shader files", e);
		}

        cacheBeforeUpdateScripts();
        cacheAfterUpdateScripts();

        if (StageActivity.pendingNotificationId != null) {
            final String idToTrigger = StageActivity.pendingNotificationId;
            StageActivity.pendingNotificationId = null;

            onNotificationActionTriggered(idToTrigger);
            onNotificationClicked(idToTrigger);
        }
	}

    public boolean isFinished() {
        return finished;
    }

    public boolean isBackgroundModeEnabled = false;

    public void backgroundTick(float delta) {
        processPendingNotificationActions();

        if (paused && isBackgroundModeEnabled && !finished) {
            try {
                if (physicsWorld != null) {
                    physicsWorld.step(delta);
                }
                if (stage != null) stage.act(delta);
                if (uiStage != null) uiStage.act(delta);

                if (threeDManager != null) threeDManager.update(delta);

                if (hasBeforeUpdateScripts) executeBeforeUpdateScripts(delta);
                if (hasAfterUpdateScripts) executeAfterUpdateScripts(delta);
            } catch (Exception e) {
                Log.e("BACKGROUND_TICK", "Error during background logic tick", e);
            }
        }
    }

    private void processPendingNotificationActions() {
        java.util.concurrent.ConcurrentLinkedQueue<String> pending = NewCatroidNotificationManager.INSTANCE.getPendingActions();

        while (!pending.isEmpty()) {
            String actionId = pending.poll();
            if (actionId != null) {
                onNotificationActionTriggered(actionId);
                onNotificationClicked(actionId);
            }
        }
    }

	public void setCameraPosition(float x, float y) {
		if (camera != null) {
			camera.position.set(x, y, 0);
		}
	}

	public void setCameraZoom(float zoom) {
		if (camera != null && zoom > 0) {
			camera.zoom = zoom;
		}
	}

	public void setCameraRotation(float degrees) {
		if (camera != null) {

			camera.rotate(cameraRotation - degrees);
			cameraRotation = degrees;
		}
	}

    public void pinSpriteToCamera(Sprite sprite) {
        if (sprite == null || camera == null) return;

        float worldX = sprite.look.getX();
        float worldY = sprite.look.getY();

        float dx = worldX - camera.position.x;
        float dy = worldY - camera.position.y;


        float cos = com.badlogic.gdx.math.MathUtils.cosDeg(-cameraRotation);
        float sin = com.badlogic.gdx.math.MathUtils.sinDeg(-cameraRotation);
        float unrotatedX = dx * cos - dy * sin;
        float unrotatedY = dx * sin + dy * cos;

        float baseRx = unrotatedX / camera.zoom;
        float baseRy = unrotatedY / camera.zoom;

        PinnedState state = new PinnedState();
        state.rx = baseRx;
        state.ry = baseRy;
        state.originalRotation = sprite.look.getRotation() - cameraRotation;
        state.originalScaleX = sprite.look.getScaleX() / camera.zoom;
        state.originalScaleY = sprite.look.getScaleY() / camera.zoom;

        pinnedSprites.put(sprite, state);
    }

    public void unpinSpriteFromCamera(Sprite sprite) {
        if (sprite == null) return;
        pinnedSprites.remove(sprite);
    }

    private void updatePinnedSprites() {
        if (pinnedSprites.isEmpty() || camera == null) return;

        float cos = com.badlogic.gdx.math.MathUtils.cosDeg(cameraRotation);
        float sin = com.badlogic.gdx.math.MathUtils.sinDeg(cameraRotation);

        for (java.util.Map.Entry<Sprite, PinnedState> entry : pinnedSprites.entrySet()) {
            Sprite sprite = entry.getKey();
            PinnedState state = entry.getValue();

            float sx = state.rx * camera.zoom;
            float sy = state.ry * camera.zoom;

            float worldOffsetX = sx * cos - sy * sin;
            float worldOffsetY = sx * sin + sy * cos;

            sprite.look.setPosition(
                    camera.position.x + worldOffsetX,
                    camera.position.y + worldOffsetY
            );

            sprite.look.setScale(
                    state.originalScaleX * camera.zoom,
                    state.originalScaleY * camera.zoom
            );

            sprite.look.setRotation(state.originalRotation + cameraRotation);
        }
    }

	private boolean vmGeometryDirty = false;

	public void setVmScreenGeometry(float x, float y, float width, float height) {

		this.vmX = x;
		this.vmY = y;
		this.vmWidth = width;
		this.vmHeight = height;


		this.vmGeometryDirty = true;
	}

	private boolean isVmDisplayVisible = false;


	public void setVmDisplayVisible(boolean visible) {
		this.isVmDisplayVisible = visible;
		Log.d("Display", "Display is: " + isVmDisplayVisible);
	}

	private void updateVmScreenMesh() {
		float x2 = vmX + vmWidth;
		float y2 = vmY + vmHeight;

		float[] vertices = {
				// X,  Y,  Z,  U, V
				vmX, vmY,  0,  0, 1,
				x2,  vmY,  0,  1, 1,
				x2,  y2,  0,  1, 0,
				vmX, y2,  0,  0, 0
		};
		short[] indices = { 0, 1, 2, 2, 3, 0 };


		if (vmScreenMesh == null) {
			vmScreenMesh = new Mesh(true, 4, 6,
					new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
		}

		vmScreenMesh.setVertices(vertices);
		vmScreenMesh.setIndices(indices);


		vmGeometryDirty = false;
	}

	private void resetConditionScriptTriggers() {
		for (Sprite sprite : sprites) {
			sprite.resetConditionScriptTriggers();
		}
	}

	public void setPaused(boolean paused) {
		this.paused = paused;

	}

	private BitmapFont getLabelFont(Project project) {
		BitmapFont font = new BitmapFont();
		font.setColor(AXIS_COLOR);
		font.getData().setScale(
				getFontScaleFactor(project, font, new GlyphLayout()));
		return font;
	}

	@VisibleForTesting
	public float getFontScaleFactor(Project project, BitmapFont font, GlyphLayout tempAxisLabelLayout) {
		tempAxisLabelLayout.setText(font, String.valueOf(project.getXmlHeader().virtualScreenWidth / 2));

		float shortDisplaySide;
		if (project.getXmlHeader().islandscapeMode()) {
			shortDisplaySide = project.getXmlHeader().getVirtualScreenHeight();
		} else {
			shortDisplaySide = project.getXmlHeader().getVirtualScreenWidth();
		}

		return AXIS_FONT_SIZE_SCALE_FACTOR * shortDisplaySide / tempAxisLabelLayout.height;
	}

	private void createNewStage() {
		time = 0f;
		GlobalShaderManager.INSTANCE.clear();

		if (!project.getXmlHeader().customResolution) {
			virtualWidth = project.getXmlHeader().getVirtualScreenWidth();
			virtualHeight = project.getXmlHeader().getVirtualScreenHeight();
		} else {
			virtualWidth = Gdx.graphics.getWidth();
			virtualHeight = Gdx.graphics.getHeight();
		}

		virtualWidthHalf = virtualWidth / 2;
		virtualHeightHalf = virtualHeight / 2;

		if (sceneFbo != null) {
			sceneFbo.dispose();
		}
		sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888,
				Math.round(virtualWidth),
				Math.round(virtualHeight),
				false);
		if (postProcessFbo != null) {
			postProcessFbo.dispose();
		}
		postProcessFbo = new FrameBuffer(Pixmap.Format.RGBA8888,
				Math.round(virtualWidth),
				Math.round(virtualHeight),
				false);
		fboRegion = new TextureRegion(sceneFbo.getColorBufferTexture());
		fboRegion.flip(false, true);

		camera = new OrthographicCamera();
		cameraPositioner = new CameraPositioner(camera, virtualHeightHalf, virtualWidthHalf);
		viewPort = new ExtendViewport(virtualWidth, virtualHeight, camera);
		if (batch == null) {
			batch = new SpriteBatch(8000);
		}
        if (fastTwoDManager == null) {
            fastTwoDManager = new FastTwoDManager();
        }
        fastTwoDManager.init((SpriteBatch) batch);
		if (postProcessBatch == null) {
			postProcessBatch = new SpriteBatch();
		}

		stage = new Stage(viewPort, batch);

		uiCamera = new OrthographicCamera();
		uiViewPort = new ScreenViewport(uiCamera);
		uiStage = new Stage(uiViewPort, batch);

        InputProcessor cameraInputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return threeDManager.handleTouchDown(screenX, screenY, pointer, uiStage, stage);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                threeDManager.handleTouchDragged(screenX, screenY, pointer);
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                threeDManager.handleTouchUp(pointer);
                return false;
            }
        };

		inputMultiplexer = new InputMultiplexer();

        inputMultiplexer.addProcessor(cameraInputProcessor);
		inputMultiplexer.addProcessor(uiStage);
		inputMultiplexer.addProcessor(stage);

		initMouseInputAdapter();


		SensorHandler.timerReferenceValue = SystemClock.uptimeMillis();
	}

	private final Vector3 tempVec3ForTouch = new Vector3();

	private void initStageInputListener() {
		if (stage != null) {
			stage.getRoot().clearListeners();
		}
		if (uiStage != null) {
			uiStage.getRoot().clearListeners();
		}

		InputListener gameListener = new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchDown(event.getStageX(), event.getStageY(), pointer);
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchUp(pointer);
			}

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                TouchUtil.updatePosition(event.getStageX(), event.getStageY(), pointer);

                if (sprites != null) {
                    EventWrapper e = new EventWrapper(new EventId(EventId.FINGER_MOVED_ON_SCREEN), false);
                    for (Sprite sprite : sprites) {
                        if (sprite != null && sprite.look != null) {
                            sprite.look.fire(e);
                        }
                    }
                }
            }
		};

		InputListener uiPassThroughListener = new InputListener() {
			private void updateTouchInWorldCoords(float screenX, float screenY, int pointer, boolean isDown) {
                tempVec3ForTouch.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer), 0);
                viewPort.unproject(tempVec3ForTouch);

                if (isDown) {
                    TouchUtil.touchDown(tempVec3ForTouch.x, tempVec3ForTouch.y, pointer);
                } else {
                    TouchUtil.updatePosition(tempVec3ForTouch.x, tempVec3ForTouch.y, pointer);
                }
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (event.getTarget() != uiStage.getRoot()) {
					updateTouchInWorldCoords(event.getStageX(), event.getStageY(), pointer, true);
					return true;
				}
				return false;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchUp(pointer);
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if (event.getTarget() != uiStage.getRoot()) {
					updateTouchInWorldCoords(event.getStageX(), event.getStageY(), pointer, false);
				}
			}
		};

		stage.addListener(gameListener);
		uiStage.addListener(uiPassThroughListener);
	}

	private VmMonitorActor vmMonitorActor;

    private void initActors(List<Sprite> sprites) {
        vmMonitorActor = new VmMonitorActor(vncSwizzleShader);

        vmMonitorActor.setSize(virtualWidth, virtualHeight);
        vmMonitorActor.setPosition(-virtualWidthHalf, -virtualHeightHalf);

        stage.addActor(vmMonitorActor);
        vmMonitorActor.setZIndex(0);

        if (sprites.isEmpty()) {
            penActor = new PenActor();
            stage.addActor(penActor);

            plotActor = new PlotActor();
            stage.addActor(plotActor);

            float screenRatio = calculateScreenRatio();
            EmbroideryActor embroideryActor = new EmbroideryActor(screenRatio, embroideryPatternManager, shapeRenderer);
            stage.addActor(embroideryActor);
            return;
        }

        Sprite backgroundSprite = sprites.get(0);
        backgroundSprite.resetSprite();
        backgroundSprite.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
        stage.addActor(backgroundSprite.look);

        penActor = new PenActor();
        stage.addActor(penActor);

        plotActor = new PlotActor();
        stage.addActor(plotActor);

        float screenRatio = calculateScreenRatio();
        EmbroideryActor embroideryActor = new EmbroideryActor(screenRatio, embroideryPatternManager, shapeRenderer);
        stage.addActor(embroideryActor);

        for (int i = 1; i < sprites.size(); i++) {
            Sprite sprite = sprites.get(i);
            sprite.resetSprite();
            sprite.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
            stage.addActor(sprite.look);
        }
    }

    public void onNotificationClicked(String clickedId) {
        if (sprites == null || clickedId == null) return;

        for (Sprite sprite : sprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof WhenNotificationClickedScript && !script.isCommentedOut()) {
                    WhenNotificationClickedBrick brick = (WhenNotificationClickedBrick) script.getScriptBrick();
                    try {
                        org.catrobat.catroid.content.Scope tempScope = new org.catrobat.catroid.content.Scope(project, sprite, null);
                        String expectedId = brick.getNotificationIdFormula().interpretString(tempScope);

                        if (clickedId.equals(expectedId)) {
                            ScriptSequenceAction sequence = sprite.createSequenceAction(script);
                            sequence.restart();
                            sprite.look.addAction(sequence);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

	public void cloneSpriteAndAddToStage(Sprite cloneMe) {
		Sprite copy = new SpriteController().copyForCloneBrick(cloneMe);
		if (cloneMe.isClone) {
			copy.myOriginal = cloneMe.myOriginal;
		} else {
			copy.myOriginal = cloneMe;
		}

		copy.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
		addCloneActorToStage(stage, stage.getRoot(), cloneMe.look, copy.look);
		sprites.add(copy);
		if (!copy.getLookList().isEmpty()) {
			int currentLookDataIndex = cloneMe.getLookList().indexOf(cloneMe.look.getLookData());
			copy.look.setLookData(copy.getLookList().get(currentLookDataIndex));
		}
		copy.initializeEventThreads(EventId.START_AS_CLONE);
		copy.initConditionScriptTriggers();
        cacheBeforeUpdateScripts();
        cacheAfterUpdateScripts();
	}

	public void cloneSpriteAndAddToStage(Sprite cloneMe, String newName) {
		Sprite copy = new SpriteController().copyForCloneBrick(cloneMe, newName);
		if (cloneMe.isClone) {
			copy.myOriginal = cloneMe.myOriginal;
		} else {
			copy.myOriginal = cloneMe;
		}

		copy.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
		addCloneActorToStage(stage, stage.getRoot(), cloneMe.look, copy.look);
		sprites.add(copy);
		if (!copy.getLookList().isEmpty()) {
			int currentLookDataIndex = cloneMe.getLookList().indexOf(cloneMe.look.getLookData());
			copy.look.setLookData(copy.getLookList().get(currentLookDataIndex));
		}
		copy.initializeEventThreads(EventId.START_AS_CLONE);
		copy.initConditionScriptTriggers();
        cacheBeforeUpdateScripts();
        cacheAfterUpdateScripts();
	}

	public void addCloneActorToStage(Stage stage, Group rootGroup, Look cloneMeLook, Look copyLook) {
		if (!stage.getActors().contains(cloneMeLook, true)) {
			rootGroup.addActor(cloneMeLook);
		}
		rootGroup.addActorBefore(cloneMeLook, copyLook);
	}

	public boolean removeClonedSpriteFromStage(Sprite sprite) {
		if (!sprite.isClone) {
			return false;
		}
		boolean removedSprite = sprites.remove(sprite);
		if (removedSprite) {
			sprite.look.destroy();
			sprite.invalidate();
            cacheBeforeUpdateScripts();
            cacheAfterUpdateScripts();
		}
		return removedSprite;
	}

	private void removeAllClonedSpritesFromStage() {
		List<Sprite> spritesCopy = new ArrayList<>(sprites);
		for (Sprite sprite : spritesCopy) {
			if (sprite.isClone) {
				removeClonedSpriteFromStage(sprite);
			}
		}
		StageActivity.resetNumberOfClonedSprites();
	}

	public List<Sprite> getAllClonesOfSprite(Sprite sprite) {
		List<Sprite> clonesOfSprite = new ArrayList<>();
		for (Sprite spriteOfStage : sprites) {
			if (spriteIsCloneOfSprite(sprite, spriteOfStage)) {
				clonesOfSprite.add(spriteOfStage);
			}
		}
		return clonesOfSprite;
	}

	private Boolean spriteIsCloneOfSprite(Sprite sprite, Sprite cloneSprite) {
		if (!cloneSprite.isClone) {
			return false;
		}
		String cloneNameExtensionRegexPattern = "\\-c\\d+$";
		String[] splitCloneNameStrings = cloneSprite.getName().split(cloneNameExtensionRegexPattern);
		return splitCloneNameStrings[0].contentEquals(sprite.getName());
	}

	private void disposeClonedSprites() {
		for (Scene scene : ProjectManager.getInstance().getCurrentProject().getSceneList()) {
			scene.removeClonedSprites();
		}
	}

	private InputProcessor mouseInputAdapter;
	private final Vector3 tempVec3 = new Vector3();

	private void initMouseInputAdapter() {
		if (mouseInputAdapter == null) {
			mouseInputAdapter = new InputAdapter() {
				@Override
				public boolean mouseMoved(int screenX, int screenY) {
                    tempVec3.set(screenX, screenY, 0);
                    viewPort.unproject(tempVec3);
                    SensorHandler.getInstance(null).updateMousePosition(tempVec3.x, tempVec3.y);
                    return false;
				}

				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					if (pointer == 0) {
						return mouseMoved(screenX, screenY);
					}
					return false;
				}

				@Override
				public boolean scrolled(float amountX, float amountY) {
					SensorHandler.getInstance(null).setLastScrollAmount(-amountY);

					EventWrapper e = new EventWrapper(new EventId(EventId.MOUSE_WHEEL_SCROLLED), false);
					if (project != null) project.fireToAllSprites(e);

					return true;
				}
			};
		}
	}

	void menuResume() {
		if (reloadProject) {
			return;
		}
		paused = false;
	}

	void menuPause() {
		if (finished || reloadProject) {
			return;
		}

		paused = true;
		webConnectionHolder.onPause();
	}

	public void transitionToScene(String sceneName) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		stageBackupMap.put(scene.getName(), saveToBackup());
		pause();

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		if (stageBackupMap.containsKey(scene.getName())) {
			restoreFromBackup(stageBackupMap.get(scene.getName()));
		}

		if (scene.firstStart) {
			create();
		} else {
			resume();
		}
		Gdx.input.setInputProcessor(stage);
	}

	public void transitionToScene(String sceneName, Boolean stopSounds) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		stageBackupMap.put(scene.getName(), saveToBackup());
		if(stopSounds) {
			pause();
		}

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		if (stageBackupMap.containsKey(scene.getName())) {
			restoreFromBackup(stageBackupMap.get(scene.getName()));
		}

		if (scene.firstStart) {
			create();
		} else {
			resume();
		}
		Gdx.input.setInputProcessor(stage);
	}

	public void transitionToScene(String sceneName, Boolean stopSounds, Boolean save) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		if(save) {
			stageBackupMap.put(scene.getName(), saveToBackup());
		}
		if(stopSounds) {
			pause();
		}

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		if (stageBackupMap.containsKey(scene.getName())) {
			restoreFromBackup(stageBackupMap.get(scene.getName()));
		}

		if (scene.firstStart) {
			create();
		} else {
			resume();
		}
		Gdx.input.setInputProcessor(stage);
	}

	public void clearScene(String name) {
		stageBackupMap.remove(name);
	}

	public void startScene(String sceneName, Boolean stopSound) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		stageBackupMap.put(scene.getName(), saveToBackup());
		if(stopSound) {
			pause();
		}

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}

		if(stopSound) {
			SoundManager.getInstance().clear();
		}
		get(SpeechRecognitionHolderFactory.class).getInstance().destroy();

		stageBackupMap.remove(sceneName);

		Gdx.input.setInputProcessor(stage);

		scene.firstStart = true;
		create();
	}

	public void startScene(String sceneName, Boolean stopSound, Boolean save) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		if(save) {
			stageBackupMap.put(scene.getName(), saveToBackup());
		}
		if(stopSound) {
			pause();
		}

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}

		if(stopSound) {
			SoundManager.getInstance().clear();
		}
		get(SpeechRecognitionHolderFactory.class).getInstance().destroy();

		stageBackupMap.remove(sceneName);

		Gdx.input.setInputProcessor(stage);

		scene.firstStart = true;
		create();
	}

	public void startScene(String sceneName) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		stageBackupMap.put(scene.getName(), saveToBackup());
		pause();

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}

		SoundManager.getInstance().clear();
		get(SpeechRecognitionHolderFactory.class).getInstance().destroy();

		stageBackupMap.remove(sceneName);

		Gdx.input.setInputProcessor(stage);

		scene.firstStart = true;
		create();
	}

	public void startSceneById(Integer sceneId) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneById(sceneId);
		String sceneName = ProjectManager.getInstance().getCurrentProject().getSceneNameById(sceneId);

		if (newScene == null) {
			return;
		}

		stageBackupMap.put(scene.getName(), saveToBackup());
		pause();

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}

		SoundManager.getInstance().clear();
		get(SpeechRecognitionHolderFactory.class).getInstance().destroy();

		stageBackupMap.remove(sceneName);

		Gdx.input.setInputProcessor(stage);

		scene.firstStart = true;
		create();
	}

	public void reloadProject(StageDialog stageDialog) {
		executeExitScriptsSynchronously();

		if (reloadProject) {
			return;
		}

		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null) {
			stageActivity.removeAllNativeViews();
		}

		if (threeDManager != null) {
			threeDManager.dispose();
		}
		threeDManager = null;
        SquareController.getInstance().clearSquares();
        SquareActor.disposeShared();

        if (sceneManager != null) {
            sceneManager.clearScene();
        }

        if (fastTwoDManager != null) {
            fastTwoDManager.clearScene();
        }

		isVmDisplayVisible = false;

		this.stageDialog = stageDialog;
		if (!ProjectManager.getInstance().getStartScene().getName().equals(scene.getName())) {
			transitionToScene(ProjectManager.getInstance().getStartScene().getName());
		}
		stageBackupMap.clear();
		embroideryPatternManager.clear();
        pinnedSprites.clear();

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			cameraManager.reset();
		}
		VibrationManager vibrationManager = StageActivity.getActiveVibrationManager();
		if (vibrationManager != null) {
			vibrationManager.reset();
		}
		TouchUtil.reset();
		MidiSoundManager.getInstance().reset();
		removeAllClonedSpritesFromStage();

		UserDataWrapper.resetAllUserData(ProjectManager.getInstance().getCurrentProject());
        org.catrobat.catroid.utils.NewCatroidMqttManager.INSTANCE.disconnectAll();

		for (Scene scene : ProjectManager.getInstance().getCurrentProject().getSceneList()) {
			scene.firstStart = true;
		}
		GlobalManager.Companion.setStopSounds(true);
		GlobalManager.Companion.setSaveScenes(true);

		try {
			RenderManager.INSTANCE.initialize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			threeDManager = new ThreeDManager();
			sceneManager = new SceneManager(threeDManager);
		} catch (Exception e) {
			Log.e("StageListener", "INITIALIZE ERROR: " + e);
		}

        org.catrobat.catroid.utils.ActionThreadRegistry.clear();

		reloadProject = true;
	}

    private void cacheBeforeUpdateScripts() {
        beforeUpdateActions.clear();
        hasBeforeUpdateScripts = false;
        if (sprites == null) return;

        for (Sprite sprite : sprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof BeforeUpdateScript && !script.isCommentedOut()) {
                    ScriptSequenceAction action = sprite.createSequenceAction(script);
                    action.setActor(sprite.look);

                    beforeUpdateActions.add(action);
                }
            }
        }
        hasBeforeUpdateScripts = !beforeUpdateActions.isEmpty();
    }

    private void cacheAfterUpdateScripts() {
        afterUpdateActions.clear();
        hasAfterUpdateScripts = false;
        if (sprites == null) return;

        for (Sprite sprite : sprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof AfterUpdateScript && !script.isCommentedOut()) {
                    ScriptSequenceAction action = sprite.createSequenceAction(script);
                    action.setActor(sprite.look);
                    afterUpdateActions.add(action);
                }
            }
        }
        hasAfterUpdateScripts = !afterUpdateActions.isEmpty();
    }

    private void executeAfterUpdateScripts(float delta) {
        for (int i = 0; i < afterUpdateActions.size(); i++) {
            ScriptSequenceAction action = afterUpdateActions.get(i);
            action.restart();

            int iterations = 0;
            while (iterations < 10000) {
                if (action.act(delta)) {
                    break;
                }
                iterations++;
            }
        }
    }

    private void executeBeforeUpdateScripts(float delta) {
        for (int i = 0; i < beforeUpdateActions.size(); i++) {
            ScriptSequenceAction action = beforeUpdateActions.get(i);
            action.restart();

            int iterations = 0;
            while (iterations < 10000) {
                if (action.act(delta)) {
                    break;
                }
                iterations++;
            }
        }
    }

	@Override
	public void resume() {
        if (firstFrameDrawn) {
            broadcastEventToAllSprites(new EventId(EventId.APP_RESTORED));
        }

		if (!paused) {
			setSchedulerStateForAllLooks(ThreadScheduler.RUNNING);
			SoundManager.getInstance().resume();
		}

		for (Sprite sprite : sprites) {
			sprite.look.refreshTextures(true);
		}
	}

	@Override
	public void pause() {
        if (finished) {
            return;
        }

        StageActivity activity = StageActivity.activeStageActivity.get();
        boolean isFinishing = (activity != null && activity.isFinishing());

        if (!finished && !isFinishing) {
            broadcastEventToAllSprites(new EventId(EventId.APP_MINIMIZED));
        }

        if (!paused) {
            if (!isBackgroundModeEnabled) {
                setSchedulerStateForAllLooks(ThreadScheduler.SUSPENDED);
            }
            SoundManager.getInstance().pause();
        }
	}

	private Texture vmTexture;

	public void setVmScreenSize(int width, int height) {
		Gdx.app.postRunnable(() -> {
			if (vmTexture != null) {
				vmTexture.dispose();
			}
			vmTexture = new Texture(width, height, Pixmap.Format.RGBA8888);
			vmTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

			if (vmMonitorActor != null) {
				vmMonitorActor.setTexture(vmTexture);
			}
		});
	}

	public void setVmMonitorBounds(float x, float y, float width, float height) {
		if (vmMonitorActor != null) {
			vmMonitorActor.setPosition(x, y);
			vmMonitorActor.setSize(width, height);
		}
	}

	public void resizeVmMonitor(float width, float height) {
		if (vmMonitorActor != null) {
			float oldCenterX = vmMonitorActor.getX() + vmMonitorActor.getWidth() / 2;
			float oldCenterY = vmMonitorActor.getY() + vmMonitorActor.getHeight() / 2;

			vmMonitorActor.setSize(width, height);

			vmMonitorActor.setPosition(oldCenterX - width / 2, oldCenterY - height / 2);
		}
	}

	private volatile boolean captureNextFrame = false;


	public void captureAndSaveVmTexture() {
		this.captureNextFrame = true;
	}

	public float getVirtualWidth() { return virtualWidth; }
	public float getVirtualHeight() { return virtualHeight; }
	public int getVmWidth() { return (int) vmWidth; }
	public int getVmHeight() { return (int) vmHeight; }

	@Override
	public void render() {
        processPendingNotificationActions();

        long framePhysicsTime = 0;
        long frameLogicTime = 0;
        long endLogic = 0;
		try {
			Look.tickGlobalFrame();

            org.catrobat.catroid.common.NativeViewBindingManager.updateBindings(camera, viewPort);

			float color = 0f;

            Gdx.gl20.glClearColor(color, color, color, 0f);
			Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			StageActivity stageActivity = StageActivity.activeStageActivity.get();

            if (isVmDisplayVisible && vmTexture != null) {
                if (stageActivity != null) {
                    VncClient client = stageActivity.vncClients.get(StageActivity.DEFAULT_VM_NAME);
                    if (client != null && stageActivity.frameReadyToRender) {
                        try {
                            vmTexture.bind();
                            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
                            client.uploadFrameTexture();
                            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4);
                        } catch (Exception e) {
                            Log.e("VNC_RENDER", "Error uploading VM frame", e);
                        } finally {
                            stageActivity.frameReadyToRender = false;
                        }
                    }
                }
            }

			if (reloadProject) {
				if (threeDManager != null) {
					threeDManager.dispose();
				}
                if (sceneManager != null) sceneManager.clearScene();

				threeDManager = new ThreeDManager();
				sceneManager = new SceneManager(threeDManager);

				stage.clear();
				if (penActor != null) {
					penActor.dispose();
				}

				if (plotActor != null) {
					plotActor.dispose();
				}

				embroideryPatternManager.clear();

				SoundManager.getInstance().clear();

				physicsWorld = scene.resetPhysicsWorld();

				initActors(sprites);
				//stage.addActor(passepartout);

				initStageInputListener();

				paused = true;
				scene.firstStart = true;
				reloadProject = false;

				cameraPositioner.reset();
				resetCamera();

				if (stageDialog != null) {
					synchronized (stageDialog) {
						stageDialog.notify();
					}
				}
			}

            org.catrobat.catroid.content.RenderTextureManager.INSTANCE.renderAllTargets(batch);
			batch.setProjectionMatrix(camera.combined);
			shapeRenderer.setProjectionMatrix(camera.combined);

			if (scene.firstStart) {
				for (Sprite sprite : sprites) {
					sprite.initializeEventThreads(EventId.START);
					sprite.initConditionScriptTriggers();
					sprite.initIfConditionBrickTriggers();
					if (!sprite.getLookList().isEmpty()) {
						sprite.look.setLookData(sprite.getLookList().get(0));
					}
				}
				scene.firstStart = false;
			}

            if (!paused) {
                long logicStartTime = System.nanoTime();

                float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

                if (sceneManager != null) {
                    sceneManager.update(deltaTime);
                }

                int steps = (int) Math.max(1f, deltaActionTimeDivisor);
                float optimizedDeltaTime = deltaTime / steps;

                for (int i = 0; i < steps; i++) {
                    long pStart = System.nanoTime();
                    physicsWorld.step(optimizedDeltaTime);
                    framePhysicsTime += (System.nanoTime() - pStart);

                    stage.act(optimizedDeltaTime);
                    uiStage.act(optimizedDeltaTime);
                }

                endLogic = System.nanoTime();
                frameLogicTime = (endLogic - logicStartTime) - framePhysicsTime;

                long executionTimeOfActionsUpdate = (System.nanoTime() - logicStartTime) / 1_000_000;
                if (executionTimeOfActionsUpdate <= ACTIONS_COMPUTATION_TIME_MAXIMUM) {
                    deltaActionTimeDivisor += 1f;
                    deltaActionTimeDivisor = Math.min(DELTA_ACTIONS_DIVIDER_MAXIMUM, deltaActionTimeDivisor);
                } else {
                    deltaActionTimeDivisor -= 1f;
                    deltaActionTimeDivisor = Math.max(1f, deltaActionTimeDivisor);
                }
                DebugMenuManager.getInstance().updateIfVisible();
            } else {
                endLogic = System.nanoTime();
            }

            if (isVmDisplayVisible && vmTexture != null && vncSwizzleShader != null && vncSwizzleShader.isCompiled()) {
				batch.setProjectionMatrix(camera.combined);
				batch.setShader(vncSwizzleShader);
				batch.begin();

				batch.draw(vmTexture, vmX, vmY, vmWidth, vmHeight);

				batch.end();
				batch.setShader(null);
            }
            if (!finished) {
                try {
                    if (!paused) {
                        if (threeDManager != null) {
                            threeDManager.update(Gdx.graphics.getDeltaTime());
                        }
                    }
                    updatePinnedSprites();

                    if (hasBeforeUpdateScripts && !paused) {
                        executeBeforeUpdateScripts(Gdx.graphics.getDeltaTime());
                    }
                    try {
                        if (threeDManager != null && org.catrobat.catroid.content.RenderTextureManager.isMain3DRenderEnabled()) {
                            threeDManager.render();
                        }
                    } catch (Exception e) {
                        Log.e("3DRENDER", "ERROR: " + e);
                    }

                    if (fastTwoDManager != null && !paused && org.catrobat.catroid.content.RenderTextureManager.isMainFast2DRenderEnabled()) {
                        fastTwoDManager.updateAndRender(Gdx.graphics.getDeltaTime());
                    }

                    if (org.catrobat.catroid.content.RenderTextureManager.isMain2DRenderEnabled()) {
                        stage.draw();
                        uiStage.draw();
                    }
                } catch (Exception e) {
                    Log.e("RENDER", "FATAL ERROR: " + e);
                }
                firstFrameDrawn = true;
            }

            if (makeScreenshot) {
                Scene scene = ProjectManager.getInstance().getCurrentlyEditedScene();
                String manualScreenshotPath = scene.getDirectory()
                        + "/" + SCREENSHOT_MANUAL_FILE_NAME;
                File manualScreenshot = new File(manualScreenshotPath);
                if (!manualScreenshot.exists() || Objects.equals(screenshotName,
                        SCREENSHOT_MANUAL_FILE_NAME)) {

                    int actualX = screenshotX;
                    int actualY = screenshotY;
                    int actualW = screenshotWidth;
                    int actualH = screenshotHeight;

                    StageActivity activity = StageActivity.activeStageActivity.get();
                    boolean isFreeStageEnabled = (activity != null && "StageWorkspaceActivity".equals(activity.getClass().getSimpleName()));

                    if (isFreeStageEnabled) {
                        actualX = 0;
                        actualY = 0;
                        actualW = Gdx.graphics.getWidth();
                        actualH = Gdx.graphics.getHeight();
                    } else {
                        actualX = Math.max(0, actualX);
                        actualY = Math.max(0, actualY);
                        actualW = Math.min(Gdx.graphics.getWidth() - actualX, actualW);
                        actualH = Math.min(Gdx.graphics.getHeight() - actualY, actualH);
                    }

                    if (actualW <= 0) actualW = 1;
                    if (actualH <= 0) actualH = 1;

                    byte[] screenshot = ScreenUtils
                            .getFrameBufferPixels(actualX, actualY, actualW, actualH, true);

                    ScreenshotSaver tempSaver = new ScreenshotSaver(Gdx.files, getScreenshotPath(), actualW, actualH);
                    tempSaver.saveScreenshotAndNotify(
                            screenshot,
                            screenshotName,
                            this::notifyScreenshotCallbackAndCleanup,
                            GlobalScope.INSTANCE
                    );
                }
                String automaticScreenShotPath = scene.getDirectory()
                        + "/" + SCREENSHOT_AUTOMATIC_FILE_NAME;
                File automaticScreenShot = new File(automaticScreenShotPath);
                if (manualScreenshot.exists() && automaticScreenShot.exists()) {
                    automaticScreenShot.delete();
                }
                makeScreenshot = false;
            }

			if (axesOn && !finished) {
				drawAxes();
			}

			if (PhysicsDebugSettings.Render.RENDER_PHYSIC_OBJECT_LABELING) {
				printPhysicsLabelOnScreen();
			}

			if (PhysicsDebugSettings.Render.RENDER_COLLISION_FRAMES && !finished) {
				physicsWorld.render(camera.combined);
			}

			if (makeTestPixels) {
				testPixels = ScreenUtils.getFrameBufferPixels(testX, testY, testWidth, testHeight, false);
				makeTestPixels = false;
			}

			cameraPositioner.updateCameraPositionForFocusedSprite();

            if (hasAfterUpdateScripts && !paused) {
                executeAfterUpdateScripts(Gdx.graphics.getDeltaTime());
            }
		} catch (Exception e) {
			Log.e("RENDER_CRASH", "Fatal error during render loop", e);
		}

        long endRender = System.nanoTime();

        PerformanceTracker.recordFrame(
                framePhysicsTime,
                frameLogicTime,
                endRender - endLogic
        );
	}

    private void printPhysicsLabelOnScreen() {
        PhysicsObject tempPhysicsObject;
        final int fontOffset = 5;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Sprite sprite : sprites) {
            if (sprite.look instanceof PhysicsLook) {
                tempPhysicsObject = physicsWorld.getPhysicsObject(sprite);
                float x = tempPhysicsObject.getX();
                float y = tempPhysicsObject.getY();
                float h = font.getXHeight();

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("velocity_x: ").append(tempPhysicsObject.getVelocity().x);
                font.draw(batch, debugTextBuilder, x, y);

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("velocity_y: ").append(tempPhysicsObject.getVelocity().y);
                font.draw(batch, debugTextBuilder, x, y + h + fontOffset);

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("angular velocity: ").append(tempPhysicsObject.getRotationSpeed());
                font.draw(batch, debugTextBuilder, x, y + h * 2 + fontOffset * 2);

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("direction: ").append(tempPhysicsObject.getDirection());
                font.draw(batch, debugTextBuilder, x, y + h * 3 + fontOffset * 3);
            }
        }
        batch.end();
    }

	private void drawAxes() {
		GlyphLayout layout = axesLabelLayout;
		layout.setText(font, String.valueOf((int) virtualWidthHalf));

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(axes, -virtualWidthHalf, -AXIS_WIDTH / 2, virtualWidth, AXIS_WIDTH);
		batch.draw(axes, -AXIS_WIDTH / 2, -virtualHeightHalf, AXIS_WIDTH, virtualHeight);

		final float fontOffset = layout.height / 2;

		font.draw(batch, "-" + (int) virtualWidthHalf, -virtualWidthHalf + fontOffset, -fontOffset);
		font.draw(batch, String.valueOf((int) virtualWidthHalf), virtualWidthHalf - layout.width - fontOffset,
				-fontOffset);

		font.draw(batch, "-" + (int) virtualHeightHalf, fontOffset, -virtualHeightHalf + layout.height + fontOffset);
		font.draw(batch, String.valueOf((int) virtualHeightHalf), fontOffset, virtualHeightHalf - fontOffset);

		font.draw(batch, "0", fontOffset, -fontOffset);
		batch.end();
	}

    public void setActorZIndexSafely(com.badlogic.gdx.scenes.scene2d.Actor actor, int desiredZIndex) {
        if (actor == null || stage == null) return;
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.scenes.scene2d.Actor> actors = stage.getActors();
        if (actors == null || actors.size == 0) return;

        int safeZIndex = Math.max(0, Math.min(desiredZIndex, actors.size - 1));
        actor.setZIndex(safeZIndex);
    }

	public PenActor getPenActor() {
		return penActor;
	}

	public PlotActor getPlotActor() {
		return plotActor;
	}

    @Override
    public void resize(int width, int height) {
        StageActivity activity = StageActivity.activeStageActivity.get();
        boolean isFreeStageEnabled = (activity instanceof StageWorkspaceActivity);

        boolean isScreenLandscape = width > height;
        boolean isVirtualLandscape = virtualWidth > virtualHeight;

        if (!isFreeStageEnabled && isScreenLandscape != isVirtualLandscape) {
            float temp = virtualWidth;
            virtualWidth = virtualHeight;
            virtualHeight = temp;

            virtualWidthHalf = virtualWidth / 2f;
            virtualHeightHalf = virtualHeight / 2f;

            if (sceneFbo != null) sceneFbo.dispose();
            sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888, Math.round(virtualWidth), Math.round(virtualHeight), false);

            if (postProcessFbo != null) postProcessFbo.dispose();
            postProcessFbo = new FrameBuffer(Pixmap.Format.RGBA8888, Math.round(virtualWidth), Math.round(virtualHeight), false);

            if (fboRegion != null) {
                fboRegion.setTexture(sceneFbo.getColorBufferTexture());
            } else {
                fboRegion = new TextureRegion(sceneFbo.getColorBufferTexture());
            }
            fboRegion.flip(false, true);
            initScreenMode();

            if (stage != null) {
                stage.setViewport(viewPort);
            }

            if (cameraPositioner != null) {
                cameraPositioner = new CameraPositioner(camera, virtualHeightHalf, virtualWidthHalf);
            }

            if (maxViewPort != null) {
                passepartout = new Passepartout(
                        width, height,
                        maxViewPort.getWidth(), maxViewPort.getHeight(),
                        virtualWidth, virtualHeight);
            }
        }
        if (stage != null && viewPort != null) {
            stage.setViewport(viewPort);
        }
        if (uiStage != null && uiViewPort != null) {
            uiStage.setViewport(uiViewPort);
        }
        camera.viewportWidth = width;
        camera.viewportHeight = height;

        if (viewPort != null) {
            viewPort.update(width, height, false);
        }
        if (uiViewPort != null) {
            uiViewPort.update(width, height, true);
        }

        if (camera != null) {
            camera.update();
        }

        if (threeDManager != null) threeDManager.resize(width, height);
        if (fastTwoDManager != null) fastTwoDManager.resize(width, height);

        if (isFirstResize) {
            isFirstResize = false;
        } else {
            broadcastEventToAllSprites(new EventId(EventId.WINDOW_RESIZED));
        }
    }

    public float getCameraPositionX() {
        return camera != null ? camera.position.x : 0f;
    }

    public float getCameraPositionY() {
        return camera != null ? camera.position.y : 0f;
    }

    public float getCameraZoom() {
        return camera != null ? camera.zoom : 1.0f;
    }

    public float getCameraRotation() {
        return cameraRotation;
    }


	public void executeExitScriptsSynchronously() {
		Log.d("StageListener", "Force-executing exit scripts...");
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project == null || sprites == null) {
			Log.e("StageListener", "Cannot execute exit scripts, project or sprites are null.");
			return;
		}

		for (Sprite sprite : sprites) {
			for (Script script : sprite.getScriptList()) {

				if (script instanceof ExitProjectScript && !script.isCommentedOut()) {
					Log.d("StageListener", "Found exit script in sprite: " + sprite.getName());
					ScriptSequenceAction sequence = sprite.createSequenceAction(script);
					sequence.act(Float.MAX_VALUE);
				}
			}
		}
		Log.d("StageListener", "Finished executing exit scripts.");
	}



	private void broadcastEventToAllSprites(EventId eventId) {

		if (sprites == null) {
			return;
		}

		for (Sprite sprite : sprites) {
			Multimap<EventId, ScriptSequenceAction> eventMap = sprite.getIdToEventThreadMap();
			if (eventMap != null && eventMap.containsKey(eventId)) {
				for (ScriptSequenceAction sequence : eventMap.get(eventId)) {
					sequence.restart();
					sprite.look.addAction(sequence);
				}
			}
		}
	}

    public void onNotificationActionTriggered(String clickedActionId) {
        if (sprites == null || clickedActionId == null) return;

        for (Sprite sprite : sprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof WhenNotificationActionTriggeredScript && !script.isCommentedOut()) {
                    WhenNotificationActionTriggeredBrick brick = (WhenNotificationActionTriggeredBrick) script.getScriptBrick();
                    try {
                        org.catrobat.catroid.content.Scope tempScope = new org.catrobat.catroid.content.Scope(project, sprite, null);
                        String expectedActionId = brick.getActionIdFormula().interpretString(tempScope);

                        if (clickedActionId.equals(expectedActionId)) {
                            ScriptSequenceAction sequence = sprite.createSequenceAction(script);
                            sequence.restart();
                            sprite.look.addAction(sequence);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

	public void resetCamera() {
		if (camera != null) {
			camera.position.set(0, 0, 0);

			camera.zoom = 1.0f;

			camera.rotate(-cameraRotation);
			cameraRotation = 0f;

			camera.update();
		}
	}

	@Override
	public void dispose() {
        if (isBackgroundModeEnabled) {
            isBackgroundModeEnabled = false;
            Context context = CatroidApplication.getAppContext();
            if (context != null) {
                Intent intent = new Intent(context, org.catrobat.catroid.common.NewCatroidBackgroundService.class);
                context.stopService(intent);
            }
        }

		executeExitScriptsSynchronously();

		if (stage != null) {
			for (Actor actor : stage.getActors()) {
				if (actor instanceof Look) {
					((Look) actor).destroy();
				}
			}
		}
		if (uiStage != null) {
			for (Actor actor : uiStage.getActors()) {
				if (actor instanceof Look) {
					((Look) actor).destroy();
				}
			}
		}
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
		if (uiStage != null) {
			uiStage.dispose();
			uiStage = null;
		}
		if (vmScreenMesh != null) {
			vmScreenMesh.dispose();
		}
		if (brightnessContrastHueShader != null) {
			brightnessContrastHueShader.dispose();
		}
		if (vncSwizzleShader != null) {
			vncSwizzleShader.dispose();
		}
		if (!finished) {
			this.finish();
		}
		if (fullscreenQuad != null) {
			fullscreenQuad.dispose();
		}
		if (postProcessFbo != null) {
			postProcessFbo.dispose();
		}

		if (threeDManager != null) {
			threeDManager.dispose();
		}
        if (sceneManager != null) sceneManager.clearScene();

		if (postProcessShader != null) {
			postProcessShader.dispose();
		}

        SquareController.getInstance().clearSquares();
        SquareActor.disposeShared();

		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null) {
			stageActivity.removeAllNativeViews();
		}

        if (fastTwoDManager != null) {
            fastTwoDManager.dispose();
            fastTwoDManager = null;
        }

		RenderManager.INSTANCE.dispose();
        org.catrobat.catroid.utils.NewCatroidMqttManager.INSTANCE.disconnectAll();
        MLBridge.nativeResetEngine();

        try {
            if (MainMenuActivity.pythonEngine != null) {
                MainMenuActivity.pythonEngine.clearEnvironment();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        org.catrobat.catroid.content.Look.disposeMaskShader();
        org.catrobat.catroid.common.NativeViewBindingManager.clearAll();
		disposeStageButKeepActors();
		font.dispose();
		axes.dispose();
		ColorAtXYDetection.Companion.disposeShared();

		sceneFbo.dispose();
		GlobalShaderManager.INSTANCE.dispose();
		GlobalShaderManager.INSTANCE.clear();
        org.catrobat.catroid.content.RenderTextureManager.INSTANCE.clearAll();

		SoundCacheManager.getInstance().release();

		disposeTextures();
		disposeClonedSprites();

		SoundManager.getInstance().clear();
		PhysicsShapeBuilder.getInstance().reset();
		embroideryPatternManager = null;
		if (penActor != null) {
			penActor.dispose();
		}

		if(plotActor != null) {
			plotActor.dispose();
		}

		if (postProcessBatch != null) {
			postProcessBatch.dispose();
		}
		if (vmTexture != null) {
			vmTexture.dispose();
			vmTexture = null;
		}
	}

	public void finish() {
		finished = true;
	}

    public void requestTakingScreenshot(@NonNull String screenshotName,
                                        @NonNull ScreenshotSaverCallback screenshotCallback) {
        this.screenshotName = screenshotName;
        this.screenshotSaverCallback = screenshotCallback;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            StageActivity activity = StageActivity.activeStageActivity.get();
            if (activity != null) {
                activity.captureScreenWithNativeViews(getScreenshotPath(), screenshotName, screenshotCallback);
                return;
            }
        }

        makeScreenshot = true;
    }

	private void notifyScreenshotCallbackAndCleanup(Boolean success) {
		if (screenshotSaverCallback != null) {
			screenshotSaverCallback.screenshotSaved(success);
			this.screenshotSaverCallback = null;
		} else {
			Log.e("StageListener", "Lost reference to screenshot callback");
		}
	}

	public byte[] getPixels(int x, int y, int width, int height) {
		testX = x;
		testY = y;
		testWidth = width;
		testHeight = height;
		makeTestPixels = true;
		while (makeTestPixels) {
			Thread.yield();
		}
		byte[] copyOfTestPixels = new byte[testPixels.length];
		System.arraycopy(testPixels, 0, copyOfTestPixels, 0, testPixels.length);
		return copyOfTestPixels;
	}

	public void toggleScreenMode() {
		switch (project.getScreenMode()) {
			case MAXIMIZE:
				project.setScreenMode(ScreenModes.STRETCH);
				break;
			case STRETCH:
				project.setScreenMode(ScreenModes.MAXIMIZE);
				break;
		}

		initScreenMode();
	}

	public void clearBackground() {
		penActor.reset();
		plotActor.reset();
	}

	private void initScreenMode() {
        int currentWidth = Gdx.graphics.getWidth();
        int currentHeight = Gdx.graphics.getHeight();

        if (currentWidth <= 0) currentWidth = ScreenValues.currentScreenResolution.getWidth();
        if (currentHeight <= 0) currentHeight = ScreenValues.currentScreenResolution.getHeight();

        screenshotWidth = ScreenValues.getResolutionForProject(project).getWidth();
        screenshotHeight = ScreenValues.getResolutionForProject(project).getHeight();

		switch (project.getScreenMode()) {
			case STRETCH:
				screenshotX = 0;
				screenshotY = 0;
				viewPort = new ScalingViewport(Scaling.stretch, virtualWidth, virtualHeight, camera);
				shapeRenderer.identity();
				break;
			case MAXIMIZE:
				float yScale = 1.0f;
				float xScale = 1.0f;
				if (screenshotWidth != maxViewPort.getWidth() && maxViewPort.getWidth() > 0) {
					xScale = screenshotWidth / (float) maxViewPort.getWidth();
				}
				if (screenshotHeight != maxViewPort.getHeight() && maxViewPort.getHeight() > 0) {
					yScale = screenshotHeight / (float) maxViewPort.getHeight();
				}

				screenshotWidth = maxViewPort.getWidth();
				screenshotHeight = maxViewPort.getHeight();
				screenshotX = maxViewPort.getOffsetX();
				screenshotY = maxViewPort.getOffsetY();

				viewPort = new ExtendViewport(virtualWidth, virtualHeight, camera);
				shapeRenderer.scale(xScale, yScale, 1.0f);
				break;
			default:
				break;
		}

        if (stage != null) {
            stage.setViewport(viewPort);
        }

        viewPort.update(currentWidth, currentHeight, false);
        camera.position.set(0, 0, 0);
        camera.update();
        shapeRenderer.updateMatrices();
	}

	private void disposeTextures() {
		for (Scene scene : project.getSceneList()) {
			for (Sprite sprite : scene.getSpriteList()) {
				for (LookData lookData : sprite.getLookList()) {
					lookData.dispose();
				}
			}
		}
	}

	private void disposeStageButKeepActors() {
		if (stage != null) {
			stage.unfocusAll();
		}
		if (batch != null) {
			batch.dispose();
		}
	}

	public void gamepadPressed(String buttonType) {

		if (project == null) {
			Log.e("StageListener", "Gamepad event received, but project is null. Ignoring.");
			return;
		}
		EventId eventId = new GamepadEventId(buttonType);
		EventWrapper gamepadEvent = new EventWrapper(eventId, false);
		project.fireToAllSprites(gamepadEvent);
	}

	public void addActor(Actor actor) {
		stage.addActor(actor);
	}

	public Stage getStage() {
		return stage;
	}

	private void setSchedulerStateForAllLooks(@ThreadScheduler.SchedulerState int state) {
		for (Actor actor : stage.getActors()) {
			if (actor instanceof Look) {
				Look look = (Look) actor;
				look.setSchedulerState(state);
			}
		}
	}

	public void setBubbleActorForSprite(Sprite sprite, ShowBubbleActor showBubbleActor) {
		addActor(showBubbleActor);
		bubbleActorMap.put(sprite, showBubbleActor);
	}

	public void removeBubbleActorForSprite(Sprite sprite) {
		getBubbleActorForSprite(sprite).close();
		getStage().getActors().removeValue(getBubbleActorForSprite(sprite), true);
		bubbleActorMap.remove(sprite);
	}

	public ShowBubbleActor getBubbleActorForSprite(Sprite sprite) {
		return bubbleActorMap.get(sprite);
	}

	public List<Sprite> getSpritesFromStage() {
		return sprites;
	}

	@VisibleForTesting
	public static class StageBackup {

		List<Sprite> sprites;
		Array<Actor> actors;
		PenActor penActor;
		PlotActor plotActor;
		EmbroideryPatternManager embroideryPatternManager;
		Map<Sprite, ShowBubbleActor> bubbleActorMap;
		List<SoundBackup> soundBackupList;

		boolean paused;
		boolean finished;
		boolean reloadProject;
		boolean flashState;
		long timeToVibrate;

		PhysicsWorld physicsWorld;
		OrthographicCamera camera;
		Sprite spriteToFocusOn;
		Batch batch;
		BitmapFont font;
		Passepartout passepartout;
		Viewport viewPort;

		boolean axesOn;
		float deltaActionTimeDivisor;
		boolean cameraRunning;
	}

	private StageBackup saveToBackup() {
		StageBackup backup = new StageBackup();
		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		VibrationManager vibrationManager = StageActivity.getActiveVibrationManager();

		backup.sprites = new ArrayList<>(sprites);
		backup.actors = new Array<>(stage.getActors());
		backup.penActor = penActor;
		backup.plotActor = plotActor;
		backup.bubbleActorMap = new HashMap<>(bubbleActorMap);
		backup.embroideryPatternManager = embroideryPatternManager;

		backup.paused = paused;
		backup.finished = finished;
		backup.reloadProject = reloadProject;
		backup.flashState = cameraManager != null && cameraManager.getFlashOn();
		if (backup.flashState) {
			cameraManager.disableFlash();
		}
		if (vibrationManager != null && vibrationManager.hasActiveVibration()) {
			vibrationManager.pause();
			backup.timeToVibrate = vibrationManager.getTimeToVibrate();
			vibrationManager.reset();
		}
		backup.physicsWorld = physicsWorld;
		backup.camera = camera;
		backup.spriteToFocusOn = cameraPositioner.getSpriteToFocusOn();
		cameraPositioner.reset();
		backup.batch = batch;
		backup.font = font;
		backup.passepartout = passepartout;
		backup.viewPort = viewPort;

		backup.axesOn = axesOn;
		backup.deltaActionTimeDivisor = deltaActionTimeDivisor;
		backup.cameraRunning = cameraManager != null && cameraManager.isCameraActive();
		if (backup.cameraRunning) {
			cameraManager.pause();
		}
		backup.soundBackupList = new ArrayList<>();
		backup.soundBackupList.addAll(SoundManager.getInstance().getPlayingSoundBackups());
		return backup;
	}

	private void restoreFromBackup(StageBackup backup) {
		sprites.clear();
		sprites.addAll(backup.sprites);
		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		VibrationManager vibrationManager = StageActivity.getActiveVibrationManager();

		stage.clear();
		for (Actor actor : backup.actors) {
			stage.addActor(actor);
		}

		penActor = backup.penActor;
		plotActor = backup.plotActor;

		bubbleActorMap.clear();
		bubbleActorMap.putAll(backup.bubbleActorMap);

		embroideryPatternManager = backup.embroideryPatternManager;

		paused = backup.paused;
		finished = backup.finished;
		reloadProject = backup.reloadProject;
		if (backup.flashState && cameraManager != null) {
			cameraManager.enableFlash();
		}
		if (backup.timeToVibrate > 0 && vibrationManager != null) {
			vibrationManager.setTimeToVibrate(backup.timeToVibrate);
			vibrationManager.resume();
		} else if (vibrationManager != null) {
			vibrationManager.pause();
		}
		physicsWorld = backup.physicsWorld;
		camera = backup.camera;
		cameraPositioner.setSpriteToFocusOn(backup.spriteToFocusOn);
		cameraPositioner.updateCameraPositionForFocusedSprite();
		batch = backup.batch;
		font = backup.font;
		passepartout = backup.passepartout;
		viewPort = backup.viewPort;
		axesOn = backup.axesOn;
		deltaActionTimeDivisor = backup.deltaActionTimeDivisor;
		if (backup.cameraRunning && cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}
		for (SoundBackup soundBackup : backup.soundBackupList) {
			SoundManager.getInstance().playSoundFileWithStartTime(soundBackup.getPathToSoundFile(),
					soundBackup.getStartedBySprite(), soundBackup.getCurrentPosition());
		}
        initStageInputListener();
        cacheBeforeUpdateScripts();
        cacheAfterUpdateScripts();
	}

	private float calculateScreenRatio() {
		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		XmlHeader header = ProjectManager.getInstance().getCurrentProject().getXmlHeader();
		float deviceDiagonalPixel = (float) Math.sqrt(Math.pow(metrics.widthPixels, 2) + Math.pow(metrics.heightPixels, 2));
		float creatorDiagonalPixel = (float) Math.sqrt(Math.pow(header.getVirtualScreenWidth(), 2)
				+ Math.pow(header.getVirtualScreenHeight(), 2));
		return creatorDiagonalPixel / deviceDiagonalPixel;
	}

	@VisibleForTesting
	public String getScreenshotPath() {
		return scene.getDirectory().getAbsolutePath() + "/";
	}

    public void executeConsoleScript(Sprite targetSprite, Script script) {
        if (targetSprite == null || script == null) return;

        Gdx.app.postRunnable(() -> {
            try {
                ScriptSequenceAction sequence = targetSprite.createSequenceAction(script);
                sequence.restart();

                targetSprite.look.addAction(sequence);

            } catch (Exception e) {
                Log.e("RuntimeConsole", "Failed to execute sandbox script", e);
            }
        });
    }
}
