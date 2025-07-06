#include <jni.h>
#include <string>
#include <vector>
#include <numeric> // Для std::accumulate
#include <cmath>
#include <algorithm> // для std::min/max

#include "onnxruntime_cxx_api.h"

// Глобальные переменные
Ort::Env env;
Ort::Session session{nullptr};
Ort::AllocatorWithDefaultOptions allocator;

// ВАШ МАКРОС С ПРАВИЛЬНЫM ПУТЕМ
#define JNI_FUNCTION(name) Java_org_catrobat_catroid_NN_OnnxSessionManager_##name

// Функция loadModelJNI остается без изменений
extern "C" JNIEXPORT jint JNICALL
JNI_FUNCTION(loadModelJNI)(JNIEnv* env, jobject /* this */, jstring modelPath) {
    const char* model_path_chars = env->GetStringUTFChars(modelPath, nullptr);
    try {
        session = Ort::Session(::env, model_path_chars, Ort::SessionOptions{nullptr});
    } catch (const Ort::Exception& e) {
        env->ReleaseStringUTFChars(modelPath, model_path_chars);
        return -1;
    }
    env->ReleaseStringUTFChars(modelPath, model_path_chars);
    return 0;
}

// ===== ГЛАВНОЕ ИЗМЕНЕНИЕ ЗДЕСЬ =====
extern "C" JNIEXPORT jfloatArray JNICALL
JNI_FUNCTION(runInferenceJNI)(JNIEnv* env, jobject /* this */, jfloatArray inputData) {
    if (!session) { return nullptr; }

    // --- Шаг 1: Динамически получаем ИМЯ входного слоя ---
    // (Для простоты предполагаем, что у модели один вход, что верно в 99% случаев)
    if (session.GetInputCount() == 0) { return nullptr; } // Ошибка: нет входов
    // GetInputNameAllocated требует, чтобы мы сами освободили память
    Ort::AllocatedStringPtr input_name_ptr = session.GetInputNameAllocated(0, allocator);
    const char* input_name_chars[] = { input_name_ptr.get() };


    // --- Шаг 2: Динамически получаем ФОРМУ входного слоя ---
    Ort::TypeInfo input_type_info = session.GetInputTypeInfo(0);
    auto tensor_info = input_type_info.GetTensorTypeAndShapeInfo();
    std::vector<int64_t> input_shape = tensor_info.GetShape();

    // --- Шаг 3: Обрабатываем "динамические" измерения (например, batch size) ---
    // Модели часто имеют размерность -1, что означает "любой размер".
    // Мы заменяем это на 1, так как обрабатываем один набор данных за раз.
    for (int64_t &dim : input_shape) {
        if (dim < 1) {
            dim = 1;
        }
    }

    // --- Шаг 4: Проверяем, совпадают ли данные от пользователя с ожиданиями модели ---
    jsize userInputSize = env->GetArrayLength(inputData);
    // Считаем, сколько всего элементов ожидает модель (перемножаем все измерения)
    size_t expectedInputSize = std::accumulate(input_shape.begin(), input_shape.end(), 1LL, std::multiplies<int64_t>());

    if (userInputSize != expectedInputSize) {
        // Если пользователь подал 783 числа вместо 784, возвращаем ошибку
        // (в Kotlin это можно будет обработать и показать сообщение)
        return nullptr;
    }

    jfloat* input_floats = env->GetFloatArrayElements(inputData, nullptr);
    std::vector<float> input_vec(input_floats, input_floats + userInputSize);

    // --- Шаг 5: Создаем входной тензор с правильной формой и именем ---
    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
            memory_info, input_vec.data(), input_vec.size(), input_shape.data(), input_shape.size());

    // --- Шаг 6: Динамически получаем имя выходного слоя ---
    if (session.GetOutputCount() == 0) { return nullptr; }
    Ort::AllocatedStringPtr output_name_ptr = session.GetOutputNameAllocated(0, allocator);
    const char* output_name_chars[] = { output_name_ptr.get() };


    // --- Шаг 7: Запускаем модель ---
    auto output_tensors = session.Run(Ort::RunOptions{nullptr}, input_name_chars, &input_tensor, 1, output_name_chars, 1);

    // --- Шаг 8: Обрабатываем выход (этот код у нас уже был гибким) ---
    const auto& output_tensor = output_tensors[0];
    auto output_shape_info = output_tensor.GetTensorTypeAndShapeInfo();
    size_t output_size = output_shape_info.GetElementCount();
    const float* output_data = output_tensor.GetTensorData<float>();

    jfloatArray resultArray = env->NewFloatArray(output_size);
    env->SetFloatArrayRegion(resultArray, 0, output_size, output_data);

    env->ReleaseFloatArrayElements(inputData, input_floats, 0);
    return resultArray;
}

// Функция unloadModelJNI остается без изменений
extern "C" JNIEXPORT void JNICALL
JNI_FUNCTION(unloadModelJNI)(JNIEnv* env, jobject /* this */) {
    session = Ort::Session(nullptr);
}

struct Transform {
    float x, y;
    float scaleX, scaleY;
    float rotation; // в градусах
    float originX, originY;
};

// Функция, которая делает всю математику
// vertices - это плоский массив [x1, y1, x2, y2, ...]
std::vector<float> transform_vertices(const std::vector<float>& vertices, const Transform& transform) {
    std::vector<float> transformed_vertices;
    transformed_vertices.reserve(vertices.size());

    float rotation_rad = transform.rotation * (M_PI / 180.0f);
    float cos_r = cos(rotation_rad);
    float sin_r = sin(rotation_rad);

    for (size_t i = 0; i < vertices.size(); i += 2) {
        // 1. Смещение относительно origin
        float vx = vertices[i] - transform.originX;
        float vy = vertices[i+1] - transform.originY;

        // 2. Масштабирование
        vx *= transform.scaleX;
        vy *= transform.scaleY;

        // 3. Поворот
        float rotated_x = vx * cos_r - vy * sin_r;
        float rotated_y = vx * sin_r + vy * cos_r;

        // 4. Возвращаем origin и добавляем позицию объекта
        transformed_vertices.push_back(rotated_x + transform.originX + transform.x);
        transformed_vertices.push_back(rotated_y + transform.originY + transform.y);
    }

    return transformed_vertices;
}

#define JNI_OPTIMIZER_FUNCTION(name) Java_org_catrobat_catroid_utils_NativeLookOptimizer_##name

extern "C" JNIEXPORT jfloatArray JNICALL
JNI_OPTIMIZER_FUNCTION(transformPolygon)(
        JNIEnv* env,
        jclass /* this */,
        jfloatArray jvertices,
        jfloat x, jfloat y,
        jfloat scaleX, jfloat scaleY,
        jfloat rotation,
        jfloat originX, jfloat originY) {

    // 1. Конвертируем jfloatArray в std::vector<float>
    jsize len = env->GetArrayLength(jvertices);
    jfloat* vertex_elements = env->GetFloatArrayElements(jvertices, nullptr);
    std::vector<float> vertices_vec(vertex_elements, vertex_elements + len);
    env->ReleaseFloatArrayElements(jvertices, vertex_elements, JNI_ABORT); // JNI_ABORT, т.к. мы не меняли исходные данные

    // 2. Заполняем структуру Transform
    Transform t = {x, y, scaleX, scaleY, rotation, originX, originY};

    // 3. Вызываем нашу "чистую" C++ функцию
    std::vector<float> transformed_vertices = transform_vertices(vertices_vec, t);

    // 4. Конвертируем результат обратно в jfloatArray
    jfloatArray result_array = env->NewFloatArray(transformed_vertices.size());
    env->SetFloatArrayRegion(result_array, 0, transformed_vertices.size(), transformed_vertices.data());

    return result_array;
}

// В catroid.cpp

// Функция, которая вычисляет AABB (Axis-Aligned Bounding Box)
// Возвращает массив из 4-х float: [minX, minY, maxX, maxY]
std::vector<float> calculate_aabb(const Transform& transform, float width, float height) {
    // Вершины локального прямоугольника
    float local_coords[8] = {
            0, 0,          // левый нижний
            width, 0,      // правый нижний
            width, height, // правый верхний
            0, height      // левый верхний
    };

    float rotation_rad = transform.rotation * (M_PI / 180.0f);
    float cos_r = cos(rotation_rad);
    float sin_r = sin(rotation_rad);

    // Трансформируем все 4 вершины
    std::vector<float> transformed_x;
    std::vector<float> transformed_y;
    transformed_x.reserve(4);
    transformed_y.reserve(4);

    for (size_t i = 0; i < 8; i += 2) {
        float vx = local_coords[i] - transform.originX;
        float vy = local_coords[i+1] - transform.originY;

        vx *= transform.scaleX;
        vy *= transform.scaleY;

        float rotated_x = vx * cos_r - vy * sin_r;
        float rotated_y = vx * sin_r + vy * cos_r;

        transformed_x.push_back(rotated_x + transform.originX + transform.x);
        transformed_y.push_back(rotated_y + transform.originY + transform.y);
    }

    // Находим min/max среди трансформированных вершин
    float min_x = *std::min_element(transformed_x.begin(), transformed_x.end());
    float max_x = *std::max_element(transformed_x.begin(), transformed_x.end());
    float min_y = *std::min_element(transformed_y.begin(), transformed_y.end());
    float max_y = *std::max_element(transformed_y.begin(), transformed_y.end());

    return {min_x, min_y, max_x - min_x, max_y - min_y}; // Возвращаем {x, y, width, height}
}


// JNI-обертка
extern "C" JNIEXPORT jfloatArray JNICALL
JNI_OPTIMIZER_FUNCTION(getTransformedBoundingBox)(
        JNIEnv* env,
        jclass /* this */,
        jfloat x, jfloat y,
        jfloat width, jfloat height,
        jfloat scaleX, jfloat scaleY,
        jfloat rotation,
        jfloat originX, jfloat originY) {

    Transform t = {x, y, scaleX, scaleY, rotation, originX, originY};
    // Используем размер *до* масштабирования
    std::vector<float> aabb = calculate_aabb(t, width / scaleX, height / scaleY);

    jfloatArray result_array = env->NewFloatArray(4);
    env->SetFloatArrayRegion(result_array, 0, 4, aabb.data());
    return result_array;
}

struct AABB {
    float minX, minY, maxX, maxY;
};

// Проверяет, пересекаются ли две рамки
bool aabbs_overlap(const AABB& a, const AABB& b) {
    if (a.maxX < b.minX || b.maxX < a.minX) return false;
    if (a.maxY < b.minY || b.maxY < a.minY) return false;
    return true;
}

// Проверяет пересечение двух выпуклых полигонов методом SAT (Separating Axis Theorem)
// Это очень быстрая математическая проверка.
// `vertsA` и `vertsB` - это плоские массивы вершин [x1, y1, x2, y2, ...]
bool polygons_overlap(const float* vertsA, int countA, const float* vertsB, int countB) {
    // --- Проверка по осям полигона A ---
    for (int i = 0; i < countA; i += 2) {
        // Находим вектор нормали к текущему ребру
        float p1x = vertsA[i];
        float p1y = vertsA[i + 1];
        float p2x = vertsA[(i + 2) % countA];
        float p2y = vertsA[(i + 3) % countA];
        float axisX = -(p2y - p1y);
        float axisY = p2x - p1x;

        // Проецируем обе фигуры на эту ось
        float minA = 1e9, maxA = -1e9, minB = 1e9, maxB = -1e9;
        for (int j = 0; j < countA; j += 2) {
            float dot = vertsA[j] * axisX + vertsA[j + 1] * axisY;
            minA = std::min(minA, dot);
            maxA = std::max(maxA, dot);
        }
        for (int j = 0; j < countB; j += 2) {
            float dot = vertsB[j] * axisX + vertsB[j + 1] * axisY;
            minB = std::min(minB, dot);
            maxB = std::max(maxB, dot);
        }

        // Если проекции не пересекаются - столкновения нет
        if (maxA < minB || maxB < minA) return false;
    }

    // --- Проверка по осям полигона B (аналогично) ---
    for (int i = 0; i < countB; i += 2) {
        float p1x = vertsB[i];
        float p1y = vertsB[i + 1];
        float p2x = vertsB[(i + 2) % countB];
        float p2y = vertsB[(i + 3) % countB];
        float axisX = -(p2y - p1y);
        float axisY = p2x - p1x;

        float minA = 1e9, maxA = -1e9, minB = 1e9, maxB = -1e9;
        for (int j = 0; j < countA; j += 2) {
            float dot = vertsA[j] * axisX + vertsA[j+1] * axisY;
            minA = std::min(minA, dot);
            maxA = std::max(maxA, dot);
        }
        for (int j = 0; j < countB; j += 2) {
            float dot = vertsB[j] * axisX + vertsB[j+1] * axisY;
            minB = std::min(minB, dot);
            maxB = std::max(maxB, dot);
        }

        if (maxA < minB || maxB < minA) return false;
    }

    // Если все оси пересекаются - есть столкновение
    return true;
}


// JNI-функция. Имя класса NativeLookOptimizer - то же, что и раньше.
extern "C" JNIEXPORT jintArray JNICALL
Java_org_catrobat_catroid_utils_NativeLookOptimizer_checkAllCollisions(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray allSpritesPolygons // Массив массивов полигонов для каждого спрайта
) {
    int spriteCount = env->GetArrayLength(allSpritesPolygons);
    if (spriteCount < 2) {
        return env->NewIntArray(0);
    }

    std::vector<std::vector<std::vector<float>>> spritesData(spriteCount);
    std::vector<AABB> spriteAABBs(spriteCount);

    // 1. Извлекаем все данные из Java в C++ структуры
    for (int i = 0; i < spriteCount; ++i) {
        auto polygonsArray = (jobjectArray)env->GetObjectArrayElement(allSpritesPolygons, i);
        int polygonCount = env->GetArrayLength(polygonsArray);
        spritesData[i].resize(polygonCount);

        float spriteMinX = 1e9, spriteMinY = 1e9, spriteMaxX = -1e9, spriteMaxY = -1e9;

        for (int j = 0; j < polygonCount; ++j) {
            auto verticesArray = (jfloatArray)env->GetObjectArrayElement(polygonsArray, j);
            jfloat* vertices = env->GetFloatArrayElements(verticesArray, nullptr);
            int vertexCount = env->GetArrayLength(verticesArray);
            spritesData[i][j].assign(vertices, vertices + vertexCount);

            // Вычисляем AABB для всего спрайта
            for (int k = 0; k < vertexCount; k += 2) {
                spriteMinX = std::min(spriteMinX, vertices[k]);
                spriteMinY = std::min(spriteMinY, vertices[k+1]);
                spriteMaxX = std::max(spriteMaxX, vertices[k]);
                spriteMaxY = std::max(spriteMaxY, vertices[k+1]);
            }
            env->ReleaseFloatArrayElements(verticesArray, vertices, JNI_ABORT);
            env->DeleteLocalRef(verticesArray);
        }
        spriteAABBs[i] = {spriteMinX, spriteMinY, spriteMaxX, spriteMaxY};
        env->DeleteLocalRef(polygonsArray);
    }

    // 2. Основной цикл проверки столкновений (N^2) - но теперь в быстром C++!
    std::vector<int> collidingPairs;
    for (int i = 0; i < spriteCount; ++i) {
        for (int j = i + 1; j < spriteCount; ++j) {
            // Broad phase: быстрая проверка по общим рамкам спрайтов
            if (aabbs_overlap(spriteAABBs[i], spriteAABBs[j])) {
                // Narrow phase: детальная проверка полигонов
                bool collisionFound = false;
                for (const auto& polyA : spritesData[i]) {
                    for (const auto& polyB : spritesData[j]) {
                        if (polygons_overlap(polyA.data(), polyA.size(), polyB.data(), polyB.size())) {
                            collidingPairs.push_back(i);
                            collidingPairs.push_back(j);
                            collisionFound = true;
                            break;
                        }
                    }
                    if (collisionFound) break;
                }
            }
        }
    }

    // 3. Возвращаем результат в Java
    jintArray resultArray = env->NewIntArray(collidingPairs.size());
    env->SetIntArrayRegion(resultArray, 0, collidingPairs.size(), collidingPairs.data());
    return resultArray;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_catrobat_catroid_utils_NativeLookOptimizer_checkSingleCollision(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray firstLookPolygons,  // Полигоны первого объекта
        jobjectArray secondLookPolygons  // Полигоны второго объекта
) {
    int firstPolygonCount = env->GetArrayLength(firstLookPolygons);
    int secondPolygonCount = env->GetArrayLength(secondLookPolygons);

    // Главный цикл проверки в C++
    for (int i = 0; i < firstPolygonCount; ++i) {
        auto polyA_jfloatArray = (jfloatArray)env->GetObjectArrayElement(firstLookPolygons, i);
        jfloat* vertsA = env->GetFloatArrayElements(polyA_jfloatArray, nullptr);
        int countA = env->GetArrayLength(polyA_jfloatArray);

        for (int j = 0; j < secondPolygonCount; ++j) {
            auto polyB_jfloatArray = (jfloatArray)env->GetObjectArrayElement(secondLookPolygons, j);
            jfloat* vertsB = env->GetFloatArrayElements(polyB_jfloatArray, nullptr);
            int countB = env->GetArrayLength(polyB_jfloatArray);

            // Проверяем пару полигонов
            if (polygons_overlap(vertsA, countA, vertsB, countB)) {
                // Столкновение найдено! Освобождаем память и выходим.
                env->ReleaseFloatArrayElements(polyA_jfloatArray, vertsA, JNI_ABORT);
                env->ReleaseFloatArrayElements(polyB_jfloatArray, vertsB, JNI_ABORT);
                env->DeleteLocalRef(polyA_jfloatArray);
                env->DeleteLocalRef(polyB_jfloatArray);
                return JNI_TRUE; // Возвращаем true
            }

            // Освобождаем память для внутреннего цикла
            env->ReleaseFloatArrayElements(polyB_jfloatArray, vertsB, JNI_ABORT);
            env->DeleteLocalRef(polyB_jfloatArray);
        }

        // Освобождаем память для внешнего цикла
        env->ReleaseFloatArrayElements(polyA_jfloatArray, vertsA, JNI_ABORT);
        env->DeleteLocalRef(polyA_jfloatArray);
    }

    // Если ни одна пара не столкнулась
    return JNI_FALSE; // Возвращаем false
}