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
/* ... (Лицензия и package) ... */
package org.catrobat.catroid.physics.shapebuilder;

import android.util.Log; // Используем Android Log

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
// --- Новые импорты Box2D 3.0 ---
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2Vec2.b2Vec2Pointer;
import com.badlogic.gdx.box2d.structs.b2Hull;
import com.badlogic.gdx.box2d.structs.b2Hull.b2HullPointer;
// --- Убираем старые импорты, если они были ---
// import com.badlogic.gdx.physics.box2d.PolygonShape; // Старый
// import com.badlogic.gdx.physics.box2d.Shape; // Старый

import org.catrobat.catroid.physics.PhysicsWorldConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import androidx.annotation.Nullable; // Для Nullable

public final class PhysicsShapeBuilderStrategyFastHull implements PhysicsShapeBuilderStrategy {

	private static final String TAG = "FastHullStrategy";
	private static final int MINIMUM_PIXEL_ALPHA_VALUE = 1;
	// Box2D максимальное количество вершин в полигоне (обычно 8)
	private static final int MAX_POLYGON_VERTICES = com.badlogic.gdx.box2d.Constants.B2_MAX_POLYGON_VERTICES; // Используем константу


	/**
	 * Строит массив физических геометрий (b2Polygon) на основе Pixmap.
	 * @param pixmap Исходное изображение.
	 * @param scale  Этот параметр здесь не используется (масштабирование происходит позже).
	 * @return Массив Object[], содержащий b2Polygon, или null при ошибке.
	 */
	@Override
	@Nullable
	public Object[] build(@Nullable Pixmap pixmap, float scale) { // Возвращаемый тип изменен
		if (pixmap == null || pixmap.isDisposed()) {
			Log.e(TAG, "Pixmap is null or disposed, cannot build shape.");
			return null;
		}

		// --- Часть 1: Построение выпуклой оболочки (Convex Hull) ---
		// Эта часть остается в основном без изменений, т.к. работает с пикселями и Vector2
		int width = pixmap.getWidth();
		int height = pixmap.getHeight();
		if (width <= 0 || height <= 0) {
			Log.e(TAG, "Pixmap has invalid dimensions (<=0).");
			return null;
		}

		float coordinateAdjustmentValue = 1.0f; // Для смещения точек при обходе
		Stack<Vector2> convexHullStack = new Stack<>();

		Vector2 startPoint = findStartingPoint(pixmap, width, height);
		if (startPoint == null) {
			Log.w(TAG, "No non-transparent pixels found in pixmap.");
			return null; // Пустое изображение
		}
		addPoint(convexHullStack, startPoint);

		Vector2 currentPoint = startPoint;
		currentPoint = traceHullRight(pixmap, width, height, currentPoint, convexHullStack, coordinateAdjustmentValue);
		currentPoint = traceHullBottom(pixmap, width, height, currentPoint, startPoint, convexHullStack, coordinateAdjustmentValue);
		currentPoint = traceHullLeft(pixmap, width, height, currentPoint, startPoint, convexHullStack, coordinateAdjustmentValue);
		traceHullTop(pixmap, width, height, currentPoint, startPoint, convexHullStack, coordinateAdjustmentValue);


		// Удаляем лишние точки, если они образовались в конце
		if (convexHullStack.size() > 2) {
			removeNonConvexPoints(convexHullStack, startPoint);
			// Возможно, нужно удалить последнюю точку, если она совпадает с первой?
			if (convexHullStack.size() > 1 && convexHullStack.peek().equals(convexHullStack.firstElement())) {
				convexHullStack.pop();
			}
		}


		if (convexHullStack.size() < 3) {
			Log.w(TAG, "Convex hull has less than 3 points, cannot form a polygon.");
			return null; // Невозможно создать полигон
		}

		// --- Часть 2: Преобразование координат и разделение на полигоны Box2D ---
		Vector2[] hullPoints = convexHullStack.toArray(new Vector2[0]);
		return createPolygonsFromHull(hullPoints, width, height);
	}

	// --- Вспомогательные методы для построения оболочки (без изменений) ---

	@Nullable
	private Vector2 findStartingPoint(Pixmap pixmap, int width, int height) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					return new Vector2(x, y);
				}
			}
		}
		return null; // Не найдено
	}

	private Vector2 traceHullRight(Pixmap pixmap, int width, int height, Vector2 start, Stack<Vector2> hull, float adj) {
		Vector2 current = new Vector2(start);
		for (int x = (int) start.x + 1; x < width; x++) {
			for (int y = height - 1; y >= (int) start.y; y--) { // Идем снизу вверх
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					Vector2 point = new Vector2(x, y + adj); // Точка на правом краю
					addPoint(hull, point);
					current.set(point); // Обновляем текущую самую правую точку
					break; // Переходим к следующему столбцу X
				}
			}
		}
		return current;
	}

	private Vector2 traceHullBottom(Pixmap pixmap, int width, int height, Vector2 start, Vector2 first, Stack<Vector2> hull, float adj) {
		Vector2 current = new Vector2(start);
		// Идем по нижней границе справа налево
		for (int y = (int) (start.y > first.y ? start.y - adj : start.y); // Начинаем чуть выше или с текущей Y
			 y >= (int)first.y; y--) {
			for (int x = width - 1; x >= (int)first.x; x--) { // Справа налево
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					Vector2 point = new Vector2(x + adj, y + adj); // Точка на нижнем краю
					addPoint(hull, point);
					current.set(point);
					break; // Переходим к следующей строке Y (выше)
				}
			}
		}
		return current;
	}

	private Vector2 traceHullLeft(Pixmap pixmap, int width, int height, Vector2 start, Vector2 first, Stack<Vector2> hull, float adj) {
		Vector2 current = new Vector2(start);
		// Идем по левой границе снизу вверх
		for (int x = (int) (start.x > first.x ? start.x - adj : start.x); // Начинаем чуть правее или с текущей X
			 x >= (int)first.x; x--) {
			for (int y = (int) first.y; y < height; y++) { // Снизу вверх
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					Vector2 point = new Vector2(x + adj, y); // Точка на левом краю
					addPoint(hull, point);
					current.set(point);
					break; // Переходим к следующему столбцу X (левее)
				}
			}
		}
		return current;
	}

	private Vector2 traceHullTop(Pixmap pixmap, int width, int height, Vector2 start, Vector2 first, Stack<Vector2> hull, float adj) {
		Vector2 current = new Vector2(start);
		// Идем по верхней границе слева направо
		for (int y = (int) first.y; y < height; y++) { // Снизу вверх (до текущей высоты start?)
			for (int x = (int) first.x; x < width; x++) { // Слева направо
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					Vector2 point = new Vector2(x, y); // Точка на верхнем краю
					addPoint(hull, point);
					current.set(point);
					break; // Переходим к следующей строке Y (ниже)
				}
			}
			if(y > current.y) break; // Останавливаемся, если прошли выше текущей точки?
		}
		return current;
	}


	private void addPoint(Stack<Vector2> convexHull, Vector2 point) {
		removeNonConvexPoints(convexHull, point);
		// Избегаем добавления дубликатов подряд
		if (convexHull.isEmpty() || !convexHull.peek().equals(point)) {
			convexHull.add(point);
		}
	}

	private void removeNonConvexPoints(Stack<Vector2> convexHull, Vector2 newTop) {
		while (convexHull.size() >= 2) { // Нужно минимум 2 точки для проверки поворота
			Vector2 top = convexHull.peek();
			Vector2 secondTop = convexHull.get(convexHull.size() - 2);

			// Если поворот налево (или прямо), то сохраняем точку top
			// leftTurn < 0 означает поворот налево
			// leftTurn == 0 означает коллинеарность (можно оставить или убрать)
			if (leftTurnOrStraight(secondTop, top, newTop)) {
				break; // Предыдущая точка подходит
			}
			// Если поворот направо, удаляем точку top
			convexHull.pop();
		}
	}

	// Проверяет левый поворот или коллинеарность (>= 0)
	private boolean leftTurnOrStraight(Vector2 a, Vector2 b, Vector2 c) {
		// Крестное произведение векторов (b-a) и (c-a)
		return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) <= 0; // Изменено на <=
	}

	/**
	 * Преобразует координаты оболочки, делит ее на полигоны Box2D
	 * и создает соответствующие объекты b2Polygon.
	 *
	 * @param hullPoints Массив вершин выпуклой оболочки в координатах Pixmap.
	 * @param width      Ширина Pixmap.
	 * @param height     Высота Pixmap.
	 * @return Массив Object[], содержащий b2Polygon, или null при ошибке.
	 */
	@Nullable
	private Object[] createPolygonsFromHull(Vector2[] hullPoints, int width, int height) {
		if (hullPoints == null || hullPoints.length < 3) {
			Log.e(TAG, "Not enough points in hull to create polygon.");
			return null;
		}

		// --- Преобразование координат ---
		Vector2[] box2dVertices = new Vector2[hullPoints.length];
		for (int i = 0; i < hullPoints.length; i++) {
			Vector2 point = hullPoints[i];
			// Сдвиг центра и инверсия Y
			float centeredX = point.x - width / 2.0f;
			float centeredY = height / 2.0f - point.y;
			// Масштабирование в Box2D координаты
			box2dVertices[i] = PhysicsWorldConverter.convertCatroidToBox2dVector(new Vector2(centeredX, centeredY));
		}

		// --- Разделение на полигоны (если нужно) ---
		if (box2dVertices.length <= MAX_POLYGON_VERTICES) {
			// Оболочка уже подходит, создаем один полигон
			b2Polygon polygon = createB2PolygonFromVertices(box2dVertices);
			if (polygon != null) {
				return new Object[]{polygon};
			} else {
				Log.e(TAG, "Failed to create single polygon from hull.");
				return null; // Ошибка создания
			}
		} else {
			// Нужно разделить на несколько полигонов (метод "веера")
			Log.d(TAG, "Hull has " + box2dVertices.length + " vertices. Dividing into smaller polygons.");
			List<Object> shapes = new ArrayList<>();
			List<Vector2> pointsPerShape = new ArrayList<>(MAX_POLYGON_VERTICES);

			Vector2 fanCenter = box2dVertices[0]; // Первая точка - центр веера
			int vertexIndex = 1; // Начинаем со второй точки

			while (vertexIndex < box2dVertices.length) {
				pointsPerShape.clear();
				pointsPerShape.add(fanCenter); // Добавляем центр веера

				// Добавляем следующие вершины, пока не достигнем лимита
				// Нужно взять MAX_POLYGON_VERTICES - 1 вершин, т.к. одна вершина - это fanCenter
				int pointsToAdd = MAX_POLYGON_VERTICES - 1;
				int endPointIndex = Math.min(vertexIndex + pointsToAdd, box2dVertices.length);

				for (int i = vertexIndex; i < endPointIndex; i++) {
					pointsPerShape.add(box2dVertices[i]);
				}

				// Создаем полигон из этого "веера"
				if (pointsPerShape.size() >= 3) { // Нужно минимум 3 точки
					b2Polygon polygon = createB2PolygonFromVertices(pointsPerShape.toArray(new Vector2[0]));
					if (polygon != null) {
						shapes.add(polygon);
					} else {
						Log.e(TAG, "Failed to create sub-polygon during division.");
						// Можно либо вернуть null, либо проигнорировать ошибку и продолжить
						// return null; // Если критично
					}
				} else {
					Log.w(TAG, "Skipping sub-polygon creation, not enough points: " + pointsPerShape.size());
				}


				// Сдвигаем индекс для следующего веера
				// Следующий веер начнется с последней точки предыдущего
				vertexIndex = endPointIndex - 1;
				if (vertexIndex <= 0) { // Предохранитель от бесконечного цикла
					Log.e(TAG, "Error in polygon division logic: vertexIndex became non-positive.");
					return null;
				}
			}

			if (shapes.isEmpty()) {
				Log.e(TAG, "Polygon division resulted in zero valid shapes.");
				return null;
			} else {
				return shapes.toArray(); // Возвращаем массив созданных полигонов
			}
		}
	}

	/**
	 * Вспомогательный метод для создания одного b2Polygon из массива вершин.
	 * Использует b2ComputeHull и b2MakePolygon.
	 *
	 * @param vertices Массив вершин в координатах Box2D.
	 * @return Созданный b2Polygon (управляемый GC) или null при ошибке.
	 */
	@Nullable
	private b2Polygon createB2PolygonFromVertices(Vector2[] vertices) {
		if (vertices == null || vertices.length < 3 || vertices.length > MAX_POLYGON_VERTICES) {
			// Box2D.b2ComputeHull может принять и больше вершин, но b2MakePolygon - нет?
			// Или сама логика деления должна гарантировать <= MAX_POLYGON_VERTICES
			if (vertices != null && vertices.length > MAX_POLYGON_VERTICES) {
				Log.e(TAG, "Too many vertices (" + vertices.length + ") passed to createB2PolygonFromVertices. Max is " + MAX_POLYGON_VERTICES);
			} else {
				Log.e(TAG, "Invalid vertex array passed to createB2PolygonFromVertices (null, <3 points). Length: " + (vertices != null ? vertices.length : "null"));
			}
			return null;
		}

		b2Vec2Pointer scaledVerticesPtr = null;
		b2Hull hull = null;
		b2Polygon scaledPolygon = null;

		try {
			// --- Шаг 1: Конвертация в b2Vec2Pointer ---
			int count = vertices.length;
			scaledVerticesPtr = new b2Vec2Pointer(count, true); // true - для GC
			if (scaledVerticesPtr.isNull()) {
				Log.e(TAG,"Failed to allocate native memory for b2Vec2Pointer.");
				return null;
			}

			for (int i = 0; i < count; i++) {
				b2Vec2 destVertex = scaledVerticesPtr.get(i);
				Vector2 srcVertex = vertices[i];
				if (destVertex == null || destVertex.isNull() || srcVertex == null) {
					Log.e(TAG,"Null vertex encountered during conversion to b2Vec2Pointer at index " + i);
					return null; // scaledVerticesPtr освободится GC
				}
				destVertex.x(srcVertex.x);
				destVertex.y(srcVertex.y);
			}

			// --- Шаг 2: Вычисление выпуклой оболочки ---
			// Даже если мы думаем, что точки уже выпуклые, Box2D 3.0 требует этот шаг
			hull = Box2d.b2ComputeHull(scaledVerticesPtr, count);
			if (hull == null || hull.isNull()) {
				Log.e(TAG,"b2ComputeHull failed. Input vertices might be collinear or invalid. Count: " + count);
				// Log детали вершин для отладки, если нужно
				// StringBuilder sb = new StringBuilder("Vertices: ");
				// for(Vector2 v : vertices) sb.append(String.format("(%.2f, %.2f) ", v.x, v.y));
				// Log.d(TAG, sb.toString());
				return null; // scaledVerticesPtr освободится GC
			}

			// --- Шаг 3: Создание полигона из оболочки ---
			b2HullPointer hullPtr = hull.asPointer();
			// Используем радиус 0 для острых углов
			long polygonPtr = Box2d.b2MakePolygon_internal(hullPtr.getPointer(), 0.0f, 0);
			if (polygonPtr == 0) {
				Log.e(TAG,"b2MakePolygon_internal returned a null pointer.");
				return null; // hull и scaledVerticesPtr освободятся GC
			}
			// Создаем Java-обертку, управляемую GC
			scaledPolygon = new b2Polygon(polygonPtr, true);

			if (scaledPolygon.isNull()) {
				Log.e(TAG,"Created b2Polygon has null pointer after creation.");
				return null;
			}

			// Проверка количества вершин в созданном полигоне (не должно превышать лимит)
			int finalVertexCount = scaledPolygon.count();
			if (finalVertexCount > MAX_POLYGON_VERTICES) {
				Log.w(TAG, "Created polygon has " + finalVertexCount + " vertices, exceeding max " + MAX_POLYGON_VERTICES);
				// Возможно, стоит вернуть null или пытаться упростить? Пока просто предупреждаем.
			} else if (finalVertexCount < 3) {
				Log.w(TAG, "Created polygon has less than 3 vertices (" + finalVertexCount + "). Invalid.");
				// Освобождаем память, если она не управляется GC (хотя мы указали true)
				// if (!scaledPolygon.getFreeOnGC()) scaledPolygon.dispose(); // Проверь API
				return null; // Невалидный полигон
			}


			Log.d(TAG, "Successfully created b2Polygon with " + finalVertexCount + " vertices.");
			return scaledPolygon; // Успех

		} catch (Exception e) {
			Log.e(TAG,"Exception during polygon creation from vertices: ", e);
			return null; // Ошибка
		} finally {
			// Явное освобождение не требуется при использовании freeOnGC=true
		}
	}
}







/*package org.catrobat.catroid.physics.shapebuilder;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2ShapeId;

import org.catrobat.catroid.physics.PhysicsWorldConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public final class PhysicsShapeBuilderStrategyFastHull implements PhysicsShapeBuilderStrategy {

	private static final int MINIMUM_PIXEL_ALPHA_VALUE = 1;

	@Override
	public Shape[] build(Pixmap pixmap, float scale) {
		if (pixmap == null) {
			return null;
		}

		int width = pixmap.getWidth();
		int height = pixmap.getHeight();
		float coordinateAdjustmentValue = 1.0f;
		Stack<Vector2> convexHull = new Stack<Vector2>();

		Vector2 point = new Vector2(width, height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < point.x; x++) {
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					point = new Vector2(x, y);
					addPoint(convexHull, point);
					break;
				}
			}
		}

		if (convexHull.isEmpty()) {
			return null;
		}

		for (int x = (int) point.x; x < width; x++) {
			for (int y = height - 1; y > point.y; y--) {
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					point = new Vector2(x, y);
					addPoint(convexHull, new Vector2(x, y + coordinateAdjustmentValue));
					break;
				}
			}
		}

		Vector2 firstPoint = convexHull.firstElement();
		for (int y = (int) point.y; y >= firstPoint.y; y--) {
			for (int x = width - 1; x > point.x; x--) {
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					point = new Vector2(x, y);
					addPoint(convexHull, new Vector2(x + coordinateAdjustmentValue, y + coordinateAdjustmentValue));
					break;
				}
			}
		}

		for (int x = (int) point.x; x > firstPoint.x; x--) {
			for (int y = (int) firstPoint.y; y < point.y; y++) {
				if ((pixmap.getPixel(x, y) & 0xff) >= MINIMUM_PIXEL_ALPHA_VALUE) {
					point = new Vector2(x, y);
					addPoint(convexHull, new Vector2(x + coordinateAdjustmentValue, y));
					break;
				}
			}
		}

		if (convexHull.size() > 2) {
			removeNonConvexPoints(convexHull, firstPoint);
		}

		return devideShape(convexHull.toArray(new Vector2[convexHull.size()]), width, height);
	}

	private void addPoint(Stack<Vector2> convexHull, Vector2 point) {
		removeNonConvexPoints(convexHull, point);
		convexHull.add(point);
	}

	private void removeNonConvexPoints(Stack<Vector2> convexHull, Vector2 newTop) {
		while (convexHull.size() > 1) {
			Vector2 top = convexHull.peek();
			Vector2 secondTop = convexHull.get(convexHull.size() - 2);

			if (leftTurn(secondTop, top, newTop)) {
				break;
			}

			if (top.y > newTop.y && top.y > secondTop.y) {
				break;
			}

			convexHull.pop();
		}
	}

	private boolean leftTurn(Vector2 a, Vector2 b, Vector2 c) {
		return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) < 0;
	}

	private Shape[] devideShape(Vector2[] convexpoints, int width, int height) {
		for (int index = 0; index < convexpoints.length; index++) {
			Vector2 point = convexpoints[index];
			point.x -= width / 2.0f;
			point.y = height / 2.0f - point.y;
			convexpoints[index] = PhysicsWorldConverter.convertCatroidToBox2dVector(point);
		}

		if (convexpoints.length < 9) {
			PolygonShape polygon = new PolygonShape();
			polygon.set(convexpoints);
			return new Shape[] {polygon};
		}

		List<Shape> shapes = new ArrayList<Shape>(convexpoints.length / 6 + 1);
		List<Vector2> pointsPerShape = new ArrayList<Vector2>(8);

		Vector2 rome = convexpoints[0];
		int index = 1;
		while (index < convexpoints.length - 1) {
			int k = index + 7;

			int remainingPointsCount = convexpoints.length - index;
			if (remainingPointsCount > 7 && remainingPointsCount < 9) {
				k -= 3;
			}

			pointsPerShape.add(rome);
			for (; index < k && index < convexpoints.length; index++) {
				pointsPerShape.add(convexpoints[index]);
			}

			PolygonShape polygon = new PolygonShape();
			polygon.set(pointsPerShape.toArray(new Vector2[pointsPerShape.size()]));
			shapes.add(polygon);

			pointsPerShape.clear();
			index--;
		}

		return shapes.toArray(new Shape[shapes.size()]);
	}
}
*/