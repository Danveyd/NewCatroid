/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 * ... (остальная лицензия) ...
 */
package org.catrobat.catroid.physics;

import android.util.Log; // Используем Android Log

import com.badlogic.gdx.box2d.Box2d;
// Убираем VoidPointer, если не используется для userData
// import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.box2d.structs.b2BodyId;
import com.badlogic.gdx.box2d.structs.b2BodyDef;
import com.badlogic.gdx.box2d.enums.b2BodyType;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
// Убираем b2ShapeId, если не используется
// import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2WorldId;
// Убираем b2WorldDef, если не используется
// import com.badlogic.gdx.box2d.structs.b2WorldDef;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2Rot;

// Добавляем импорты для Map и HashMap
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // Для сравнения bodyId

import androidx.annotation.Nullable; // Для аннотации @Nullable

public class PhysicsBoundaryBox {

	private static final String TAG = "PhysicsBoundaryBox";
	public static final int FRAME_SIZE = 5;

	private final b2WorldId world;
	// --- НОВОЕ ПОЛЕ: Карта для хранения ID тел границ и их идентификаторов ---
	private final Map<b2BodyId, BoundaryBoxIdentifier> boundaryBodies = new HashMap<>();

	public enum BoundaryBoxIdentifier {BBI_HORIZONTAL, BBI_VERTICAL}

	public PhysicsBoundaryBox(b2WorldId world) {
		if (world == null || world.isNull()) {
			Log.e(TAG, "PhysicsBoundaryBox created with a null or invalid worldId!");
			// Бросаем исключение, т.к. без мира нельзя создать границы
			throw new IllegalArgumentException("worldId cannot be null or invalid");
		}
		this.world = world;
	}

	/**
	 * Создает четыре статических тела, представляющих границы мира.
	 * Сохраняет их b2BodyId и тип (горизонтальный/вертикальный).
	 *
	 * @param width  Ширина видимой области мира (в координатах Catroid).
	 * @param height Высота видимой области мира (в координатах Catroid).
	 */
	public void create(int width, int height) {
		// Очищаем старые записи на случай повторного вызова create()
		// (хотя это не должно происходить без dispose)
		clearBoundaryBodies(); // Добавим метод очистки

		float boxWidth = PhysicsWorldConverter.convertNormalToBox2dCoordinate(width);
		float boxHeight = PhysicsWorldConverter.convertNormalToBox2dCoordinate(height);
		float boxElementSize = PhysicsWorldConverter.convertNormalToBox2dCoordinate(PhysicsBoundaryBox.FRAME_SIZE);
		float halfBoxElementSize = boxElementSize / 2.0f;

		// Создаем каждую сторону, передавая идентификатор
		// Top
		createSide(new Vector2(0.0f, (boxHeight / 2.0f) + halfBoxElementSize), boxWidth, boxElementSize, BoundaryBoxIdentifier.BBI_HORIZONTAL);
		// Bottom
		createSide(new Vector2(0.0f, -(boxHeight / 2.0f) - halfBoxElementSize), boxWidth, boxElementSize, BoundaryBoxIdentifier.BBI_HORIZONTAL);
		// Left
		createSide(new Vector2(-(boxWidth / 2.0f) - halfBoxElementSize, 0.0f), boxElementSize, boxHeight, BoundaryBoxIdentifier.BBI_VERTICAL);
		// Right
		createSide(new Vector2((boxWidth / 2.0f) + halfBoxElementSize, 0.0f), boxElementSize, boxHeight, BoundaryBoxIdentifier.BBI_VERTICAL);

		Log.d(TAG, "Created " + boundaryBodies.size() + " boundary bodies.");
	}

	/**
	 * Создает одно статическое тело (стенку) с заданными параметрами и
	 * сохраняет его b2BodyId и идентификатор в карту boundaryBodies.
	 *
	 * @param center     Центр стенки (в координатах Box2D).
	 * @param width      Ширина стенки (в координатах Box2D).
	 * @param height     Высота стенки (в координатах Box2D).
	 * @param identifier Тип границы (HORIZONTAL или VERTICAL).
	 */
	private void createSide(Vector2 center, float width, float height, BoundaryBoxIdentifier identifier) {
		b2BodyDef bodyDef = new b2BodyDef();
		bodyDef.type(b2BodyType.b2_staticBody);
		bodyDef.enableSleep(false); // Статические тела и так не спят, но для ясности

		// Установка позиции тела в bodyDef (более стандартный способ)
		b2Vec2 positionVec = bodyDef.position(); // Получаем ссылку на вектор позиции в bodyDef
		positionVec.x(center.x);
		positionVec.y(center.y);
		// bodyDef.angle(0.0f); // Угол по умолчанию 0

		// Создаем тело
		b2BodyId bodyId = Box2d.b2CreateBody(world, bodyDef.asPointer());

		if (bodyId == null || bodyId.isNull()) {
			Log.e(TAG, "Failed to create boundary body for identifier: " + identifier + " at center: " + center);
			return; // Не удалось создать тело, не можем продолжить
		}

		// Создаем форму (полигон - прямоугольник)
		// Используем хелпер b2MakeBox для создания полигона правильного размера
		// Он НЕ требует смещения, т.к. позиция уже установлена в bodyDef
		b2Polygon shape = Box2d.b2MakeBox(width / 2.0f, height / 2.0f);
		if (shape == null || shape.isNull()) {
			Log.e(TAG, "Failed to create polygon shape (b2MakeBox) for boundary: " + identifier);
			// Важно: Если форма не создалась, нужно удалить тело, которое мы только что создали!
			Box2d.b2DestroyBody(bodyId); // Очистка
			return;
		}


		// Создаем определение фикстуры (b2ShapeDef)
		b2ShapeDef fixtureDef = new b2ShapeDef();
		// Устанавливаем фильтрацию коллизий
		fixtureDef.filter().maskBits(PhysicsWorld.MASK_BOUNDARYBOX);
		fixtureDef.filter().categoryBits(PhysicsWorld.CATEGORY_BOUNDARYBOX);
		// Устанавливаем свойства материала (для статики не так важно, но можно задать)
		fixtureDef.density(0.0f); // Плотность 0 для статики
		//fixtureDef.friction(0.6f); // Примерное трение
		// НЕ УСТАНАВЛИВАЕМ userData, т.к. используем поиск по bodyId
		// fixtureDef.userData(...);

		// Создаем фикстуру на теле, используя созданную форму и определение
		// !!! ПРОВЕРЬ ТОЧНОЕ ИМЯ МЕТОДА API для создания фикстуры из полигона !!!
		// Возможные варианты: b2Body_CreatePolygonFixture, b2CreatePolygonShape (менее вероятно)
		// ПРЕДПОЛОЖЕНИЕ: Используем метод, принимающий bodyId, ShapeDef и Polygon
		try {
			Box2d.b2CreatePolygonShape(bodyId, fixtureDef.asPointer(), shape.asPointer()); // ПРОВЕРЬ ЭТОТ ВЫЗОВ!
			// Если фикстура успешно создана, сохраняем ID тела и идентификатор
			boundaryBodies.put(bodyId, identifier);
			Log.d(TAG, "Successfully created boundary: " + identifier + " with bodyId: " + bodyId);

		} catch (Exception e) {
			Log.e(TAG, "Failed to create fixture for boundary: " + identifier + " with bodyId: " + bodyId, e);
			// Если фикстура не создалась, удаляем тело
			Box2d.b2DestroyBody(bodyId);
		} finally {
			// Освобождаем память, выделенную для временной формы shape, если она управляется GC (freeOnGC=true)
			// Если b2MakeBox возвращает объект с freeOnGC=false, нужно вызвать shape.dispose() или shape.free()
			// Скорее всего, хелперы вроде b2MakeBox возвращают управляемые объекты.
			// if (shape != null && !shape.isNull() && !shape.getFreeOnGC()) shape.dispose();
		}
	}

	/**
	 * Находит идентификатор границы (HORIZONTAL/VERTICAL) по идентификатору тела Box2D.
	 * Вызывается из PhysicsCollisionListener.
	 *
	 * @param bodyId Идентификатор тела Box2D для проверки.
	 * @return Соответствующий BoundaryBoxIdentifier, если тело является одной из границ, иначе null.
	 */
	@Nullable
	public BoundaryBoxIdentifier getIdentifierForBody(b2BodyId bodyId) {
		if (bodyId == null || bodyId.isNull()) {
			return null;
		}
		// Ищем ID в нашей карте. .get() вернет значение (Identifier) или null, если ключ не найден.
		// Сравнение b2BodyId должно работать корректно, если equals/hashCode переопределены в структуре.
		return boundaryBodies.get(bodyId);
	}

	/**
	 * Очищает карту хранения тел границ.
	 * Вызывать перед destroyWorld или при пересоздании границ.
	 */
	private void clearBoundaryBodies() {
		// Не нужно уничтожать тела здесь, т.к. b2DestroyWorld должен это сделать.
		// Просто очищаем нашу Java-карту.
		boundaryBodies.clear();
	}

	/**
	 * Очищает ресурсы, связанные с границами (в данном случае, просто карту).
	 * Вызывать из PhysicsWorld.dispose().
	 */
	public void dispose() {
		Log.d(TAG, "Disposing PhysicsBoundaryBox...");
		clearBoundaryBodies();
		// Если бы создавались другие нативные ресурсы, их нужно было бы чистить здесь.
	}
}