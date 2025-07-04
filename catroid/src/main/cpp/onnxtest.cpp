#include <jni.h>
#include <string>
#include <vector>

// Подключаем заголовочный файл ONNX
#include "onnxruntime_cxx_api.h"

// Глобальные переменные для хранения сессии ONNX
Ort::Env env;
Ort::Session session{nullptr};
Ort::AllocatorWithDefaultOptions allocator;

extern "C" JNIEXPORT jint JNICALL
// Убедитесь, что имя пакета здесь (com_example_onnxtestapp) совпадает с вашим
Java_com_danvexteam_onnxtest_MainActivity_loadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath) {

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


extern "C" JNIEXPORT jfloatArray JNICALL
// И здесь тоже
Java_com_danvexteam_onnxtest_MainActivity_runInference(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray inputData) {

    if (!session) { return nullptr; }

    jfloat* input_floats = env->GetFloatArrayElements(inputData, nullptr);
    std::vector<float> input_vec(input_floats, input_floats + 2);
    std::vector<int64_t> input_shape = {1, 2};

    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
            memory_info, input_vec.data(), input_vec.size(), input_shape.data(), input_shape.size());

    const char* input_names[] = {"input"};
    const char* output_names[] = {"output"};

    auto output_tensors = session.Run(Ort::RunOptions{nullptr}, input_names, &input_tensor, 1, output_names, 1);

    float* output_data = output_tensors[0].GetTensorMutableData<float>();

    jfloatArray resultArray = env->NewFloatArray(1);
    env->SetFloatArrayRegion(resultArray, 0, 1, output_data);

    env->ReleaseFloatArrayElements(inputData, input_floats, 0);
    return resultArray;
}