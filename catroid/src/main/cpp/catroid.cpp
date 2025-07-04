#include <jni.h>
#include <string>
#include <vector>
#include <numeric> // Для std::accumulate

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