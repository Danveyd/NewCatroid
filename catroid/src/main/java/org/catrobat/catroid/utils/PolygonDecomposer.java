// Файл: org/catrobat/catroid/utils/PolygonDecomposer.java
package org.catrobat.catroid.utils;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Утилита для разложения вогнутого полигона на массив выпуклых полигонов.
 * Использует алгоритм декомпозиции Bayazit.
 * Адаптировано из различных open-source реализаций для libGDX.
 */
public final class PolygonDecomposer {

    private PolygonDecomposer() {
        // Статический класс-утилита, конструктор не нужен
    }

    /**
     * Основной метод. Принимает один (возможно вогнутый) полигон и возвращает массив выпуклых частей.
     * @param concavePolygon Полигон для декомпозиции.
     * @return Array из выпуклых полигонов.
     */
    public static Array<Polygon> decompose(Polygon concavePolygon) {
        float[] vertices = concavePolygon.getVertices();
        Array<Vector2> polygonVertices = new Array<>(vertices.length / 2);
        for (int i = 0; i < vertices.length; i += 2) {
            polygonVertices.add(new Vector2(vertices[i], vertices[i + 1]));
        }

        Array<Array<Vector2>> convexPolygons = decompose(polygonVertices);

        Array<Polygon> result = new Array<>();
        for (Array<Vector2> polyVerts : convexPolygons) {
            float[] polyFloats = new float[polyVerts.size * 2];
            for (int i = 0; i < polyVerts.size; i++) {
                polyFloats[i * 2] = polyVerts.get(i).x;
                polyFloats[i * 2 + 1] = polyVerts.get(i).y;
            }
            result.add(new Polygon(polyFloats));
        }
        return result;
    }


    private static Array<Array<Vector2>> decompose(Array<Vector2> vertices) {
        Array<Array<Vector2>> list = new Array<>();
        float d, dist1, dist2;
        Vector2 p1, p2, p3;
        int i, j, l;
        int i1, i2, i3;
        int n = vertices.size;

        if (n < 3) {
            return list;
        }

        for (i = 0; i < n; i++) {
            i1 = i;
            i2 = (i + 1) % n;
            i3 = (i + 2) % n;
            p1 = vertices.get(i1);
            p2 = vertices.get(i2);
            p3 = vertices.get(i3);
            d = area(p1, p2, p3);
            if (d > 0) {
                for (j = 0; j < n; j++) {
                    if ((j == i1) || (j == i2) || (j == i3)) {
                        continue;
                    }
                    p3 = vertices.get(j);
                    if (isInside(p1, p2, vertices.get(i3), p3)) {
                        dist1 = p1.dst2(p3);
                        dist2 = p1.dst2(vertices.get(j + 1 > n - 1 ? 0 : j + 1));
                        for (l = 0; l < n; l++) {
                            Array<Vector2> part1 = copy(vertices, i1, l);
                            Array<Vector2> part2 = copy(vertices, l, i1);
                            list.addAll(decompose(part1));
                            list.addAll(decompose(part2));
                        }
                        return list;
                    }
                }
            }
        }

        list.add(vertices);
        return list;
    }

    private static float area(Vector2 a, Vector2 b, Vector2 c) {
        return a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y);
    }

    private static boolean isInside(Vector2 a, Vector2 b, Vector2 c, Vector2 p) {
        return area(a, b, p) >= 0 && area(b, c, p) >= 0 && area(c, a, p) >= 0;
    }

    private static Array<Vector2> copy(Array<Vector2> vertices, int i, int j) {
        Array<Vector2> p = new Array<>();
        while (i != j) {
            p.add(vertices.get(i));
            i = (i + 1) % vertices.size;
        }
        p.add(vertices.get(j));
        return p;
    }
}