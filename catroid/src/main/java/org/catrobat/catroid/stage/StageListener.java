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
import org.catrobat.catroid.content.EventWrapper;
import org.catrobat.catroid.content.ExitProjectScript;
import org.catrobat.catroid.content.GlobalManager;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.SoundBackup;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.XmlHeader;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.GamepadEventId;
import org.catrobat.catroid.content.eventids.MouseButtonEventId;
import org.catrobat.catroid.embroidery.DSTPatternManager;
import org.catrobat.catroid.embroidery.EmbroideryPatternManager;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.formulaeditor.UserDataWrapper;
import org.catrobat.catroid.io.SoundCacheManager;
import org.catrobat.catroid.io.SoundManager;
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

	private static final int AXIS_WIDTH = 4;
	private static final float DELTA_ACTIONS_DIVIDER_MAXIMUM = 50f;
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

	public void setMaxViewPort(Resolution maxViewPort) {
		this.maxViewPort = maxViewPort;
	}

	public boolean axesOn = false;
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

	/*private ModelBatch modelBatch;
	private Model yourModel;
	private ModelInstance yourModelInstance;*/
	private ThreeDManager threeDManager;

	public SceneManager sceneManager;

	// ▼▼▼ ДОБАВЬТЕ ЭТОТ МЕТОД ▼▼▼
	public ThreeDManager getThreeDManager() {
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

	public StageListener() {
		webConnectionHolder = new WebConnectionHolder();
	}

	private ShaderProgram vncSwizzleShader;

	@Override
	public void create() {
		deltaActionTimeDivisor = 10f;

		brightnessContrastHueShader = new Look.BrightnessContrastHueShader(); // <-- ДОБАВЬТЕ ЭТУ СТРОКУ
		shapeRenderer = new ShapeRenderer();

		project = ProjectManager.getInstance().getCurrentProject();
		scene = ProjectManager.getInstance().getCurrentlyPlayingScene();

		threeDManager = new ThreeDManager();
		threeDManager.init();

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

		Gdx.app.log("CacheWarming", "Starting asset pre-loading...");
		for (Sprite sprite : sprites) {
			// Прогреваем полигоны для каждого возможного костюма (LookData)
			if (sprite.getLookList() != null) {
				for (LookData lookData : sprite.getLookList()) {
					if (lookData != null) {
						// Эта строка заставит загрузить полигоны, если их еще нет
						lookData.getCollisionInformation().loadCollisionPolygon();
					}
				}
			}
			// Также можно принудительно создать и скомпилировать шейдеры,
			// но мы уже вынесли это на уровень сцены, что хорошо.
		}
		Gdx.app.log("CacheWarming", "Pre-loading finished.");

		passepartout = new Passepartout(
				ScreenValues.currentScreenResolution.getWidth(),
				ScreenValues.currentScreenResolution.getHeight(),
				maxViewPort.getWidth(),
				maxViewPort.getHeight(),
				virtualWidth,
				virtualHeight);
		//stage.addActor(passepartout);

		axes = new Texture(Gdx.files.internal("stage/red_pixel.bmp"));

		if (fullscreenQuad == null) {
			fullscreenQuad = new Mesh(true, 4, 6,
					new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

			float[] vertices = {
					-1.0f, -1.0f,  // левый нижний
					0.0f,  0.0f,  // UV
					1.0f, -1.0f,  // правый нижний
					1.0f,  0.0f,  // UV
					1.0f,  1.0f,  // правый верхний
					1.0f,  1.0f,  // UV
					-1.0f,  1.0f,  // левый верхний
					0.0f,  1.0f   // UV
			};

			short[] indices = { 0, 1, 2, 2, 3, 0 };

			fullscreenQuad.setVertices(vertices);
			fullscreenQuad.setIndices(indices);
		}

		try {
			String vertexShader = Gdx.files.internal("vnc_shader.vert").readString();
			String fragmentShader = Gdx.files.internal("vnc_shader.frag").readString();
			vncSwizzleShader = new ShaderProgram(vertexShader, fragmentShader);

			if (!vncSwizzleShader.isCompiled()) {
				Log.e("SHADER_ERROR", "VNC Swizzle Shader failed to compile: " + vncSwizzleShader.getLog());
			} else {
				Log.i("SHADER_INFO", "VNC Swizzle Shader compiled successfully.");
			}
		} catch (Exception e) {
			Log.e("SHADER_ERROR", "Could not load VNC Swizzle Shader files", e);
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
			// Вращаем на разницу между новым и старым углом, чтобы установить, а не добавить
			camera.rotate(cameraRotation - degrees);
			cameraRotation = degrees;
		}
	}

	public void pinSpriteToCamera(Sprite sprite) {
		if (sprite == null || uiStage == null || stage == null) return;

		Look look = sprite.look;
		if (look == null || look.getParent() == uiStage.getRoot()) {
			return; // Спрайт уже привязан или не существует
		}

		// Конвертируем мировые координаты в координаты экрана
		Vector3 screenCoords = new Vector3(look.getX(), look.getY(), 0);
		camera.project(screenCoords); // Теперь screenCoords содержит позицию на экране

		// Удаляем из игрового мира и добавляем в мир UI
		look.remove();
		uiStage.addActor(look);
		look.setPosition(screenCoords.x, screenCoords.y);
	}

	public void unpinSpriteFromCamera(Sprite sprite) {
		if (sprite == null || uiStage == null || stage == null) return;

		Look look = sprite.look;
		if (look == null || look.getParent() == stage.getRoot()) {
			return; // Спрайт уже отвязан или не существует
		}

		// Конвертируем координаты экрана обратно в мировые
		Vector3 worldCoords = new Vector3(look.getX(), look.getY(), 0);
		camera.unproject(worldCoords); // Теперь worldCoords содержит позицию в игровом мире

		// Удаляем из мира UI и возвращаем в игровой мир
		look.remove();
		stage.addActor(look);
		look.setPosition(worldCoords.x, worldCoords.y);
	}

	private boolean vmGeometryDirty = false;

	public void setVmScreenGeometry(float x, float y, float width, float height) {
		// Просто сохраняем значения
		this.vmX = x;
		this.vmY = y;
		this.vmWidth = width;
		this.vmHeight = height;

		// Выставляем флаг, что геометрию нужно пересоздать в потоке рендера
		this.vmGeometryDirty = true;
	}

	private boolean isVmDisplayVisible = false;

	// Добавьте этот публичный метод
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

		// Создаем Mesh, только если его еще нет
		if (vmScreenMesh == null) {
			vmScreenMesh = new Mesh(true, 4, 6,
					new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
		}

		vmScreenMesh.setVertices(vertices);
		vmScreenMesh.setIndices(indices);

		// Сбрасываем флаг
		vmGeometryDirty = false;
	}

	private void resetConditionScriptTriggers() {
		for (Sprite sprite : sprites) {
			sprite.resetConditionScriptTriggers();
		}
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
		//throw new RuntimeException("Test Crash from DanVexTeam!");
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
		fboRegion.flip(false, true); // обязательно!

		camera = new OrthographicCamera();
		cameraPositioner = new CameraPositioner(camera, virtualHeightHalf, virtualWidthHalf);
		viewPort = new ExtendViewport(virtualWidth, virtualHeight, camera);
		if (batch == null) {
			batch = new SpriteBatch();
		}
		if (postProcessBatch == null) {
			postProcessBatch = new SpriteBatch();
		}

		stage = new Stage(viewPort, batch);

		uiCamera = new OrthographicCamera();
		uiViewPort = new ScreenViewport(uiCamera); // ScreenViewport идеален для UI
		uiStage = new Stage(uiViewPort, batch);

		inputMultiplexer = new InputMultiplexer();
		// Сначала добавляем UI-сцену. Она получит нажатие первой и, если обработает,
		// событие не пойдет дальше, в игровую сцену.
		inputMultiplexer.addProcessor(uiStage);
		inputMultiplexer.addProcessor(stage);

		initMouseInputAdapter();
		//inputMultiplexer.addProcessor(mouseInputAdapter);

		SensorHandler.timerReferenceValue = SystemClock.uptimeMillis();
	}

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
			}
		};

		InputListener uiPassThroughListener = new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return event.getTarget() != uiStage.getRoot();
            }

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchUp(pointer);
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				TouchUtil.updatePosition(event.getStageX(), event.getStageY(), pointer);
			}
		};

		stage.addListener(gameListener);
		uiStage.addListener(uiPassThroughListener);
	}

	private void initActors(List<Sprite> sprites) {
		if (sprites.isEmpty()) {
			return;
		}

		for (Sprite sprite : sprites) {
			sprite.resetSprite();
			//sprite.look.createBrightnessContrastHueShader();
			stage.addActor(sprite.look);
		}

		penActor = new PenActor();
		stage.addActor(penActor);
		penActor.setZIndex(Z_LAYER_PEN_ACTOR);

		plotActor = new PlotActor();
		stage.addActor(plotActor);
		plotActor.setZIndex(Z_LAYER_PEN_ACTOR);

		float screenRatio = calculateScreenRatio();
		EmbroideryActor embroideryActor = new EmbroideryActor(screenRatio, embroideryPatternManager, shapeRenderer);
		stage.addActor(embroideryActor);
		embroideryActor.setZIndex(Z_LAYER_EMBROIDERY_ACTOR);
	}

	public void cloneSpriteAndAddToStage(Sprite cloneMe) {
		Sprite copy = new SpriteController().copyForCloneBrick(cloneMe);
		if (cloneMe.isClone) {
			copy.myOriginal = cloneMe.myOriginal;
		} else {
			copy.myOriginal = cloneMe;
		}
		//copy.look.createBrightnessContrastHueShader();
		addCloneActorToStage(stage, stage.getRoot(), cloneMe.look, copy.look);
		sprites.add(copy);
		if (!copy.getLookList().isEmpty()) {
			int currentLookDataIndex = cloneMe.getLookList().indexOf(cloneMe.look.getLookData());
			copy.look.setLookData(copy.getLookList().get(currentLookDataIndex));
		}
		copy.initializeEventThreads(EventId.START_AS_CLONE);
		copy.initConditionScriptTriggers();
	}

	public void cloneSpriteAndAddToStage(Sprite cloneMe, String newName) {
		Sprite copy = new SpriteController().copyForCloneBrick(cloneMe, newName);
		if (cloneMe.isClone) {
			copy.myOriginal = cloneMe.myOriginal;
		} else {
			copy.myOriginal = cloneMe;
		}
		//copy.look.createBrightnessContrastHueShader();
		addCloneActorToStage(stage, stage.getRoot(), cloneMe.look, copy.look);
		sprites.add(copy);
		if (!copy.getLookList().isEmpty()) {
			int currentLookDataIndex = cloneMe.getLookList().indexOf(cloneMe.look.getLookData());
			copy.look.setLookData(copy.getLookList().get(currentLookDataIndex));
		}
		copy.initializeEventThreads(EventId.START_AS_CLONE);
		copy.initConditionScriptTriggers();
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

	private InputProcessor mouseInputAdapter; // --- НОВОЕ: Отдельный обработчик для мыши ---
	private final Vector3 tempVec3 = new Vector3(); // --- НОВОЕ: Временный вектор для избежания создания мусора ---

	private void initMouseInputAdapter() {
		if (mouseInputAdapter == null) {
			mouseInputAdapter = new InputAdapter() {
				@Override
				public boolean mouseMoved(int screenX, int screenY) {
					// Конвертируем координаты экрана в мировые координаты Catroid
					tempVec3.set(screenX, screenY, 0);
					camera.unproject(tempVec3); // Используем основную игровую камеру

					// Обновляем позицию в SensorHandler
					SensorHandler.getInstance(null).updateMousePosition(tempVec3.x, tempVec3.y);
					return false; // Не "поглощаем" событие
				}

				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					if (pointer == 0) { // Только для мыши/основного касания
						return mouseMoved(screenX, screenY);
					}
					return false;
				}

				@Override
				public boolean scrolled(float amountX, float amountY) {
					// amountY будет 1 для прокрутки вниз, -1 для вверх. Инвертируем для интуитивности.
					SensorHandler.getInstance(null).setLastScrollAmount(-amountY);

					// Стреляем событием (этот код можно оставить здесь или перенести в initStageInputListener)
					EventWrapper e = new EventWrapper(new EventId(EventId.MOUSE_WHEEL_SCROLLED), false);
					if (project != null) project.fireToAllSprites(e);

					return true; // "Поглощаем" событие прокрутки
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

		isVmDisplayVisible = false;

		this.stageDialog = stageDialog;
		if (!ProjectManager.getInstance().getStartScene().getName().equals(scene.getName())) {
			transitionToScene(ProjectManager.getInstance().getStartScene().getName());
		}
		stageBackupMap.clear();
		embroideryPatternManager.clear();

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

		for (Scene scene : ProjectManager.getInstance().getCurrentProject().getSceneList()) {
			scene.firstStart = true;
		}
		GlobalManager.Companion.setStopSounds(true);
		GlobalManager.Companion.setSaveScenes(true);

		try {
			RenderManager.INSTANCE.initialize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			threeDManager = new ThreeDManager();
			threeDManager.init();
			sceneManager = new SceneManager(threeDManager);
		} catch (Exception e) {
			Log.e("StageListener", "INITIALIZE ERROR: " + e);
		}

		reloadProject = true;
	}

	@Override
	public void resume() {
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
		if (!paused) {
			setSchedulerStateForAllLooks(ThreadScheduler.SUSPENDED);
			SoundManager.getInstance().pause();
		}
	}

	private Texture vmTexture; // Текстура для экрана виртуальной машины

	public void setVmScreenSize(int width, int height) {
		Gdx.app.postRunnable(() -> {
			if (vmTexture != null) {
				vmTexture.dispose();
			}
			vmTexture = new Texture(width, height, Pixmap.Format.RGBA8888);

			// --- НАЧАЛО: Финальная, правильная настройка ---
			// Устанавливаем режим "обрезки" по краям, как в aVNC
			vmTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

			// Устанавливаем режим фильтрации (сглаживания) при растягивании/сжатии, как в aVNC
			vmTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			// --- КОНЕЦ: Финальная, правильная настройка ---

			vmWidth = width;
			vmHeight = height;
			Log.i("StageListener", "VM Texture resized to " + width + "x" + height + " with aVNC settings.");
		});
	}

	private volatile boolean captureNextFrame = false; // Флаг для захвата

	// Метод, который мы будем вызывать из StageActivity, чтобы включить флаг
	public void captureAndSaveVmTexture() {
		this.captureNextFrame = true;
	}

	public float getVirtualWidth() { return virtualWidth; }
	public float getVirtualHeight() { return virtualHeight; }
	public int getVmWidth() { return (int) vmWidth; }
	public int getVmHeight() { return (int) vmHeight; }

	@Override
	public void render() {
		try {
			Look.tickGlobalFrame();
			//SensorHandler.getInstance(null).resetMouseDelta();

			float color = 0f;
			Gdx.gl20.glClearColor(color, color, color, 0f);
			Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			StageActivity stageActivity = StageActivity.activeStageActivity.get();

			if (isVmDisplayVisible && stageActivity != null && stageActivity.frameReadyToRender && vmTexture != null) {
				if(!VirtualMachineManager.INSTANCE.isWorking()) return;
				VncClient client = stageActivity.vncClients.get("default_vm");
				if (client != null) {
					vmTexture.bind();
					client.uploadFrameTexture();
					stageActivity.frameReadyToRender = false;
				}
			}

			if (reloadProject) {
				if (threeDManager != null) {
					threeDManager.dispose();
				}


				threeDManager = new ThreeDManager();
				threeDManager.init();
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
				float deltaTime = Gdx.graphics.getDeltaTime();

				float optimizedDeltaTime = deltaTime / deltaActionTimeDivisor;
				long timeBeforeActionsUpdate = SystemClock.uptimeMillis();

				while (deltaTime > 0f) {
					physicsWorld.step(optimizedDeltaTime);
					stage.act(optimizedDeltaTime);
					uiStage.act(optimizedDeltaTime);
					deltaTime -= optimizedDeltaTime;
				}

				long executionTimeOfActionsUpdate = SystemClock.uptimeMillis() - timeBeforeActionsUpdate;
				if (executionTimeOfActionsUpdate <= ACTIONS_COMPUTATION_TIME_MAXIMUM) {
					deltaActionTimeDivisor += 1f;
					deltaActionTimeDivisor = Math.min(DELTA_ACTIONS_DIVIDER_MAXIMUM, deltaActionTimeDivisor);
				} else {
					deltaActionTimeDivisor -= 1f;
					deltaActionTimeDivisor = Math.max(1f, deltaActionTimeDivisor);
				}
				DebugMenuManager.getInstance().updateIfVisible();
			}

            if (isVmDisplayVisible && vmTexture != null && vncSwizzleShader != null && vncSwizzleShader.isCompiled()) {

                vncSwizzleShader.bind();


                vmTexture.bind(0);




                vncSwizzleShader.setUniformi("u_texture", 0);
                vncSwizzleShader.setUniformMatrix("u_projectionMatrix", camera.combined);



                fullscreenQuad.render(vncSwizzleShader, GL20.GL_TRIANGLES);
            }
            if (!finished) {
                try {
                    if (!paused) {
                        if (threeDManager != null) {
                            threeDManager.update(Gdx.graphics.getDeltaTime());
                        }
                    }
                    try {
                        if (threeDManager != null) threeDManager.render();
                    } catch (Exception e) {
                        Log.e("3DRENDER", "ERROR: " + e);
                    }
					//Log.d("StageListener", stage.getActors().toString());
                    stage.draw();
					uiStage.draw();
                    // RenderManager.INSTANCE.render();
                } catch (Exception e) {
                    Log.e("RENDER", "FATAL ERROR: " + e.toString());
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
					byte[] screenshot = ScreenUtils
							.getFrameBufferPixels(screenshotX, screenshotY, screenshotWidth, screenshotHeight, true);
					screenshotSaver.saveScreenshotAndNotify(
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
		} catch (Exception e) {

			Log.e("RENDER_CRASH", "Fatal error during render loop", e);


		}
	}

	private void renderSceneNormally(Stage stage) {
		if (!finished) {
			stage.draw(); // <-- ЗАКОММЕНТИРУЙТЕ ИЛИ УДАЛИТЕ ЭТУ СТРОКУ

			// ▼▼▼ НАЧАЛО НОВОГО КОДА ▼▼▼

			/*Array<Actor> actors = stage.getActors();
			camera.update(); // Убедимся, что камера обновлена
			batch.setProjectionMatrix(camera.combined);

			batch.begin();

			// ПРОХОД 1: Рисуем все обычные объекты (без кастомного шейдера)
			for (Actor actor : actors) {
				if (actor instanceof Look) {
					Look look = (Look) actor;
					/*if (!look.needsCustomShader()) {
						actor.draw(batch, 1.0f);
					}*//*
					actor.draw(batch, 1.0f);
				} else {
					// Рисуем все остальные акторы (PenActor, PlotActor, и т.д.)
					actor.draw(batch, 1.0f);
				}
			}*/

			/*batch.flush(); // Завершаем первый проход

			// ПРОХОД 2: Рисуем все объекты с эффектами
			batch.setShader(brightnessContrastHueShader); // Устанавливаем шейдер ОДИН РАЗ
			for (Actor actor : actors) {
				if (actor instanceof Look) {
					Look look = (Look) actor;
					if (look.needsCustomShader()) {
						// Устанавливаем уникальные параметры для этого объекта
						brightnessContrastHueShader.setBrightness(look.getBrightnessValue());
						brightnessContrastHueShader.setHue(look.getHueValue());
						// Рисуем
						look.draw(batch, 1.0f);
					}
				}
			}
			batch.setShader(null); // Сбрасываем шейдер ОДИН РАЗ*/

			//batch.end();

			// ▲▲▲ КОНЕЦ НОВОГО КОДА ▲▲▲

			firstFrameDrawn = true;
		}
	}

	/*@Override
	public void render() {
		Log.d("ShaderDebug", " "); // Пустая строка для разделения кадров
		Log.d("ShaderDebug", "--- FRAME START ---");

		// =================================================================
		// ШАГ 1: ОБНОВЛЕНИЕ ЛОГИКИ ИГРЫ (ВСЕГДА ВЫПОЛНЯЕТСЯ ОДИН РАЗ ЗА КАДР)
		// =================================================================

		// Логика перезагрузки проекта (остается здесь)
		if (reloadProject) {
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

			batch.setShader(null);
			GlobalShaderManager.INSTANCE.clear();

			initActors(sprites);
			stage.addActor(passepartout);

			initStageInputListener();

			paused = true;
			scene.firstStart = true;
			reloadProject = false;

			cameraPositioner.reset();

			if (stageDialog != null) {
				synchronized (stageDialog) {
					stageDialog.notify();
				}
			}
		}

		// Логика первого запуска сцены (переносим сюда из renderSceneNormally)
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

		// Логика обновления физики и актеров (переносим сюда из renderSceneNormally)
		if (!paused) {
			time += Gdx.graphics.getDeltaTime();
			Look.tickGlobalFrame();
			float deltaTime = Gdx.graphics.getDeltaTime();
			float optimizedDeltaTime = deltaTime / deltaActionTimeDivisor;
			long timeBeforeActionsUpdate = SystemClock.uptimeMillis();

			while (deltaTime > 0f) {
				physicsWorld.step(optimizedDeltaTime);
				stage.act(optimizedDeltaTime);
				deltaTime -= optimizedDeltaTime;
			}

			long executionTimeOfActionsUpdate = SystemClock.uptimeMillis() - timeBeforeActionsUpdate;
			if (executionTimeOfActionsUpdate <= ACTIONS_COMPUTATION_TIME_MAXIMUM) {
				deltaActionTimeDivisor += 1f;
				deltaActionTimeDivisor = Math.min(DELTA_ACTIONS_DIVIDER_MAXIMUM, deltaActionTimeDivisor);
			} else {
				deltaActionTimeDivisor -= 1f;
				deltaActionTimeDivisor = Math.max(1f, deltaActionTimeDivisor);
			}
		}

		if (plotActor != null) {
			// Убедитесь, что вы передаете ему ShapeRenderer из StageListener
			plotActor.updateBuffer(this.shapeRenderer);
		}
		if (penActor != null) {
			penActor.updatePenLayer(this.shapeRenderer);
		}

		// =================================================================
		// ШАГ 2: ЛОГИКА ОТРИСОВКИ (ИСПОЛЬЗУЕМ ОБНОВЛЕННОЕ СОСТОЯНИЕ)
		// =================================================================

		ShaderProgram customShader = GlobalShaderManager.INSTANCE.getCustomSceneShader();
		Camera cameraToUse = this.camera;//(customShader == null) ? this.camera : new OrthographicCamera(virtualWidth, virtualHeight);

		if (customShader != null) {
			// Настраиваем FBO камеру, если она нужна
			//cameraToUse.position.set(0, 0, 0);
			cameraToUse.update();
			sceneFbo.begin(); // Начинаем перенаправлять рендер в FBO
		}

// === ОБЩАЯ ЛОГИКА РЕНДЕРИНГА (И ДЛЯ ЭКРАНА, И ДЛЯ FBO) ===

// 1. Очищаем текущую цель (либо экран, либо FBO)
        CameraManager cameraManager = StageActivity.getActiveCameraManager();
        if (cameraManager != null && cameraManager.getPreviewVisible()) {
            // Если камера активна, очищаем фон прозрачным цветом
            Gdx.gl.glClearColor(0, 0, 0, 0);
        } else {
            // Иначе - стандартным белым
            Gdx.gl.glClearColor(1, 1, 1, 1);
        }
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

// 2. ФАЗА СПРАЙТОВ (SpriteBatch)

		batch.setProjectionMatrix(cameraToUse.combined);
		batch.begin();

// 1a. Рисуем Look без эффектов
		batch.setShader(null);
		for (Actor actor : stage.getActors()) {
			if (actor instanceof Look && !((Look) actor).needsCustomShader() && ((Look) actor).isLookVisible()) {
				actor.draw(batch, 1.0f);
			}
		}

		batch.flush();

// 1b. Рисуем Look с эффектами
		//batch.setShader(brightnessContrastHueShader);
		for (Actor actor : stage.getActors()) {
			if (actor instanceof Look) {
				Look look = (Look) actor;
				if (look.needsCustomShader() && look.isLookVisible()) {
					// look.applyShaderParameters(brightnessContrastHueShader) - более правильный путь,
					// но ваш текущий подход тоже будет работать
					//brightnessContrastHueShader.setBrightness(look.getBrightnessValue());
					//brightnessContrastHueShader.setHue(look.getHueValue());
					//look.applyShaderParameters(brightnessContrastHueShader);
					look.draw(batch, 1.0f);
				}
			}
		}

		batch.flush();
		//batch.setShader(null);

// 1c. Рисуем актеров, которые теперь просто рисуют текстуру (наш PlotActor)
// и другие, которые могли использовать batch (Passepartout)
		for (Actor actor : stage.getActors()) {
			if (actor instanceof PlotActor || actor instanceof PenActor || actor instanceof Passepartout) {
				actor.draw(batch, 1.0f);
			} else if(!(actor instanceof EmbroideryActor)) {
				actor.draw(batch, 1.0f);
			}
		}
		batch.end(); // ЗАВЕРШАЕМ ВСЕ ОПЕРАЦИИ С BATCH

		// === ФАЗА 3: Рисуем все, что использует ShapeRenderer ===
		shapeRenderer.setProjectionMatrix(cameraToUse.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (Actor actor : stage.getActors()) {
			// Явно ищем актеров, которые должны рисовать линии/фигуры.
			// PenActor и EmbroideryActor - главные кандидаты.
			// Их методы draw должны использовать shapeRenderer, а не batch.
			if (actor instanceof EmbroideryActor) {
				// Убедитесь, что их методы draw() теперь используют shapeRenderer
				actor.draw(batch, 1.0f); // Параметр batch будет проигнорирован, если их draw() исправлен
			}
		}
		shapeRenderer.end();


		if (customShader != null) {
			// Если мы рисовали в FBO, завершаем работу с ним
			sceneFbo.end();

			// ВОССТАНАВЛИВАЕМ ОСНОВНУЮ ПРОЕКЦИЮ ДЛЯ ДАЛЬНЕЙШЕГО РЕНДЕРА
			batch.setProjectionMatrix(camera.combined);

			// --- ФИНАЛЬНЫЙ ЭТАП: Отрисовка FBO на экран с шейдером ---
			// Ваш код для postProcessBatch здесь остается без изменений, он был правильным.
			Gdx.gl.glClearColor(1, 1, 1, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			//postProcessFbo.begin();

			// --- ШАГ 1: Брутальный сброс состояния OpenGL ---
			Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			// --- ШАГ 2: Полный сброс матриц SpriteBatch ---
			postProcessBatch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			//batch.setTransformMatrix(new Matrix4()); // Сброс в единичную матрицу

			// --- ШАГ 3: Установка шейдера и ручное связывание ---
			postProcessBatch.setShader(customShader);

			// Явно активируем текстурный юнит 1 (чтобы не конфликтовать с юнитом 0, который мог использовать SpriteBatch)
			sceneFbo.getColorBufferTexture().bind(); // Привязываем нашу текстуру к активному юниту (1)

			// Говорим шейдеру читать из юнита 1
			customShader.bind();
			customShader.setUniformi("u_texture", unit_to_shader);
			customShader.setUniformf("u_time", time);

			//postProcessFbo.end();


			// --- ШАГ 4: Финальная отрисовка ---
			postProcessBatch.begin();

			// !!! НАЧАЛО ФИНАЛЬНОГО, АБСОЛЮТНОГО ИСПРАВЛЕНИЯ !!!

			float fboWidth = sceneFbo.getWidth();
			float fboHeight = sceneFbo.getHeight();
			float screenWidth = Gdx.graphics.getWidth();
			float screenHeight = Gdx.graphics.getHeight();

			// 1. Расчет размера (этот код правильный)
			com.badlogic.gdx.math.Vector2 scaledSize = Scaling.fit.apply(fboWidth, fboHeight, screenWidth, screenHeight);

			// 2. Расчет позиции (этот код правильный)
			float x = (screenWidth - scaledSize.x) / 2;
			float y = (screenHeight - scaledSize.y) / 2;

			// 3. Используем САМУЮ ЯВНУЮ версию draw(), чтобы избежать любых капризов SpriteBatch
			postProcessBatch.draw(
					fboRegion,  // Даем ему чистую текстуру
					x, y,                    // Позиция на экране
					scaledSize.x,
					scaledSize.y
					//fboWidth,            // Ширина на экране
			);

			// !!! КОНЕЦ ФИНАЛЬНОГО, АБСОЛЮТНОГО ИСПРАВЛЕНИЯ !!!

			postProcessBatch.end();
			//fullscreenQuad.render(customShader, GL20.GL_TRIANGLES);

			// --- ШАГ 5: Полный сброс ---
			postProcessBatch.setShader(null);
			Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0); // Возвращаем активный юнит на 0 для порядка
			postProcessBatch.setProjectionMatrix(camera.combined); // Возвращаем камеру для остальной части сцены
		}

		// =================================================================
		// ШАГ 3: ОТРИСОВКА ПОВЕРХ ВСЕГО (ОСИ, ДЕБАГ И Т.Д.)
		// =================================================================

		// Возвращаем проекцию мировой камеры для отрисовки осей и т.д.
		batch.setProjectionMatrix(camera.combined);

		if (axesOn && !finished) {
			drawAxes();
		}

		if (makeScreenshot) {
			Scene scene = ProjectManager.getInstance().getCurrentlyEditedScene();
			String manualScreenshotPath = scene.getDirectory()
					+ "/" + SCREENSHOT_MANUAL_FILE_NAME;
			File manualScreenshot = new File(manualScreenshotPath);
			if (!manualScreenshot.exists() || Objects.equals(screenshotName,
					SCREENSHOT_MANUAL_FILE_NAME)) {
				byte[] screenshot = ScreenUtils
						.getFrameBufferPixels(screenshotX, screenshotY, screenshotWidth, screenshotHeight, true);
				screenshotSaver.saveScreenshotAndNotify(
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

		//if (PhysicsDebugSettings.Render.RENDER_PHYSIC_OBJECT_LABELING) {
		//	printPhysicsLabelOnScreen();
		//}

		//if (PhysicsDebugSettings.Render.RENDER_COLLISION_FRAMES && !finished) {
		//	physicsWorld.render(camera.combined);
		//}

		//if (makeTestPixels) {
		//	testPixels = ScreenUtils.getFrameBufferPixels(testX, testY, testWidth, testHeight, false);
		//	makeTestPixels = false;
		//}

		cameraPositioner.updateCameraPositionForFocusedSprite();

		Log.d("ShaderDebug", "--- FRAME END ---");
		firstFrameDrawn = true;
	}*/

	private final Integer unit_to_shader = 0;

	private void saveFboToFile(FrameBuffer fbo, String fileName) {
		try {
			// Обязательно начинаем fbo
			fbo.begin();

			// Получаем размеры (float → int)
			int fboWidth = fbo.getWidth();
			int fboHeight = fbo.getHeight();

			// Получаем пиксели из FBO
			// ВНИМАНИЕ: true = flip vertically (нужно для корректного PNG)
			byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, fboWidth, fboHeight, true);

			// Создаем pixmap из пикселей
			Pixmap pixmap = new Pixmap(fboWidth, fboHeight, Pixmap.Format.RGBA8888);
			ByteBuffer buffer = pixmap.getPixels();
			buffer.clear(); // сбрасываем указатель
			buffer.put(pixels);
			buffer.position(0);

			// Путь к файлу
			FileHandle file = Gdx.files.external("Download/" + fileName);

			// Сохраняем
			PixmapIO.writePNG(file, pixmap);
			pixmap.dispose();

			Log.i("ShaderDebug_CAPTURE", "✅ FBO сохранен: " + file.file().getAbsolutePath());
		} catch (Exception e) {
			Log.e("ShaderDebug_CAPTURE", "❌ Ошибка сохранения FBO: ", e);
		} finally {
			fbo.end();
		}
	}


	private boolean fboSaved = true ;


	// В StageListener.java
	/*private void renderSceneNormally(Stage stage, Camera cam) {
		// МЕТОД БОЛЬШЕ НЕ ВЫЗЫВАЕТ begin()/end()!

		Log.d("ShaderDebug", "  [Render] >> renderSceneNormally START (within active batch)");

		cam.update();
		Array<Actor> actors = stage.getActors();

		// ПРОХОД 1: Обычные объекты (без кастомного шейдера)
		// Убедимся, что используется стандартный шейдер batch'а
		batch.setShader(null);
		for (Actor actor : actors) {
			if (actor instanceof Look) {
				if (!((Look) actor).needsCustomShader()) {
					actor.draw(batch, 1.0f);
				}
			} else if (actor != null) {
				actor.draw(batch, 1.0f);
			}
		}
		// ВАЖНО: Принудительно сбрасываем batch, чтобы нарисованное в Проходе 1
		// не смешалось с тем, что будет в Проходе 2.
		batch.flush();

		// ПРОХОД 2: Объекты с графическими эффектами
		batch.setShader(brightnessContrastHueShader);
		for (Actor actor : actors) {
			if (actor instanceof Look) {
				Look look = (Look) actor;
				if (look.needsCustomShader()) {
					// look.applyShaderParameters(brightnessContrastHueShader) - более правильный путь,
					// но ваш текущий подход тоже будет работать
					brightnessContrastHueShader.setBrightness(look.getBrightnessValue());
					brightnessContrastHueShader.setHue(look.getHueValue());
					look.draw(batch, 1.0f);
				}
			}
		}
		// Еще один flush, чтобы гарантировать отрисовку Прохода 2.
		batch.flush();

		// Возвращаем стандартный шейдер для batch'а, чтобы не влиять на другие части рендера
		batch.setShader(null);

		Log.d("ShaderDebug", "  [Render] >> renderSceneNormally END");
	}*/

	// В StageListener.java
	private void renderSceneNormally(Stage stage, Camera cameraToUse) {
		Log.d("ULTIMATE_DEBUG", "ПОИСК ВИНОВНИКА. Рисуем актеров по одному.");
		cameraToUse.update();
		Array<Actor> actors = stage.getActors();

		batch.setShader(null);
		for (Actor actor : actors) {
			if (actor != null) {
				// Пропускаем актеров, которые должны рисоваться во втором проходе
				if (actor instanceof Look && ((Look) actor).needsCustomShader()) {
					continue;
				}

				Log.d("ULTIMATE_DEBUG", "Сейчас рисуется: " + actor.getClass().getSimpleName());
				actor.draw(batch, 1.0f);
				batch.flush(); // Вызываем flush после каждого, чтобы проверить состояние
				Log.d("ULTIMATE_DEBUG", "    ...нарисован успешно.");
			}
		}
		Log.d("ULTIMATE_DEBUG", "Поиск завершен. Если вы видите этот лог, все актеры нарисовались.");
	}

	private void render3DScene() {

	}


	private void printPhysicsLabelOnScreen() {
		PhysicsObject tempPhysicsObject;
		final int fontOffset = 5;
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		for (Sprite sprite : sprites) {
			if (sprite.look instanceof PhysicsLook) {
				tempPhysicsObject = physicsWorld.getPhysicsObject(sprite);
				font.draw(batch, "velocity_x: " + tempPhysicsObject.getVelocity().x, tempPhysicsObject.getX(),
						tempPhysicsObject.getY());
				font.draw(batch, "velocity_y: " + tempPhysicsObject.getVelocity().y, tempPhysicsObject.getX(),
						tempPhysicsObject.getY() + font.getXHeight() + fontOffset);
				font.draw(batch, "angular velocity: " + tempPhysicsObject.getRotationSpeed(), tempPhysicsObject.getX(),
						tempPhysicsObject.getY() + font.getXHeight() * 2 + fontOffset * 2);
				font.draw(batch, "direction: " + tempPhysicsObject.getDirection(), tempPhysicsObject.getX(),
						tempPhysicsObject.getY() + font.getXHeight() * 3 + fontOffset * 3);
			}
		}
		batch.end();
	}

	private void drawAxes() {
		GlyphLayout layout = new GlyphLayout();
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

	public PenActor getPenActor() {
		return penActor;
	}

	public PlotActor getPlotActor() {
		return plotActor;
	}

	@Override
	public void resize(int width, int height) {
		// --- ИЗМЕНЕНИЕ ---
		// Теперь нужно обновлять оба viewport'а
		if (viewPort != null) {
			viewPort.update(width, height, false);
		}
		if (uiViewPort != null) {
			uiViewPort.update(width, height, true); // true = center camera
		}
	}

	/**
	 * ПРИНУДИТЕЛЬНО и СИНХРОННО выполняет все скрипты "При выходе из проекта".
	 * Этот метод не зависит от игрового цикла и флага 'paused'.
	 */
	public void executeExitScriptsSynchronously() {
		Log.d("StageListener", "Force-executing exit scripts...");
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project == null || sprites == null) {
			Log.e("StageListener", "Cannot execute exit scripts, project or sprites are null.");
			return;
		}

		for (Sprite sprite : sprites) {
			for (Script script : sprite.getScriptList()) {
				// Ищем конкретно наш тип скрипта
				if (script instanceof ExitProjectScript && !script.isCommentedOut()) {
					Log.d("StageListener", "Found exit script in sprite: " + sprite.getName());
					// 1. Создаем последовательность действий (как это делает система)
					ScriptSequenceAction sequence = sprite.createSequenceAction(script);

					// 2. ПРИНУДИТЕЛЬНО ВЫПОЛНЯЕМ ЕЕ
					// Метод act() с большим delta временем заставит выполниться все действия внутри
					// последовательности за один вызов.
					sequence.act(Float.MAX_VALUE);
				}
			}
		}
		Log.d("StageListener", "Finished executing exit scripts.");
	}

	/**
	 * "Транслирует" событие всем спрайтам на текущей сцене.
	 * @param eventId ID события для запуска.
	 */
	/**
	 * "Транслирует" событие всем спрайтам на текущей сцене.
	 * @param eventId ID события для запуска.
	 */
	private void broadcastEventToAllSprites(EventId eventId) {
		// Проверка на случай, если спрайты еще не инициализированы
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

	public void resetCamera() {
		if (camera != null) {
			// Сбрасываем позицию в центр (0, 0)
			camera.position.set(0, 0, 0);

			// Сбрасываем зум к 1.0 (без приближения/отдаления)
			camera.zoom = 1.0f;

			// Вращаем камеру на ОБРАТНЫЙ угол, чтобы вернуть ее в 0 градусов
			camera.rotate(-cameraRotation);
			cameraRotation = 0f; // Также сбрасываем наш собственный счетчик вращения

			// Применяем все изменения
			camera.update();
		}
	}

	@Override
	public void dispose() {
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

		if (postProcessShader != null) {
			postProcessShader.dispose();
		}

		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null) {
			stageActivity.removeAllNativeViews();
		}

		RenderManager.INSTANCE.dispose();

		try {
			MainMenuActivity.pythonEngine.clearEnvironment();
		} catch (Exception e) {
			e.printStackTrace();
		}

		disposeStageButKeepActors();
		font.dispose();
		axes.dispose();
		ColorAtXYDetection.Companion.disposeShared();

		sceneFbo.dispose();
		GlobalShaderManager.INSTANCE.dispose();
		GlobalShaderManager.INSTANCE.clear();

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
		viewPort.update(ScreenValues.currentScreenResolution.getWidth(),
				ScreenValues.currentScreenResolution.getHeight(),
				false);
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
		if (stage != null) { // <--- ДОБАВИТЬ ЭТУ ПРОВЕРКУ
			stage.unfocusAll();
		}
		if (batch != null) { // <--- И ЭТУ ТОЖЕ, НА ВСЯКИЙ СЛУЧАЙ
			batch.dispose();
		}
	}

	public void gamepadPressed(String buttonType) {
		// ИЗМЕНЕНО: Добавляем проверку на null
		if (project == null) {
			Log.e("StageListener", "Gamepad event received, but project is null. Ignoring.");
			return; // Просто выходим, чтобы избежать крэша
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
}