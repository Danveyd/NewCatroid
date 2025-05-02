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

import com.badlogic.gdx.box2d.enums.b2BodyType;
import com.badlogic.gdx.box2d.structs.b2Rot;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.box2d.structs.b2BodyDef;
import com.badlogic.gdx.box2d.structs.b2WorldDef;
import com.badlogic.gdx.box2d.structs.b2BodyDef.b2BodyDefPointer;
import com.badlogic.gdx.box2d.structs.b2Circle;
//import com.badlogic.gdx.box2d.structs.shape;
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.structs.b2Filter;
import com.badlogic.gdx.box2d.structs.b2WorldId;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2BodyId;
import com.badlogic.gdx.box2d.structs.b2ChainDef;
import com.badlogic.gdx.box2d.structs.b2ChainId;
import com.badlogic.gdx.box2d.structs.b2BodyMoveEvent;
import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.FFITypes.*;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2Transform;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import org.catrobat.catroid.content.Sprite;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

public class PhysicsObject {

	public enum Type {
		DYNAMIC, FIXED, NONE
	}

	private static final String TAG = "PhysicsObject";

	public static final float DEFAULT_DENSITY = 1.0f;
	public static final float DEFAULT_FRICTION = 0.2f;
	public static final float MAX_FRICTION = 1.0f;
	public static final float MIN_FRICTION = 0.0f;
	public static final float MIN_DENSITY = 0.0f;
	public static final float MIN_BOUNCE_FACTOR = 0.0f;
	public static final float DEFAULT_BOUNCE_FACTOR = 0.8f;
	public static final float DEFAULT_MASS = 1.0f;
	public static final float MIN_MASS = 0.000001f;

	private short collisionMaskRecord = 0;
	private short categoryMaskRecord = PhysicsWorld.CATEGORY_PHYSICSOBJECT;

	private b2BodyDef body;

	private b2BodyId bodyId;

	private b2WorldDef world;

	private b2WorldId worldId;

	private b2ChainId chainId;
	private b2ChainDef chain;
	private b2ShapeDef fixtureDef = new b2ShapeDef();
	private b2ShapeId fixtureId = new b2ShapeId();
	private b2ShapeId[] shapes;

	private b2ShapeDef[] shapesDef;
	private Type type;
	private float mass;
	private float circumference;
	private boolean ifOnEdgeBounce = false;

	private final PhysicsWorld physicsWorldWrapper; // Ссылка на обертку PhysicsWorld для вызова статических методов Box2d.*
	//private final Array<b2ShapeId> shapeIds = new Array<>(false, 4); // ID форм, принадлежащих телу (начальная емкость 4)

	private Vector2 bodyAabbLowerLeft;
	private Vector2 bodyAabbUpperRight;
	private Vector2 fixtureAabbLowerLeft;
	private Vector2 fixtureAabbUpperRight;
	private Vector2 tmpVertice;

	private Vector2 velocity = new Vector2();
	private float rotationSpeed = 0;
	private float gravityScale = 0;
	private Type savedType = Type.NONE;

	private final Sprite sprite;

	private float friction = DEFAULT_FRICTION;  // Текущее трение
	private float bounceFactor = DEFAULT_BOUNCE_FACTOR; // Текущий коэфф. упругости
	private float density = DEFAULT_DENSITY;    // Текущая плотность (важна для DYNAMIC)
	private boolean fixedRotation = false;      // Фиксировано ли вращение

	private final ObjectMap<b2BodyId, Sprite> bodyIdToSpriteMap = new ObjectMap<>();

	private final b2ShapeDef shapeDefTemplate = new b2ShapeDef();

	/*public PhysicsObject(b2BodyId bodyId, b2WorldId worldId, PhysicsWorld physicsWorldWrapper, Sprite sprite) {
		if (bodyId == null || bodyId.isNull() || worldId == null || worldId.isNull() || physicsWorldWrapper == null) {
			throw new IllegalArgumentException("Valid BodyId, WorldId, and PhysicsWorld wrapper are required for PhysicsObject");
		}
		this.bodyId = bodyId;
		this.worldId = worldId;
		this.physicsWorldWrapper = physicsWorldWrapper; // Сохраняем ссылку на обертку
		bodyIdToSpriteMap.put(bodyId, sprite);

		// --- Проверка UserData (опционально, но полезно для отладки) ---
		// Предполагаем, что PhysicsWorld сохранил Sprite в своей карте bodyIdToUserDataMap
		/*Object currentData = physicsWorldWrapper.getUserDataForBody(bodyId); // Нужен метод в PhysicsWorld для этого!
		if (currentData != sprite) {
			// Можно кинуть исключение или вывести серьезное предупреждение
			System.err.println("CRITICAL WARNING: UserData mismatch in PhysicsObject constructor! Expected Sprite: "
					+ (sprite != null ? sprite.getName() : "null") + ", but found: " + currentData);
			// Это может указывать на проблемы с управлением UserData в PhysicsWorld
		}

		// --- Инициализация шаблона свойств формы ---
		shapeDefTemplate.density(PhysicsObject.DEFAULT_DENSITY);
		chainId = Box2d.b2CreateChain(bodyId, chain.asPointer());
		/*shapeDefTemplate.friction = PhysicsObject.DEFAULT_FRICTION;
		shapeDefTemplate.restitution = PhysicsObject.DEFAULT_BOUNCE_FACTOR;
		// Установка фильтров по умолчанию (категория объекта, маска зависит от типа)
		shapeDefTemplate.filter.categoryBits = PhysicsWorld.CATEGORY_PHYSICSOBJECT;
		// Маска будет установлена при вызове setType

		// --- Установка начального состояния ---
		setType(Type.NONE); // Устанавливаем начальный тип (это вызовет API Box2D)
		mass = PhysicsObject.DEFAULT_MASS; // Устанавливаем массу по умолчанию
	}*/

	public PhysicsObject(b2BodyId bodyId, b2WorldId worldId, PhysicsWorld physicsWorldWrapper, Sprite sprite) {
		if (bodyId == null || bodyId.isNull() || worldId == null || worldId.isNull() || physicsWorldWrapper == null || sprite == null) {
			throw new IllegalArgumentException("Valid BodyId, WorldId, PhysicsWorld wrapper, and Sprite are required for PhysicsObject");
		}
		this.bodyId = bodyId;
		this.worldId = worldId;
		this.physicsWorldWrapper = physicsWorldWrapper;
		this.sprite = sprite;

		// Добавляем в карту в PhysicsWorld (если она там используется)
		// physicsWorldWrapper.registerPhysicsObject(bodyId, this); // Пример

		// Настраиваем шаблон свойств формы
		shapeDefTemplate.density(this.density);
		shapeDefTemplate.material().friction(this.friction); // В Box2D 3.0 трение в материале
		shapeDefTemplate.material().restitution(this.bounceFactor); // И упругость тоже

		// Устанавливаем фильтры по умолчанию (категория объекта, маска зависит от типа)
		b2Filter filter = shapeDefTemplate.filter(); // Получаем объект фильтра
		filter.categoryBits(this.categoryMaskRecord);
		filter.maskBits(this.collisionMaskRecord); // Маска будет обновлена в setType

		// --- Установка начального состояния ---
		setType(Type.NONE); // Устанавливаем начальный тип (применит настройки к телу)
		// Масса теперь вычисляется Box2D из плотности и формы

		Log.d(TAG, "PhysicsObject created for sprite: " + sprite.getName() + " with bodyId: " + bodyId);
	}


	// --- Getters ---
	public b2BodyId getBodyId() {
		return bodyId;
	}

	public Sprite getSprite() {
		return sprite;
	}

	private Object getUserDataFromShapeId(b2ShapeId shapeId) {
		if (shapeId == null || shapeId.isNull()) {
			return null;
		}

		try {
			// --- ШАГ 1: Получить BodyId из ShapeId ---
			// ПРЕДПОЛОЖЕНИЕ API: Нужен метод вроде b2Shape_GetBody
			b2BodyId bodyId = Box2d.b2Shape_GetBody(shapeId); // ЗАМЕНИ НА РЕАЛЬНЫЙ API!

			if (bodyId != null && !bodyId.isNull()) {
				// --- ШАГ 2: Найти Sprite/PhysicsObject по BodyId через PhysicsWorld ---
				// Используем PhysicsWorld как центральное хранилище связи BodyId <-> Sprite/PhysicsObject
				// Нужен соответствующий метод в PhysicsWorld!
				Object userData = physicsWorldWrapper.getUserDataForBody(bodyId); // ЗАМЕНИ НА РЕАЛЬНЫЙ МЕТОД!
				if (userData != null) {
					// Мы ожидаем получить либо Sprite, либо BoundaryBoxIdentifier
					if (userData instanceof Sprite || userData instanceof PhysicsBoundaryBox.BoundaryBoxIdentifier) {
						return userData;
					} else {
						Log.w(TAG, "Unexpected UserData type found for bodyId " + bodyId + ": " + userData.getClass().getName());
					}
				} else {
					// Log.w(TAG, "No UserData found in PhysicsWorld map for bodyId: " + bodyId);
				}
			} else {
				Log.w(TAG, "Could not get valid BodyId from shapeId: " + shapeId);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting UserData for shapeId: " + shapeId, e);
		}

		return null; // Возвращаем null, если что-то пошло не так
	}

	public void destroyPhysicsObject(PhysicsObject physicsObject) {
		if (physicsObject == null) return;
		b2BodyId bodyId = physicsObject.bodyId; // Нужен геттер для bodyId в PhysicsObject

		if (bodyId != null && !bodyId.isNull()) {
			// Удаляем из карты
			bodyIdToSpriteMap.remove(bodyId);
			// Уничтожаем тело Box2D
			Box2d.b2DestroyBody(bodyId);
		}
	}

	public void copyTo(PhysicsObject destination) {
		destination.setType(this.getType());
		destination.setPosition(this.getPosition());
		destination.setDirection(this.getDirection());
		destination.setMass(this.getMass());
		destination.setRotationSpeed(this.getRotationSpeed());
		destination.setBounceFactor(this.getBounceFactor());
		destination.setFriction(this.getFriction());
		destination.setVelocity(this.getVelocity());
	}

	public void setShape(b2ShapeDef[] shapes) {
		if (Arrays.equals(this.shapesDef, shapesDef)) {
			return;
		}

		if (shapes != null) {
			this.shapesDef = Arrays.copyOf(shapesDef, shapesDef.length);
		} else {
			this.shapesDef = null;
		}

		while (Box2d.b2Body_GetShapeCount(bodyId) > 0) {
			b2ShapeId.b2ShapeIdPointer xz = null;
			int capacity = 5;
			Box2d.b2Body_GetShapes(bodyId, xz, capacity);
			b2ShapeId oldFixture = xz.get(0);
			Box2d.b2DestroyShape(oldFixture, true);
		}

		if (shapes != null) {
			for (b2ShapeDef tempShape : shapesDef) {
				fixtureDef = tempShape;
				b2Polygon box = Box2d.b2MakeBox(1.0f, 1.0f);
				fixtureId = Box2d.b2CreatePolygonShape(bodyId, fixtureDef.asPointer(), box.asPointer());
			}
		}

		setMass(mass);
		calculateCircumference();
	}

	private void calculateCircumference() {
		if (Box2d.b2Body_GetShapeCount(bodyId) == 0) {
			//Log.d(TAG, "No fixtures, so reset circumference to zero");
			circumference = 0;
			return;
		}
		circumference = PhysicsWorldConverter.convertNormalToBox2dCoordinate(getBoundaryBoxDimensions().len() / 2.0f);
	}

	public Type getType() {
		return type;
	}

	// --- Управление типом тела ---
	public void setType(Type type) {
		if (this.type == type || bodyId == null || bodyId.isNull()) {
			return;
		}
		Log.d(TAG, "Setting type to " + type + " for sprite: " + sprite.getName());
		this.type = type;
		short newMask = PhysicsWorld.MASK_PHYSICSOBJECT; // Маска по умолчанию

		switch (type) {
			case DYNAMIC:
				Box2d.b2Body_SetType(bodyId, b2BodyType.b2_dynamicBody);
				Box2d.b2Body_SetGravityScale(bodyId, 1.0f); // Включаем гравитацию
				Box2d.b2Body_SetBullet(bodyId, true);      // Включаем CCD для быстрых объектов
				setDensityInternal(this.density);        // Применяем плотность, чтобы масса рассчиталась
				newMask = PhysicsWorld.MASK_PHYSICSOBJECT;
				break;
			case FIXED:
				// В Box2D 3.0 нет KINEMATIC для фиксированных объектов, которые не двигаются
				// Используем STATIC. Если нужно движение по скрипту, используем KINEMATIC.
				// Для "неподвижных" объектов Catroid лучше STATIC.
				Box2d.b2Body_SetType(bodyId, b2BodyType.b2_staticBody);
				// Для STATIC гравитация не нужна, скорость обнуляется
				Box2d.b2Body_SetGravityScale(bodyId, 0.0f);
				b2Vec2 v = new b2Vec2();
				v.x(0.0f);
				v.y(0.0f);
				Box2d.b2Body_SetLinearVelocity(bodyId, v); // Обнуляем скорость
				Box2d.b2Body_SetAngularVelocity(bodyId, 0.0f);   // Обнуляем вращение
				newMask = PhysicsWorld.MASK_PHYSICSOBJECT;
				break;
			case NONE:
				// Для "невидимых" физически объектов используем STATIC и отключаем столкновения
				Box2d.b2Body_SetType(bodyId, b2BodyType.b2_staticBody); // Или KINEMATIC, если нужно двигать скриптом? STATIC безопаснее.
				Box2d.b2Body_SetGravityScale(bodyId, 0.0f);
				b2Vec2 v2 = new b2Vec2();
				v2.x(0.0f);
				v2.y(0.0f);
				Box2d.b2Body_SetLinearVelocity(bodyId, v2);
				Box2d.b2Body_SetAngularVelocity(bodyId, 0.0f);
				newMask = PhysicsWorld.MASK_NO_COLLISION;
				break;
		}
		// Обновляем фильтр столкновений для всех фикстур
		setCollisionMask(newMask);
		// Сохраняем маску для возможного восстановления
		this.collisionMaskRecord = newMask;
	}


	private void setDensityInternal(float newDensity) {
		if (bodyId == null || bodyId.isNull()) return;
		int shapeCount = Box2d.b2Body_GetShapeCount(bodyId);
		if (shapeCount > 0) {
			b2ShapeId.b2ShapeIdPointer shapeIdPtr = new b2ShapeId.b2ShapeIdPointer(shapeCount, false);
			int fetchedCount = Box2d.b2Body_GetShapes(bodyId, shapeIdPtr, shapeCount);
			for (int i = 0; i < fetchedCount; i++) {
				b2ShapeId shapeId = shapeIdPtr.get(i);
				if (shapeId != null && !shapeId.isNull()) {
					try {
						// true - автоматически пересчитать массу тела
						Box2d.b2Shape_SetDensity(shapeId, newDensity, true);
					} catch (Exception e) {
						Log.e(TAG, "Error setting density for shapeId: " + shapeId, e);
					}
				}
			}
			// Box2d.b2Body_ApplyMassFromShapes(bodyId); // Пересчет массы должен быть автоматическим с флагом true
		}
	}


	public float getDirection() {
		if (bodyId == null || bodyId.isNull()) return 0f;
		b2Rot rotation = Box2d.b2Body_GetRotation(bodyId);
		if (rotation == null || rotation.isNull()) return 0f;

		// Вычисляем угол в радианах из синуса и косинуса
		double radians = Math.atan2(rotation.s(), rotation.c());
		// Конвертируем в градусы
		return (float) Math.toDegrees(radians);
	}

	// --- Метод setDirection() ---
	public void setDirection(float degrees) {
		if (bodyId == null || bodyId.isNull()) return;

		float radians = MathUtils.degreesToRadians * degrees;
		b2Vec2 currentPosition = Box2d.b2Body_GetPosition(bodyId);

		// Создаем новый объект b2Rot и устанавливаем синус и косинус
		b2Rot newRotation = new b2Rot();
		newRotation.s((float) Math.sin(radians)); // Устанавливаем синус
		newRotation.c((float) Math.cos(radians)); // Устанавливаем косинус

		// Применяем трансформацию
		Box2d.b2Body_SetTransform(bodyId, currentPosition, newRotation);
	}

	public float getX() {
		return PhysicsWorldConverter.convertBox2dToNormalCoordinate(Box2d.b2Body_GetPosition(bodyId).x());
	}

	public float getY() {
		return PhysicsWorldConverter.convertBox2dToNormalCoordinate(Box2d.b2Body_GetPosition(bodyId).y());
	}

	public Vector2 getMassCenter() {
		return new Vector2(Box2d.b2Body_GetWorldCenterOfMass(bodyId).x(), Box2d.b2Body_GetWorldCenterOfMass(bodyId).y());
	}

	public float getCircumference() {
		return PhysicsWorldConverter.convertBox2dToNormalCoordinate(circumference);
	}

	public Vector2 getPosition() {
		return PhysicsWorldConverter.convertBox2dToNormalVector(new Vector2(Box2d.b2Body_GetPosition(bodyId).x(), Box2d.b2Body_GetPosition(bodyId).y()));
	}

	public void setX(float x) {
		b2Vec2 v = new b2Vec2();
		v.x(PhysicsWorldConverter.convertNormalToBox2dCoordinate(x));
		v.y(Box2d.b2Body_GetPosition(bodyId).y());
		Box2d.b2Body_SetTransform(bodyId, v, Box2d.b2Body_GetRotation(bodyId));
	}

	public void setY(float y) {
		b2Vec2 v = new b2Vec2();
		v.x(Box2d.b2Body_GetPosition(bodyId).x());
		v.y(PhysicsWorldConverter.convertNormalToBox2dCoordinate(y));
		Box2d.b2Body_SetTransform(bodyId, v,
				Box2d.b2Body_GetRotation(bodyId));
	}

	public void setPosition(float x, float y) {
		x = PhysicsWorldConverter.convertNormalToBox2dCoordinate(x);
		y = PhysicsWorldConverter.convertNormalToBox2dCoordinate(y);
		b2Vec2 v = new b2Vec2();
		v.x(x);
		v.y(y);
		Box2d.b2Body_SetTransform(bodyId, v, Box2d.b2Body_GetRotation(bodyId));
	}

	public void setPosition(Vector2 position) {
		setPosition(position.x, position.y);
	}

	public float getRotationSpeed() {
		return (float) Math.toDegrees(Box2d.b2Body_GetAngularDamping(bodyId));
	}

	public void setRotationSpeed(float degreesPerSecond) {
		Box2d.b2Body_SetAngularDamping(bodyId, (float) Math.toRadians(degreesPerSecond));
	}

	public Vector2 getVelocity() {
		return PhysicsWorldConverter.convertBox2dToNormalVector(new Vector2(Box2d.b2Body_GetLinearVelocity(bodyId).x(), Box2d.b2Body_GetLinearVelocity(bodyId).y()));
	}

	public void setVelocity(float x, float y) {
		b2Vec2 v = new b2Vec2();
		v.x(PhysicsWorldConverter.convertNormalToBox2dCoordinate(x));
		v.y(PhysicsWorldConverter.convertNormalToBox2dCoordinate(y));
		Box2d.b2Body_SetLinearVelocity(bodyId, v);
	}

	public void setVelocity(Vector2 velocity) {
		setVelocity(velocity.x, velocity.y);
	}

	public float getMass() {
		return this.mass;
	}

	public float getBounceFactor() {
		return Box2d.b2Chain_GetRestitution(chainId);
	}

	public void setMass(float mass) {
		this.mass = mass;

		if (mass < 0) {
			this.mass = PhysicsObject.MIN_MASS;
		}
		if (mass < PhysicsObject.MIN_MASS) {
			mass = PhysicsObject.MIN_MASS;
		}
		if (isStaticObject()) {
			return;
		}
		float area = Box2d.b2Body_GetMass(bodyId)/ fixtureDef.density();
		float density = mass / area;
		setDensity(density);
	}

	private boolean isStaticObject() {
		return Box2d.b2Body_GetMass(bodyId) == 0.0f;
	}

	@VisibleForTesting
	public void setDensity(float density) {
		if (density < MIN_DENSITY) {
			density = PhysicsObject.MIN_DENSITY;
		}
		fixtureDef.density(density);
		@NotNull
		b2ShapeId.b2ShapeIdPointer xz = null;
		int capacity = 5;
		int i = 0;
		Box2d.b2Body_GetShapes(bodyId, xz, capacity);
		for (i = 0; i < xz.getSize(); i++) {
			b2ShapeId fixture = xz.get(i);
			Box2d.b2Shape_SetDensity(fixture, density, true);
		}
		Box2d.b2Body_ApplyMassFromShapes(bodyId);
	}

	public float getFriction() {
		return Box2d.b2Shape_GetFriction(fixtureId);
	}

	public void setFriction(float friction) {
		if (friction < MIN_FRICTION) friction = MIN_FRICTION;
		if (friction > MAX_FRICTION) friction = MAX_FRICTION;
		if (this.friction == friction) return;
		this.friction = friction;
		this.shapeDefTemplate.material().friction(friction); // Обновляем шаблон

		// Применяем ко всем фикстурам
		if (bodyId == null || bodyId.isNull()) return;
		int shapeCount = Box2d.b2Body_GetShapeCount(bodyId);
		if (shapeCount > 0) {
			b2ShapeId.b2ShapeIdPointer shapeIdPtr = new b2ShapeId.b2ShapeIdPointer(shapeCount, false);
			int fetchedCount = Box2d.b2Body_GetShapes(bodyId, shapeIdPtr, shapeCount);
			for (int i = 0; i < fetchedCount; i++) {
				b2ShapeId shapeId = shapeIdPtr.get(i);
				if (shapeId != null && !shapeId.isNull()) {
					try {
						Box2d.b2Shape_SetFriction(shapeId, friction);
					} catch (Exception e) {
						Log.e(TAG, "Error setting friction for shapeId: " + shapeId, e);
					}
				}
			}
		}
	}

	public void setBounceFactor(float bounceFactor) {
		if (bounceFactor < MIN_BOUNCE_FACTOR) bounceFactor = MIN_BOUNCE_FACTOR;
		// Верхнего предела обычно нет, но можно добавить, если нужно
		if (this.bounceFactor == bounceFactor) return;
		this.bounceFactor = bounceFactor;
		this.shapeDefTemplate.material().restitution(bounceFactor); // Обновляем шаблон

		// Применяем ко всем фикстурам
		if (bodyId == null || bodyId.isNull()) return;
		int shapeCount = Box2d.b2Body_GetShapeCount(bodyId);
		if (shapeCount > 0) {
			b2ShapeId.b2ShapeIdPointer shapeIdPtr = new b2ShapeId.b2ShapeIdPointer(shapeCount, false);
			int fetchedCount = Box2d.b2Body_GetShapes(bodyId, shapeIdPtr, shapeCount);
			for (int i = 0; i < fetchedCount; i++) {
				b2ShapeId shapeId = shapeIdPtr.get(i);
				if (shapeId != null && !shapeId.isNull()) {
					try {
						Box2d.b2Shape_SetRestitution(shapeId, bounceFactor);
					} catch (Exception e) {
						Log.e(TAG, "Error setting restitution for shapeId: " + shapeId, e);
					}
				}
			}
		}
	}


	public void setGravityScale(float scale) {
		if (bodyId == null || bodyId.isNull()) return;
		Box2d.b2Body_SetGravityScale(bodyId, scale);
	}

	public float getGravityScale() {
		if (bodyId == null || bodyId.isNull()) return 0f;
		return Box2d.b2Body_GetGravityScale(bodyId);
	}

	public void setFixedRotation(boolean flag) {
		if (this.fixedRotation == flag || bodyId == null || bodyId.isNull()) return;
		this.fixedRotation = flag;
		Box2d.b2Body_SetFixedRotation(bodyId, flag);
	}

	public boolean getFixedRotation() {
		if (bodyId == null || bodyId.isNull()) return false;
		return Box2d.b2Body_IsFixedRotation(bodyId);
	}

	public void setIfOnEdgeBounce(boolean bounce, Sprite sprite) { // sprite аргумент больше не нужен здесь
		if (ifOnEdgeBounce == bounce) {
			return;
		}
		this.ifOnEdgeBounce = bounce;

		short newMask;
		if (bounce) {
			newMask = PhysicsWorld.MASK_TO_BOUNCE; // Сталкиваться со всем
			// UserData уже должен быть установлен как sprite при создании фикстуры
			// Если UserData нужно менять динамически, это сложнее
		} else {
			// Восстанавливаем маску, соответствующую текущему типу
			switch(this.type) {
				case DYNAMIC:
				case FIXED:
					newMask = PhysicsWorld.MASK_PHYSICSOBJECT;
					break;
				case NONE:
				default:
					newMask = PhysicsWorld.MASK_NO_COLLISION;
					break;
			}
		}
		// Применяем новую маску ко всем фикстурам
		setCollisionMask(newMask);
	}

	private void setCollisionMask(short maskBits) {
		// Сохраняем для шаблона
		shapeDefTemplate.filter().maskBits(maskBits);

		// Применяем ко всем существующим фикстурам
		if (bodyId == null || bodyId.isNull()) return;
		int shapeCount = Box2d.b2Body_GetShapeCount(bodyId);
		if (shapeCount > 0) {
			b2ShapeId.b2ShapeIdPointer shapeIdPtr = new b2ShapeId.b2ShapeIdPointer(shapeCount, false);
			int fetchedCount = Box2d.b2Body_GetShapes(bodyId, shapeIdPtr, shapeCount);
			b2Filter tempFilter = new b2Filter(); // Временный фильтр для установки

			for (int i = 0; i < fetchedCount; i++) {
				b2ShapeId shapeId = shapeIdPtr.get(i);
				if (shapeId != null && !shapeId.isNull()) {
					// Получаем текущий фильтр фикстуры
					Box2d.b2Shape_GetFilter(shapeId, tempFilter); // ПРЕДПОЛОЖЕНИЕ API
					// Меняем только маску
					tempFilter.maskBits(maskBits);
					// Устанавливаем обновленный фильтр
					try {
						Box2d.b2Shape_SetFilter(shapeId, tempFilter); // ПРЕДПОЛОЖЕНИЕ API
					} catch (Exception e) {
						Log.e(TAG, "Error setting filter data for shapeId: " + shapeId, e);
					}
				}
			}
			// Обновляем состояние не-столкновения в Look (если нужно)
			updateNonCollidingState();
		}
	}



	// Устанавливает категорию столкновений для ВСЕХ фикстур тела
	private void setCollisionCategory(short categoryBits) {
		// Сохраняем для шаблона
		shapeDefTemplate.filter().categoryBits(categoryBits);
		this.categoryMaskRecord = categoryBits; // Сохраняем как текущую категорию

		// Применяем ко всем существующим фикстурам
		if (bodyId == null || bodyId.isNull()) return;
		int shapeCount = Box2d.b2Body_GetShapeCount(bodyId);
		if (shapeCount > 0) {
			b2ShapeId.b2ShapeIdPointer shapeIdPtr = new b2ShapeId.b2ShapeIdPointer(shapeCount, false);
			int fetchedCount = Box2d.b2Body_GetShapes(bodyId, shapeIdPtr, shapeCount);
			b2Filter tempFilter = new b2Filter(); // Временный фильтр для установки

			for (int i = 0; i < fetchedCount; i++) {
				b2ShapeId shapeId = shapeIdPtr.get(i);
				if (shapeId != null && !shapeId.isNull()) {
					// Получаем текущий фильтр
					Box2d.b2Shape_GetFilter(shapeId, tempFilter);
					// Меняем только категорию
					tempFilter.categoryBits(categoryBits);
					// Устанавливаем обновленный фильтр
					try {
						Box2d.b2Shape_SetFilter(shapeId, tempFilter);
					} catch (Exception e) {
						Log.e(TAG, "Error setting filter data for shapeId: " + shapeId, e);
					}
				}
			}
		}
	}

	protected void setCollisionBits(short categoryBits, short maskBits) {
		setCollisionBits(categoryBits, maskBits, true);
	}

	protected void setCollisionBits(short categoryBits, short maskBits, boolean updateState) {
		b2Filter filterM = fixtureDef.filter();
		filterM.categoryBits(categoryBits);
		filterM.maskBits(maskBits);

		if (updateState) {
			updateNonCollidingState();
		}
	}


	/**
	 * Обновляет физическую форму (фикстуры) объекта на основе предоставленных геометрий.
	 * Удаляет все старые фикстуры и создает новые.
	 *
	 * @param newGeometries Массив Object[], содержащий b2Polygon и/или b2Circle,
	 *                      или null / пустой массив для удаления всех форм.
	 */
	public void updateFixtures(@Nullable Object[] newGeometries) {
		// Проверка валидности основного ID тела
		if (bodyId == null || bodyId.isNull()) {
			Log.e(TAG, "Cannot update fixtures, bodyId is null or invalid for sprite: " + (sprite != null ? sprite.getName() : "unknown"));
			return;
		}
		Log.d(TAG, "Updating fixtures for sprite: " + (sprite != null ? sprite.getName() : "unknown"));

		// --- Шаг 1: Удаление старых фикстур ---
		try {
			int shapeCount = Box2d.b2Body_GetShapeCount(bodyId);
			if (shapeCount > 0) {
				Log.d(TAG, "Removing " + shapeCount + " old fixtures.");
				// Получаем массив ID существующих форм
				// Создаем указатель для получения ID. false - мы не владеем памятью этого указателя.
				b2ShapeId.b2ShapeIdPointer shapeIdPtr = new b2ShapeId.b2ShapeIdPointer(shapeCount, false);

				// Заполняем указатель фактическими ID форм тела
				// !!! ПРОВЕРЬ ЭТОТ API ВЫЗОВ: Имя и параметры могут отличаться !!!
				int fetchedCount = Box2d.b2Body_GetShapes(bodyId, shapeIdPtr, shapeCount);

				if (fetchedCount != shapeCount) {
					Log.w(TAG, "Mismatch in shape count while fetching shapes for removal. Expected " + shapeCount + ", got " + fetchedCount);
					// Продолжаем с тем, что получили, но это странно
				}

				// Создаем временный массив для хранения ID, чтобы избежать проблем при итерации и удалении
				b2ShapeId[] idsToRemove = new b2ShapeId[fetchedCount];
				for (int i = 0; i < fetchedCount; i++) {
					idsToRemove[i] = shapeIdPtr.get(i); // Копируем ID (объект-обертку)
				}

				// Удаляем фикстуры по скопированным ID
				for (b2ShapeId oldShapeId : idsToRemove) {
					if (oldShapeId != null && !oldShapeId.isNull()) {
						// !!! ПРОВЕРЬ ЭТОТ API ВЫЗОВ !!!
						Box2d.b2DestroyShape(oldShapeId, true); // true - обновить массу тела после удаления
					}
				}
				// Освобождать shapeIdPtr не нужно, т.к. freeOnGC=false
			} else {
				Log.d(TAG, "No old fixtures to remove.");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error removing old fixtures for sprite: " + (sprite != null ? sprite.getName() : "unknown"), e);
			// Продолжаем попытку добавить новые, но состояние может быть некорректным
		}

		// --- Шаг 2: Создание новых фикстур ---
		if (newGeometries != null && newGeometries.length > 0) {
			Log.d(TAG, "Adding " + newGeometries.length + " new fixtures.");
			for (Object geometry : newGeometries) {
				if (geometry == null) {
					Log.w(TAG, "Skipping null geometry in newGeometries array.");
					continue;
				}

				// Создаем новый Def для этой фикстуры
				b2ShapeDef currentDef = new b2ShapeDef();
				b2ShapeId newShapeId = null; // Для хранения ID созданной формы

				try {
					// --- Настройка b2ShapeDef ---
					// Копируем актуальные свойства из полей PhysicsObject
					currentDef.density(this.density);
					currentDef.material().friction(this.friction);
					currentDef.material().restitution(this.bounceFactor);
					currentDef.isSensor(false); // По умолчанию не сенсор

					// UserData НЕ устанавливаем здесь, т.к. b2ShapeDef принимает VoidPointer.
					// Связь Sprite <-> BodyId поддерживается в PhysicsWorld.

					// Копируем актуальные настройки фильтра из шаблона (который обновляется в setCollisionMask/Category)
					b2Filter currentFilter = currentDef.filter();
					b2Filter templateFilter = shapeDefTemplate.filter();
					currentFilter.categoryBits(templateFilter.categoryBits());
					currentFilter.maskBits(templateFilter.maskBits());
					currentFilter.groupIndex(templateFilter.groupIndex());

					// --- Создание фикстуры в зависимости от типа геометрии ---
					if (geometry instanceof b2Polygon) {
						b2Polygon polygon = (b2Polygon) geometry;
						if (!polygon.isNull()) {
							// !!! ПРОВЕРЬ ЭТОТ API ВЫЗОВ !!!
							newShapeId = Box2d.b2CreatePolygonShape(bodyId, currentDef.asPointer(), polygon.asPointer());
						} else {
							Log.w(TAG, "Skipping null/invalid b2Polygon geometry.");
						}
					} else if (geometry instanceof b2Circle) {
						b2Circle circle = (b2Circle) geometry;
						if (!circle.isNull()) {
							// !!! ПРОВЕРЬ ЭТОТ API ВЫЗОВ !!!
							newShapeId = Box2d.b2CreateCircleShape(bodyId, currentDef.asPointer(), circle.asPointer());
						} else {
							Log.w(TAG, "Skipping null/invalid b2Circle geometry.");
						}
					} else {
						Log.w(TAG, "Unsupported geometry type in updateFixtures: " + geometry.getClass().getName());
					}

					// Проверка результата создания
					if (newShapeId == null || newShapeId.isNull()) {
						// Не удалось создать фикстуру, но не прерываем цикл, пытаемся создать остальные
						Log.e(TAG, "Failed to create fixture for geometry type: " + geometry.getClass().getSimpleName());
					} else {
						Log.d(TAG, "Successfully created fixture with shapeId: " + newShapeId + " for geometry: " + geometry.getClass().getSimpleName());
						// Здесь можно было бы добавить newShapeId в какой-нибудь локальный список, если он нужен
					}

				} catch (Exception e) {
					// Ошибка при настройке b2ShapeDef или создании фикстуры
					Log.e(TAG, "Error processing or creating fixture for geometry: " + geometry.getClass().getSimpleName(), e);
					// Продолжаем со следующей геометрией
				}
			}
		} else {
			Log.d(TAG, "No new geometries provided or array is empty.");
		}

		// --- Шаг 3: Обновление массы (Опционально) ---
		// Обычно Box2D 3.0 делает это автоматически при создании/удалении фикстур с флагом updateMass=true
		// Если есть проблемы с массой, можно раскомментировать:
		// try {
		//     Box2d.b2Body_ApplyMassFromShapes(bodyId);
		// } catch (Exception e) {
		//     Log.e(TAG, "Error applying mass from shapes", e);
		// }

		Log.d(TAG, "Fixtures update process finished for sprite: " + (sprite != null ? sprite.getName() : "unknown"));
	}

	private void updateNonCollidingState() {
		Sprite sprite = getSpriteForBody(this.bodyId);
		Object look = sprite.look;
		if (look instanceof PhysicsLook) {
			((PhysicsLook) look).setNonColliding(isNonColliding());
		}
	}

	public void getBoundaryBox(Vector2 lowerLeft, Vector2 upperRight) {
		calculateAabb();
		lowerLeft.x = PhysicsWorldConverter.convertBox2dToNormalVector(bodyAabbLowerLeft).x;
		lowerLeft.y = PhysicsWorldConverter.convertBox2dToNormalVector(bodyAabbLowerLeft).y;
		upperRight.x = PhysicsWorldConverter.convertBox2dToNormalVector(bodyAabbUpperRight).x;
		upperRight.y = PhysicsWorldConverter.convertBox2dToNormalVector(bodyAabbUpperRight).y;
	}

	public Vector2 getBoundaryBoxDimensions() {
		calculateAabb();
		float aabbWidth = PhysicsWorldConverter.convertBox2dToNormalCoordinate(Math.abs(bodyAabbUpperRight.x - bodyAabbLowerLeft.x)) + 1.0f;
		float aabbHeight = PhysicsWorldConverter.convertBox2dToNormalCoordinate(Math.abs(bodyAabbUpperRight.y - bodyAabbLowerLeft.y)) + 1.0f;
		return new Vector2(aabbWidth, aabbHeight);
	}

	public void activateHangup() {
		velocity = new Vector2(getVelocity());
		rotationSpeed = getRotationSpeed();
		gravityScale = getGravityScale();

		setGravityScale(0);
		setVelocity(0, 0);
		setRotationSpeed(0);
	}

	public void deactivateHangup(boolean record) {
		if (record) {
			setGravityScale(gravityScale);
			setVelocity(velocity.x, velocity.y);
			setRotationSpeed(rotationSpeed);
		} else {
			setGravityScale(1);
		}
	}

	public void activateNonColliding(boolean updateState) {
		setCollisionBits(categoryMaskRecord, PhysicsWorld.MASK_NO_COLLISION, updateState);
	}

	public void deactivateNonColliding(boolean record, boolean updateState) {
		if (record) {
			setCollisionBits(categoryMaskRecord, collisionMaskRecord, updateState);
		}
	}

	public void activateFixed() {
		savedType = getType();
		setType(Type.FIXED);
	}

	public void deactivateFixed(boolean record) {
		if (record) {
			setType(savedType);
		}
	}

	public boolean isNonColliding() {
		return collisionMaskRecord == PhysicsWorld.MASK_NO_COLLISION;
	}

	public Sprite getSpriteForBody(b2BodyId bodyId) {
		if (bodyId == null || bodyId.isNull()) {
			return null;
		}
		return bodyIdToSpriteMap.get(bodyId);
	}

	private void calculateAabb() {
		bodyAabbLowerLeft = new Vector2(Integer.MAX_VALUE, Integer.MAX_VALUE);
		bodyAabbUpperRight = new Vector2(Integer.MIN_VALUE, Integer.MIN_VALUE);
		b2Transform transform = Box2d.b2Body_GetTransform(bodyId);
		int len = Box2d.b2Body_GetShapeCount(bodyId);
		//Array<b2ShapeId> fixtures = body.getFixtureList();
		b2ShapeId.b2ShapeIdPointer xz = null;
		int capacity = 5;
		Box2d.b2Body_GetShapes(bodyId, xz, capacity);
		if (xz.getSize() == 0) {
			bodyAabbLowerLeft.x = 0;
			bodyAabbLowerLeft.y = 0;
			bodyAabbUpperRight.x = 0;
			bodyAabbUpperRight.y = 0;
		}
		for (int i = 0; i < len; i++) {
			b2ShapeId fixturei = xz.get(i);
			calculateAabb(fixturei, transform);
		}
	}

	private void calculateAabb(b2ShapeId fixture, b2Transform transform) {
		fixtureAabbLowerLeft = new Vector2(Integer.MAX_VALUE, Integer.MAX_VALUE);
		fixtureAabbUpperRight = new Vector2(Integer.MIN_VALUE, Integer.MIN_VALUE);
		/*if (fixture.getFFIType() == Shape.Type.Circle) {
			//b2Circle shape = (b2Circle) xz.get(0);
			/*float radius = shape.radius();
			tmpVertice.set(new Vector2(Box2d.b2Body_GetPosition(bodyId).x(), Box2d.b2Body_GetPosition(bodyId).y()));
			tmpVertice.rotate(transform.getRotation()).add(transform.getPosition());
			fixtureAabbLowerLeft.set(tmpVertice.x - radius, tmpVertice.y - radius);
			fixtureAabbUpperRight.set(tmpVertice.x + radius, tmpVertice.y + radius);
		} else if (fixture.getFFIType() == Shape.Type.Polygon) {
			b2Polygon shape = (b2Polygon) fixture.getShape();
			int vertexCount = shape.getVertexCount();

			shape.getVertex(0, tmpVertice);
			fixtureAabbLowerLeft.set(transform.mul(tmpVertice));
			fixtureAabbUpperRight.set(fixtureAabbLowerLeft);
			for (int i = 1; i < vertexCount; i++) {
				shape.getVertex(i, tmpVertice);
				transform.mul(tmpVertice);
				fixtureAabbLowerLeft.x = Math.min(fixtureAabbLowerLeft.x, tmpVertice.x);
				fixtureAabbLowerLeft.y = Math.min(fixtureAabbLowerLeft.y, tmpVertice.y);
				fixtureAabbUpperRight.x = Math.max(fixtureAabbUpperRight.x, tmpVertice.x);
				fixtureAabbUpperRight.y = Math.max(fixtureAabbUpperRight.y, tmpVertice.y);
			}
		}*/

		bodyAabbLowerLeft.x = Math.min(fixtureAabbLowerLeft.x, bodyAabbLowerLeft.x);
		bodyAabbLowerLeft.y = Math.min(fixtureAabbLowerLeft.y, bodyAabbLowerLeft.y);
		bodyAabbUpperRight.x = Math.max(fixtureAabbUpperRight.x, bodyAabbUpperRight.x);
		bodyAabbUpperRight.y = Math.max(fixtureAabbUpperRight.y, bodyAabbUpperRight.y);
	}
}
