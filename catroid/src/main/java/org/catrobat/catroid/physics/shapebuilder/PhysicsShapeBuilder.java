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
package org.catrobat.catroid.physics.shapebuilder;

import android.util.Log;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2ShapeId;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2Rot;

import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public final class PhysicsShapeBuilder {

	private static final String TAG = PhysicsShapeBuilder.class.getSimpleName();
	private static final float[] ACCURACY_LEVELS = {0.125f, 0.25f, 0.50f, 0.75f, 1.0f};

	private static PhysicsShapeBuilder instance = null;

	public static PhysicsShapeBuilder getInstance() {
		if (instance == null) {
			instance = new PhysicsShapeBuilder();
		}
		return instance;
	}

	private PhysicsShapeBuilderStrategy strategy = new PhysicsShapeBuilderStrategyFastHull();
	private Map<String, ImageShapes> imageShapesMap = new HashMap<>();

	private PhysicsShapeBuilder() {
	}

	public void reset() {
		strategy = new PhysicsShapeBuilderStrategyFastHull();
		imageShapesMap = new HashMap<>();
	}

	/**
	 * Получает масштабированные физические геометрии (b2Polygon, b2Circle) для данного LookData.
	 * Важно: Этот метод НЕ возвращает b2ShapeDef. Вызывающий код должен сам создать
	 * b2ShapeDef и использовать его вместе с возвращенными геометриями при создании Fixture.
	 *
	 * @param lookData    Данные внешнего вида.
	 * @param scaleFactor Конечный коэффициент масштабирования.
	 * @return Массив Object[], содержащий экземпляры b2Polygon и/или b2Circle,
	 *         масштабированные до scaleFactor. Null в случае ошибки.
	 * @throws RuntimeException Если lookData == null или scaleFactor < 0.
	 */
	public synchronized Object[] getScaledGeometries(LookData lookData, float scaleFactor) throws RuntimeException {
		if (scaleFactor < 0) {
			throw new RuntimeException("scaleFactor can not be smaller than 0");
		}
		if (lookData == null) {
			throw new RuntimeException("getScaledGeometries for null lookData not possible");
		}

		Pixmap pixmap = lookData.getPixmap();
		if (pixmap == null) {
			// pixmap может быть null, если ресурс не найден или не загружен
			Log.e(TAG, "Pixmap is null for lookData: " + lookData.getFile().getAbsolutePath());
			return null; // Не можем построить форму без Pixmap
		}
		if (pixmap.isDisposed()) {
			Log.e(TAG, "Pixmap is disposed for lookData: " + lookData.getFile().getAbsolutePath());
			return null; // Не можем построить форму из удаленного Pixmap
		}


		String imageIdentifier = Utils.md5Checksum(lookData.getFile());
		if (imageIdentifier == null) {
			Log.e(TAG, "Could not compute MD5 checksum for lookData: " + lookData.getFile().getAbsolutePath());
			return null; // Не можем идентифицировать изображение
		}

		// Синхронизация доступа к imageShapesMap
		ImageShapes shapesForImage;
		synchronized (imageShapesMap) {
			if (!imageShapesMap.containsKey(imageIdentifier)) {
				try {
					// Передаем pixmap в конструктор ImageShapes
					imageShapesMap.put(imageIdentifier, new ImageShapes(pixmap));
				} catch (RuntimeException e) {
					Log.e(TAG, "Failed to create ImageShapes for " + imageIdentifier, e);
					return null;
				}
			}
			shapesForImage = imageShapesMap.get(imageIdentifier);
		}

		if (shapesForImage == null) {
			// Эта ситуация не должна возникать из-за синхронизации, но проверим
			Log.e(TAG, "Internal error: ImageShapes is null after put/get for " + imageIdentifier);
			return null;
		}

		float accuracyLevel = getAccuracyLevel(scaleFactor);

		// Получаем геометрии базового масштаба (1.0) из кэша
		Object[] baseGeometries;
		try {
			baseGeometries = shapesForImage.getGeometries(accuracyLevel);
		} catch (RuntimeException e) {
			Log.e(TAG, "Failed to get base geometries for " + imageIdentifier + " at accuracy " + accuracyLevel, e);
			return null;
		}


		if (baseGeometries == null) {
			Log.e(TAG, "Base geometries are null for " + imageIdentifier + " at accuracy " + accuracyLevel);
			return null; // Ошибка при вычислении или получении из кэша
		}

		// Масштабируем базовые геометрии до конечного scaleFactor
		Object[] finalScaledGeometries = PhysicsShapeScaleUtils.scaleGeometries(baseGeometries, scaleFactor);

		if (finalScaledGeometries == null) {
			Log.e(TAG, "Failed to scale geometries to final scaleFactor " + scaleFactor + " for " + imageIdentifier);
			// Ошибка уже залогирована в scaleGeometries
			return null;
		}

		return finalScaledGeometries; // Возвращаем массив масштабированных геометрий
	}

	private static float getAccuracyLevel(float scaleFactor) {
		if (ACCURACY_LEVELS.length == 0) {
			return 0;
		}

		if (ACCURACY_LEVELS.length == 1) {
			return ACCURACY_LEVELS[0];
		}

		for (int accuracyIdx = 0; accuracyIdx < ACCURACY_LEVELS.length - 1; accuracyIdx++) {
			float average = (ACCURACY_LEVELS[accuracyIdx] + ACCURACY_LEVELS[accuracyIdx]) / 2;
			if (scaleFactor < average) {
				return ACCURACY_LEVELS[accuracyIdx];
			}
		}
		return ACCURACY_LEVELS[ACCURACY_LEVELS.length - 1];
	}

	/**
	 * Saves computed shapes in different accuracies for one image. (All in baseline -> 100%)
	 */
	private class ImageShapes {

		private static final int MAX_ORIGINAL_PIXMAP_SIZE = 512;

		// Кэш теперь хранит String -> Object[] (массив геометрий)
		private Map<String, Object[]> geometryMap = new HashMap<>();
		private Pixmap originalPixmap; // Храним ссылку на оригинальный Pixmap
		private float sizeAdjustmentScaleFactor = 1.0f; // Инициализируем

		ImageShapes(Pixmap pixmap) {
			if (pixmap == null || pixmap.isDisposed()) {
				// Проверка на null и disposed обязательна
				throw new RuntimeException("Pixmap must not be null or disposed");
			}
			// Не копируем Pixmap, используем оригинал. Важно, чтобы он не был удален извне.
			this.originalPixmap = pixmap;

			int width = this.originalPixmap.getWidth();
			int height = this.originalPixmap.getHeight();

			// Вычисляем sizeAdjustmentScaleFactor, если изображение слишком большое
			if (width > MAX_ORIGINAL_PIXMAP_SIZE || height > MAX_ORIGINAL_PIXMAP_SIZE) {
				if (width > height) {
					sizeAdjustmentScaleFactor = (float) MAX_ORIGINAL_PIXMAP_SIZE / width;
				} else {
					sizeAdjustmentScaleFactor = (float) MAX_ORIGINAL_PIXMAP_SIZE / height;
				}
				Log.d(TAG, "Image larger than max size, applying adjustment factor: " + sizeAdjustmentScaleFactor);
			} else {
				sizeAdjustmentScaleFactor = 1.0f; // Явно устанавливаем 1.0, если调整 не нужен
			}
		}

		private String getGeometryKey(float accuracyLevel) {
			// Ключ на основе точности
			return String.valueOf((int) (accuracyLevel * 100));
		}

		/**
		 * Вычисляет ИЛИ получает из кэша геометрии базового масштаба (1.0) для заданной точности.
		 * @param accuracyLevel Уровень точности (от 0 до 1).
		 * @return Массив Object[] с геометриями (b2Polygon/b2Circle) масштаба 1.0.
		 * @throws RuntimeException если произошла ошибка при построении или масштабировании.
		 */
		public synchronized Object[] getGeometries(float accuracyLevel) throws RuntimeException {
			String geometryKey = getGeometryKey(accuracyLevel);

			if (!geometryMap.containsKey(geometryKey)) {
				Log.d(TAG, "Cache miss for geometry key: " + geometryKey + ". Computing new shapes.");
				Object[] computedGeometries = computeAndCacheGeometries(accuracyLevel);
				if (computedGeometries == null) {
					// Ошибка уже залогирована в computeAndCacheGeometries
					throw new RuntimeException("Failed to compute geometries for accuracy: " + accuracyLevel);
				}
				// Кладём в кэш вычисленные геометрии
				geometryMap.put(geometryKey, computedGeometries);
			} else {
				Log.d(TAG, "Cache hit for geometry key: " + geometryKey);
			}

			// Возвращаем из кэша
			return geometryMap.get(geometryKey);
		}


		/**
		 * Вычисляет геометрии для заданной точности и масштабирует их к базовому масштабу 1.0.
		 * Этот метод НЕ использует кэш для чтения, только для записи результата.
		 * @param accuracy Уровень точности.
		 * @return Массив Object[] с геометриями масштаба 1.0 или null при ошибке.
		 */
		private Object[] computeAndCacheGeometries(float accuracy) {
			// Проверяем оригинальный Pixmap перед использованием
			if (originalPixmap == null || originalPixmap.isDisposed()) {
				Log.e(TAG, "Original pixmap is null or disposed during computeNewShape.");
				return null;
			}

			int originalWidth = originalPixmap.getWidth();
			int originalHeight = originalPixmap.getHeight();

			// Вычисляем размеры для Pixmap с учетом точности и ограничения размера
			int scaledWidth = Math.round(originalWidth * sizeAdjustmentScaleFactor * accuracy);
			int scaledHeight = Math.round(originalHeight * sizeAdjustmentScaleFactor * accuracy);

			// Минимальный размер 1x1
			if (scaledWidth < 1) scaledWidth = 1;
			if (scaledHeight < 1) scaledHeight = 1;

			Pixmap scaledPixmap = null;
			Object[] rawGeometries = null;
			Object[] finalBaseGeometries = null;

			try {
				// Создаем временный масштабированный Pixmap
				scaledPixmap = new Pixmap(scaledWidth, scaledHeight, originalPixmap.getFormat());
				// Фильтр важен для сохранения резких краев при масштабировании
				scaledPixmap.setFilter(Pixmap.Filter.NearestNeighbour);
				// Копируем/масштабируем данные из оригинала
				scaledPixmap.drawPixmap(originalPixmap, 0, 0, originalWidth, originalHeight, 0, 0, scaledWidth, scaledHeight);

				// --- Вызов стратегии построения ---
				// Предполагаем, что strategy.build возвращает Object[] геометрий
				// Масштаб 1.0f передается, т.к. Pixmap уже масштабирован до нужного размера
				rawGeometries = strategy.build(scaledPixmap, 1.0f);

				if (rawGeometries == null) {
					Log.e(TAG, "Strategy returned null geometries for accuracy: " + accuracy);
					return null;
				}

				// --- Масштабирование к базовому размеру (1.0) ---
				// Геометрии были созданы для Pixmap размера (scaledWidth, scaledHeight),
				// который соответствует масштабу (sizeAdjustmentScaleFactor * accuracy) от оригинала.
				// Нам нужно привести их к масштабу 1.0 относительно оригинала.
				float inverseScale = 1.0f / (sizeAdjustmentScaleFactor * accuracy);
				if (Float.isInfinite(inverseScale) || Float.isNaN(inverseScale) || inverseScale <= 0) {
					Log.e(TAG, "Invalid inverse scale calculated: " + inverseScale + " from accuracy: " + accuracy + " and adjustment: " + sizeAdjustmentScaleFactor);
					return null;
				}


				finalBaseGeometries = PhysicsShapeScaleUtils.scaleGeometries(rawGeometries, inverseScale);

				if (finalBaseGeometries == null) {
					Log.e(TAG, "Failed to scale raw geometries to base scale (1.0) for accuracy: " + accuracy);
					// Ошибка залогирована в scaleGeometries
					return null;
				}

				Log.d(TAG, "Successfully computed and scaled geometries to base scale for accuracy: " + accuracy);
				return finalBaseGeometries; // Успех

			} catch (Exception e) {
				Log.e(TAG, "Exception during geometry computation for accuracy " + accuracy, e);
				return null; // Ошибка
			} finally {
				// Освобождаем временный Pixmap, если он был создан
				if (scaledPixmap != null && !scaledPixmap.isDisposed()) {
					scaledPixmap.dispose();
				}
				// --- Управление памятью rawGeometries ---
				// Если strategy.build() вернула геометрии с freeOnGC=false,
				// И finalBaseGeometries != null (т.е. созданы новые копии),
				// то здесь нужно было бы освободить память rawGeometries.
				// Но мы предполагаем, что всё управляется GC.
			}
		}
	}
}
