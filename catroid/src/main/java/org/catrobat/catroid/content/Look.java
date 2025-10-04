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

import android.graphics.PointF;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ThreadScheduler;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.ScriptSequenceActionWithWaiter;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.physics.ParticleConstants;
import org.catrobat.catroid.sensing.CollisionInformation;
import org.catrobat.catroid.utils.NativeLookOptimizer;
import org.catrobat.catroid.utils.TouchUtil;

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

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ROTATION_STYLE_LEFT_RIGHT_ONLY, ROTATION_STYLE_ALL_AROUND, ROTATION_STYLE_NONE})
	public @interface RotationStyle {}
	public static final int ROTATION_STYLE_LEFT_RIGHT_ONLY = 0;
	public static final int ROTATION_STYLE_ALL_AROUND = 1;
	public static final int ROTATION_STYLE_NONE = 2;

	public static final float DEGREE_UI_OFFSET = 90.0f;
	private static final float COLOR_SCALE = 200.0f;

	private final Rectangle cachedHitbox = new Rectangle();

	private static int globalFrameTicker = 0;
	private static final int UPDATE_BUCKETS = 4; // Делим все объекты на 4 группы
	private int myUpdateBucket = -1; // Уникальная "группа" для этого объекта

	// --- Поля для кэширования полигонов ---
	private Polygon[] cachedTransformedPolygons = null;
	// Используем AtomicBoolean для базовой потокобезопасности при доступе к флагу
	private final AtomicBoolean collisionDirty = new AtomicBoolean(true);
	private LookData lastUsedLookDataForCache = null;
	// ----------------------------------------

	private boolean assumesConvexPolygons = true;
	private boolean lookVisible = true;
	private boolean simultaneousMovementXY = false;
	private int lookListIndexBeforeLookRequest = -1;
	protected LookData lookData;

	private BrightnessContrastHueShader shader;
	// Добавьте флаг, чтобы понимать, нужен ли объекту вообще особый шейдер
	private boolean useCustomShader = false;
	public LookData lookData2 = null;
	public Sprite sprite;
	protected float alpha = 1f;
	protected float brightness = 1f;
	protected float hue = 0f;

	protected float height = 1f;
	protected float width = 1f;
	protected Pixmap pixmap;
	private int rotationMode = ROTATION_STYLE_ALL_AROUND;
	private float rotation = 90f;
	private float realRotation = rotation;
	private ThreadScheduler scheduler;
	private ParticleEffect particleEffect;

	public boolean hasParticleEffect = false;
	public boolean isAdditive = true;

	private boolean isParticleEffectPaused = false;

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

	// В Look.java
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

				// Мировые координаты клика
				float stageX = event.getStageX();
				float stageY = event.getStageY();

				// Проверяем попадание по полигонам
				Polygon[] collisionPolygons = getCurrentCollisionPolygon();
				for (Polygon poly : collisionPolygons) {
					if (poly.contains(stageX, stageY)) {
						// Попали!
						EventWrapper e = new EventWrapper(new EventId(EventId.TAP), false);
						sprite.look.fire(e);
						return true; // Обработали событие, возвращаем true
					}
				}

				// Если не попали ни в один полигон, передаем событие дальше
				setTouchable(Touchable.disabled);
				Actor target = getParent().hit(stageX, stageY, true);
				if (target != null) {
					target.fire(event);
					target.fire(event);
				}
				setTouchable(Touchable.enabled);

				// ВОТ ИСПРАВЛЕНИЕ: Нужно вернуть false, если мы не обработали событие
				return false;
			}
		});
		this.addListener(new EventWrapperListener(this));
	}

	public void setAssumesConvexPolygons(boolean convex) {
		this.assumesConvexPolygons = convex;
		// Примечание: Если бы LookData сам содержал флаг выпуклости,
		// можно было бы автоматически устанавливать assumesConvexPolygons
		// при вызове setLookData/setLookData2.
		// Сейчас ответственность лежит на том, кто управляет объектом Look.
	}

	/**
	 * @return true, если полигоны этого Look считаются выпуклыми (по умолчанию true).
	 */
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

	@Override
	public boolean remove() {
		notifyAllWaiters();
		setLookVisible(false);
		boolean returnValue = super.remove();
		for (EventListener listener : this.getListeners()) {
			this.removeListener(listener);
		}
		getActions().clear();
		scheduler = null;
		this.sprite = null;
		this.lookData = null;
		return returnValue;
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

		// Старый, медленный способ:
		// if (isFlipped()) { x = (getWidth() - 1) - x; }
		// y = (getHeight() - 1) - y;
		// if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()
		//         && ((pixmap != null && ((pixmap.getPixel((int) x, (int) y) & 0x000000FF) > 10)))) { ... }

		// Новый, быстрый способ:
		// x и y здесь - это локальные координаты внутри Actor'а.
		// Нам нужно проверить, лежит ли эта точка внутри одного из полигонов.
		Polygon[] polygons = getCurrentCollisionPolygon(); // Он уже кэшированный!

		// Но getCurrentCollisionPolygon возвращает полигоны в мировых координатах.
		// Нам нужно получить мировые координаты клика. Их нам дает InputEvent.
		// Поэтому менять нужно не doTouchDown, а логику в InputListener.

		// --- ПРАВИЛЬНЫЙ ПОДХОД ---
		// В методе addListeners()
		this.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (!isLookVisible()) {
					return false;
				}

				// event.getStageX() и event.getStageY() - это мировые координаты клика
				Polygon[] collisionPolygons = getCurrentCollisionPolygon(); // Получаем кэшированные полигоны
				for (Polygon poly : collisionPolygons) {
					if (poly.contains(event.getStageX(), event.getStageY())) {
						// Попали! Запускаем событие.
						EventWrapper e = new EventWrapper(new EventId(EventId.TAP), false);
						sprite.look.fire(e);
						return true; // Обработали нажатие
					}
				}

				// Если не попали, передаем событие дальше, как и раньше
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

	/*@Override
	public synchronized void draw(Batch batch, float parentAlpha) {
		if (!isParticleEffectPaused) {
			if (hasParticleEffect) {
				fadeInParticles();
			} else {
				if (particleEffect != null) {
					fadeOutParticles();
				}
			}
		}

		if (particleEffect != null) {
			particleEffect.draw(batch);
		}

		batch.setShader(shader);
		super.setVisible(alpha != 0.0f);

		if (isLookVisible() && this.getDrawable() != null) {
			super.draw(batch, this.alpha);
		}
		batch.setShader(null);
	}*/

	// в файле Look.java

	@Override
	public synchronized void draw(Batch batch, float parentAlpha) {
		// !!! ВАЖНО: ЗАМЕНИТЕ "Sprite1" НА ИМЯ ВАШЕГО РЕАЛЬНОГО СПРАЙТА !!!
		boolean shouldLog = false;

		if (shouldLog) {
			Log.d("ShaderDebug", "    [Draw] >>> Drawing Look for: " + sprite.getName());
		}

		// Логика частиц и смешивания (оставляем исправление на всякий случай)
		if (particleEffect != null) {
			if (shouldLog) Log.d("ShaderDebug", "    [Draw] Drawing particle effect.");
			particleEffect.draw(batch);
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		}

		// Проверка видимости
		if (!isLookVisible() || getDrawable() == null) {
			if (shouldLog) {
				Log.w("ShaderDebug", "    [Draw] Look is NOT drawn. isLookVisible: " + isLookVisible() + ", getDrawable() is null: " + (getDrawable() == null));
			}
			return; // Не рисуем, если невидимый или нет текстуры
		}

		if (shouldLog) {
			Log.d("ShaderDebug", "    [Draw] Look is visible and has drawable. Alpha: " + this.alpha);
			Log.d("ShaderDebug", "    [Draw] Batch blend func (SRC): " + batch.getBlendSrcFunc() + ", (DST): " + batch.getBlendDstFunc());
			Log.d("ShaderDebug", "    [Draw] Position (X,Y): " + getX() + "," + getY() + " | Size (W,H): " + getWidth() + "," + getHeight());
		}

		// Рисуем сам объект
		super.setVisible(alpha != 0.0f);
		batch.setShader(shader);
		super.setVisible(alpha != 0.0f);

		if (isLookVisible() && this.getDrawable() != null) {
			super.draw(batch, this.alpha);
		}
		batch.setShader(null);
		/*TextureRegionDrawable trd = (TextureRegionDrawable) getDrawable();
		if (trd != null && trd.getRegion().getTexture() != null) {
			TextureRegion region = trd.getRegion();
			batch.setColor(1f, 1f, 1f, this.alpha); // Устанавливаем цвет
			batch.draw(
					region,
					getX(), getY(),
					getOriginX(), getOriginY(),
					getWidth(), getHeight(),
					getScaleX(), getScaleY(),
					getRotation()
			);
			Log.d("ShaderDebug", "    [Draw] >>> MANUAL DRAW <<<");
		}*/


		if (shouldLog) {
			Log.d("ShaderDebug", "    [Draw] <<< super.draw() called.");
		}

		Drawable drawable = getDrawable();
		if (drawable != null) {
			if (shouldLog) {
				Log.d("ShaderDebug", "    [Draw] Drawable class: " + drawable.getClass().getSimpleName());
			}
			if (drawable instanceof TextureRegionDrawable) {
				TextureRegion region = ((TextureRegionDrawable) drawable).getRegion();
				if (shouldLog) {
					Log.d("ShaderDebug", "    [Draw] TextureRegion: " + region);
					Log.d("ShaderDebug", "    [Draw] Texture is null: " + (region.getTexture() == null));
				}
			}
		}

	}

	public static void tickGlobalFrame() {
		globalFrameTicker++;
	}

	@Override
	public void act(float delta) {
		scheduler.tick(delta); // Базовую логику оставляем
		if (sprite != null) {
			// ▼▼▼ НАЧАЛО ИЗМЕНЕНИЙ ▼▼▼
			// Проверяем, настал ли наш черед обновляться
			if (myUpdateBucket == globalFrameTicker % UPDATE_BUCKETS) {
				// Да, наша очередь! Выполняем тяжелую логику.
				sprite.runningStitch.update();
				sprite.evaluateConditionScriptTriggers();
			}
			// ▲▲▲ КОНЕЦ ИЗМЕНЕНИЙ ▲▲▲
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
		collisionDirty.set(true); // Поворот изменился, кэш неактуален
		super.rotationChanged(); // Вызов родительского метода (хотя в Image он пуст)
	}

	@Override
	protected void sizeChanged() {
		collisionDirty.set(true); // Размер/масштаб изменился, кэш неактуален
		super.sizeChanged(); // Вызов родительского метода (хотя в Image он пуст)
		// Обновляем origin, так как он зависит от размера
		setOrigin(getWidth() / 2f, getHeight() / 2f);
	}

	public synchronized void refreshTextures(boolean refreshShader) {
		if(lookData2 != null) {
			if (lookData == null) {
				setBounds(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0f, 0f);
				setDrawable(null);
				return;
			}
			pixmap = lookData2.getPixmap();
			//Pixmap pixmap2 = lookData2.getPixmap();
			if (pixmap != null) {
				float newX = getX() - (pixmap.getWidth() - getWidth()) / 2f;
				float newY = getY() - (pixmap.getHeight() - getHeight()) / 2f;
				setSize(pixmap.getWidth(), pixmap.getHeight());
				setPosition(newX, newY);
				setOrigin(getWidth() / 2f, getHeight() / 2f);
				TextureRegion region = lookData2.getTextureRegion();
				TextureRegionDrawable drawable = new TextureRegionDrawable(region);
				setDrawable(drawable);
				flipLookDataIfNeeded(getRotationMode());
				if (refreshShader) {
					refreshShader();
				}
			}
		} else {
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
	}

	private void refreshShader() {
		createShaderIfNotExisting();
		shader.setBrightness(brightness);
		shader.setHue(hue);
	}

	public synchronized LookData getLookData() {
		return lookData;
	}

	public synchronized void setLookData(LookData lookData) {
		if (this.lookData != lookData) {
			this.lookData = lookData;
			collisionDirty.set(true); // Сменили lookData, кэш неактуален
			refreshTextures(false); // Обновляем текстуру и размер/origin
		}
	}

	public synchronized void setLookData2(LookData lookData) {
		if (this.lookData2 != lookData) {
			this.lookData2 = lookData;
			collisionDirty.set(true); // Сменили lookData2, кэш неактуален
			refreshTextures(false); // Обновляем текстуру и размер/origin
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

	public float getXInUserInterfaceDimensionUnit() {
		return getX() + getWidth() / 2f;
	}

	public void setXInUserInterfaceDimensionUnit(float x) {
		setX(x - getWidth() / 2f);
	}

	public float getYInUserInterfaceDimensionUnit() {
		return getY() + getHeight() / 2f;
	}

	public void setYInUserInterfaceDimensionUnit(float y) {
		setY(y - getHeight() / 2f);
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

	public void setPositionInUserInterfaceDimensionUnit(float x, float y) {
		adjustSimultaneousMovementXY(x, y);
		setXInUserInterfaceDimensionUnit(x);
		adjustSimultaneousMovementXY(getXInUserInterfaceDimensionUnit(), y);
		setYInUserInterfaceDimensionUnit(y);
	}

	@Override
	public void setPosition(float x, float y) {
		if (getX() != x || getY() != y) {
			super.setPosition(x, y);
			// positionChanged() будет вызван автоматически libGDX
		}
	}

	@Override
	public void setX(float x) {
		/*if (getX() != x) {
			super.setX(x);
			// positionChanged() будет вызван автоматически libGDX
		}*/
		super.setX(x);
	}

	@Override
	public void setY(float y) {
		/*if (getY() != y) {
			super.setY(y);
			// positionChanged() будет вызван автоматически libGDX
		}*/
		super.setY(y);
	}

	@Override
	public void setRotation(float degrees) {
		/*if (getRotation() != degrees) {
			super.setRotation(degrees);
			// rotationChanged() будет вызван автоматически libGDX
		}*/
		super.setRotation(degrees);
	}

	@Override
	public void setScale(float scaleXY) {
		if (getScaleX() != scaleXY || getScaleY() != scaleXY) {
			super.setScale(scaleXY);
			// sizeChanged() будет вызван автоматически libGDX
		}
	}

	@Override
	public void setScale(float scaleX, float scaleY) {
		if (getScaleX() != scaleX || getScaleY() != scaleY) {
			super.setScale(scaleX, scaleY);
			// sizeChanged() будет вызван автоматически libGDX
		}
	}

	@Override
	public void setScaleX(float scaleX) {
		if (getScaleX() != scaleX) {
			super.setScaleX(scaleX);
			// sizeChanged() будет вызван автоматически libGDX
		}
	}

	@Override
	public void setScaleY(float scaleY) {
		if (getScaleY() != scaleY) {
			super.setScaleY(scaleY);
			// sizeChanged() будет вызван автоматически libGDX
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
			if (lookData2 != null) {
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
		// Вызываем статический метод из другого класса, передавая ему данные этого объекта
		float[] box = NativeLookOptimizer.getTransformedBoundingBox(
				getX(),
				getY(),
				getWidth(), // Текущий размер (уже содержит масштаб)
				getHeight(),
				getScaleX(),
				getScaleY(),
				getRotation(),
				getOriginX(),
				getOriginY()
		);
		// Обновляем существующий объект Rectangle, чтобы не создавать мусор
		cachedHitbox.set(box[0], box[1], box[2], box[3]);
		return cachedHitbox;
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
					if(lookData2 != null) {
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
		//if (percent < 0) {
		//	percent = 0;
		//}
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

	public float getBrightnessValue() { // Название изменено, чтобы не путать с getBrightness() из Actor
		return brightness;
	}

	public float getHueValue() {
		return hue;
	}
	// Этот метод будет устанавливать параметры, но не переключать шейдер в batch!
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

	// ЗАМЕНИТЕ ВЕСЬ СУЩЕСТВУЮЩИЙ КЛАСС BrightnessContrastHueShader НА ЭТОТ:
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

	/*public Polygon[] getCurrentCollisionPolygon() {
		if(lookData2 != null) {
			Polygon[] originalPolygons;
			CollisionInformation collisionInformation = lookData2.getCollisionInformation();
			if (collisionInformation.collisionPolygons == null) {
				collisionInformation.loadCollisionPolygon();
			}
			originalPolygons = collisionInformation.collisionPolygons;

			Polygon[] transformedPolygons = new Polygon[originalPolygons.length];

			for (int p = 0; p < transformedPolygons.length; p++) {
				Polygon poly = new Polygon(originalPolygons[p].getTransformedVertices());
				poly.translate(getX(), getY());
				poly.setRotation(getRotation());
				poly.setScale(getScaleX(), getScaleY());
				poly.setOrigin(getOriginX(), getOriginY());
				transformedPolygons[p] = poly;
			}
			return transformedPolygons;
		} else {
			Polygon[] originalPolygons;
			if (getLookData() == null) {
				originalPolygons = new Polygon[0];
			} else {
				CollisionInformation collisionInformation = getLookData().getCollisionInformation();
				if (collisionInformation.collisionPolygons == null) {
					collisionInformation.loadCollisionPolygon();
				}
				originalPolygons = collisionInformation.collisionPolygons;
			}

			Polygon[] transformedPolygons = new Polygon[originalPolygons.length];

			for (int p = 0; p < transformedPolygons.length; p++) {
				Polygon poly = new Polygon(originalPolygons[p].getTransformedVertices());
				poly.translate(getX(), getY());
				poly.setRotation(getRotation());
				poly.setScale(getScaleX(), getScaleY());
				poly.setOrigin(getOriginX(), getOriginY());
				transformedPolygons[p] = poly;
			}
			return transformedPolygons;
		}
	}*/

	/*public Polygon[] getCurrentCollisionPolygon() {
		// Определяем, какой LookData используется сейчас
		LookData currentLookData = (lookData2 != null) ? lookData2 : lookData;

		// Проверяем, актуален ли кэш
		// Используем compareAndSet для проверки и установки флага в одной атомарной операции,
		// чтобы избежать гонки состояний, если метод вызван из разных потоков одновременно
		// (хотя в типичном игровом цикле это маловероятно).
		// Если collisionDirty был true, он станет false, и мы пересчитаем.
		// Если был false, он останется false, и мы используем кэш (если lookData тот же).
		boolean needsRecalculation = collisionDirty.compareAndSet(true, false);

		if (!needsRecalculation && lastUsedLookDataForCache == currentLookData && cachedTransformedPolygons != null) {
			// Кэш актуален и относится к текущему LookData
			return cachedTransformedPolygons;
		}

		// --- Пересчет полигонов ---
		Polygon[] originalPolygons;
		if (currentLookData == null) {
			originalPolygons = new Polygon[0]; // Нет данных - нет полигонов
		} else {
			CollisionInformation collisionInformation = currentLookData.getCollisionInformation();
			if (collisionInformation == null) {
				// На всякий случай, если CollisionInformation может быть null
				originalPolygons = new Polygon[0];
			} else {
				if (collisionInformation.collisionPolygons == null) {
					// Загружаем полигоны, если они еще не загружены
					collisionInformation.loadCollisionPolygon();
				}
				originalPolygons = collisionInformation.collisionPolygons;
				if (originalPolygons == null) {
					// Если после загрузки все еще null
					originalPolygons = new Polygon[0];
				}
			}
		}

		// Создаем массив для трансформированных полигонов
		Polygon[] transformedPolygons = new Polygon[originalPolygons.length];

		// Получаем текущие параметры трансформации один раз
		float currentX = getX();
		float currentY = getY();
		float currentRotation = getRotation();
		float currentScaleX = getScaleX();
		float currentScaleY = getScaleY();
		float currentOriginX = getOriginX();
		float currentOriginY = getOriginY();

		for (int p = 0; p < originalPolygons.length; p++) {
			Polygon originalPoly = originalPolygons[p];
			if (originalPoly == null) {
				// Пропускаем, если исходный полигон почему-то null
				transformedPolygons[p] = null; // Или можно создать пустой new Polygon()? Зависит от ожиданий CollisionDetection.
				continue;
			}

			// Важно: Создаем НОВЫЙ полигон на основе НЕ ТРАНСФОРМИРОВАННЫХ вершин оригинала!
			// getVertices() возвращает исходные вершины без учета трансформации самого полигона.
			float[] vertices = originalPoly.getVertices();
			if (vertices == null || vertices.length == 0) {
				transformedPolygons[p] = new Polygon(); // Пустой полигон
				continue;
			}

			Polygon transformedPoly = new Polygon(vertices); // Создаем копию с оригинальными вершинами

			// Применяем трансформации Actor'а (Look) к этому новому полигону
			transformedPoly.setOrigin(currentOriginX, currentOriginY); // Устанавливаем Origin ДО трансформаций
			transformedPoly.setScale(currentScaleX, currentScaleY);
			transformedPoly.setRotation(currentRotation);
			transformedPoly.translate(currentX, currentY);

			transformedPolygons[p] = transformedPoly;
		}

		// Сохраняем результат в кэш
		this.cachedTransformedPolygons = transformedPolygons;
		this.lastUsedLookDataForCache = currentLookData;
		// collisionDirty уже установлен в false через compareAndSet (или не был true изначально)

		return this.cachedTransformedPolygons;
	}*/

	public Polygon[] getCurrentCollisionPolygon() {
		LookData currentLookData = (lookData2 != null) ? lookData2 : lookData;
		boolean needsRecalculation = collisionDirty.compareAndSet(true, false);

		if (!needsRecalculation && lastUsedLookDataForCache == currentLookData && cachedTransformedPolygons != null) {
			return cachedTransformedPolygons;
		}

		// --- Пересчет полигонов (теперь с C++) ---
		Polygon[] originalPolygons;
		if (currentLookData == null) {
			originalPolygons = new Polygon[0];
		} else {
			CollisionInformation ci = currentLookData.getCollisionInformation();
			if (ci == null || ci.collisionPolygons == null) {
				ci.loadCollisionPolygon(); // Предполагаем, что это загружает полигоны
			}
			originalPolygons = ci.collisionPolygons;
			if (originalPolygons == null) {
				originalPolygons = new Polygon[0];
			}
		}

		// --- Оптимизация здесь ---
		// Переиспользуем массив, если это возможно, чтобы уменьшить работу GC
		if (cachedTransformedPolygons == null || cachedTransformedPolygons.length != originalPolygons.length) {
			cachedTransformedPolygons = new Polygon[originalPolygons.length];
			for (int i = 0; i < originalPolygons.length; i++) {
				cachedTransformedPolygons[i] = new Polygon(); // Создаем один раз
			}
		}

		// Получаем параметры трансформации один раз
		float currentX = getX();
		float currentY = getY();
		float currentRotation = getRotation();
		float currentScaleX = getScaleX();
		float currentScaleY = getScaleY();
		float currentOriginX = getOriginX();
		float currentOriginY = getOriginY();

		for (int p = 0; p < originalPolygons.length; p++) {
			Polygon originalPoly = originalPolygons[p];
			if (originalPoly == null || originalPoly.getVertices() == null) {
				continue;
			}

			// Вызываем наш нативный метод!
			float[] transformedVertices = NativeLookOptimizer.transformPolygon(
					originalPoly.getVertices(),
					currentX, currentY,
					currentScaleX, currentScaleY,
					currentRotation,
					currentOriginX, currentOriginY
			);

			// Обновляем существующий полигон вместо создания нового
			cachedTransformedPolygons[p].setVertices(transformedVertices);
		}

		this.lastUsedLookDataForCache = currentLookData;
		return this.cachedTransformedPolygons;
	}

	/*public Polygon[] getCurrentCollisionPolygon() {
		LookData currentLookData = (lookData2 != null) ? lookData2 : lookData;
		boolean needsRecalculation = collisionDirty.compareAndSet(true, false);

		if (!needsRecalculation && lastUsedLookDataForCache == currentLookData && cachedTransformedPolygons != null) {
			// Кэш актуален и для текущего LookData, используем его
			return cachedTransformedPolygons;
		}

		// --- Пересчет полигонов ---
		Polygon[] originalPolygons;
		if (currentLookData == null) {
			originalPolygons = new Polygon[0];
		} else {
			CollisionInformation collisionInformation = currentLookData.getCollisionInformation();
			if (collisionInformation == null) {
				originalPolygons = new Polygon[0];
			} else {
				if (collisionInformation.collisionPolygons == null) {
					collisionInformation.loadCollisionPolygon();
				}
				originalPolygons = collisionInformation.collisionPolygons;
				if (originalPolygons == null) {
					originalPolygons = new Polygon[0];
				}
			}
		}

		// --- Переиспользование или создание кэш-массива и его содержимого ---
		if (cachedTransformedPolygons == null || cachedTransformedPolygons.length != originalPolygons.length) {
			// Создаем новый массив, если кэш не существует или размер отличается
			cachedTransformedPolygons = new Polygon[originalPolygons.length];
			// Заполняем его новыми пустыми объектами Polygon
			for (int i = 0; i < cachedTransformedPolygons.length; i++) {
				cachedTransformedPolygons[i] = new Polygon();
			}
		} else {
			// Убедимся, что все Polygon объекты существуют (на всякий случай)
			for (int i = 0; i < cachedTransformedPolygons.length; i++) {
				if (cachedTransformedPolygons[i] == null) {
					cachedTransformedPolygons[i] = new Polygon();
				}
			}
		}

		// Получаем текущие трансформации один раз
		float currentX = getX();
		float currentY = getY();
		float currentRotation = getRotation();
		float currentScaleX = getScaleX();
		float currentScaleY = getScaleY();
		float currentOriginX = getOriginX();
		float currentOriginY = getOriginY();

		for (int p = 0; p < originalPolygons.length; p++) {
			Polygon originalPoly = originalPolygons[p];
			// Получаем существующий объект Polygon из кэша для обновления
			Polygon targetPoly = cachedTransformedPolygons[p];

			if (originalPoly == null || originalPoly.getVertices() == null || originalPoly.getVertices().length < 6) { // Полигону нужно >= 3 вершин (6 floats)
				// Если исходный полигон невалиден, делаем целевой полигон пустым
				targetPoly.setVertices(new float[0]); // Устанавливаем пустые вершины
				continue;
			}

			// --- Обновляем существующий targetPoly ---
			// 1. Устанавливаем вершины из оригинала (метод setVertices копирует массив)
			targetPoly.setVertices(originalPoly.getVertices());

			// 2. Применяем трансформации к targetPoly
			targetPoly.setOrigin(currentOriginX, currentOriginY); // Устанавливаем origin ДО трансформаций
			targetPoly.setScale(currentScaleX, currentScaleY);
			targetPoly.setRotation(currentRotation);
			targetPoly.translate(currentX, currentY); // Применяем смещение Actor'а
		}

		// Сохраняем ссылку на LookData, для которого был сделан кэш
		this.lastUsedLookDataForCache = currentLookData;
		// collisionDirty уже установлен в false через compareAndSet

		return this.cachedTransformedPolygons;
	}*/

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
