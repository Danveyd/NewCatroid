package org.catrobat.catroid.sensing;

import android.graphics.PointF;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.utils.NativeLookOptimizer;

import java.util.ArrayList;
import java.util.List;

@LunoClass
public final class CollisionDetection {

	// --- Pre-allocated vectors to reduce garbage collection ---
	private static final Vector2 intersectV1 = new Vector2();
	private static final Vector2 intersectV2 = new Vector2();
	private static final Vector2 edgeFirstPoint = new Vector2();
	private static final Vector2 edgeSecondPoint = new Vector2();
	private static final Vector2 fingerStart = new Vector2();
	private static final Vector2 fingerEnd = new Vector2();
	private static final Vector2 fingerCenter = new Vector2();

	private static final Circle fingerCollisionCircle = new Circle(); // <-- ДОБАВЬТЕ ЭТУ СТРОКУ
	// ----------------------------------------------------------

	private CollisionDetection() {
	}

	public static boolean checkCollisionBetweenLooks(Look firstLook, Look secondLook) {
		// Этап 1: Быстрые проверки в Java (остаются без изменений)
		if (firstLook == null || secondLook == null ||
				!firstLook.isVisible() || !firstLook.isLookVisible() ||
				!secondLook.isVisible() || !secondLook.isLookVisible()) {
			return false;
		}

		// Этап 2: Проверка ограничивающих рамок (AABB) (тоже остается)
		Rectangle firstHitbox = firstLook.getHitbox();
		Rectangle secondHitbox = secondLook.getHitbox();
		if (firstHitbox == null || secondHitbox == null || !firstHitbox.overlaps(secondHitbox)) {
			return false;
		}

		// Этап 3: Подготовка данных и вызов C++
		Polygon[] firstPolygons = firstLook.getCurrentCollisionPolygon();
		Polygon[] secondPolygons = secondLook.getCurrentCollisionPolygon();

		if (firstPolygons == null || secondPolygons == null || firstPolygons.length == 0 || secondPolygons.length == 0) {
			return false; // Если у кого-то нет полигонов, столкновения нет
		}

		// Конвертируем Polygon[] в float[][], который понимает JNI
		float[][] firstPreparedPolys = new float[firstPolygons.length][];
		for (int i = 0; i < firstPolygons.length; i++) {
			firstPreparedPolys[i] = (firstPolygons[i] != null) ? firstPolygons[i].getTransformedVertices() : new float[0];
		}

		float[][] secondPreparedPolys = new float[secondPolygons.length][];
		for (int i = 0; i < secondPolygons.length; i++) {
			secondPreparedPolys[i] = (secondPolygons[i] != null) ? secondPolygons[i].getTransformedVertices() : new float[0];
		}

		// Один-единственный вызов, который делает всю тяжелую работу!
		return NativeLookOptimizer.checkSingleCollision(firstPreparedPolys, secondPreparedPolys);
	}

	// Renamed from checkCollisionBetweenPolygons to avoid confusion with the single polygon check
	private static boolean checkCollisionBetweenPolygonArrays(Polygon[] first, Polygon[] second) {
		// Optimization: Pre-calculate bounding boxes for sub-polygons
		Rectangle[] firstBoxes = createBoundingBoxesOfCollisionPolygons(first);
		Rectangle[] secondBoxes = createBoundingBoxesOfCollisionPolygons(second);

		// 1. Check for edge intersections between any pair of sub-polygons
		for (int firstIndex = 0; firstIndex < first.length; firstIndex++) {
			for (int secondIndex = 0; secondIndex < second.length; secondIndex++) {
				// Check overlap of sub-polygon bounding boxes first
				if (firstBoxes[firstIndex].overlaps(secondBoxes[secondIndex])) {
					// If boxes overlap, perform precise polygon intersection check
					if (intersectPolygons(first[firstIndex], second[secondIndex])) {
						return true; // Found an intersection, collision detected
					}
				}
			}
		}

		// 2. If no edges intersected, check for containment (one polygon fully inside another)
		// This covers cases where shapes are nested without touching edges.
		if (checkContainment(first, second, firstBoxes, secondBoxes)) {
			return true;
		}

		return false; // No edge intersection and no containment found
	}


	// Новый метод, использующий SAT (для выпуклых полигонов)
	private static boolean checkCollisionBetweenPolygonArraysSAT(Polygon[] first, Polygon[] second, Rectangle[] firstBoxes, Rectangle[] secondBoxes) {
		if (first == null || second == null) return false;

		for (int firstIndex = 0; firstIndex < first.length; firstIndex++) {
			Polygon firstPolygon = first[firstIndex];
			// Проверяем валидность полигона (нужно минимум 3 вершины)
			if (firstPolygon == null || firstPolygon.getTransformedVertices() == null || firstPolygon.getTransformedVertices().length < 6) {
				continue;
			}

			for (int secondIndex = 0; secondIndex < second.length; secondIndex++) {
				Polygon secondPolygon = second[secondIndex];
				if (secondPolygon == null || secondPolygon.getTransformedVertices() == null || secondPolygon.getTransformedVertices().length < 6) {
					continue;
				}

				// 1. Быстрая проверка пересечения AABB (ограничивающих рамок)
				if (firstBoxes[firstIndex] != null && secondBoxes[secondIndex] != null &&
						firstBoxes[firstIndex].overlaps(secondBoxes[secondIndex]))
				{
					// 2. Точная проверка с помощью SAT, если рамки пересекаются
					// Этот метод работает с уже трансформированными полигонами из getCurrentCollisionPolygon()
					if (Intersector.overlapConvexPolygons(firstPolygon, secondPolygon)) {
						return true; // Найдено пересечение/вложение
					}
				}
			}
		}
		// Если ни одна пара полигонов не пересеклась
		return false;
	}

	// Helper to create bounding boxes (no changes needed, seems efficient enough)
	private static Rectangle[] createBoundingBoxesOfCollisionPolygons(Polygon[] polygons) {
		Rectangle[] boundingBoxes = new Rectangle[polygons.length];
		for (int i = 0; i < polygons.length; i++) {
			// Add null check for safety
			if (polygons[i] != null) {
				boundingBoxes[i] = polygons[i].getBoundingRectangle();
			} else {
				// Handle null polygon case - create an empty rectangle? Or rely on checks elsewhere?
				// For now, create an empty one to avoid NullPointerException downstream.
				boundingBoxes[i] = new Rectangle();
			}
		}
		return boundingBoxes;
	}

	/**
	 * Checks if any edge of the first polygon intersects with the second polygon.
	 * Note: This checks intersection of first's edges against second polygon area.
	 * LibGDX's intersectSegmentPolygon is generally robust enough.
	 */
	public static boolean intersectPolygons(Polygon first, Polygon second) {
		if (first == null || second == null) return false; // Safety check

		float[] firstVertices = first.getTransformedVertices();
		int firstLength = firstVertices.length;

		// Need at least 2 vertices (1 edge) to form a segment
		if (firstLength < 4) {
			return false;
		}

		// Use pre-allocated vectors
		Vector2 v1 = CollisionDetection.intersectV1;
		Vector2 v2 = CollisionDetection.intersectV2;

		for (int firstIndex = 0; firstIndex < firstLength; firstIndex += 2) {
			v1.x = firstVertices[firstIndex];
			v1.y = firstVertices[firstIndex + 1];
			// Get the next vertex, wrapping around for the last edge
			v2.x = firstVertices[(firstIndex + 2) % firstLength];
			v2.y = firstVertices[(firstIndex + 3) % firstLength];

			// Check if the segment (v1, v2) intersects the second polygon
			if (Intersector.intersectSegmentPolygon(v1, v2, second)) {
				return true; // Intersection found
			}
		}
		return false; // No edge of the first polygon intersects the second
	}


	/**
	 * Checks if any polygon from the 'inner' set is contained within any polygon
	 * from the 'outer' set. Uses bounding box checks for a quick exit.
	 * Checks containment by testing if the *first vertex* of an inner polygon
	 * lies within an outer polygon. This is a heuristic, more robust than the original,
	 * but still not perfectly accurate for all complex containment scenarios involving multiple polygons.
	 * It assumes that if containment exists, at least one vertex will be inside.
	 */
	private static boolean checkVertexContainment(Polygon[] inner, Polygon[] outer, Rectangle[] innerBoxes, Rectangle[] outerBoxes) {
		for (int i = 0; i < inner.length; i++) {
			Polygon innerPolygon = inner[i];
			if (innerPolygon == null) continue;

			float[] innerVertices = innerPolygon.getTransformedVertices();
			// Need at least one vertex
			if (innerVertices.length < 2) {
				continue;
			}
			float testX = innerVertices[0];
			float testY = innerVertices[1];

			// Quick check: if the test point isn't even in the bounding box of the inner polygon, skip
			// (This check seems redundant if test point IS from the inner polygon, but kept for consistency)
			// More useful: Check if the test point is within *any* outer bounding box first.
			boolean potentiallyContained = false;
			for(Rectangle outerBox : outerBoxes) {
				if (outerBox != null && outerBox.contains(testX, testY)) {
					potentiallyContained = true;
					break;
				}
			}
			if (!potentiallyContained) continue; // Vertex not in any outer bounding box

			// Precise check: Is the vertex inside any of the outer polygons?
			for (int j = 0; j < outer.length; j++) {
				Polygon outerPolygon = outer[j];
				if (outerPolygon == null) continue;

				// Check AABB overlap between the specific inner and outer polygons first
				if (innerBoxes[i] != null && outerBoxes[j] != null && innerBoxes[i].overlaps(outerBoxes[j])) {
					// Use libGDX's point-in-polygon test
					if (outerPolygon.contains(testX, testY)) {
						// Check if the point is NOT on the boundary, as boundary cases
						// should ideally be caught by intersectPolygons.
						// However, floating point inaccuracies might occur.
						// A simple 'contains' is often sufficient in practice here.
						return true; // Found a vertex of an inner polygon inside an outer one
					}
				}
			}
		}
		return false; // No vertex of any inner polygon found inside any outer polygon
	}

	/**
	 * New containment check function replacing the old checkCollisionForPolygonsInPolygons.
	 * Checks both directions: first inside second, and second inside first.
	 */
	private static boolean checkContainment(Polygon[] first, Polygon[] second, Rectangle[] firstBoxes, Rectangle[] secondBoxes) {
		// Check if any vertex of 'first' is inside any polygon of 'second'
		if (checkVertexContainment(first, second, firstBoxes, secondBoxes)) {
			return true;
		}
		// Check if any vertex of 'second' is inside any polygon of 'first'
		if (checkVertexContainment(second, first, secondBoxes, firstBoxes)) {
			return true;
		}
		return false;
	}


	// --- Other methods --- (Applying pre-allocated vector optimization)

	// Original getSecondSpriteNameFromCollisionFormulaString - likely not performance critical
	public static String getSecondSpriteNameFromCollisionFormulaString(String formula, Project currentProject) {
		if (currentProject == null || formula == null) return null; // Safety checks

		int indexOfSpriteInFormula = formula.length();
		String secondSpriteName = null; // Initialize to null

		// Check against sprites in all scenes might be slow if many scenes/sprites exist
		// But assuming this isn't called in the main game loop per frame frequently.
		for (Scene scene : currentProject.getSceneList()) {
			if (scene == null) continue;
			for (Sprite sprite : scene.getSpriteList()) {
				if (sprite == null || sprite.getName() == null) continue;
				// Use endsWith for clarity and potential slight optimization
				if (formula.endsWith(sprite.getName())) {
					int index = formula.lastIndexOf(sprite.getName());
					// Ensure it's not just a substring match somewhere else (index > 0 check isn't sufficient)
					// The original logic seems okay here: find the last occurrence that ends the string.
					if (index >= 0 && index < indexOfSpriteInFormula) { // index can be 0 if sprite name is the whole formula
						indexOfSpriteInFormula = index;
						secondSpriteName = sprite.getName(); // Store the potential name
					}
				}
			}
		}
		// No need to substring if we stored the name directly
		return secondSpriteName;
	}

	public static boolean collidesWithEdge(Polygon[] currentCollisionPolygon, Rectangle screen) {
		if (currentCollisionPolygon == null || screen == null) return false;

		// Use pre-allocated vectors
		Vector2 firstPoint = CollisionDetection.edgeFirstPoint;
		Vector2 secondPoint = CollisionDetection.edgeSecondPoint;

		// Check if any line segment of the collision polygons crosses the screen boundary
		for (Polygon polygon : currentCollisionPolygon) {
			if (polygon == null) continue;
			float[] transformedVertices = polygon.getTransformedVertices();
			int len = transformedVertices.length;

			if (len < 4) continue; // Need at least 2 vertices for a line segment

			for (int i = 0; i < len; i += 2) {
				firstPoint.set(transformedVertices[i], transformedVertices[i + 1]);
				// Get the next vertex, wrapping around for the last edge
				secondPoint.set(transformedVertices[(i + 2) % len], transformedVertices[(i + 3) % len]);

				// If one point is inside the screen and the other is outside, the edge crosses the boundary
				boolean firstIn = screen.contains(firstPoint);
				boolean secondIn = screen.contains(secondPoint);

				if (firstIn != secondIn) { // XOR check: one is in, one is out
					return true; // Collision with edge detected
				}

				// Added check: If both points are outside, but the line segment might still cross the screen
				// This happens if a small object passes entirely through a corner in one frame.
				// Use Intersector.intersectSegmentRectangle
				if (!firstIn && !secondIn) {
					if (Intersector.intersectSegmentRectangle(firstPoint, secondPoint, screen)) {
						return true;
					}
				}
			}
		}
		return false; // No edge collision detected
	}


	public static double collidesWithFinger(Polygon[] currentCollisionPolygon, ArrayList<PointF> touchingPoints) {
		if (currentCollisionPolygon == null || touchingPoints == null || touchingPoints.isEmpty()) {
			return 0d;
		}

		// Use pre-allocated vectors
		Vector2 start = CollisionDetection.fingerStart;
		Vector2 end = CollisionDetection.fingerEnd;
		Vector2 center = CollisionDetection.fingerCenter;
		float touchRadius = Constants.COLLISION_WITH_FINGER_TOUCH_RADIUS;
		float touchRadiusSq = touchRadius * touchRadius; // Pre-calculate squared radius for intersection test

		for (PointF point : touchingPoints) {
			if (point == null) continue;
			center.set(point.x, point.y);
			boolean intersected = false;
			int containedInCount = 0;

			for (Polygon polygon : currentCollisionPolygon) {
				if (polygon == null) continue;

				// Broad phase: Check if touch circle potentially overlaps polygon's bounding box
				Rectangle boundingRectangle = polygon.getBoundingRectangle();
				// Expand bounding box by touch radius for the check
				// Стало:
				fingerCollisionCircle.set(center.x, center.y, touchRadius); // Используем наше статическое поле
				if (!Intersector.overlaps(fingerCollisionCircle, boundingRectangle)) {
					continue; // Circle doesn't overlap bounding box, skip detailed check for this polygon
				}


				// Narrow phase 1: Check intersection with polygon edges
				float[] vertices = polygon.getTransformedVertices();
				int numVertices = vertices.length / 2;
				if (numVertices < 2) continue; // Need at least 2 vertices for an edge

				for (int i = 0; i < numVertices; i++) {
					int i2 = (i + 1) % numVertices; // Next vertex index, wraps around
					start.set(vertices[i * 2], vertices[i * 2 + 1]);
					end.set(vertices[i2 * 2], vertices[i2 * 2 + 1]);

					if (Intersector.intersectSegmentCircle(start, end, center, touchRadiusSq)) {
						intersected = true;
						break; // Found intersection, no need to check other edges of this polygon
					}
				}

				if (intersected) {
					break; // Found intersection, no need to check other polygons for this touch point
				}

				// Narrow phase 2: If no edges intersected, check if the center of the touch point is inside the polygon
				// This handles cases where the touch circle is fully contained within the polygon.
				if (polygon.contains(center.x, center.y)) {
					containedInCount++;
				}
			} // End loop through polygons

			if (intersected) {
				return 1d; // Collision detected via segment intersection
			}

			// If the point center is contained in an odd number of polygons (handles holes using non-zero winding rule implicitly by contains)
			// This logic seems derived from the original, assuming 'contains' correctly handles complex shapes/holes.
			if (containedInCount % 2 != 0) {
				return 1d; // Collision detected via containment
			}

		} // End loop through touching points

		return 0d; // No collision found for any touching point
	}

	public static List<Sprite[]> findAllCollisions(List<Sprite> sprites) {
		if (sprites == null || sprites.size() < 2) {
			return new ArrayList<>(); // Пустой список, если не с чем сталкиваться
		}

		// 1. Собираем данные для C++
		// Собираем только видимые и активные спрайты
		List<Sprite> activeSprites = new ArrayList<>();
		for (Sprite sprite : sprites) {
			if (sprite != null && sprite.look != null && sprite.look.isVisible() && sprite.look.isLookVisible()) {
				activeSprites.add(sprite);
			}
		}

		if (activeSprites.size() < 2) {
			return new ArrayList<>();
		}

		float[][][] allPolygons = new float[activeSprites.size()][][];
		for (int i = 0; i < activeSprites.size(); i++) {
			Polygon[] lookPolygons = activeSprites.get(i).look.getCurrentCollisionPolygon();
			if (lookPolygons == null) {
				allPolygons[i] = new float[0][]; // Пустой массив, если полигонов нет
				continue;
			}
			allPolygons[i] = new float[lookPolygons.length][];
			for (int j = 0; j < lookPolygons.length; j++) {
				if (lookPolygons[j] != null) {
					allPolygons[i][j] = lookPolygons[j].getTransformedVertices();
				} else {
					allPolygons[i][j] = new float[0];
				}
			}
		}

		// 2. Один-единственный вызов в C++!
		int[] collidingPairs = NativeLookOptimizer.checkAllCollisions(allPolygons);

		// 3. Разбираем результат
		List<Sprite[]> result = new ArrayList<>();
		if (collidingPairs != null) {
			for (int i = 0; i < collidingPairs.length; i += 2) {
				Sprite sprite1 = activeSprites.get(collidingPairs[i]);
				Sprite sprite2 = activeSprites.get(collidingPairs[i+1]);
				result.add(new Sprite[]{sprite1, sprite2});
			}
		}
		return result;
	}
}