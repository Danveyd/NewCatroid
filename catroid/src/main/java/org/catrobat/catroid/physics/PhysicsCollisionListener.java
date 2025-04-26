/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 * ... (остальная лицензия) ...
 */
package org.catrobat.catroid.physics;

// Убраны импорты Box2D - они теперь не нужны здесь

import org.catrobat.catroid.content.Sprite;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // Используем для CollidingSprites

/**
 * Вспомогательный класс для отслеживания состояния столкновений между спрайтами
 * и обработки логики Catroid при столкновениях.
 * Вызывается из экземпляра b2ContactEvents, созданного в PhysicsWorld.
 */
public class PhysicsCollisionListener {

	// Константы можно оставить, если они используются где-то еще
	public static final String COLLISION_MESSAGE_ESCAPE_CHAR = "\t";
	public static final String COLLISION_MESSAGE_CONNECTOR = "<" + COLLISION_MESSAGE_ESCAPE_CHAR
			+ "-" + COLLISION_MESSAGE_ESCAPE_CHAR + ">";

	private final PhysicsWorld physicsWorld; // Ссылка для вызова bouncedOnEdge
	// Карта для отслеживания активных столкновений между парами спрайтов
	private final Map<CollidingSprites, PhysicalCollision> collidingSpritesToCollisionMap = new HashMap<>();

	public PhysicsCollisionListener(PhysicsWorld physicsWorld) {
		this.physicsWorld = physicsWorld;
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