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
package org.catrobat.catroid.physics;

import android.util.Log;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.box2d.structs.b2BodyDef;
import com.badlogic.gdx.box2d.structs.b2BodyId;
import com.badlogic.gdx.box2d.structs.b2DebugDraw;
import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2Rot;
import com.badlogic.gdx.box2d.structs.b2WorldId;
import com.badlogic.gdx.box2d.structs.b2ContactEvents;
import com.badlogic.gdx.box2d.FFITypes;
import com.badlogic.gdx.box2d.structs.b2WorldDef;
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.utils.GdxNativesLoader;

import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.XmlHeader;
import com.badlogic.gdx.box2d.FFITypes.*;
import org.catrobat.catroid.physics.shapebuilder.PhysicsShapeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class PhysicsWorld {
	static {
		GdxNativesLoader.load();
	}

	private static final String TAG = PhysicsWorld.class.getSimpleName();

	// CATEGORY
	public static final short CATEGORY_NO_COLLISION = 0x0000;
	public static final short CATEGORY_BOUNDARYBOX = 0x0002;
	public static final short CATEGORY_PHYSICSOBJECT = 0x0004;

	// COLLISION_MODE
	public static final short MASK_BOUNDARYBOX = CATEGORY_PHYSICSOBJECT; // collides with physics_objects
	public static final short MASK_PHYSICSOBJECT = ~CATEGORY_BOUNDARYBOX; // collides with everything but not with the boundarybox
	public static final short MASK_TO_BOUNCE = -1; // collides with everything
	public static final short MASK_NO_COLLISION = 0; // collides with NOBODY

	public static float ACTIVE_AREA_WIDTH_FACTOR = 3.0f;
	public static float ACTIVE_AREA_HEIGHT_FACTOR = 2.0f;

	public static final float RATIO = 10.0f;
	public static final int VELOCITY_ITERATIONS = 3;
	public static final int POSITION_ITERATIONS = 3;

	public static final Vector2 DEFAULT_GRAVITY = new Vector2(0.0f, -10.0f);
	public static final boolean IGNORE_SLEEPING_OBJECTS = false;
	public static Vector2 activeArea;

	public static final int STABILIZING_STEPS = 6;

	private static final b2WorldDef worldDef = new b2WorldDef();
	private static b2WorldId worldId = null;
	private final Map<Sprite, PhysicsObject> physicsObjects = new HashMap<>();
	private final ArrayList<Sprite> activeVerticalBounces = new ArrayList<>();
	private final ArrayList<Sprite> activeHorizontalBounces = new ArrayList<>();
	private PhysicsCollisionListener collisionListener = new PhysicsCollisionListener(this);
	private b2DebugDraw renderer;
	private int stabilizingSteCounter = 0;
	private PhysicsBoundaryBox boundaryBox;

	private final PhysicsShapeBuilder physicsShapeBuilder = PhysicsShapeBuilder.getInstance();

	public PhysicsWorld(Project project) {
		this(ScreenValues.currentScreenResolution.getWidth(), ScreenValues.currentScreenResolution.getHeight(), project);
	}

	public PhysicsWorld(int width, int height, Project project) {
		worldDef.enableSleep(PhysicsWorld.IGNORE_SLEEPING_OBJECTS);
		worldId = Box2d.b2CreateWorld(worldDef.asPointer());
		if (worldId == null || worldId.isNull()) {
			Log.e(TAG, "Failed to create b2World!");
			// Здесь нужно обработать ошибку, возможно, бросить исключение
			throw new RuntimeException("Failed to create Box2D world");
		}
		collisionListener =  new PhysicsCollisionListener(this);
		b2Vec2 v = new b2Vec2();
		v.x(DEFAULT_GRAVITY.x);
		v.y(DEFAULT_GRAVITY.y);
		Box2d.b2World_SetGravity(worldId, v);
		XmlHeader xml = project.getXmlHeader();
		ACTIVE_AREA_WIDTH_FACTOR = xml.getPhysicsWidthArea();
		ACTIVE_AREA_HEIGHT_FACTOR = xml.getPhysicsHeightArea();
		boundaryBox = new PhysicsBoundaryBox(worldId);
		boundaryBox.create(width, height);
		activeArea = new Vector2(width * ACTIVE_AREA_WIDTH_FACTOR, height * ACTIVE_AREA_HEIGHT_FACTOR);
		//worldId.setContactListener(new PhysicsCollisionListener(this));
	}


	public PhysicsWorld(int width, int height) {
		ACTIVE_AREA_WIDTH_FACTOR = 3.0f;
		ACTIVE_AREA_HEIGHT_FACTOR = 2.0f;
		boundaryBox = new PhysicsBoundaryBox(worldId);
		boundaryBox.create(width, height);
		activeArea = new Vector2(width * ACTIVE_AREA_WIDTH_FACTOR, height * ACTIVE_AREA_HEIGHT_FACTOR);
		//worldId.setContactListener(new PhysicsCollisionListener(this));
	}

	public b2WorldId getWorldId() {
		return worldId;
	}

	public void setBounceOnce(Sprite sprite, PhysicsBoundaryBox.BoundaryBoxIdentifier boundaryBoxIdentifier) {
		if (physicsObjects.containsKey(sprite)) {
			PhysicsObject physicsObject = physicsObjects.get(sprite);
			physicsObject.setIfOnEdgeBounce(true, sprite);
			switch (boundaryBoxIdentifier) {
				case BBI_HORIZONTAL:
					activeHorizontalBounces.add(sprite);
					break;
				case BBI_VERTICAL:
					activeVerticalBounces.add(sprite);
					break;
			}
		}
	}

	public void step(float deltaTime) {
		if (worldId == null || worldId.isNull()) {
			Log.e(TAG, "Attempting to step a null or invalid world!");
			return;
		}
		if (stabilizingSteCounter < STABILIZING_STEPS) {
			stabilizingSteCounter++;
		} else {
			try {
				// В Box2D 3.0 второй параметр - это velocity iterations, третьего (position iterations) нет
				Box2d.b2World_Step(worldId, deltaTime, PhysicsWorld.VELOCITY_ITERATIONS);

				// Получаем события контакта
				b2ContactEvents contactEvents = Box2d.b2World_GetContactEvents(worldId);

				if (contactEvents != null && !contactEvents.isNull()) {
					// Обрабатываем события
					collisionListener.processContacts(contactEvents);
				}
			} catch (Exception exception) {
				// Логируем более подробно
				Log.e(TAG, "Exception during PhysicsWorld.step()", exception);
			}
		}
	}

	public void render(Matrix4 perspectiveMatrix) {
		if (renderer == null) {
			renderer = new b2DebugDraw();/*PhysicsDebugSettings.Render.RENDER_BODIES,
					PhysicsDebugSettings.Render.RENDER_JOINTS, PhysicsDebugSettings.Render.RENDER_AABB,
					PhysicsDebugSettings.Render.RENDER_INACTIVE_BODIES, PhysicsDebugSettings.Render.RENDER_VELOCITIES,
					PhysicsDebugSettings.Render.RENDER_CONTACTS);*/
			renderer.drawShapes(PhysicsDebugSettings.Render.RENDER_BODIES);
			renderer.drawJoints(PhysicsDebugSettings.Render.RENDER_JOINTS);
			renderer.drawContacts(PhysicsDebugSettings.Render.RENDER_CONTACTS);
		}
		Box2d.b2World_Draw(worldId, renderer.asPointer());
		//renderer.render(world, perspectiveMatrix.scl(PhysicsWorld.RATIO));
	}

	public void setGravity(float x, float y) {
		b2Vec2 v = new b2Vec2();
		v.x(x);
		v.y(y);
		Box2d.b2World_SetGravity(worldId, v);
	}

	public Vector2 getGravity() {
		if (worldId == null || worldId.isNull()) return new Vector2(DEFAULT_GRAVITY); // Возвращаем дефолт, если мир невалиден
		b2Vec2 currentGravity = Box2d.b2World_GetGravity(worldId);
		return new Vector2(currentGravity.x(), currentGravity.y());
	}

	/**
	 * Находит PhysicsObject по его идентификатору тела Box2D.
	 * Используется CollisionListener'ом для определения столкнувшегося спрайта.
	 *
	 * @param bodyId Идентификатор тела Box2D.
	 * @return Найденный PhysicsObject или null, если не найден.
	 */
	@Nullable
	public PhysicsObject findPhysicsObjectByBodyId(b2BodyId bodyId) {
		if (bodyId == null || bodyId.isNull()) {
			return null;
		}
		// Проходим по всем значениям (PhysicsObject) в нашей карте
		for (PhysicsObject physicsObject : physicsObjects.values()) {
			if (physicsObject != null && physicsObject.getBodyId() != null && physicsObject.getBodyId().equals(bodyId)) {
				// Используем equals() для сравнения b2BodyId, т.к. он должен быть переопределен
				return physicsObject;
			}
		}
		// Не нашли соответствующий PhysicsObject
		return null;
	}

	/**
	 * Определяет, принадлежит ли данное тело одной из границ мира, и возвращает ее идентификатор.
	 * Используется CollisionListener'ом.
	 * Требует реализации метода getIdentifierForBody(b2BodyId) в классе PhysicsBoundaryBox.
	 *
	 * @param bodyId Идентификатор тела Box2D.
	 * @return BoundaryBoxIdentifier (LEFT, RIGHT, TOP, BOTTOM) или null, если тело не является границей.
	 */
	@Nullable
	public PhysicsBoundaryBox.BoundaryBoxIdentifier getBoundaryIdentifierFromBodyId(b2BodyId bodyId) {
		if (bodyId == null || bodyId.isNull() || boundaryBox == null) {
			// Если ID невалиден или boundaryBox не создан (не должно быть)
			return null;
		}
		// Делегируем проверку объекту boundaryBox
		// !!! ТРЕБУЕТСЯ РЕАЛИЗАЦИЯ МЕТОДА getIdentifierForBody в PhysicsBoundaryBox !!!
		try {
			return boundaryBox.getIdentifierForBody(bodyId);
		} catch (UnsupportedOperationException e) {
			Log.e(TAG, "PhysicsBoundaryBox does not implement getIdentifierForBody(b2BodyId) yet!", e);
			return null; // Или бросить исключение дальше
		} catch (Exception e) {
			Log.e(TAG, "Error checking boundary identifier for bodyId: " + bodyId, e);
			return null;
		}
	}

	// В классе PhysicsWorld

	public void changeLook(PhysicsObject physicsObject, Look look) {
		Object[] geometries = null; // Тип изменен на Object[]
		if (look != null && look.getLookData() != null && look.getLookData().getFile() != null) {
			try {
				// Вызываем переименованный метод
				geometries = physicsShapeBuilder.getScaledGeometries(look.getLookData(),
						look.getSizeInUserInterfaceDimensionUnit() / 100f);
			} catch (Exception e) {
				Log.e(TAG, "Failed to get scaled geometries for look: " + look.getName(), e);
				geometries = null; // Устанавливаем в null при ошибке
			}
		}

		// УБИРАЕМ СТАРЫЙ ВЫЗОВ:
		// physicsObject.setShape(shapes);

		// --- НОВЫЙ ПОДХОД ---
		// Теперь PhysicsObject сам должен обновить свои фикстуры, используя новые геометрии.
		// Добавь метод в PhysicsObject, например: updateFixtures(Object[] newGeometries)
		// Этот метод должен:
		// 1. Удалить все существующие фикстуры у b2Body объекта.
		// 2. Пройти по массиву newGeometries.
		// 3. Для каждой геометрии:
		//    a. Создать new b2ShapeDef().
		//    b. Задать свойства (density, friction, restitution, filter...).
		//    c. Вызвать метод создания фикстуры (например, Box2d.b2Body_CreatePolygonFixture / b2Body_CreateCircleFixture
		//       или какой-то универсальный b2Body_CreateFixture, принимающий def и геометрию).
		//       !!! Найди точную сигнатуру метода создания фикстуры в API !!!
		if (physicsObject != null) {
			try {
				physicsObject.updateFixtures(geometries); // ПРИМЕР вызова нового метода
			} catch (Exception e) {
				Log.e(TAG, "Failed to update physics object fixtures for sprite", e);
			}
		} else {
			Log.w(TAG, "PhysicsObject is null in changeLook, cannot update fixtures.");
		}
	}

	/**
	 * Получает связанный Java-объект (UserData) для данного b2BodyId.
	 * Возвращает либо Sprite (если тело принадлежит спрайту), либо
	 * PhysicsBoundaryBox.BoundaryBoxIdentifier (если тело является границей мира),
	 * либо null, если тело не распознано.
	 * Вызывается из PhysicsCollisionListener.
	 *
	 * @param bodyId Идентификатор тела Box2D.
	 * @return Sprite, BoundaryBoxIdentifier или null.
	 */
	@Nullable
	public Object getUserDataForBody(b2BodyId bodyId) {
		if (bodyId == null || bodyId.isNull()) {
			return null;
		}

		// 1. Проверяем, не принадлежит ли тело спрайту
		PhysicsObject physicsObject = findPhysicsObjectByBodyId(bodyId);
		if (physicsObject != null) {
			// Нашли PhysicsObject, возвращаем связанный Sprite
			return physicsObject.getSprite();
		}

		// 2. Если не спрайт, проверяем, не граница ли это мира
		if (boundaryBox != null) {
			// Делегируем проверку объекту boundaryBox.
			// !!! ЭТОТ МЕТОД НУЖНО РЕАЛИЗОВАТЬ В PhysicsBoundaryBox !!!
			PhysicsBoundaryBox.BoundaryBoxIdentifier boundaryId = boundaryBox.getIdentifierForBody(bodyId);
			if (boundaryId != null) {
				// Да, это граница, возвращаем ее идентификатор
				return boundaryId;
			}
		}

		// 3. Тело не распознано (ни спрайт, ни граница)
		Log.w(TAG, "getUserDataForBody: BodyId " + bodyId + " not found among known sprites or boundaries.");
		return null;
	}



	public PhysicsObject getPhysicsObject(Sprite sprite) {
		if (sprite == null) {
			throw new NullPointerException();
		}

		if (physicsObjects.containsKey(sprite)) {
			return physicsObjects.get(sprite);
		}

		PhysicsObject physicsObject = createPhysicsObject(sprite);
		physicsObjects.put(sprite, physicsObject);

		return physicsObject;
		//throw new NullPointerException();
	}

	private PhysicsObject createPhysicsObject(Sprite sprite) {
		b2BodyDef bodyDef = new b2BodyDef();
		b2BodyId bodyId = Box2d.b2CreateBody(worldId, bodyDef.asPointer());
		return new PhysicsObject(bodyId, worldId, this, sprite);
	}

	public void bouncedOnEdge(Sprite sprite, PhysicsBoundaryBox.BoundaryBoxIdentifier boundaryBoxIdentifier) {
		if (physicsObjects.containsKey(sprite)) {
			PhysicsObject physicsObject = physicsObjects.get(sprite);
			switch (boundaryBoxIdentifier) {
				case BBI_HORIZONTAL:
					if (activeHorizontalBounces.remove(sprite) && !activeVerticalBounces.contains(sprite)) {
						physicsObject.setIfOnEdgeBounce(false, sprite);
						PhysicalCollision.fireBounceOffEvent(sprite, null);
					}
					break;
				case BBI_VERTICAL:
					if (activeVerticalBounces.remove(sprite) && !activeHorizontalBounces.contains(sprite)) {
						physicsObject.setIfOnEdgeBounce(false, sprite);
						PhysicalCollision.fireBounceOffEvent(sprite, null);
					}
					break;
			}


		}
	}

	public void dispose() {
		Log.d(TAG, "Disposing PhysicsWorld...");
		// Удалить все тела? Box2d.b2DestroyWorld должен делать это.
		// Очищаем нашу карту объектов
		physicsObjects.clear();
		activeVerticalBounces.clear();
		activeHorizontalBounces.clear();

		// Уничтожаем мир Box2D
		if (worldId != null && !worldId.isNull()) {
			Box2d.b2DestroyWorld(worldId);
			worldId = null; // Помечаем как невалидный
		}
		// Уничтожаем отладчик рендера
		if (renderer != null && !renderer.isNull()) {
			// renderer.dispose(); // Есть ли метод dispose у b2DebugDraw? Проверь API.
			renderer = null;
		}
		// Очистка boundaryBox (если у него есть ресурсы)
		if (boundaryBox != null) {
			// boundaryBox.dispose(); // Если нужно
			boundaryBox = null;
		}
		// Сброс PhysicsShapeBuilder (если нужно глобально)
		// physicsShapeBuilder.reset();
		Log.d(TAG, "PhysicsWorld disposed.");
	}
}
