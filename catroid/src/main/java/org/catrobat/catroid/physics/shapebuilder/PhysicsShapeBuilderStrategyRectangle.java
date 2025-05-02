/* ... (Лицензия и package) ... */
package org.catrobat.catroid.physics.shapebuilder;

import android.graphics.Point;
import android.util.Log; // Используем Android Log

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2; // Vector2 может быть не нужен, если центр не вычисляем здесь

// --- Новые импорты Box2D 3.0 ---
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.structs.b2Polygon;
// --- Убираем старые импорты ---
// import com.badlogic.gdx.physics.box2d.PolygonShape;
// import com.badlogic.gdx.physics.box2d.Shape;

import org.catrobat.catroid.physics.PhysicsWorldConverter;

import androidx.annotation.Nullable; // Добавляем для @Nullable

public class PhysicsShapeBuilderStrategyRectangle implements PhysicsShapeBuilderStrategy {

	private static final String TAG = "RectangleStrategy";

	/**
	 * Строит прямоугольную геометрию b2Polygon на основе границ непрозрачных пикселей Pixmap.
	 * @param pixmap Исходное изображение.
	 * @param scale  Не используется.
	 * @return Массив Object[], содержащий один b2Polygon, или null при ошибке.
	 */
	@Override
	@Nullable
	public Object[] build(@Nullable Pixmap pixmap, float scale) { // Изменен возвращаемый тип
		if (pixmap == null || pixmap.isDisposed()) {
			Log.e(TAG, "Pixmap is null or disposed.");
			return null;
		}

		// --- Находим границы непрозрачных пикселей ---
		Point start = null;
		Point end = null;
		int pixmapWidth = pixmap.getWidth();
		int pixmapHeight = pixmap.getHeight();

		if (pixmapWidth <= 0 || pixmapHeight <= 0) {
			Log.e(TAG, "Pixmap has invalid dimensions.");
			return null;
		}

		for (int y = 0; y < pixmapHeight; y++) {
			for (int x = 0; x < pixmapWidth; x++) {
				// Проверяем альфа-канал (последние 8 бит)
				int alpha = pixmap.getPixel(x, y) & 0xff;

				if (alpha > 0) { // Пиксель непрозрачный
					if (start == null) {
						// Первая найденная точка
						start = new Point(x, y);
						end = new Point(x, y);
					} else {
						// Обновляем границы
						if (x < start.x) start.x = x;
						if (x > end.x) end.x = x;
						if (y < start.y) start.y = y;
						if (y > end.y) end.y = y;
					}
				}
			}
		}

		// Если не найдено ни одного непрозрачного пикселя
		if (start == null) {
			Log.w(TAG, "No non-transparent pixels found in pixmap.");
			return null;
		}

		// --- Рассчитываем размеры в пикселях ---
		// Ширина/высота как количество пикселей (включительно)
		int pixelWidth = (end.x - start.x) + 1;
		int pixelHeight = (end.y - start.y) + 1;

		// Минимальный размер 1x1 пиксель
		if (pixelWidth <= 0) pixelWidth = 1;
		if (pixelHeight <= 0) pixelHeight = 1;

		// --- Рассчитываем полуширину и полувысоту в координатах Box2D ---
		// Делим на 2, так как Box2D работает с полуразмерами для коробок
		float box2dHalfWidth = PhysicsWorldConverter.convertNormalToBox2dCoordinate(pixelWidth) / 2.0f;
		float box2dHalfHeight = PhysicsWorldConverter.convertNormalToBox2dCoordinate(pixelHeight) / 2.0f;

		// --- Создаем b2Polygon с помощью хелпера Box2D 3.0 ---
		b2Polygon rectanglePolygon = null;
		try {
			// ПРЕДПОЛОЖЕНИЕ: Используем b2MakeBox для создания прямоугольника,
			// центрированного в (0,0).
			// !!! ПРОВЕРЬ ЭТОТ API ВЫЗОВ !!!
			rectanglePolygon = Box2d.b2MakeBox(box2dHalfWidth, box2dHalfHeight);

			if (rectanglePolygon == null || rectanglePolygon.isNull()) {
				Log.e(TAG, "Box2d.b2MakeBox returned null or invalid polygon.");
				return null;
			}
			// Убедимся, что полигон управляется GC (b2MakeBox должен это делать)
			// Если b2MakeBox возвращает long (указатель), то нужно:
			// long rectPtr = Box2d.b2MakeBox_internal(box2dHalfWidth, box2dHalfHeight, ...);
			// if (rectPtr == 0) return null;
			// rectanglePolygon = new b2Polygon(rectPtr, true); // true для GC

			Log.d(TAG, "Successfully created rectangle polygon");

		} catch (Exception e) {
			Log.e(TAG, "Exception while creating rectangle polygon using Box2d.b2MakeBox", e);
			return null; // Ошибка при создании
		}

		// --- Возвращаем результат ---
		// Возвращаем массив Object[], содержащий один созданный полигон
		return new Object[]{rectanglePolygon};
	}
}