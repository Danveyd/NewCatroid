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

import com.badlogic.gdx.box2d.enums.b2BodyType;
import com.badlogic.gdx.box2d.structs.b2Rot;
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

import org.catrobat.catroid.content.Sprite;

import java.util.Arrays;

import androidx.annotation.VisibleForTesting;

public class PhysicsObject {

	public enum Type {
		DYNAMIC, FIXED, NONE
	}

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

	private final b2ShapeDef shapeDefTemplate = new b2ShapeDef();

	public PhysicsObject(b2BodyId bodyId, b2WorldId worldId, PhysicsWorld physicsWorldWrapper, Sprite sprite) {
		if (bodyId == null || bodyId.isNull() || worldId == null || worldId.isNull() || physicsWorldWrapper == null) {
			throw new IllegalArgumentException("Valid BodyId, WorldId, and PhysicsWorld wrapper are required for PhysicsObject");
		}
		this.bodyId = bodyId;
		this.worldId = worldId;
		this.physicsWorldWrapper = physicsWorldWrapper; // Сохраняем ссылку на обертку

		// --- Проверка UserData (опционально, но полезно для отладки) ---
		// Предполагаем, что PhysicsWorld сохранил Sprite в своей карте bodyIdToUserDataMap
		/*Object currentData = physicsWorldWrapper.getUserDataForBody(bodyId); // Нужен метод в PhysicsWorld для этого!
		if (currentData != sprite) {
			// Можно кинуть исключение или вывести серьезное предупреждение
			System.err.println("CRITICAL WARNING: UserData mismatch in PhysicsObject constructor! Expected Sprite: "
					+ (sprite != null ? sprite.getName() : "null") + ", but found: " + currentData);
			// Это может указывать на проблемы с управлением UserData в PhysicsWorld
		}*/

		// --- Инициализация шаблона свойств формы ---
		shapeDefTemplate.density(PhysicsObject.DEFAULT_DENSITY);
		Box2d.b2CreateChain(bodyId, chain.asPointer());
		/*shapeDefTemplate.friction = PhysicsObject.DEFAULT_FRICTION;
		shapeDefTemplate.restitution = PhysicsObject.DEFAULT_BOUNCE_FACTOR;
		// Установка фильтров по умолчанию (категория объекта, маска зависит от типа)
		shapeDefTemplate.filter.categoryBits = PhysicsWorld.CATEGORY_PHYSICSOBJECT;
		// Маска будет установлена при вызове setType*/

		// --- Установка начального состояния ---
		setType(Type.NONE); // Устанавливаем начальный тип (это вызовет API Box2D)
		mass = PhysicsObject.DEFAULT_MASS; // Устанавливаем массу по умолчанию
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

	public void setType(Type type) {
		if (this.type == type) {
			return;
		}
		this.type = type;

		switch (type) {
			case DYNAMIC:
				Box2d.b2Body_SetType(bodyId, b2BodyType.b2_dynamicBody);
				Box2d.b2Body_SetGravityScale(bodyId,1.0f);
				Box2d.b2Body_SetBullet(bodyId,true);
				setMass(mass);
				collisionMaskRecord = PhysicsWorld.MASK_PHYSICSOBJECT;
				break;
			case FIXED:
				Box2d.b2Body_SetType(bodyId, b2BodyType.b2_kinematicBody);
				Box2d.b2Body_SetGravityScale(bodyId,1.0f);
				collisionMaskRecord = PhysicsWorld.MASK_PHYSICSOBJECT;
				break;
			case NONE:
				Box2d.b2Body_SetType(bodyId, b2BodyType.b2_kinematicBody);
				Box2d.b2Body_SetGravityScale(bodyId,1.0f);
				collisionMaskRecord = PhysicsWorld.MASK_NO_COLLISION;
				break;
		}
		calculateCircumference();
		setCollisionBits(categoryMaskRecord, collisionMaskRecord);
	}

	public float getDirection() {
		return PhysicsWorldConverter.convertBox2dToNormalAngle(body.angularDamping());
	}

	public void setDirection(float degrees) {
		b2Rot r = new b2Rot();
		r.s(PhysicsWorldConverter.convertNormalToBox2dAngle(degrees));
		r.c(PhysicsWorldConverter.convertNormalToBox2dAngle(degrees));
		Box2d.b2Body_SetTransform(bodyId, body.position(), r);
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
		if (friction < MIN_FRICTION) {
			friction = MIN_FRICTION;
		}
		if (friction > MAX_FRICTION) {
			friction = MAX_FRICTION;
		}

		Box2d.b2Chain_SetFriction(chainId, friction);
		b2ShapeId.b2ShapeIdPointer xz = null;
		int capacity = 5;
		int i = 0;
		Box2d.b2Body_GetShapes(bodyId, xz, capacity);
		for (i = 0; i < xz.getSize(); i++) {
			b2ShapeId fixture = xz.get(i);
			b2ChainId c = Box2d.b2Shape_GetParentChain(fixture);
			Box2d.b2Chain_SetFriction(c, friction);
		}
	}

	public void setBounceFactor(float bounceFactor) {
		if (bounceFactor < MIN_BOUNCE_FACTOR) {
			bounceFactor = MIN_BOUNCE_FACTOR;
		}
		Box2d.b2Shape_SetRestitution(fixtureId, bounceFactor);
		b2ShapeId.b2ShapeIdPointer xz = null;
		int capacity = 5;
		int i = 0;
		Box2d.b2Body_GetShapes(bodyId, xz, capacity);
		for (i = 0; i < xz.getSize(); i++) {
			b2ShapeId fixture = xz.get(i);
			Box2d.b2Shape_SetRestitution(fixture, bounceFactor);
		}
	}

	public void setGravityScale(float scale) {
		body.gravityScale(scale);
	}

	public float getGravityScale() {
		return body.gravityScale();
	}

	public void setFixedRotation(boolean flag) {
		body.fixedRotation(flag);
	}

	public void setIfOnEdgeBounce(boolean bounce, Sprite sprite) {
		if (ifOnEdgeBounce == bounce) {
			return;
		}
		ifOnEdgeBounce = bounce;

		short maskBits;
		if (bounce) {
			maskBits = PhysicsWorld.MASK_TO_BOUNCE;
			Box2d.b2Body_SetUserData(bodyId, sprite);
		} else {
			maskBits = PhysicsWorld.MASK_PHYSICSOBJECT;
		}

		setCollisionBits(categoryMaskRecord, maskBits);
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

	private void updateNonCollidingState() {
		if (Box2d.b2Body_GetUserData(bodyId) != null && Box2d.b2Body_GetUserData(bodyId) instanceof Sprite) {
			Object look = ((Sprite) Box2d.b2Body_GetUserData(bodyId)).look;
			if (look != null && look instanceof PhysicsLook) {
				((PhysicsLook) look).setNonColliding(isNonColliding());
			}
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
