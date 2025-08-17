#include <jni.h>
#include <string>
#include <vector>
#include <numeric> // Для std::accumulate
#include <cmath>
#include <algorithm> // для std::min/max
#include "earcut.hpp"

#include "onnxruntime_cxx_api.h"

#include <android/log.h>
// Убедитесь, что путь и версия правильные!

// Глобальные переменные
Ort::Env env;
Ort::Env ort_env;
Ort::Session session{nullptr};
Ort::AllocatorWithDefaultOptions allocator;

#define LOG_TAG "PythonBridge"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#ifdef __aarch64__  // Эта директива истинна ТОЛЬКО при сборке для arm64-v8a
#include "include/python3.12/Python.h"

#define JNI_PYTHON_FUNCTION(name) Java_org_catrobat_catroid_python_PythonEngine_##name
static std::atomic<PyThreadState*> g_worker_thread_state(nullptr);
// Глобальный ID потока для PyThreadState_SetAsyncExc
static std::atomic<unsigned long> g_worker_thread_id(0);

// Новая JNI-функция для вызова из Kotlin
extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeForceStopPythonScript)(JNIEnv* env, jobject /* this */) {
    if (!Py_IsInitialized()) {
        LOGD("Python is not initialized or is finalizing. Skipping force stop.");
        return; // Безопасно выходим
    }

    unsigned long thread_id = g_worker_thread_id.load();
    if (thread_id != 0) {
        LOGD("Attempting to inject SystemExit exception into thread ID: %lu", thread_id);

        // 2. ЗАХВАТЫВАЕМ GIL ДЛЯ ДОПОЛНИТЕЛЬНОЙ БЕЗОПАСНОСТИ
        //    Это гарантирует, что мы не помешаем другим операциям.
        PyGILState_STATE gstate = PyGILState_Ensure();

        PyThreadState_SetAsyncExc(thread_id, PyExc_SystemExit);

        PyGILState_Release(gstate);

    } else {
        LOGD("No active Python script thread to stop.");
    }
}

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeInitPython)(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray modulePaths) { // <-- Теперь это массив строк

    PyConfig config;
    PyConfig_InitPythonConfig(&config);
    config.install_signal_handlers = 0;
    config.module_search_paths_set = 1;

    // Итерируемся по массиву путей из Kotlin и добавляем каждый в PyConfig
    int numPaths = env->GetArrayLength(modulePaths);
    LOGD("Preparing Python with %d module paths...", numPaths);
    for (int i = 0; i < numPaths; i++) {
        jstring path_jstr = (jstring) env->GetObjectArrayElement(modulePaths, i);
        const char* path_cstr = env->GetStringUTFChars(path_jstr, 0);
        wchar_t* path_wstr = Py_DecodeLocale(path_cstr, NULL);

        PyWideStringList_Append(&config.module_search_paths, path_wstr);
        LOGD("Added path %d: %s", i + 1, path_cstr);

        PyMem_RawFree(path_wstr);
        env->ReleaseStringUTFChars(path_jstr, path_cstr);
        env->DeleteLocalRef(path_jstr);
    }

    // Инициализируем Python
    PyStatus status = Py_InitializeFromConfig(&config);
    if (PyStatus_Exception(status)) {
        LOGD("FATAL: Py_InitializeFromConfig failed.");
        Py_ExitStatusException(status);
    } else {
        LOGD("!!! SUCCESS: Python has been initialized correctly !!!");
    }

    PyConfig_Clear(&config);
}

// ДОБАВЬТЕ ЭТУ ФУНКЦИЮ В catroid.cpp
extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeFinalizePython)(JNIEnv* env, jobject /* this */) {
    if (Py_IsInitialized()) {
        Py_FinalizeEx();
        LOGD("Python environment has been finalized.");
    } else {
        LOGD("Python was not initialized, skipping finalization.");
    }
}

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeInitPython2)(
        JNIEnv* env,
        jobject /* this */,
        jstring pythonHome,
        jstring projectLibsPath) {

    // --- Ресурсы для очистки ---
    const char* pyHome_cstr = env->GetStringUTFChars(pythonHome, 0);
    const char* pyLibsPath_cstr = env->GetStringUTFChars(projectLibsPath, 0);
    wchar_t *pyHomeW = Py_DecodeLocale(pyHome_cstr, NULL);
    wchar_t *pyLibsPathW = Py_DecodeLocale(pyLibsPath_cstr, NULL);
    PyConfig config;
    PyConfig_InitPythonConfig(&config);
    bool success = false;

    if (pyHomeW && pyLibsPathW) {
        LOGD("Preparing Python with FULL PATH OVERRIDE...");
        config.install_signal_handlers = 0;
        config.verbose = 0;
        PyStatus status;

        // ---> НАЧАЛО: ФИНАЛЬНЫЙ И САМЫЙ ВАЖНЫЙ ФИКС <---

        // 1. Говорим Python, что мы полностью переопределяем пути поиска.
        //    Он больше не будет пытаться угадать их сам.
        config.module_search_paths_set = 1;

        // 2. Создаем три необходимых пути.
        std::wstring path_stdlib(pyHomeW); // Путь к стандартным .py файлам
        std::wstring path_pylibs(pyLibsPathW); // Путь к нашим библиотекам (telebot)
        std::wstring path_dynload = std::wstring(pyHomeW) + L"/lib-dynload"; // !! КЛЮЧЕВОЙ ПУТЬ К .so МОДУЛЯМ !!

        // 3. Добавляем эти три пути в список поиска модулей.
        status = PyWideStringList_Append(&config.module_search_paths, path_stdlib.c_str());
        if (!PyStatus_Exception(status)) {
            status = PyWideStringList_Append(&config.module_search_paths, path_pylibs.c_str());
        }
        if (!PyStatus_Exception(status)) {
            status = PyWideStringList_Append(&config.module_search_paths, path_dynload.c_str());
        }

        // ---> КОНЕЦ ФИНАЛЬНОГО ФИКСА <---

        if (PyStatus_Exception(status)) {
            LOGD("FATAL: Failed to construct module search paths.");
        } else {
            LOGD("Module Search Paths set to:");
            LOGD("1: %ls", path_stdlib.c_str());
            LOGD("2: %ls", path_pylibs.c_str());
            LOGD("3: %ls", path_dynload.c_str());
            LOGD("Initializing with Py_InitializeFromConfig...");

            status = Py_InitializeFromConfig(&config);
            if (PyStatus_Exception(status)) {
                LOGD("FATAL: Py_InitializeFromConfig failed.");
                Py_ExitStatusException(status);
            } else {
                LOGD("!!! SUCCESS: Python has been initialized correctly !!!");
                success = true;
            }
        }
    } else {
        LOGD("FATAL: Failed to decode Python paths.");
    }

    // --- Очистка ---
    PyConfig_Clear(&config);
    if (pyHomeW) PyMem_RawFree(pyHomeW);
    if (pyLibsPathW) PyMem_RawFree(pyLibsPathW);
    env->ReleaseStringUTFChars(pythonHome, pyHome_cstr);
    env->ReleaseStringUTFChars(projectLibsPath, pyLibsPath_cstr);

    if (!success) {
        LOGD("Python initialization failed.");
    }
}

extern "C" JNIEXPORT jstring JNICALL
JNI_PYTHON_FUNCTION(nativeRunScript2)(
        JNIEnv* env,
        jobject /* this */,
        jstring script) {

    if (!Py_IsInitialized()) {
        return env->NewStringUTF("Python is not initialized.");
    }

    PyGILState_STATE gstate = PyGILState_Ensure();

    const char* scriptStr = env->GetStringUTFChars(script, 0);
    int result = PyRun_SimpleString(scriptStr);
    env->ReleaseStringUTFChars(script, scriptStr);

    jstring errorMessage = NULL;

    if (result != 0) {
        LOGD("Python script failed! Using traceback module to get full error...");

        if (PyErr_Occurred()) {
            // Сохраняем ошибку, так как следующие вызовы могут ее перезаписать
            PyObject *pType, *pValue, *pTraceback;
            PyErr_Fetch(&pType, &pValue, &pTraceback);
            PyErr_NormalizeException(&pType, &pValue, &pTraceback);

            // Восстанавливаем ошибку, чтобы модуль traceback мог ее увидеть
            PyErr_Restore(pType, pValue, pTraceback);

            // --- ИСПОЛЬЗУЕМ МОДУЛЬ TRACEBACK ---
            PyObject* traceback_module = PyImport_ImportModule("traceback");
            if (traceback_module != NULL) {
                PyObject* format_exc_func = PyObject_GetAttrString(traceback_module, "format_exc");
                if (format_exc_func != NULL) {
                    PyObject* formatted_exception = PyObject_CallObject(format_exc_func, NULL);
                    if (formatted_exception != NULL) {
                        const char* err_str = PyUnicode_AsUTF8(formatted_exception);
                        if (err_str) {
                            errorMessage = env->NewStringUTF(err_str);
                        }
                        Py_DECREF(formatted_exception);
                    }
                    Py_DECREF(format_exc_func);
                }
                Py_DECREF(traceback_module);
            }
        }
    }

    if (errorMessage == NULL && result != 0) {
        // Если даже traceback не сработал, возвращаем старое сообщение
        errorMessage = env->NewStringUTF("Unknown Python error, and traceback module failed.");
    }

    PyGILState_Release(gstate);
    return errorMessage;
}

// В файле catroid.cpp

extern "C" JNIEXPORT jstring JNICALL
JNI_PYTHON_FUNCTION(nativeRunScript)(
        JNIEnv* env,
        jobject /* this */,
        jstring script) {

    if (!Py_IsInitialized()) {
        return env->NewStringUTF("Python is not initialized.");
    }

    PyGILState_STATE gstate = PyGILState_Ensure();

    g_worker_thread_id = PyThread_get_thread_ident();

    // --- НАЧАЛО: ПОЛНАЯ СИСТЕМА ПЕРЕХВАТА ВЫВОДА ---

    // 1. Получаем глобальное пространство имен (__main__.__dict__)
    PyObject* main_module = PyImport_AddModule("__main__");
    PyObject* main_dict = PyModule_GetDict(main_module);

    // 2. Создаем "обертку" для выполнения кода с перехватом вывода.
    //    Этот Python-скрипт определяет функцию, которая делает всю грязную работу.
    const char* capture_script =
            "import sys, io, traceback\n"
            "def __run_and_capture(code_to_run):\n"
            "    buffer = io.StringIO()\n"
            "    sys.stdout = buffer\n"
            "    sys.stderr = buffer\n"
            "    try:\n"
            "        exec(code_to_run, globals())\n"
            "    except SystemExit:\n"
            "        print('\\nScipt stopped')\n"
            "    except Exception:\n"
            "        traceback.print_exc()\n"
            "    sys.stdout = sys.__stdout__\n"
            "    sys.stderr = sys.__stderr__\n"
            "    return buffer.getvalue()\n";

    // 3. Выполняем этот скрипт, чтобы функция __run_and_capture появилась в __main__
    PyRun_String(capture_script, Py_file_input, main_dict, main_dict);

    // 4. Получаем саму эту функцию
    PyObject* capture_func = PyDict_GetItemString(main_dict, "__run_and_capture");

    jstring result_string = NULL;

    if (capture_func && PyCallable_Check(capture_func)) {
        // 5. Готовим аргументы: сам скрипт, который передал пользователь
        PyObject* pArgs = PyTuple_New(1);
        const char* script_cstr = env->GetStringUTFChars(script, 0);
        PyObject* pScript = PyUnicode_FromString(script_cstr);
        PyTuple_SetItem(pArgs, 0, pScript);
        env->ReleaseStringUTFChars(script, script_cstr);

        // 6. Вызываем нашу функцию: __run_and_capture(script)
        PyObject* pResult = PyObject_CallObject(capture_func, pArgs);
        Py_DECREF(pArgs); // pScript будет очищен вместе с pArgs

        // 7. Конвертируем результат (весь захваченный вывод) в строку для Kotlin
        if (pResult != NULL) {
            const char* result_cstr = PyUnicode_AsUTF8(pResult);
            if (result_cstr) {
                result_string = env->NewStringUTF(result_cstr);
            }
            Py_DECREF(pResult);
        } else {
            // Если даже наша функция-обертка упала, сообщаем об этом
            PyErr_Print();
            result_string = env->NewStringUTF("FATAL: The C++ capture function itself failed.");
        }
    } else {
        result_string = env->NewStringUTF("FATAL: Could not find the __run_and_capture helper function.");
    }

    // --- КОНЕЦ СИСТЕМЫ ПЕРЕХВАТА ---
     g_worker_thread_id = 0;

    PyGILState_Release(gstate);
    return result_string;
}

#else // ---> Для всех остальных архитектур (x86_64 и т.д.)

#include <android/log.h>
#define JNI_PYTHON_FUNCTION(name) Java_org_catrobat_catroid_python_PythonEngine_##name

static std::atomic<unsigned long> g_worker_thread_id(0);

// Новая JNI-функция для вызова из Kotlin
extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeForceStopPythonScript)(JNIEnv* env, jobject /* this */) {
    LOGD("Python is not supported on this device architecture.");
}

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeInitPython)(JNIEnv*, jobject, jobjectArray) {
    // Ничего не делаем
    __android_log_print(ANDROID_LOG_WARN, "PythonEngine", "nativeInitPython called on unsupported architecture. Doing nothing.");
}

extern "C" JNIEXPORT jstring JNICALL
JNI_PYTHON_FUNCTION(nativeRunScript)(JNIEnv* env, jobject, jstring) {
    // Возвращаем сообщение об ошибке
    return env->NewStringUTF("Python is not supported on this device architecture.");
}

// ДОБАВЬТЕ ЭТУ ФУНКЦИЮ В catroid.cpp
extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeFinalizePython)(JNIEnv* env, jobject /* this */) {
    LOGD("Python is not supported on this device architecture.");
}

#endif

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

std::vector<uint32_t> triangulate(const std::vector<float>& vertices) {
    if (vertices.size() < 6) { // Невозможно триангулировать менее 3 вершин
        return {};
    }
    // Earcut требует специфичный формат данных: std::vector<std::vector<std::array<float, 2>>>
    std::vector<std::vector<std::array<float, 2>>> polygon_data;
    std::vector<std::array<float, 2>> ring;
    ring.reserve(vertices.size() / 2);
    for (size_t i = 0; i < vertices.size(); i += 2) {
        ring.push_back({vertices[i], vertices[i+1]});
    }
    polygon_data.push_back(ring);

    // Вызываем функцию триангуляции
    return mapbox::earcut<uint32_t>(polygon_data);
}

// Ваша функция проверки столкновения ВЫПУКЛЫХ полигонов.
// Она остается БЕЗ ИЗМЕНЕНИЙ, так как идеально подходит для проверки треугольников.
bool polygons_overlap(const float* vertsA, int countA, const float* vertsB, int countB) {
    // --- Проверка по осям полигона A ---
    for (int i = 0; i < countA; i += 2) {
        float p1x = vertsA[i];
        float p1y = vertsA[i + 1];
        float p2x = vertsA[(i + 2) % countA];
        float p2y = vertsA[(i + 3) % countA];
        float axisX = -(p2y - p1y);
        float axisY = p2x - p1x;

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
    return true;
}


// НОВАЯ, ИСПРАВЛЕННАЯ ВЕРСИЯ checkSingleCollision
extern "C" JNIEXPORT jboolean JNICALL
Java_org_catrobat_catroid_utils_NativeLookOptimizer_checkSingleCollision(
        JNIEnv* env,
        jclass /* this */,
        jobjectArray firstLookPolygons,
        jobjectArray secondLookPolygons
) {
    // 1. Извлекаем все полигоны в C++ векторы для удобства
    int firstPolygonCount = env->GetArrayLength(firstLookPolygons);
    std::vector<std::vector<float>> firstPolys(firstPolygonCount);
    for (int i = 0; i < firstPolygonCount; ++i) {
        auto poly_jfloatArray = (jfloatArray)env->GetObjectArrayElement(firstLookPolygons, i);
        jfloat* verts = env->GetFloatArrayElements(poly_jfloatArray, nullptr);
        int count = env->GetArrayLength(poly_jfloatArray);
        firstPolys[i].assign(verts, verts + count);
        env->ReleaseFloatArrayElements(poly_jfloatArray, verts, JNI_ABORT);
        env->DeleteLocalRef(poly_jfloatArray);
    }

    int secondPolygonCount = env->GetArrayLength(secondLookPolygons);
    std::vector<std::vector<float>> secondPolys(secondPolygonCount);
    for (int i = 0; i < secondPolygonCount; ++i) {
        auto poly_jfloatArray = (jfloatArray)env->GetObjectArrayElement(secondLookPolygons, i);
        jfloat* verts = env->GetFloatArrayElements(poly_jfloatArray, nullptr);
        int count = env->GetArrayLength(poly_jfloatArray);
        secondPolys[i].assign(verts, verts + count);
        env->ReleaseFloatArrayElements(poly_jfloatArray, verts, JNI_ABORT);
        env->DeleteLocalRef(poly_jfloatArray);
    }

    // 2. Основной цикл: триангулируем полигоны и проверяем треугольники
    for (const auto& polyA_verts : firstPolys) {
        std::vector<uint32_t> trianglesA_indices = triangulate(polyA_verts);

        for (const auto& polyB_verts : secondPolys) {
            std::vector<uint32_t> trianglesB_indices = triangulate(polyB_verts);

            // 3. Проверяем каждую пару треугольников
            for (size_t i = 0; i < trianglesA_indices.size(); i += 3) {
                float triangleA[6] = {
                        polyA_verts[trianglesA_indices[i] * 2], polyA_verts[trianglesA_indices[i] * 2 + 1],
                        polyA_verts[trianglesA_indices[i+1] * 2], polyA_verts[trianglesA_indices[i+1] * 2 + 1],
                        polyA_verts[trianglesA_indices[i+2] * 2], polyA_verts[trianglesA_indices[i+2] * 2 + 1]
                };

                for (size_t j = 0; j < trianglesB_indices.size(); j += 3) {
                    float triangleB[6] = {
                            polyB_verts[trianglesB_indices[j] * 2], polyB_verts[trianglesB_indices[j] * 2 + 1],
                            polyB_verts[trianglesB_indices[j+1] * 2], polyB_verts[trianglesB_indices[j+1] * 2 + 1],
                            polyB_verts[trianglesB_indices[j+2] * 2], polyB_verts[trianglesB_indices[j+2] * 2 + 1]
                    };

                    // Вызываем вашу быструю функцию SAT
                    if (polygons_overlap(triangleA, 6, triangleB, 6)) {
                        return JNI_TRUE; // Столкновение найдено!
                    }
                }
            }
        }
    }

    return JNI_FALSE; // Столкновений не найдено
}


// НОВАЯ, ИСПРАВЛЕННАЯ ВЕРСИЯ checkAllCollisions
extern "C" JNIEXPORT jintArray JNICALL
Java_org_catrobat_catroid_utils_NativeLookOptimizer_checkAllCollisions(
        JNIEnv* env,
        jclass /* this */,
        jobjectArray allSpritesPolygons
) {
    int spriteCount = env->GetArrayLength(allSpritesPolygons);
    if (spriteCount < 2) {
        return env->NewIntArray(0);
    }

    // 1. Извлекаем все данные из Java и считаем AABB
    std::vector<std::vector<std::vector<float>>> spritesData(spriteCount);
    std::vector<AABB> spriteAABBs(spriteCount);

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

    // 2. Основной цикл проверки столкновений
    std::vector<int> collidingPairs;
    for (int i = 0; i < spriteCount; ++i) {
        for (int j = i + 1; j < spriteCount; ++j) {
            // Broad phase: быстрая проверка по AABB
            if (aabbs_overlap(spriteAABBs[i], spriteAABBs[j])) {
                // Narrow phase: детальная проверка с триангуляцией
                bool collisionFound = false;
                for (const auto& polyA : spritesData[i]) {
                    std::vector<uint32_t> trianglesA = triangulate(polyA);

                    for (const auto& polyB : spritesData[j]) {
                        std::vector<uint32_t> trianglesB = triangulate(polyB);

                        for (size_t ti_a = 0; ti_a < trianglesA.size(); ti_a += 3) {
                            float triangleA_verts[6] = {
                                    polyA[trianglesA[ti_a] * 2], polyA[trianglesA[ti_a] * 2 + 1],
                                    polyA[trianglesA[ti_a+1] * 2], polyA[trianglesA[ti_a+1] * 2 + 1],
                                    polyA[trianglesA[ti_a+2] * 2], polyA[trianglesA[ti_a+2] * 2 + 1]
                            };

                            for (size_t ti_b = 0; ti_b < trianglesB.size(); ti_b += 3) {
                                float triangleB_verts[6] = {
                                        polyB[trianglesB[ti_b] * 2], polyB[trianglesB[ti_b] * 2 + 1],
                                        polyB[trianglesB[ti_b+1] * 2], polyB[trianglesB[ti_b+1] * 2 + 1],
                                        polyB[trianglesB[ti_b+2] * 2], polyB[trianglesB[ti_b+2] * 2 + 1]
                                };

                                if (polygons_overlap(triangleA_verts, 6, triangleB_verts, 6)) {
                                    collidingPairs.push_back(i);
                                    collidingPairs.push_back(j);
                                    collisionFound = true;
                                    break;
                                }
                            }
                            if (collisionFound) break;
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