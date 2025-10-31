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
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.physics.box2d.joints.GearJointDef;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.physics.box2d.joints.PulleyJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.XmlHeader;
import org.catrobat.catroid.physics.shapebuilder.PhysicsShapeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@LunoClass
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
	public static final boolean IGNORE_SLEEPING_OBJECTS = true;
	public static Vector2 activeArea;

	public static final int STABILIZING_STEPS = 6;
	private final World world = new World(PhysicsWorld.DEFAULT_GRAVITY, PhysicsWorld.IGNORE_SLEEPING_OBJECTS);
	private final Map<Sprite, PhysicsObject> physicsObjects = new HashMap<>();
	private final ArrayList<Sprite> activeVerticalBounces = new ArrayList<>();
	private final ArrayList<Sprite> activeHorizontalBounces = new ArrayList<>();
	private final Map<String, Joint> joints = new HashMap<>();

	private Box2DDebugRenderer renderer;
	private int stabilizingSteCounter = 0;
	private PhysicsBoundaryBox boundaryBox;

	private PhysicsShapeBuilder physicsShapeBuilder = PhysicsShapeBuilder.getInstance();

	public static class RayCastResult {
		public boolean hasHit = false;
		public Sprite hitSprite = null;
		public Vector2 hitPoint = new Vector2();
		public Vector2 hitNormal = new Vector2();
		public float hitFraction = -1f; // Расстояние от 0.0 до 1.0
	}
	private final Map<String, RayCastResult> rayCastResults = new HashMap<>();
	private final RayCastResult currentRayCastResult = new RayCastResult();
	private final ClosestRayCastCallback closestRayCastCallback = new ClosestRayCastCallback();

	public PhysicsWorld(Project project) {
		this(ScreenValues.currentScreenResolution.getWidth(), ScreenValues.currentScreenResolution.getHeight(), project);
	}

	public PhysicsWorld(int width, int height, Project project) {
		XmlHeader xml = project.getXmlHeader();
		ACTIVE_AREA_WIDTH_FACTOR = xml.getPhysicsWidthArea();
		ACTIVE_AREA_HEIGHT_FACTOR = xml.getPhysicsHeightArea();
		boundaryBox = new PhysicsBoundaryBox(world);
		boundaryBox.create(width, height);
		activeArea = new Vector2(width * ACTIVE_AREA_WIDTH_FACTOR, height * ACTIVE_AREA_HEIGHT_FACTOR);
		world.setContactListener(new PhysicsCollisionListener(this));
	}

	public PhysicsWorld(int width, int height) {
		ACTIVE_AREA_WIDTH_FACTOR = 3.0f;
		ACTIVE_AREA_HEIGHT_FACTOR = 2.0f;
		boundaryBox = new PhysicsBoundaryBox(world);
		boundaryBox.create(width, height);
		activeArea = new Vector2(width * ACTIVE_AREA_WIDTH_FACTOR, height * ACTIVE_AREA_HEIGHT_FACTOR);
		world.setContactListener(new PhysicsCollisionListener(this));
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
		if (stabilizingSteCounter < STABILIZING_STEPS) {
			stabilizingSteCounter++;
		} else {
			try {
				world.step(deltaTime, PhysicsWorld.VELOCITY_ITERATIONS, PhysicsWorld.POSITION_ITERATIONS);
			} catch (Exception exception) {
				Log.e(TAG, Log.getStackTraceString(exception));
			}
		}
	}

	public void render(Matrix4 perspectiveMatrix) {
		if (renderer == null) {
			renderer = new Box2DDebugRenderer(PhysicsDebugSettings.Render.RENDER_BODIES,
					PhysicsDebugSettings.Render.RENDER_JOINTS, PhysicsDebugSettings.Render.RENDER_AABB,
					PhysicsDebugSettings.Render.RENDER_INACTIVE_BODIES, PhysicsDebugSettings.Render.RENDER_VELOCITIES,
					PhysicsDebugSettings.Render.RENDER_CONTACTS);
		}
		renderer.render(world, perspectiveMatrix.scl(PhysicsWorld.RATIO));
	}

	public boolean createPrismaticJoint(String jointId, Sprite spriteA, Sprite spriteB, Vector2 worldAnchor, Vector2 worldAxis) {
		if (jointId == null || jointId.isEmpty() || joints.containsKey(jointId)) return false;
		PhysicsObject objA = getPhysicsObject(spriteA);
		PhysicsObject objB = getPhysicsObject(spriteB);

		PrismaticJointDef jointDef = new PrismaticJointDef();
		jointDef.initialize(objA.body, objB.body, PhysicsWorldConverter.convertCatroidToBox2dVector(worldAnchor), PhysicsWorldConverter.convertCatroidToBox2dVector(worldAxis));
		jointDef.collideConnected = false;

		Joint joint = world.createJoint(jointDef);
		joints.put(jointId, joint);
		return true;
	}

	public boolean createRevoluteJoint(String jointId, Sprite spriteA, Sprite spriteB, Vector2 worldAnchorPoint) {
		if (jointId == null || jointId.isEmpty() || joints.containsKey(jointId)) return false;

		PhysicsObject objA = getPhysicsObject(spriteA);
		PhysicsObject objB = getPhysicsObject(spriteB);
		if (objA == null || objB == null) return false;

		RevoluteJointDef jointDef = new RevoluteJointDef();
		Vector2 box2dAnchor = PhysicsWorldConverter.convertCatroidToBox2dVector(worldAnchorPoint);
		jointDef.initialize(objA.body, objB.body, box2dAnchor);
		jointDef.collideConnected = false;

		Joint joint = world.createJoint(jointDef);
		joints.put(jointId, joint);
		return true;
	}

	public boolean createDistanceJoint(String jointId, Sprite spriteA, Sprite spriteB, Float length, Float frequency, Float damping) {
		if (jointId == null || jointId.isEmpty() || joints.containsKey(jointId)) return false;

		PhysicsObject objA = getPhysicsObject(spriteA);
		PhysicsObject objB = getPhysicsObject(spriteB);
		if (objA == null || objB == null) return false;

		DistanceJointDef jointDef = new DistanceJointDef();
		jointDef.initialize(objA.body, objB.body, objA.body.getWorldCenter(), objB.body.getWorldCenter());
		jointDef.collideConnected = false;

		if (length != null) {
			jointDef.length = PhysicsWorldConverter.convertNormalToBox2dCoordinate(length);
		}
		if (frequency != null) {
			jointDef.frequencyHz = frequency;
		}
		if (damping != null) {
			jointDef.dampingRatio = damping;
		}

		Joint joint = world.createJoint(jointDef);
		joints.put(jointId, joint);
		return true;
	}

	public boolean createWeldJoint(String jointId, Sprite spriteA, Sprite spriteB, Vector2 worldAnchorPoint) {
		if (jointId == null || jointId.isEmpty() || joints.containsKey(jointId)) return false;

		PhysicsObject objA = getPhysicsObject(spriteA);
		PhysicsObject objB = getPhysicsObject(spriteB);
		if (objA == null || objB == null) return false;

		WeldJointDef jointDef = new WeldJointDef();
		Vector2 box2dAnchor = PhysicsWorldConverter.convertCatroidToBox2dVector(worldAnchorPoint);
		jointDef.initialize(objA.body, objB.body, box2dAnchor);
		jointDef.collideConnected = false;

		Joint joint = world.createJoint(jointDef);
		joints.put(jointId, joint);
		return true;
	}

	public boolean createPulleyJoint(String jointId, Sprite spriteA, Sprite spriteB, Vector2 groundAnchorA, Vector2 groundAnchorB, float ratio) {
		if (jointId == null || jointId.isEmpty() || joints.containsKey(jointId)) return false;

		PhysicsObject objA = getPhysicsObject(spriteA);
		PhysicsObject objB = getPhysicsObject(spriteB);
		if (objA == null || objB == null) return false;

		PulleyJointDef jointDef = new PulleyJointDef();
		Vector2 gAnchorA_b2d = PhysicsWorldConverter.convertCatroidToBox2dVector(groundAnchorA);
		Vector2 gAnchorB_b2d = PhysicsWorldConverter.convertCatroidToBox2dVector(groundAnchorB);
		Vector2 anchorA_b2d = objA.body.getWorldCenter();
		Vector2 anchorB_b2d = objB.body.getWorldCenter();

		jointDef.initialize(objA.body, objB.body, gAnchorA_b2d, gAnchorB_b2d, anchorA_b2d, anchorB_b2d, ratio);
		jointDef.collideConnected = false;

		Joint joint = world.createJoint(jointDef);
		joints.put(jointId, joint);
		return true;
	}

	public boolean createGearJoint(String jointId, String jointAId, String jointBId, float ratio) {
		if (jointId == null || jointId.isEmpty() || joints.containsKey(jointId)) {
			return false;
		}

		Joint jointA = joints.get(jointAId);
		Joint jointB = joints.get(jointBId);

		if (jointA == null || jointB == null) {
			Log.e(TAG, "Cannot create GearJoint: one of the child joints was not found.");
			return false;
		}

		GearJointDef jointDef = new GearJointDef();
		jointDef.joint1 = jointA;
		jointDef.joint2 = jointB;

		jointDef.bodyA = jointA.getBodyA();
		jointDef.bodyB = jointB.getBodyB();

		jointDef.ratio = ratio;

		Joint joint = world.createJoint(jointDef);
		joints.put(jointId, joint);
		return true;
	}

	public void destroyJoint(String jointId) {
		if (jointId != null && joints.containsKey(jointId)) {
			Joint joint = joints.remove(jointId);
			if (joint != null) {
				world.destroyJoint(joint);
			}
		}
	}

	public void applyForce(Sprite sprite, Vector2 force, Vector2 point) {
		PhysicsObject obj = getPhysicsObject(sprite);
		obj.body.applyForce(force, obj.body.getWorldPoint(PhysicsWorldConverter.convertCatroidToBox2dVector(point)), true);
	}

	public void applyImpulse(Sprite sprite, Vector2 impulse, Vector2 point) {
		PhysicsObject obj = getPhysicsObject(sprite);
		obj.body.applyLinearImpulse(impulse, obj.body.getWorldPoint(PhysicsWorldConverter.convertCatroidToBox2dVector(point)), true);
	}

	public void applyTorque(Sprite sprite, float torque) {
		getPhysicsObject(sprite).body.applyTorque(torque, true);
	}

	public void applyAngularImpulse(Sprite sprite, float impulse) {
		getPhysicsObject(sprite).body.applyAngularImpulse(impulse, true);
	}

	private class ClosestRayCastCallback implements RayCastCallback {
		public ClosestRayCastCallback() {
			super();
		}

		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			if (fraction < currentRayCastResult.hitFraction || !currentRayCastResult.hasHit) {
				currentRayCastResult.hasHit = true;
				currentRayCastResult.hitSprite = (Sprite) fixture.getBody().getUserData();
				currentRayCastResult.hitPoint = PhysicsWorldConverter.convertBox2dToNormalVector(point);
				currentRayCastResult.hitNormal = normal.cpy();
				currentRayCastResult.hitFraction = fraction;
			}
			return fraction;
		}
	}

	public void performRayCast(String rayId, Vector2 start, Vector2 end) {
		currentRayCastResult.hasHit = false;
		currentRayCastResult.hitFraction = -1f;

		world.rayCast(closestRayCastCallback, PhysicsWorldConverter.convertCatroidToBox2dVector(start), PhysicsWorldConverter.convertCatroidToBox2dVector(end));

		RayCastResult finalResult = new RayCastResult();
		finalResult.hasHit = currentRayCastResult.hasHit;
		if(finalResult.hasHit) {
			finalResult.hitSprite = currentRayCastResult.hitSprite;
			finalResult.hitPoint = currentRayCastResult.hitPoint;
			finalResult.hitNormal = currentRayCastResult.hitNormal;
			finalResult.hitFraction = currentRayCastResult.hitFraction;
		}
		rayCastResults.put(rayId, finalResult);
	}

	public RayCastResult getRayCastResult(String rayId) {
		return rayCastResults.get(rayId);
	}

	public void setGravity(float x, float y) {
		world.setGravity(new Vector2(x, y));
	}

	public Vector2 getGravity() {
		return world.getGravity();
	}

	public void changeLook(PhysicsObject physicsObject, Look look) {
		Shape[] shapes = null;
		if (look.getLookData() != null && look.getLookData().getFile() != null) {
			shapes = physicsShapeBuilder.getScaledShapes(look.getLookData(),
					look.getSizeInUserInterfaceDimensionUnit() / 100f);
		}
		physicsObject.setShape(shapes);
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
		BodyDef bodyDef = new BodyDef();
		return new PhysicsObject(world.createBody(bodyDef), sprite);
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
}
