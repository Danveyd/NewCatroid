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


/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 * ... (остальная лицензия) ...
 */

package org.catrobat.catroid.physics.shapebuilder;

// Импорты как раньше, плюс Box2d и, возможно, b2Hull
import android.util.Log;

import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.Constants;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2Circle;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2Vec2.b2Vec2Pointer;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.enums.b2ShapeType;
import com.badlogic.gdx.box2d.structs.b2Hull; // Предполагаемый импорт
import com.badlogic.gdx.box2d.structs.b2Hull.b2HullPointer; // Предполагаемый импорт

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PhysicsShapeScaleUtils {

	private PhysicsShapeScaleUtils() { }


    /**
	 * Масштабирует массив объектов геометрии (b2Polygon, b2Circle).
	 * Возвращает НОВЫЙ массив с НОВЫМИ экземплярами масштабированных геометрий.
	 * Объекты в возвращаемом массиве предполагаются управляемыми GC (freeOnGC=true).
	 *
	 * @param originalGeometries Массив оригинальных геометрий (может содержать null).
	 * @param scale              Коэффициент масштабирования (> 0).
	 * @return Новый массив с масштабированными геометриями или null в случае серьезной ошибки.
	 *         Если scale = 1.0 или входной массив некорректен, возвращает ОРИГИНАЛЬНЫЙ массив.
	 */
	@Nullable
	public static Object[] scaleGeometries(@Nullable Object[] originalGeometries, float scale) {
        String TAG = "ScaleUtils";
        if (originalGeometries == null || originalGeometries.length == 0) {
			Log.w(TAG, "Input geometry array is null or empty, returning original.");
			return originalGeometries;
		}
		if (scale <= 0.0f) {
			Log.e(TAG, "Invalid scale factor <= 0: " + scale + ", returning original array.");
			return originalGeometries; // Некорректный масштаб
		}
		if (scale == 1.0f) {
			return originalGeometries; // Масштаб 1.0, нет смысла создавать копии
		}

		List<Object> scaledList = new ArrayList<>(originalGeometries.length);
		boolean success = true;

		for (Object geom : originalGeometries) {
			Object scaledGeom = null;
			try {
				if (geom instanceof b2Polygon) {
					// scalePolygon возвращает null при ошибке (не при scale=1, т.к. мы это проверили выше)
					scaledGeom = scalePolygon((b2Polygon) geom, scale);
					if (scaledGeom == null) success = false;
				} else if (geom instanceof b2Circle) {
					// scaleCircle возвращает null при ошибке
					scaledGeom = scaleCircle((b2Circle) geom, scale);
					if (scaledGeom == null) success = false;
				} else if (geom == null) {
					scaledGeom = null; // Сохраняем null в массиве
				} else {
					Log.w(TAG, "Unsupported geometry type for scaling: " + geom.getClass().getName() + ". Skipping scaling for this element.");
					scaledGeom = geom; // Возвращаем оригинал для неподдерживаемых типов
				}
			} catch (Exception e) {
				Log.e(TAG, "Exception during scaling of geometry: " + geom, e);
				success = false;
				scaledGeom = null; // Считаем ошибкой
			}

			if (!success) {
				Log.e(TAG, "Failed to scale one of the geometries. Aborting scaling process.");
				// --- Управление памятью ---
				// Если бы мы создавали геометрии с freeOnGC=false, здесь нужна была бы очистка
				// уже созданных scaledGeom в scaledList. Но мы предполагаем freeOnGC=true.
				// cleanupManuallyCreatedGeometries(scaledList);
				return null; // Сигнализируем об общей ошибке
			}

			scaledList.add(scaledGeom);
		}

		return scaledList.toArray(); // Возвращаем новый массив
	}


	@Nullable
	public static b2Polygon scalePolygon(@Nullable b2Polygon originalPolygon, float scale) {
		if (originalPolygon == null || originalPolygon.isNull() || scale <= 0.0f) {
			System.err.println("Error: Invalid input to scalePolygon.");
			return null;
		}
		if (scale == 1.0f) {
			return null; // Нет изменений
		}

		int count = originalPolygon.count();
		if (count <= 0 || count > Constants.B2_MAX_POLYGON_VERTICES) {
			System.err.println("Error: Invalid vertex count: " + count);
			return null;
		}

		b2Vec2Pointer originalVerticesPtr = originalPolygon.vertices();
		if (originalVerticesPtr == null || originalVerticesPtr.isNull()) {
			System.err.println("Error: Could not get original vertices pointer.");
			return null;
		}

		// Шаг 1: Подготовка b2Vec2Pointer с масштабированными вершинами
		b2Vec2Pointer scaledVerticesPtr = null;
		try {
			// Создаем указатель на массив b2Vec2. true - возможно, для авто-освобождения GC? Проверь!
			scaledVerticesPtr = new b2Vec2Pointer(count, true);
			if (scaledVerticesPtr.isNull()) {
				System.err.println("Error: Failed to allocate native memory for scaled vertices.");
				return null;
			}

			// Заполняем масштабированными координатами
			for (int i = 0; i < count; i++) {
				b2Vec2 originalVertex = originalVerticesPtr.get(i);
				b2Vec2 scaledVertex = scaledVerticesPtr.get(i); // Получаем доступ к i-му b2Vec2 в выделенной памяти

				if (originalVertex == null || originalVertex.isNull() || scaledVertex == null || scaledVertex.isNull()) {
					System.err.println("Error: Null vertex pointer during scaling at index " + i);
					// Освобождаем память, если нужно (если freeOnGC=true не сработает)
					// if (scaledVerticesPtr != null && !scaledVerticesPtr.getFreeOnGC()) scaledVerticesPtr.free();
					return null;
				}
				scaledVertex.x(originalVertex.x() * scale);
				scaledVertex.y(originalVertex.y() * scale);
			}

			// Шаг 2: Вычисление выпуклой оболочки (Convex Hull)
			// !!! НУЖНО НАЙТИ ТОЧНУЮ ФУНКЦИЮ b2ComputeHull И ЕЕ СИГНАТУРУ !!!
			// Предполагаемая сигнатура: b2Hull computeHull(b2Vec2Pointer points, int count);
			// Она может быть статической в Box2d или b2Hull.
			b2Hull hull = Box2d.b2ComputeHull(scaledVerticesPtr, count); // ЗАМЕНИ НА ПРАВИЛЬНЫЙ ВЫЗОВ!

			if (hull == null || hull.isNull()) {
				System.err.println("Error: b2ComputeHull failed to produce a valid hull.");
				// Освобождаем память, если нужно
				// if (scaledVerticesPtr != null && !scaledVerticesPtr.getFreeOnGC()) scaledVerticesPtr.free();
				return null;
			}

			// Шаг 3: Создание полигона из оболочки
			b2HullPointer hullPtr = hull.asPointer(); // Получаем указатель на hull
			float originalRadius = originalPolygon.radius(); // Сохраняем оригинальное скругление
			b2Polygon scaledPolygon = Box2d.b2MakePolygon(hullPtr, originalRadius * scale); // Используем масштабированный радиус

			if (scaledPolygon == null || scaledPolygon.isNull()) {
				System.err.println("Error: b2MakePolygon failed to create polygon from hull.");
				// Освобождаем память hull, если нужно? Или она управляется GC?
				//if (hull != null && !hull.getFreeOnGC()) hull.dispose(); // Пример
				// Освобождаем память scaledVerticesPtr, если нужно
				 if (scaledVerticesPtr != null) scaledVerticesPtr.free();
				return null;
			}

			// Успех! Память для scaledPolygon управляется (судя по коду b2MakePolygon)
			return scaledPolygon;

		} catch (Exception e) {
			System.err.println("Error during polygon scaling: " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			// Освобождение памяти для scaledVerticesPtr, ЕСЛИ freeOnGC=true не сработало
			// или если ты использовал freeOnGC=false.
			// if (scaledVerticesPtr != null && !scaledVerticesPtr.isNull() && !scaledVerticesPtr.getFreeOnGC()) {
			//     scaledVerticesPtr.free(); // Или .dispose() - проверь API b2Vec2Pointer
			// }
			// Нужно ли освобождать hull? Зависит от того, как он создается и управляется.
		}
	}

	// --- Метод scaleCircle остается прежним (с оговоркой про установку центра) ---
	@Nullable
	public static b2Circle scaleCircle(@Nullable b2Circle originalCircle, float scale) {
		// ... (проверки входных данных) ...

		b2Vec2 originalPosition = originalCircle.center();
		float originalRadius = originalCircle.radius();
		// ... (проверка originalPosition) ...

		b2Circle scaledCircle = new b2Circle();
		scaledCircle.radius(originalRadius * scale);

		// --- ВНИМАНИЕ ЗДЕСЬ ---
		// Установка центра: если scaledCircle.center().x(...) не работает или ведет себя странно,
		// возможно, нужно создать новый b2Vec2 и присвоить его.
		// Проверь API твоей версии b2Circle на наличие метода вроде setCenter(b2Vec2) или setPosition(b2Vec2)
		// или попробуй так:
		b2Vec2 scaledCenter = scaledCircle.center(); // Получаем указатель/объект центра
		if (scaledCenter != null && !scaledCenter.isNull()) {
			scaledCenter.x(originalPosition.x() * scale); // Модифицируем X
			scaledCenter.y(originalPosition.y() * scale); // Модифицируем Y
		} else {
			// Альтернативный вариант, если центр нужно установить через отдельный метод
			// или если center() возвращает копию
			b2Vec2 newCenter = new b2Vec2(); // Создаем новый вектор
			newCenter.x(originalPosition.x() * scale);
			newCenter.y(originalPosition.y() * scale);
			// Попытайся найти метод установки центра, например:
			// scaledCircle.setCenter(newCenter); // ИЛИ
			// scaledCircle.position(newCenter); // ИЛИ
			// scaledCircle.setPosition(newCenter); // ИЛИ что-то подобное
			// Если такого метода нет, и предыдущий способ не сработал, API круга тоже не стандартный.
			System.err.println("Warning: Could not get or set scaled circle center reliably.");
		}


		if (scaledCircle.isNull()) {
			System.err.println("Error: Failed to create scaled circle (native object is null).");
			return null;
		}

		return scaledCircle;
	}
}

/*package org.catrobat.catroid.physics.shapebuilder;

import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2SimplexVertex;
import com.badlogic.gdx.box2d.structs.b2Vec2;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.VisibleForTesting;

public final class PhysicsShapeScaleUtils {

	public static final float COORDINATE_SCALING_DECIMAL_ACCURACY = 100.0f;

	private PhysicsShapeScaleUtils() {
	}

	public static b2ShapeDef[] scaleShapes(b2ShapeDef[] shapes, float targetScale) {
		return scaleShapes(shapes, targetScale, 1.0f);
	}

	public static b2ShapeDef[] scaleShapes(b2ShapeDef[] shapes, float targetScale, float originScale) {
		if (shapes == null || shapes.length == 0 || targetScale == 0.0f || originScale == 0.0f) {
			return null;
		}
		if (targetScale == originScale) {
			return shapes;
		}
		float scale = targetScale / originScale;
		List<b2ShapeDef> scaledShapes = new LinkedList<>();
		if (shapes != null) {
			for (b2ShapeDef shape : shapes) {
				List<Vector2> vertices = new LinkedList<>();
				b2Polygon polygon = new b2Polygon(shape.getPointer(), true);
				for (int index = 0; index < polygon.count(); index++) {
					Vector2 vertex = new Vector2();
					polygon.getVertex(index, vertex);
					vertex = scaleCoordinate(vertex, scale);
					vertices.add(vertex);
				}
				b2Polygon polygonShape = new b2Polygon();
				polygonShape.set(vertices.toArray(new Vector2[vertices.size()]));
				scaledShapes.add(polygonShape);
			}
		}
		return scaledShapes.toArray(new b2ShapeDef[scaledShapes.size()]);
	}

	private static Vector2 scaleCoordinate(Vector2 vertex, float scaleFactor) {
		Vector2 v = new Vector2(vertex);
		v.x = scaleCoordinate(v.x, scaleFactor);
		v.y = scaleCoordinate(v.y, scaleFactor);
		return v;
	}

	@VisibleForTesting
	public static float scaleCoordinate(float coordinates, float scaleFactor) {
		return Math.round(coordinates * scaleFactor * COORDINATE_SCALING_DECIMAL_ACCURACY) / COORDINATE_SCALING_DECIMAL_ACCURACY;
	}

	/*private static Vector2 scaleCoordinate(Vector2 vertex, float scaleFactor) {
		// Этот метод больше не нужен напрямую, если работаем с b2Vec2
		// Оставим на всякий случай, но scalePolygon работает с float
		Vector2 v = new Vector2(vertex);
		v.x = scaleCoordinate(v.x, scaleFactor);
		v.y = scaleCoordinate(v.y, scaleFactor);
		return v;
	}

	@VisibleForTesting
	public static float scaleCoordinate(float coordinate, float scaleFactor) {
		// Используем Math.round для округления до N знаков после запятой
		// Избегаем потенциальных проблем с точностью float
		// float result = coordinate * scaleFactor;
		// return Math.round(result * COORDINATE_SCALING_DECIMAL_ACCURACY) / COORDINATE_SCALING_DECIMAL_ACCURACY;
		// Упрощенный вариант, если округление не так критично или делается в другом месте:
		return coordinate * scaleFactor;
	}
}
*/