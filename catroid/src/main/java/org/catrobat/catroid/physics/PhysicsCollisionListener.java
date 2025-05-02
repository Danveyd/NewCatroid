/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 * ... (остальная лицензия) ...
 */
package org.catrobat.catroid.physics;

import android.util.Log; // Используем Android Log

// --- НУЖНЫ ИМПОРТЫ ДЛЯ ОБЪЕКТОВ СОБЫТИЙ BOX2D 3.0 ---
// Точные имена могут отличаться! Проверь API.
import com.badlogic.gdx.box2d.structs.b2BodyId;      // Может понадобиться для получения UserData
import com.badlogic.gdx.box2d.structs.b2ContactEvents; // Главный объект событий (ПРЕДПОЛОЖЕНИЕ!)
import com.badlogic.gdx.box2d.structs.b2ContactBeginTouchEvent; // Событие начала контакта (ПРЕДПОЛОЖЕНИЕ!)
import com.badlogic.gdx.box2d.structs.b2ContactEndTouchEvent;   // Событие конца контакта (ПРЕДПОЛОЖЕНИЕ!)
import com.badlogic.gdx.box2d.structs.b2ContactHitEvent;      // Событие "хита" (импульса) (ПРЕДПОЛОЖЕНИЕ!)
import com.badlogic.gdx.box2d.structs.b2ShapeId;        // Идентификаторы форм
import com.badlogic.gdx.box2d.structs.b2WorldId;        // Мир нужен для получения UserData? (ПРЕДПОЛОЖЕНИЕ!)
import com.badlogic.gdx.box2d.Box2d;                // Для возможных статических методов API
import com.badlogic.gdx.jnigen.runtime.pointer.VoidPointer;

import org.catrobat.catroid.content.Sprite;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Обрабатывает события столкновений Box2D 3.0, полученные из PhysicsWorld.step().
 * Отслеживает состояние контактов между спрайтами и вызывает соответствующую логику Catroid.
 */
public class PhysicsCollisionListener { // Больше не реализует ContactListener

	private static final String TAG = "PhysicsCollisionListener";

	// Константы (если нужны)
	public static final String COLLISION_MESSAGE_ESCAPE_CHAR = "\t";
	public static final String COLLISION_MESSAGE_CONNECTOR = "<" + COLLISION_MESSAGE_ESCAPE_CHAR
			+ "-" + COLLISION_MESSAGE_ESCAPE_CHAR + ">";

	private final PhysicsWorld physicsWorld; // Ссылка для вызова bouncedOnEdge и доступа к миру (если нужно для UserData)
	private b2WorldId worldId;         // Сохраняем ID мира, если он нужен для API (например, getUserData)
	private final Map<CollidingSprites, PhysicalCollision> collidingSpritesToCollisionMap = new HashMap<>();

	public PhysicsCollisionListener(PhysicsWorld physicsWorld) {
		this.physicsWorld = physicsWorld;
		// Получаем worldId из physicsWorld (добавь getter в PhysicsWorld, если его нет)
		this.worldId = physicsWorld.getWorldId();
	}

	/**
	 * Главный метод обработки событий, вызываемый из PhysicsWorld.step() после b2World_Step.
	 *
	 * @param contactEvents Объект, содержащий все события контакта за последний шаг.
	 */
	public void processContacts(b2ContactEvents contactEvents) {
		if (contactEvents == null || contactEvents.isNull()) {
			return; // Нет событий для обработки
		}

		// --- Обработка НАЧАЛА контактов ---
		// ПРЕДПОЛОЖЕНИЕ о методе getBeginCount() и getBeginContact(i)
		int beginCount = contactEvents.beginCount();
		for (int i = 0; i < beginCount; i++) {
			b2ContactBeginTouchEvent beginEvent = contactEvents.beginEvents().get(i); // ПРЕДПОЛОЖЕНИЕ
			if (beginEvent == null || beginEvent.isNull()) continue;

			// ПРЕДПОЛОЖЕНИЕ о методах shapeIdA() и shapeIdB()
			b2ShapeId shapeIdA = beginEvent.shapeIdA();
			b2ShapeId shapeIdB = beginEvent.shapeIdB();

			// Получаем UserData для каждой формы
			Object userDataA = getUserDataFromShapeId(shapeIdA);
			Object userDataB = getUserDataFromShapeId(shapeIdB);

			if (userDataA != null && userDataB != null) {
				try {
					// Используем старый метод обработки
					handleBeginContact(userDataA, userDataB);
				} catch (Exception e) {
					Log.e(TAG, "Error handling begin contact between: " + userDataA + " and " + userDataB, e);
				}
			} else {
				// Log.w(TAG, "BeginContact: UserData is null for shapeIdA=" + shapeIdA + " or shapeIdB=" + shapeIdB);
			}
		}

		// --- Обработка ОКОНЧАНИЯ контактов ---
		// ПРЕДПОЛОЖЕНИЕ о методе getEndCount() и getEndContact(i)
		int endCount = contactEvents.endCount();
		for (int i = 0; i < endCount; i++) {
			b2ContactEndTouchEvent endEvent = contactEvents.endEvents().get(i); // ПРЕДПОЛОЖЕНИЕ
			if (endEvent == null || endEvent.isNull()) continue;

			// ПРЕДПОЛОЖЕНИЕ о методах shapeIdA() и shapeIdB()
			b2ShapeId shapeIdA = endEvent.shapeIdA();
			b2ShapeId shapeIdB = endEvent.shapeIdB();

			// Получаем UserData
			Object userDataA = getUserDataFromShapeId(shapeIdA);
			Object userDataB = getUserDataFromShapeId(shapeIdB);

			if (userDataA != null && userDataB != null) {
				try {
					// Используем старый метод обработки
					handleEndContact(userDataA, userDataB);
				} catch (Exception e) {
					Log.e(TAG, "Error handling end contact between: " + userDataA + " and " + userDataB, e);
				}
			} else {
				// Log.w(TAG, "EndContact: UserData is null for shapeIdA=" + shapeIdA + " or shapeIdB=" + shapeIdB);
			}
		}

		// --- Обработка "ХИТОВ" (импульсов) ---
		// ПРЕДПОЛОЖЕНИЕ о методе getHitCount() и getHitEvent(i)
		int hitCount = contactEvents.hitCount();
		for (int i = 0; i < hitCount; i++) {
			b2ContactHitEvent hitEvent = contactEvents.hitEvents().get(i); // ПРЕДПОЛОЖЕНИЕ
			if (hitEvent == null || hitEvent.isNull()) continue;

			b2ShapeId shapeIdA = hitEvent.shapeIdA(); // ПРЕДПОЛОЖЕНИЕ
			b2ShapeId shapeIdB = hitEvent.shapeIdB(); // ПРЕДПОЛОЖЕНИЕ

			Object userDataA = getUserDataFromShapeId(shapeIdA);
			Object userDataB = getUserDataFromShapeId(shapeIdB);

			if (userDataA != null && userDataB != null) {
				try {
					// ПРЕДПОЛОЖЕНИЕ о методах для получения нормали и импульса
					float normalX = hitEvent.normal().x();
					float normalY = hitEvent.normal().y();
					float impulse = hitEvent.approachSpeed();
					// Используем старый метод обработки
					handleHitEvent(userDataA, userDataB, normalX, normalY, impulse);
				} catch (Exception e) {
					Log.e(TAG, "Error handling hit event between: " + userDataA + " and " + userDataB, e);
				}
			} else {
				// Log.w(TAG, "HitEvent: UserData is null for shapeIdA=" + shapeIdA + " or shapeIdB=" + shapeIdB);
			}
		}

		// --- Обработка других типов событий (если нужно) ---
		// Например, события сенсоров (begin/end sensor touch)
		// ПРЕДПОЛОЖЕНИЕ об API
        /*
        int sensorBeginCount = contactEvents.getSensorBeginCount();
        for (int i = 0; i < sensorBeginCount; i++) { ... }

        int sensorEndCount = contactEvents.getSensorEndCount();
        for (int i = 0; i < sensorEndCount; i++) { ... }
        */
	}

	/**
	 * Получает связанный Sprite или BoundaryBoxIdentifier для данного shapeId,
	 * выполняя поиск через BodyId.
	 *
	 * @param shapeId Идентификатор формы.
	 * @return Объект Sprite или BoundaryBoxIdentifier, или null, если не найден или тело не имеет ожидаемых данных.
	 */
	private Object getUserDataFromShapeId(b2ShapeId shapeId) {
		if (shapeId == null || shapeId.isNull()) {
			return null;
		}

		try {
			// Шаг 1: Получаем BodyId, к которому принадлежит форма
			b2BodyId bodyId = Box2d.b2Shape_GetBody(shapeId);
			if (bodyId == null || bodyId.isNull()) {
				// Это может случиться, если тело уже удалено?
				// Log.w(TAG, "Could not get body for shapeId: " + shapeId);
				return null;
			}

			// Шаг 2: Ищем PhysicsObject или BoundaryBox по BodyId
			// Нам нужен способ в PhysicsWorld найти объект по его BodyId.

			// Сначала проверяем, не граница ли это
			PhysicsBoundaryBox.BoundaryBoxIdentifier boundaryId = physicsWorld.getBoundaryIdentifierFromBodyId(bodyId); // НУЖЕН ЭТОТ МЕТОД
			if (boundaryId != null) {
				// Это одна из границ мира
				return boundaryId;
			}

			// Если не граница, ищем PhysicsObject (который содержит Sprite)
			PhysicsObject physicsObject = physicsWorld.findPhysicsObjectByBodyId(bodyId); // НУЖЕН ЭТОТ МЕТОД
			if (physicsObject != null) {
				// Нашли объект, возвращаем связанный с ним Sprite
				return physicsObject.getSpriteForBody(bodyId);
			}

			// Если не нашли ни границу, ни PhysicsObject
			Log.w(TAG, "No Sprite or Boundary found for bodyId derived from shapeId: " + shapeId + " (BodyId: " + bodyId + ")");
			return null;

		} catch (Exception e) {
			Log.e(TAG, "Error retrieving user data via BodyId lookup for shapeId: " + shapeId, e);
			return null;
		}
	}

	/**
	 * Вызывается из b2ContactEvents.beginContact, когда получены UserData.
	 * Обрабатывает начало контакта между двумя физическими объектами.
	 *
	 * @param userDataA UserData первого объекта.
	 * @param userDataB UserData второго объекта.
	 */
	public void handleBeginContact(Object userDataA, Object userDataB) {
		if (userDataA == null || userDataB == null) {
			// Избегаем NPE, если UserData не был установлен или получен
			return;
		}

		// Обработка столкновения со стенкой
		if (userDataA instanceof Sprite && userDataB instanceof PhysicsBoundaryBox.BoundaryBoxIdentifier) {
			physicsWorld.bouncedOnEdge((Sprite) userDataA, (PhysicsBoundaryBox.BoundaryBoxIdentifier) userDataB);
		} else if (userDataA instanceof PhysicsBoundaryBox.BoundaryBoxIdentifier && userDataB instanceof Sprite) {
			physicsWorld.bouncedOnEdge((Sprite) userDataB, (PhysicsBoundaryBox.BoundaryBoxIdentifier) userDataA);
		}
		// Обработка столкновения двух спрайтов
		else if (userDataA instanceof Sprite && userDataB instanceof Sprite) {
			// Убедимся, что это разные спрайты, на всякий случай
			if (userDataA != userDataB) {
				registerContact((Sprite) userDataA, (Sprite) userDataB);
			}
		}
	}

	/**
	 * Вызывается из b2ContactEvents.endContact, когда получены UserData.
	 * Обрабатывает окончание контакта между двумя физическими объектами.
	 *
	 * @param userDataA UserData первого объекта.
	 * @param userDataB UserData второго объекта.
	 */
	public void handleEndContact(Object userDataA, Object userDataB) {
		// Отменяем регистрацию столкновения только для спрайтов
		if (userDataA instanceof Sprite && userDataB instanceof Sprite) {
			// Убедимся, что это разные спрайты
			if (userDataA != userDataB) {
				unregisterContact((Sprite) userDataA, (Sprite) userDataB);
			}
		}
	}

	/**
	 * Вызывается из b2ContactEvents.hitEvent, если нужна обработка импульса столкновения.
	 *
	 * @param userDataA UserData первого объекта.
	 * @param userDataB UserData второго объекта.
	 * @param normalX   X-компонента нормали контакта.
	 * @param normalY   Y-компонента нормали контакта.
	 * @param impulse   Величина импульса.
	 */
	public void handleHitEvent(Object userDataA, Object userDataB, float normalX, float normalY, float impulse) {
		// Здесь можно реализовать логику, зависящую от силы удара,
		// например, проигрывание звука или запуск скрипта при сильном столкновении.
		// System.out.printf("Hit between %s and %s, impulse: %.2f%n", userDataA, userDataB, impulse);
	}

	// --- Приватные методы для управления картой столкновений ---

	private void registerContact(Sprite sprite1, Sprite sprite2) {
		CollidingSprites collidingSprites = new CollidingSprites(sprite1, sprite2);

		// computeIfAbsent принимает ключ (collidingSprites) и функцию для создания значения, если ключ отсутствует.
		// Лямбда получает ключ 'k' (который равен collidingSprites) в качестве параметра.
		PhysicalCollision collision = collidingSpritesToCollisionMap.computeIfAbsent(collidingSprites, k -> new PhysicalCollision(k));
		//       Передаем 'k' в конструктор ^

		// Увеличиваем счетчик у существующего или только что созданного объекта
		collision.increaseContactCounter();
	}

	private void unregisterContact(Sprite sprite1, Sprite sprite2) {
		CollidingSprites collidingSprites = new CollidingSprites(sprite1, sprite2);
		PhysicalCollision collision = collidingSpritesToCollisionMap.get(collidingSprites);
		if (collision != null) {
			collision.decreaseContactCounter();
			// System.out.println("Unregister contact: " + sprite1.getName() + " / " + sprite2.getName() + " count: " + collision.getContactCounter());
			if (collision.getContactCounter() <= 0) { // Используем <= для большей надежности
				// System.out.println("Sending bounce event and removing collision map entry for " + sprite1.getName() + " / " + sprite2.getName());
				collision.sendBounceOffEvents(); // Отправка события отскока Catroid
				collidingSpritesToCollisionMap.remove(collidingSprites);
			}
		} else {
			// System.out.println("Attempted to unregister non-existent collision: " + sprite1.getName() + " / " + sprite2.getName());
		}
	}

	/**
	 * Внутренний класс для использования в качестве ключа в Map.
	 * Гарантирует, что пара (sprite1, sprite2) и (sprite2, sprite1) считаются одинаковыми.
	 */
	/*private static class CollidingSprites {
		final Sprite spriteA;
		final Sprite spriteB;

		CollidingSprites(Sprite s1, Sprite s2) {
			// Гарантируем порядок для согласованности хэша и equals
			if (System.identityHashCode(s1) < System.identityHashCode(s2)) {
				this.spriteA = s1;
				this.spriteB = s2;
			} else {
				this.spriteA = s2;
				this.spriteB = s1;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CollidingSprites that = (CollidingSprites) o;
			// Сравниваем ссылки, так как порядок гарантирован конструктором
			return spriteA == that.spriteA && spriteB == that.spriteB;
		}

		@Override
		public int hashCode() {
			// Используем Objects.hash для генерации хэша на основе ссылок
			return Objects.hash(spriteA, spriteB);
		}
	}*/
}