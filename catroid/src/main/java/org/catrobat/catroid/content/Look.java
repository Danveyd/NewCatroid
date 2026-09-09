/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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
package org.catrobat.catroid.content;

import android.graphics.Camera;
import android.graphics.PointF;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ThreadScheduler;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.ScriptSequenceActionWithWaiter;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.physics.ParticleConstants;
import org.catrobat.catroid.sensing.CollisionInformation;
import org.catrobat.catroid.utils.TouchUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;

import static org.catrobat.catroid.physics.ParticleConstants.LIFE_HIGH_MAX_ACTIVE;
import static org.catrobat.catroid.physics.ParticleConstants.LIFE_HIGH_MAX_DEAD;
import static org.catrobat.catroid.physics.ParticleConstants.PARTICLE_SCALE;

@LunoClass
public class Look extends Image {

	@Nullable
	public Vector2 getPosition() {
		return new Vector2(getX(), getY());
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ROTATION_STYLE_LEFT_RIGHT_ONLY, ROTATION_STYLE_ALL_AROUND, ROTATION_STYLE_NONE})
	public @interface RotationStyle {}
	public static final int ROTATION_STYLE_LEFT_RIGHT_ONLY = 0;
	public static final int ROTATION_STYLE_ALL_AROUND = 1;
	public static final int ROTATION_STYLE_NONE = 2;

	public static final float DEGREE_UI_OFFSET = 90.0f;
	private static final float COLOR_SCALE = 200.0f;


	private static int globalFrameTicker = 0;
	private static final int UPDATE_BUCKETS = 4;
	private int myUpdateBucket = -1;

	private final AtomicBoolean collisionDirty = new AtomicBoolean(true);

	private boolean assumesConvexPolygons = true;
	private boolean lookVisible = true;
	private boolean simultaneousMovementXY = false;
	private int lookListIndexBeforeLookRequest = -1;
	protected LookData lookData;

	private BrightnessContrastHueShader shader;
	private boolean useCustomShader = false;
	public LookData lookData2 = null;
	public Sprite sprite;
	protected float alpha = 1f;
	protected float brightness = 1f;
	protected float hue = 0f;

	protected float height = 1f;
	protected float width = 1f;
	protected Pixmap pixmap;

	private transient OrthographicCamera gameCamera;
	private transient Viewport gameViewport;
	private transient Stage uiStage;
	private int rotationMode = ROTATION_STYLE_ALL_AROUND;
	private float rotation = 90f;
	private float realRotation = rotation;
	private ThreadScheduler scheduler;
	private ParticleEffect particleEffect;

	public boolean hasParticleEffect = false;
	public boolean isAdditive = true;

	private boolean isParticleEffectPaused = false;

    public boolean drawOnlyInBuffer = false;

    private final transient float[] hitboxVertices = new float[8];
    private final transient Polygon hitboxPolygon = new Polygon(hitboxVertices);

    public String maskBufferName = null;
    public int maskMode = 0; // 0: Stretch, 1: Screen
    private static ShaderProgram maskShader = null;

    private float topLeftOffsetX = 0f, topLeftOffsetY = 0f;
    private float topRightOffsetX = 0f, topRightOffsetY = 0f;
    private float bottomRightOffsetX = 0f, bottomRightOffsetY = 0f;
    private float bottomLeftOffsetX = 0f, bottomLeftOffsetY = 0f;
    private boolean hasCornerOffsets = false;

    private transient final float[] cornerOffsetsVerts = new float[20];

    private transient com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> activeGifAnimation = null;
    private float gifStateTime = 0f;

    private transient com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> activeSheetAnimation = null;
    private float sheetStateTime = 0f;
    private transient String cachedSheetKey = "";
    private transient TextureRegionDrawable animationDrawable;
    private transient TextureRegion[][] cachedSplitRegions = null;

    public transient final java.util.List<org.catrobat.catroid.utils.shaders.ShaderEffectInstance> shaderChain = new java.util.ArrayList<>();

	public Look(final Sprite sprite) {
		this.sprite = sprite;
		globalFrameTicker++;
		myUpdateBucket = globalFrameTicker % UPDATE_BUCKETS;
		scheduler = new ThreadScheduler(this);
		setBounds(0f, 0f, 0f, 0f);
		setOrigin(0f, 0f);
		setScale(1f, 1f);
		setRotation(0f);
		setTouchable(Touchable.enabled);
		setAssumesConvexPolygons(false);
		addListeners();
	}

    public void setCornerOffsets(float tlX, float tlY, float trX, float trY, float brX, float brY, float blX, float blY) {
        this.topLeftOffsetX = tlX;
        this.topLeftOffsetY = tlY;
        this.topRightOffsetX = trX;
        this.topRightOffsetY = trY;
        this.bottomRightOffsetX = brX;
        this.bottomRightOffsetY = brY;
        this.bottomLeftOffsetX = blX;
        this.bottomLeftOffsetY = blY;
        this.hasCornerOffsets = (tlX != 0 || tlY != 0 || trX != 0 || trY != 0 || brX != 0 || brY != 0 || blX != 0 || blY != 0);
    }

	protected void addListeners() {
		this.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if(getTouchable() == Touchable.disabled) {
					return false;
				}
				if (!isLookVisible()) {
					return false;
				}

				float stageX = event.getStageX();
				float stageY = event.getStageY();

				Polygon[] collisionPolygons = getCurrentCollisionPolygon();
				for (Polygon poly : collisionPolygons) {
					if (poly.contains(stageX, stageY)) {
						EventWrapper e = new EventWrapper(new EventId(EventId.TAP), false);
						sprite.look.fire(e);
						return true;
					}
				}

				setTouchable(Touchable.disabled);
				Actor target = getParent().hit(stageX, stageY, true);
				if (target != null) {
					target.fire(event);
					target.fire(event);
				}
				setTouchable(Touchable.enabled);

				return false;
			}

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (getTouchable() == Touchable.disabled || !isLookVisible()) {
                    return;
                }

                float stageX = event.getStageX();
                float stageY = event.getStageY();

                Polygon[] collisionPolygons = getCurrentCollisionPolygon();
                for (Polygon poly : collisionPolygons) {
                    if (poly.contains(stageX, stageY)) {
                        EventWrapper e = new EventWrapper(new EventId(EventId.FINGER_MOVED_OVER_SPRITE), false);
                        sprite.look.fire(e);
                        return;
                    }
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (getTouchable() == Touchable.disabled || !isLookVisible()) {
                    return;
                }

                float stageX = event.getStageX();
                float stageY = event.getStageY();

                Polygon[] collisionPolygons = getCurrentCollisionPolygon();
                for (Polygon poly : collisionPolygons) {
                    if (poly.contains(stageX, stageY)) {
                        EventWrapper e = new EventWrapper(new EventId(EventId.SPRITE_RELEASED), false);
                        sprite.look.fire(e);
                        return;
                    }
                }
            }
		});
		this.addListener(new EventWrapperListener(this));
	}

    private void initMaskShader() {
        if (maskShader == null) {
            String vertexShader = "attribute vec4 a_position;\n"
                    + "attribute vec4 a_color;\n"
                    + "attribute vec2 a_texCoord0;\n"
                    + "uniform mat4 u_projTrans;\n"
                    + "varying vec4 v_color;\n"
                    + "varying vec2 v_texCoords;\n"
                    + "varying vec4 v_screenPos;\n"
                    + "void main() {\n"
                    + "    v_color = a_color;\n"
                    + "    v_texCoords = a_texCoord0;\n"
                    + "    vec4 pos = u_projTrans * a_position;\n"
                    + "    v_screenPos = pos;\n"
                    + "    gl_Position = pos;\n"
                    + "}";

            String fragmentShader = "#ifdef GL_ES\n"
                    + "    precision mediump float;\n"
                    + "#endif\n"
                    + "varying vec4 v_color;\n"
                    + "varying vec2 v_texCoords;\n"
                    + "varying vec4 v_screenPos;\n"
                    + "uniform sampler2D u_texture;\n"
                    + "uniform sampler2D u_mask;\n"
                    + "uniform int u_mode;\n"
                    + "void main() {\n"
                    + "    vec4 spriteColor = texture2D(u_texture, v_texCoords);\n"
                    + "    vec2 maskCoords;\n"
                    + "    if (u_mode == 0) {\n"
                    + "        maskCoords = v_texCoords;\n"
                    + "    } else {\n"
                    + "        vec2 clipCoords = (v_screenPos.xy / v_screenPos.w) * 0.5 + 0.5;\n"
                    + "        clipCoords.y = 1.0 - clipCoords.y;\n"
                    + "        maskCoords = clipCoords;\n"
                    + "    }\n"
                    + "    vec4 maskColor = texture2D(u_mask, maskCoords);\n"
                    + "    gl_FragColor = v_color * vec4(spriteColor.rgb, spriteColor.a * maskColor.a);\n"
                    + "}";

            maskShader = new ShaderProgram(vertexShader, fragmentShader);
            if (!maskShader.isCompiled()) {
                Log.e("Look", "Error compiling Mask Shader: " + maskShader.getLog());
            }
        }
    }

    public static void disposeMaskShader() {
        if (maskShader != null) {
            maskShader.dispose();
            maskShader = null;
        }
    }

    public void playGif(final String filename) {
        stopGif();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(filename);
                    if (file != null && file.exists()) {
                        com.badlogic.gdx.files.FileHandle handle = Gdx.files.absolute(file.getAbsolutePath());

                        final org.catrobat.catroid.utils.GifDecoder decoder = new org.catrobat.catroid.utils.GifDecoder();
                        int err = decoder.read(handle.read());

                        if (err == 0 && decoder.getFrameCount() > 0) {
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Array<TextureRegion> keyFrames = new Array<>();
                                        float totalDelay = 0;
                                        int frameCount = decoder.getFrameCount();

                                        for (int i = 0; i < frameCount; i++) {
                                            Pixmap pixmap = decoder.getFrame(i);
                                            if (pixmap != null) {
                                                Texture texture = new Texture(pixmap);
                                                TextureRegion region = new TextureRegion(texture);
                                                keyFrames.add(region);
                                                totalDelay += decoder.getDelay(i) / 1000f;

                                                pixmap.dispose();
                                            }
                                        }

                                        float frameDuration = totalDelay / frameCount;
                                        if (frameDuration <= 0) frameDuration = 0.1f;

                                        activeGifAnimation = new com.badlogic.gdx.graphics.g2d.Animation<>(
                                                frameDuration,
                                                keyFrames,
                                                com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP
                                        );
                                        gifStateTime = 0f;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopGif() {
        activeSheetAnimation = null;
        if (activeGifAnimation != null) {
            Object[] frames = activeGifAnimation.getKeyFrames();
            for (int i = 0; i < frames.length; i++) {
                if (frames[i] instanceof TextureRegion) {
                    TextureRegion frame = (TextureRegion) frames[i];
                    if (frame.getTexture() != null) {
                        frame.getTexture().dispose();
                    }
                }
            }
            activeGifAnimation = null;
        }
        gifStateTime = 0f;
        refreshTextures(true);
    }

    public void playSpritesheet(int totalRows, int totalCols, int selectedRow, int frameCount, float frameDuration) {
        stopGif();
        activeSheetAnimation = null;
        sheetStateTime = 0f;

        if (lookData == null) return;
        TextureRegion originalRegion = lookData.getTextureRegion();
        if (originalRegion == null) return;
        Texture texture = originalRegion.getTexture();
        if (texture == null) return;

        try {
            String key = texture.getTextureObjectHandle() + "_" + totalRows + "x" + totalCols;
            if (!key.equals(cachedSheetKey) || cachedSplitRegions == null) {
                cachedSheetKey = key;
                int frameWidth = texture.getWidth() / totalCols;
                int frameHeight = texture.getHeight() / totalRows;
                cachedSplitRegions = TextureRegion.split(texture, frameWidth, frameHeight);
            }

            int row = Math.max(0, Math.min(selectedRow, totalRows - 1));
            int maxFrames = Math.min(frameCount, totalCols);

            Array<TextureRegion> keyFrames = new Array<>();
            for (int i = 0; i < maxFrames; i++) {
                keyFrames.add(cachedSplitRegions[row][i]);
            }

            activeSheetAnimation = new com.badlogic.gdx.graphics.g2d.Animation<>(
                    frameDuration,
                    keyFrames,
                    com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSpritesheet() {
        activeSheetAnimation = null;
        sheetStateTime = 0f;
        refreshTextures(true);
    }

	public void setRenderingContext(OrthographicCamera gameCamera, Viewport gameViewport, Stage uiStage) {
		this.gameCamera = gameCamera;
		this.gameViewport = gameViewport;
		this.uiStage = uiStage;
	}

	public boolean isPinnedToCamera() {
		return getStage() != null && getStage() == uiStage;
	}

	public void setAssumesConvexPolygons(boolean convex) {
		this.assumesConvexPolygons = convex;
	}

	public boolean getAssumesConvexPolygons() {
		return assumesConvexPolygons;
	}

	public synchronized boolean isLookVisible() {
		return lookVisible;
	}

	public synchronized void setLookVisible(boolean lookVisible) {
		this.lookVisible = lookVisible;
		if (lookVisible) {
			setTouchable(Touchable.enabled);
		} else {
			setTouchable(Touchable.disabled);
		}
	}

	public synchronized int getLookListIndexBeforeLookRequest() {
		return lookListIndexBeforeLookRequest;
	}

	public synchronized void setLookListIndexBeforeLookRequest(int lookListIndexBeforeLookRequest) {
		this.lookListIndexBeforeLookRequest = lookListIndexBeforeLookRequest;
	}

    public void destroy() {
        notifyAllWaiters();
        setLookVisible(false);
        stopGif();
        stopSpritesheet();

        for (EventListener listener : getListeners()) {
            removeListener(listener);
        }
        getActions().clear();
        scheduler = null;
        sprite = null;
        lookData = null;

        if (shader != null) {
            shader.dispose();
            shader = null;
        }
        clearParticleEffect();

        super.remove();
    }

	@Override
	public boolean remove() {
		return super.remove();
	}

	public void copyTo(final Look destination) {
		destination.setLookVisible(this.isLookVisible());
		destination.setPositionInUserInterfaceDimensionUnit(this.getXInUserInterfaceDimensionUnit(),
				this.getYInUserInterfaceDimensionUnit());
		destination.setSizeInUserInterfaceDimensionUnit(this.getSizeInUserInterfaceDimensionUnit());
		destination.setTransparencyInUserInterfaceDimensionUnit(this.getTransparencyInUserInterfaceDimensionUnit());
		destination.setColorInUserInterfaceDimensionUnit(this.getColorInUserInterfaceDimensionUnit());

		destination.setRotationMode(this.getRotationMode());
		destination.setMotionDirectionInUserInterfaceDimensionUnit(this.getMotionDirectionInUserInterfaceDimensionUnit());
		destination.setBrightnessInUserInterfaceDimensionUnit(this.getBrightnessInUserInterfaceDimensionUnit());
		destination.hasParticleEffect = hasParticleEffect;
		destination.isAdditive = isAdditive;
	}

	public boolean doTouchDown(float x, float y, int pointer) {
		if (!isLookVisible()) {
			return false;
		}

		Polygon[] polygons = getCurrentCollisionPolygon();

		this.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (!isLookVisible()) {
					return false;
				}

				Polygon[] collisionPolygons = getCurrentCollisionPolygon();
				for (Polygon poly : collisionPolygons) {
					if (poly.contains(event.getStageX(), event.getStageY())) {
						EventWrapper e = new EventWrapper(new EventId(EventId.TAP), false);
						sprite.look.fire(e);
						return true;
					}
				}

				setTouchable(Touchable.disabled);
				Actor target = getParent().hit(event.getStageX(), event.getStageY(), true);
				if (target != null) {
					target.fire(event);
				}
				setTouchable(Touchable.enabled);
				return false;
			}
		});
		return false;
	}

    public synchronized void createBrightnessContrastHueShader() {
        if (shader != null) {
            shader.dispose();
        }
        shader = new BrightnessContrastHueShader();
        shader.setBrightness(brightness);
        shader.setHue(hue);
    }

	public ParticleEffect getParticleEffect() {
		if (particleEffect == null) {
			initialiseParticleEffect();
		}
		return particleEffect;
	}

	private void initialiseParticleEffect() {
		particleEffect = new ParticleEffect();
		particleEffect.load(Gdx.files.internal("particles"), Gdx.files.internal(""));
		particleEffect.start();
	}

	public void pauseParticleEffect() {
		isParticleEffectPaused = true;
	}

	public void resumeParticleEffect() {
		isParticleEffectPaused = false;
	}

	@VisibleForTesting
	public boolean isParticleEffectPaused() {
		return isParticleEffectPaused;
	}

	public void clearParticleEffect() {
		if (particleEffect != null) {
			particleEffect.dispose();
			particleEffect = null;
		}
	}

	public void setHeightV(Float value) {
		height = value;
		this.setScaleY(value);
	}

	public void setWidthV(Float value) {
		height = value;
		this.setScaleX(value);
	}

	public ParticleEmitter getParticleEmitter() {
		return getParticleEffect().getEmitters().first();
	}

	private void setupParticleEffects(ParticleEmitter particleEmitter) {
		particleEmitter.setPosition(
				sprite.look.getX() + sprite.look.getWidth() / 2f,
				sprite.look.getY() + sprite.look.getHeight() / 2f);

		float spriteSize = sprite.look.getSizeInUserInterfaceDimensionUnit() / 2;

		float pScale = 1;
		if (sprite.getLookList().size() == 0) {
			pScale = spriteSize / PARTICLE_SCALE;
		}

		particleEmitter.getXScale().setHigh(spriteSize);
		particleEmitter.getVelocity().setHighMin(ParticleConstants.VELOCITY_HIGH_MIN * pScale);
		particleEmitter.getVelocity().setHighMax(ParticleConstants.VELOCITY_HIGH_MAX * pScale);
		particleEmitter.getGravity().setHigh(ProjectManager.getInstance().getCurrentlyPlayingScene().getPhysicsWorld().getGravity().y);
		particleEmitter.setAdditive(isAdditive);
	}

	private void fadeInParticles() {
		ParticleEmitter particleEmitter = getParticleEmitter();
		setupParticleEffects(particleEmitter);
		particleEmitter.setContinuous(true);
		particleEmitter.getLife().setHighMax(LIFE_HIGH_MAX_ACTIVE);

		particleEffect.update(Gdx.graphics.getDeltaTime());
	}

	private void fadeOutParticles() {
		ParticleEmitter particleEmitter = getParticleEmitter();
		setupParticleEffects(particleEmitter);
		particleEmitter.setContinuous(false);
		particleEmitter.getLife().setHighMax(LIFE_HIGH_MAX_DEAD);

		particleEffect.update(Gdx.graphics.getDeltaTime());
	}

    @Override
    public synchronized void draw(Batch batch, float parentAlpha) {
        if (drawOnlyInBuffer && !org.catrobat.catroid.content.RenderTextureManager.INSTANCE.isRenderingToBuffer()) {
            return;
        }

        if (particleEffect != null) {
            particleEffect.draw(batch);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        if (!isLookVisible() || getDrawable() == null) {
            return;
        }

        super.setVisible(alpha != 0.0f);

        org.catrobat.catroid.utils.shaders.ShaderEffectInstance activeSpriteShader = null;
        if (!shaderChain.isEmpty()) {
            activeSpriteShader = shaderChain.get(shaderChain.size() - 1);
        }

        if (maskBufferName != null) {
            initMaskShader();
            TextureRegion maskRegion = org.catrobat.catroid.content.RenderTextureManager.INSTANCE.getTextureRegion(maskBufferName);
            if (maskRegion != null && maskShader != null && maskShader.isCompiled()) {
                Texture maskTexture = maskRegion.getTexture();
                maskTexture.bind(1);
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
                batch.setShader(maskShader);
                maskShader.setUniformi("u_mask", 1);
                maskShader.setUniformi("u_mode", maskMode);
            }
        } else if (activeSpriteShader != null && activeSpriteShader.getShaderProgram().isCompiled()) {
            batch.setShader(activeSpriteShader.getShaderProgram());
            activeSpriteShader.applyUniformsToShader();
        } else if (shader != null) {
            batch.setShader(shader);
        }

        if (isLookVisible() && this.getDrawable() != null) {
            if (hasCornerOffsets && getDrawable() instanceof TextureRegionDrawable) {
                TextureRegion region = ((TextureRegionDrawable) getDrawable()).getRegion();
                Texture texture = region.getTexture();

                float x = getX();
                float y = getY();
                float width = getWidth() * getScaleX();
                float height = getHeight() * getScaleY();

                float ox = getOriginX();
                float oy = getOriginY();

                float x0 = x + bottomLeftOffsetX;
                float y0 = y + bottomLeftOffsetY;

                float x1 = x + topLeftOffsetX;
                float y1 = y + height + topLeftOffsetY;

                float x2 = x + width + topRightOffsetX;
                float y2 = y + height + topRightOffsetY;

                float x3 = x + width + bottomRightOffsetX;
                float y3 = y + bottomRightOffsetY;

                if (getRotation() != 0) {
                    float cos = com.badlogic.gdx.math.MathUtils.cosDeg(getRotation());
                    float sin = com.badlogic.gdx.math.MathUtils.sinDeg(getRotation());
                    float centerX = x + ox;
                    float centerY = y + oy;

                    float rx0 = x0 - centerX; float ry0 = y0 - centerY;
                    x0 = (rx0 * cos - ry0 * sin) + centerX;
                    y0 = (rx0 * sin + ry0 * cos) + centerY;

                    float rx1 = x1 - centerX; float ry1 = y1 - centerY;
                    x1 = (rx1 * cos - ry1 * sin) + centerX;
                    y1 = (rx1 * sin + ry1 * cos) + centerY;

                    float rx2 = x2 - centerX; float ry2 = y2 - centerY;
                    x2 = (rx2 * cos - ry2 * sin) + centerX;
                    y2 = (rx2 * sin + ry2 * cos) + centerY;

                    float rx3 = x3 - centerX; float ry3 = y3 - centerY;
                    x3 = (rx3 * cos - ry3 * sin) + centerX;
                    y3 = (rx3 * sin + ry3 * cos) + centerY;
                }

                float u = region.getU();
                float v = region.getV2();
                float u2 = region.getU2();
                float v2 = region.getV();

                float color = batch.getPackedColor();

                cornerOffsetsVerts[0] = x0;  cornerOffsetsVerts[1] = y0;  cornerOffsetsVerts[2] = color;  cornerOffsetsVerts[3] = u;   cornerOffsetsVerts[4] = v;
                cornerOffsetsVerts[5] = x1;  cornerOffsetsVerts[6] = y1;  cornerOffsetsVerts[7] = color;  cornerOffsetsVerts[8] = u;   cornerOffsetsVerts[9] = v2;
                cornerOffsetsVerts[10] = x2; cornerOffsetsVerts[11] = y2; cornerOffsetsVerts[12] = color; cornerOffsetsVerts[13] = u2;  cornerOffsetsVerts[14] = v2;
                cornerOffsetsVerts[15] = x3; cornerOffsetsVerts[16] = y3; cornerOffsetsVerts[17] = color; cornerOffsetsVerts[18] = u2;  cornerOffsetsVerts[19] = v;

                batch.draw(texture, cornerOffsetsVerts, 0, 20);
            } else {
                super.draw(batch, this.alpha);
            }
        }

        batch.setShader(null);
    }

	public static void tickGlobalFrame() {
		globalFrameTicker++;
	}

	private TextureRegionDrawable getOrUpdateAnimationDrawable(TextureRegion currentFrame) {
		if (animationDrawable == null) {
			animationDrawable = new TextureRegionDrawable(currentFrame);
		} else {
			animationDrawable.setRegion(currentFrame);
		}
		return animationDrawable;
	}

	@Override
	public void act(float delta) {
		scheduler.tick(delta);
        if (activeGifAnimation != null) {
            gifStateTime += delta;
            TextureRegion currentFrame = activeGifAnimation.getKeyFrame(gifStateTime, true);
            setDrawable(getOrUpdateAnimationDrawable(currentFrame));
        } else if (activeSheetAnimation != null) {
            sheetStateTime += delta;
            TextureRegion currentFrame = activeSheetAnimation.getKeyFrame(sheetStateTime, true);
            setDrawable(getOrUpdateAnimationDrawable(currentFrame));
        }
		if (sprite != null) {
			if (myUpdateBucket == globalFrameTicker % UPDATE_BUCKETS) {
				sprite.runningStitch.update();
				sprite.evaluateConditionScriptTriggers();
			}
		}
	}

	@Override
	protected void positionChanged() {
		collisionDirty.set(true);
		super.positionChanged();

		if (sprite != null && sprite.penConfiguration != null && sprite.penConfiguration.isPenDown()
				&& !simultaneousMovementXY) {
			float x = getXInUserInterfaceDimensionUnit();
			float y = getYInUserInterfaceDimensionUnit();
			sprite.penConfiguration.addPosition(new PointF(x, y));
		}
		if (sprite != null && sprite.plot != null && sprite.plot.isPlotting()
				&& !simultaneousMovementXY) {
			float x = getXInUserInterfaceDimensionUnit();
			float y = getYInUserInterfaceDimensionUnit();
			sprite.plot.addPoint(new PointF(x, y));
		}
	}

	public void startThread(ScriptSequenceAction sequenceAction) {
		if (scheduler != null) {
			scheduler.startThread(sequenceAction);
		}
	}

	public void stopThreads(Array<Action> threads) {
		if (scheduler != null) {
			scheduler.stopThreads(threads);
		}
	}

	public void stopThreadWithScript(Script script) {
		if (scheduler != null) {
			scheduler.stopThreadsWithScript(script);
		}
	}

	public void setSchedulerState(@ThreadScheduler.SchedulerState int state) {
		scheduler.setState(state);
	}

	@Override
	protected void rotationChanged() {
		collisionDirty.set(true);
		super.rotationChanged();
	}

	@Override
	protected void sizeChanged() {
		collisionDirty.set(true);
		super.sizeChanged();
	}

	public synchronized void refreshTextures(boolean refreshShader) {
		if (lookData == null) {
			setBounds(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0f, 0f);
			setDrawable(null);
			return;
		}
		pixmap = lookData.getPixmap();
		if (pixmap != null) {
			float newX = getX() - (pixmap.getWidth() - getWidth()) / 2f;
			float newY = getY() - (pixmap.getHeight() - getHeight()) / 2f;
			setSize(pixmap.getWidth(), pixmap.getHeight());
			setPosition(newX, newY);
			setOrigin(getWidth() / 2f, getHeight() / 2f);
			TextureRegion region = lookData.getTextureRegion();
			TextureRegionDrawable drawable = new TextureRegionDrawable(region);
			setDrawable(drawable);
			flipLookDataIfNeeded(getRotationMode());
			if (refreshShader) {
				refreshShader();
			}
		}
	}

	private void refreshShader() {
		createShaderIfNotExisting();
		shader.setBrightness(brightness);
		shader.setHue(hue);
	}

	public synchronized LookData getLookData() {
		return lookData;
	}

	public synchronized LookData getLookData2() {
		return lookData2;
	}

    public synchronized void setLookData(LookData lookData) {
        if (this.lookData != lookData) {
            this.lookData = lookData;
            this.lookData2 = lookData;
            collisionDirty.set(true);
            refreshTextures(false);

            if (sprite != null && sprite.getLookList() != null && sprite.getLookList().size() > 100) {
                int size = sprite.getLookList().size();
                int currentIndex = sprite.getLookList().indexOf(lookData);
                if (currentIndex != -1) {
                    for (int i = 0; i < size; i++) {
                        int diff = Math.abs(i - currentIndex);
                        diff = Math.min(diff, size - diff);
                        if (diff > 30) {
                            sprite.getLookList().get(i).dispose();
                        }
                    }
                }
            }
        }
    }

    public synchronized void setLookData2(LookData lookData) {
        if (this.lookData2 != lookData) {
            this.lookData2 = lookData;
            collisionDirty.set(true);
            refreshTextures(false);

            if (sprite != null && sprite.getLookList() != null && sprite.getLookList().size() > 100) {
                int size = sprite.getLookList().size();
                int currentIndex = sprite.getLookList().indexOf(lookData);
                if (currentIndex != -1) {
                    for (int i = 0; i < size; i++) {
                        int diff = Math.abs(i - currentIndex);
                        diff = Math.min(diff, size - diff);
                        if (diff > 30) {
                            sprite.getLookList().get(i).dispose();
                        }
                    }
                }
            }
        }
    }

	public boolean haveAllThreadsFinished() {
		return scheduler.haveAllThreadsFinished();
	}

	public synchronized String getImagePath() {
		String path;
		if (this.lookData == null) {
			path = "";
		} else {
			path = this.lookData.getFile().getAbsolutePath();
		}
		return path;
	}

	private final transient Vector3 tempVec3 = new Vector3();

	public float getXInUserInterfaceDimensionUnit() {
		if (isPinnedToCamera()) {
			tempVec3.set(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0);
			if (gameCamera != null) {
				gameCamera.unproject(tempVec3);
			}
			return tempVec3.x;
		}
		return getX() + getWidth() / 2f;
	}

	public float getYInUserInterfaceDimensionUnit() {
		if (isPinnedToCamera()) {
			tempVec3.set(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0);
			if (gameCamera != null) {
				gameCamera.unproject(tempVec3);
			}
			return tempVec3.y;
		}
		return getY() + getHeight() / 2f;
	}

	public void setXInUserInterfaceDimensionUnit(float worldX) {
		if (isPinnedToCamera()) {
			float currentWorldY = getYInUserInterfaceDimensionUnit();
			tempVec3.set(worldX, currentWorldY, 0);
			if (gameCamera != null) {
				gameCamera.project(tempVec3);
			}
			setX(tempVec3.x - getWidth() / 2f);
		} else {
			setX(worldX - getWidth() / 2f);
		}
	}

	public void setYInUserInterfaceDimensionUnit(float worldY) {
		if (isPinnedToCamera()) {
			float currentWorldX = getXInUserInterfaceDimensionUnit();
			tempVec3.set(currentWorldX, worldY, 0);
			if (gameCamera != null) {
				gameCamera.project(tempVec3);
			}
			setY(tempVec3.y - getHeight() / 2f);
		} else {
			setY(worldY - getHeight() / 2f);
		}
	}

	public void setPositionInUserInterfaceDimensionUnit(float worldX, float worldY) {
		if (isPinnedToCamera()) {
			tempVec3.set(worldX, worldY, 0);
			if (gameCamera != null) {
				gameCamera.project(tempVec3);
			}
			setPosition(tempVec3.x - getWidth() / 2f, tempVec3.y - getHeight() / 2f);
		} else {
			setPosition(worldX - getWidth() / 2f, worldY - getHeight() / 2f);
		}
	}

	public float getDistanceToTouchPositionInUserInterfaceDimensions() {
		int touchIndex = TouchUtil.getLastTouchIndex();

		float dx = TouchUtil.getX(touchIndex) - getXInUserInterfaceDimensionUnit();
		float dy = TouchUtil.getY(touchIndex) - getYInUserInterfaceDimensionUnit();

		return (float) Math.hypot(dx, dy);
	}

	public float getAngularVelocityInUserInterfaceDimensionUnit() {
		// only available in physicsLook
		return 0;
	}

	public float getXVelocityInUserInterfaceDimensionUnit() {
		if (sprite.isGliding()) {
			return sprite.getGlidingVelocityX();
		}
		return 0;
	}

	public float getYVelocityInUserInterfaceDimensionUnit() {
		if (sprite.isGliding()) {
			return sprite.getGlidingVelocityY();
		}
		return 0;
	}

	@Override
	public void setPosition(float x, float y) {
		if (getX() != x || getY() != y) {
			super.setPosition(x, y);
		}
	}

	@Override
	public void setX(float x) {
		super.setX(x);
	}

	@Override
	public void setY(float y) {
		super.setY(y);
	}

	@Override
	public void setRotation(float degrees) {
		super.setRotation(degrees);
	}

	@Override
	public void setScale(float scaleXY) {
		if (getScaleX() != scaleXY || getScaleY() != scaleXY) {
			super.setScale(scaleXY);
		}
	}

	@Override
	public void setScale(float scaleX, float scaleY) {
		if (getScaleX() != scaleX || getScaleY() != scaleY) {
			super.setScale(scaleX, scaleY);
		}
	}

	@Override
	public void setScaleX(float scaleX) {
		if (getScaleX() != scaleX) {
			super.setScaleX(scaleX);
		}
	}

	@Override
	public void setScaleY(float scaleY) {
		if (getScaleY() != scaleY) {
			super.setScaleY(scaleY);
		}
	}

	private void adjustSimultaneousMovementXY(float x, float y) {
		simultaneousMovementXY = x != getXInUserInterfaceDimensionUnit() && y != getYInUserInterfaceDimensionUnit();
	}

	public void changeXInUserInterfaceDimensionUnit(float changeX) {
		setX(getX() + changeX);
	}

	public void changeYInUserInterfaceDimensionUnit(float changeY) {

		setY(getY() + changeY);
	}

	public void changePositionInInterfaceDimensionUnit(float changeX, float changeY){
		setPosition(getX() + changeX, getY() + changeY);
	}

	public float getWidthInUserInterfaceDimensionUnit() {
		return getWidth() * width;
	}

	public float getHeightInUserInterfaceDimensionUnit() {
		return getHeight() * height;
	}

	public float getMotionDirectionInUserInterfaceDimensionUnit() {
		return realRotation;
	}

	public float getLookDirectionInUserInterfaceDimensionUnit() {
		float direction = 0f;
		switch (rotationMode) {
			case ROTATION_STYLE_NONE : direction = DEGREE_UI_OFFSET;
			break;
			case ROTATION_STYLE_ALL_AROUND : direction = realRotation;
			break;
			case ROTATION_STYLE_LEFT_RIGHT_ONLY : direction =
					isFlipped() ? -DEGREE_UI_OFFSET : DEGREE_UI_OFFSET;
		}
		return direction;
	}

	public void setRotationMode(int mode) {
		rotationMode = mode;
		flipLookDataIfNeeded(mode);
	}

	private void flipLookDataIfNeeded(int mode) {
		boolean orientedLeft = getMotionDirectionInUserInterfaceDimensionUnit() < 0;
		boolean differentModeButFlipped = mode != ROTATION_STYLE_LEFT_RIGHT_ONLY && isFlipped();
		boolean facingWrongDirection = mode == ROTATION_STYLE_LEFT_RIGHT_ONLY && (orientedLeft ^ isFlipped());
		if (differentModeButFlipped || facingWrongDirection) {
			getLookData().getTextureRegion().flip(true, false);
            if (lookData2 != null && lookData2 != lookData) {
                lookData2.getTextureRegion().flip(true, false);
            }
		}
	}

	public int getRotationMode() {
		return rotationMode;
	}

	private PointF rotatePointAroundPoint(PointF center, PointF point, float rotation) {
		float sin = (float) Math.sin(rotation);
		float cos = (float) Math.cos(rotation);
		point.x -= center.x;
		point.y -= center.y;
		float xNew = point.x * cos - point.y * sin;
		float yNew = point.x * sin + point.y * cos;
		point.x = xNew + center.x;
		point.y = yNew + center.y;
		return point;
	}

    public Rectangle getHitbox() {
        float x = getXInUserInterfaceDimensionUnit() - getWidthInUserInterfaceDimensionUnit() / 2;
        float y = getYInUserInterfaceDimensionUnit() - getHeightInUserInterfaceDimensionUnit() / 2;
        float w = getWidthInUserInterfaceDimensionUnit();
        float h = getHeightInUserInterfaceDimensionUnit();

        hitboxVertices[0] = x;     hitboxVertices[1] = y;
        hitboxVertices[2] = x;     hitboxVertices[3] = y + h;
        hitboxVertices[4] = x + w; hitboxVertices[5] = y + h;
        hitboxVertices[6] = x + w; hitboxVertices[7] = y;

        hitboxPolygon.setVertices(hitboxVertices);
        hitboxPolygon.setPosition(0, 0);
        hitboxPolygon.setOrigin(x + w / 2f, y + h / 2f);
        hitboxPolygon.setRotation(getRotation());

        return hitboxPolygon.getBoundingRectangle();
    }

    public synchronized void setLookDataKeepHitbox(LookData newLookData) {
        if (this.lookData != newLookData) {
            if (this.lookData2 == null) {
                this.lookData2 = this.lookData;
            }
            this.lookData = newLookData;
            collisionDirty.set(true);
            refreshTextures(false);

            if (sprite != null && sprite.getLookList() != null && sprite.getLookList().size() > 100) {
                int size = sprite.getLookList().size();
                int currentIndex = sprite.getLookList().indexOf(newLookData);
                if (currentIndex != -1) {
                    for (int i = 0; i < size; i++) {
                        int diff = Math.abs(i - currentIndex);
                        diff = Math.min(diff, size - diff);
                        if (diff > 30) {
                            sprite.getLookList().get(i).dispose();
                        }
                    }
                }
            }
        }
    }
	public void setMotionDirectionInUserInterfaceDimensionUnit(float degrees) {
		rotation = (-degrees + DEGREE_UI_OFFSET) % 360;
		realRotation = convertStageAngleToCatroidAngle(rotation);

		switch (rotationMode) {
			case ROTATION_STYLE_LEFT_RIGHT_ONLY:
				setRotation(0f);
				boolean orientedRight = realRotation >= 0;
				boolean orientedLeft = realRotation < 0;
				boolean needsFlipping = (isFlipped() && orientedRight) || (!isFlipped() && orientedLeft);
				if (needsFlipping && lookData != null) {
					lookData.getTextureRegion().flip(true, false);
                    if (lookData2 != null && lookData2 != lookData) {
                        lookData2.getTextureRegion().flip(true, false);
                    }
				}
				break;
			case ROTATION_STYLE_ALL_AROUND:
				setRotation(rotation);
				break;
			case ROTATION_STYLE_NONE:
				setRotation(0f);
				break;
		}
	}

	public boolean isFlipped() {
		return (lookData != null && lookData.getTextureRegion().isFlipX());
	}

	public void changeDirectionInUserInterfaceDimensionUnit(float changeDegrees) {
		setMotionDirectionInUserInterfaceDimensionUnit(
				(getMotionDirectionInUserInterfaceDimensionUnit() + changeDegrees) % 360);
	}

	public float getSizeInUserInterfaceDimensionUnit() {
		return getScaleX() * 100f;
	}

	public void setSizeInUserInterfaceDimensionUnit(float percent) {
		height = percent / 100f;
		width = percent / 100f;
		setScale(percent / 100f, percent / 100f);
	}

	public void SetSizeX(float percent) {
		setScale(percent / 100f, getScaleY());
	}

	public void SetSizeY(float percent) {
		setScale(getScaleX(), percent / 100f);
	}

	public void changeSizeInUserInterfaceDimensionUnit(float changePercent) {
		setSizeInUserInterfaceDimensionUnit(getSizeInUserInterfaceDimensionUnit() + changePercent);
	}

	public float getTransparencyInUserInterfaceDimensionUnit() {
		return (1f - alpha) * 100f;
	}

	public void setTransparencyInUserInterfaceDimensionUnit(float percent) {
		if (percent < 100.0f) {
			if (percent < 0f) {
				percent = 0f;
			}
			setVisible(true);
		} else {
			percent = 100f;
			setVisible(false);
		}

		alpha = (100f - percent) / 100f;
	}

	public void changeTransparencyInUserInterfaceDimensionUnit(float changePercent) {
		setTransparencyInUserInterfaceDimensionUnit(getTransparencyInUserInterfaceDimensionUnit() + changePercent);
	}

	public float getBrightnessInUserInterfaceDimensionUnit() {
		return brightness * 100f;
	}

	public synchronized void setBrightnessInUserInterfaceDimensionUnit(float percent) {
		if (percent < 0f) {
			percent = 0f;
		} else if (percent > 200f) {
			percent = 200f;
		}

		brightness = percent / 100f;
		useCustomShader = (brightness != 1.0f || hue != 0.0f);
		refreshTextures(true);
	}

	public void changeBrightnessInUserInterfaceDimensionUnit(float changePercent) {
		setBrightnessInUserInterfaceDimensionUnit(getBrightnessInUserInterfaceDimensionUnit() + changePercent);
	}

	public float getColorInUserInterfaceDimensionUnit() {
		return hue * COLOR_SCALE;
	}

	public synchronized void setColorInUserInterfaceDimensionUnit(float val) {
		val = val % COLOR_SCALE;
		if (val < 0) {
			val = COLOR_SCALE + val;
		}
		hue = val / COLOR_SCALE;
		useCustomShader = (brightness != 1.0f || hue != 0.0f);
		refreshTextures(true);
	}

	private void createShaderIfNotExisting() {
		if (shader == null) {
			createBrightnessContrastHueShader();
		}
	}

	public void changeColorInUserInterfaceDimensionUnit(float val) {
		setColorInUserInterfaceDimensionUnit(getColorInUserInterfaceDimensionUnit() + val);
	}

	private boolean isAngleInCatroidInterval(float catroidAngle) {
		return (catroidAngle > -180 && catroidAngle <= 180);
	}

	public boolean needsCustomShader() {
		return useCustomShader;
	}

	public float getBrightnessValue() {
		return brightness;
	}

	public float getHueValue() {
		return hue;
	}

	public void applyShaderParameters(ShaderProgram customShader) {
		if (customShader instanceof BrightnessContrastHueShader) {
			((BrightnessContrastHueShader)customShader).setBrightness(brightness);
			((BrightnessContrastHueShader)customShader).setHue(hue);
		}
	}

	public float breakDownCatroidAngle(float catroidAngle) {
		catroidAngle = catroidAngle % 360;
		if (catroidAngle >= 0 && !isAngleInCatroidInterval(catroidAngle)) {
			return catroidAngle - 360;
		} else if (catroidAngle < 0 && !isAngleInCatroidInterval(catroidAngle)) {
			return catroidAngle + 360;
		}
		return catroidAngle;
	}

	public float convertCatroidAngleToStageAngle(float catroidAngle) {
		catroidAngle = breakDownCatroidAngle(catroidAngle);
		return -catroidAngle + DEGREE_UI_OFFSET;
	}

	public float convertStageAngleToCatroidAngle(float stageAngle) {
		float catroidAngle = -stageAngle + DEGREE_UI_OFFSET;
		return breakDownCatroidAngle(catroidAngle);
	}

	public static class BrightnessContrastHueShader extends ShaderProgram {

		private static final String VERTEX_SHADER = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
				+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" + "attribute vec2 "
				+ ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" + "uniform mat4 u_projTrans;\n" + "varying vec4 v_color;\n"
				+ "varying vec2 v_texCoords;\n" + "\n" + "void main()\n" + "{\n" + " v_color = "
				+ ShaderProgram.COLOR_ATTRIBUTE + ";\n" + " v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
				+ " gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" + "}\n";
		private static final String FRAGMENT_SHADER = "#ifdef GL_ES\n"
				+ "    #define LOWP lowp\n"
				+ "    precision mediump float;\n"
				+ "#else\n"
				+ "    #define LOWP\n"
				+ "#endif\n"
				+ "varying LOWP vec4 v_color;\n"
				+ "varying vec2 v_texCoords;\n"
				+ "uniform sampler2D u_texture;\n"
				+ "uniform float brightness;\n"
				+ "uniform float contrast;\n"
				+ "uniform float hue;\n"
				+ "vec3 rgb2hsv(vec3 c)\n"
				+ "{\n"
				+ "    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n"
				+ "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n"
				+ "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n"
				+ "    float d = q.x - min(q.w, q.y);\n"
				+ "    float e = 1.0e-10;\n"
				+ "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n"
				+ "}\n"
				+ "vec3 hsv2rgb(vec3 c)\n"
				+ "{\n"
				+ "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n"
				+ "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n"
				+ "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n"
				+ "}\n"
				+ "void main()\n"
				+ "{\n"
				+ "    vec4 color = v_color * texture2D(u_texture, v_texCoords);\n"
				+ "    color.rgb /= color.a;\n"
				+ "    color.rgb = ((color.rgb - 0.5) * max(contrast, 0.0)) + 0.5;\n"
				+ "    color.rgb += brightness;\n"
				+ "    color.rgb *= color.a;\n"
				+ "    vec3 hsv = rgb2hsv(color.rgb);\n"
				+ "    hsv.x += hue;\n"
				+ "    vec3 rgb = hsv2rgb(hsv);\n"
				+ "    gl_FragColor = vec4(rgb.r, rgb.g, rgb.b, color.a);\n"
				+ " }";

		private static final String BRIGHTNESS_STRING_IN_SHADER = "brightness";
		private static final String CONTRAST_STRING_IN_SHADER = "contrast";
		private static final String HUE_STRING_IN_SHADER = "hue";

		public BrightnessContrastHueShader() {
			super(VERTEX_SHADER, FRAGMENT_SHADER);
			ShaderProgram.pedantic = false;
			if (isCompiled()) {
				begin();
				setUniformf(BRIGHTNESS_STRING_IN_SHADER, 0.0f);
				setUniformf(CONTRAST_STRING_IN_SHADER, 1.0f);
				setUniformf(HUE_STRING_IN_SHADER, 0.0f);
				end();
			}
		}

		public void setBrightness(float brightness) {
			begin();
			setUniformf(BRIGHTNESS_STRING_IN_SHADER, brightness - 1f);
			end();
		}

		public void setHue(float hue) {
			begin();
			setUniformf(HUE_STRING_IN_SHADER, hue);
			end();
		}
	}
	private Polygon[] cachedCollisionPolygons = null;

    public Polygon[] getCurrentCollisionPolygon() {
        if (cachedCollisionPolygons != null && !collisionDirty.get()) {
            return cachedCollisionPolygons;
        }

        Polygon[] originalPolygons;
        if (getLookData2() == null) {
            originalPolygons = new Polygon[0];
        } else {
            CollisionInformation collisionInformation = getLookData2().getCollisionInformation();
            if (collisionInformation.collisionPolygons == null) {
                collisionInformation.loadCollisionPolygon();
            }
            originalPolygons = collisionInformation.collisionPolygons;
        }

        if (cachedCollisionPolygons == null || cachedCollisionPolygons.length != originalPolygons.length) {
            cachedCollisionPolygons = new Polygon[originalPolygons.length];
            for (int i = 0; i < originalPolygons.length; i++) {
                cachedCollisionPolygons[i] = new Polygon(originalPolygons[i].getVertices());
            }
        }

        for (int p = 0; p < cachedCollisionPolygons.length; p++) {
            Polygon poly = cachedCollisionPolygons[p];
            poly.setVertices(originalPolygons[p].getVertices());
            poly.setPosition(getX(), getY());
            poly.setRotation(getRotation());
            poly.setScale(getScaleX(), getScaleY());
            poly.setOrigin(getOriginX(), getOriginY());
        }

        collisionDirty.set(false);
        return cachedCollisionPolygons;
    }

	void notifyAllWaiters() {
		for (Action action : getActions()) {
			if (action instanceof ScriptSequenceActionWithWaiter) {
				((ScriptSequenceActionWithWaiter) action).notifyWaiter();
			}
		}
	}

	public float getAlpha() {
		return alpha;
	}

	@VisibleForTesting
	public float getBrightness() {
		return brightness;
	}
}
