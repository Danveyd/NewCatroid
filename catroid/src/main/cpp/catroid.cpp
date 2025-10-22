#include <jni.h>
#include <map>
#include <string>
#include <mutex>
#include <vector>
#include <numeric>
#include <cmath>
#include <algorithm>
#include "earcut.hpp"
#include <android/native_window_jni.h>
#include <dlfcn.h>
#include <fstream>
#include <unwind.h>
#include <dlfcn.h>
#include <sstream>
#include <signal.h>
#include <cstring>
#include <spawn.h>
#include <unistd.h>
#include <sys/wait.h>
#include <thread>

#include "onnxruntime_cxx_api.h"

#include <android/log.h>

#include "newcatroid_gl_api.h"

extern char **environ;

Ort::Env env;
Ort::Env ort_env;
Ort::Session session{nullptr};
Ort::AllocatorWithDefaultOptions allocator;

#define LOG_TAG "PythonBridge"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static std::string g_crashLogPath;
static JavaVM* g_JavaVM = nullptr;

std::map<std::string, pid_t> g_RunningVMs;
std::mutex g_VmMutex;

struct BacktraceState {
    _Unwind_Ptr* frames;
    size_t frame_count;
    size_t max_frames;
};

static _Unwind_Reason_Code unwind_callback(struct _Unwind_Context* context, void* arg) {
    BacktraceState* state = static_cast<BacktraceState*>(arg);
    _Unwind_Ptr ip = _Unwind_GetIP(context);
    if (ip) {
        if (state->frame_count < state->max_frames) {
            state->frames[state->frame_count++] = ip;
        } else {
            return _URC_END_OF_STACK;
        }
    }
    return _URC_NO_REASON;
}

void capture_backtrace(std::ostream& out) {
    const size_t max_frames = 50;
    _Unwind_Ptr frames[max_frames];
    BacktraceState state = {frames, 0, max_frames};

    _Unwind_Backtrace(unwind_callback, &state);

    for (size_t i = 0; i < state.frame_count; ++i) {
        _Unwind_Ptr addr = frames[i];
        const char* symbol = "";
        Dl_info info;
        if (dladdr((void*)addr, &info) && info.dli_sname) {
            symbol = info.dli_sname;
        }
        out << "  #" << i << ": " << (void*)addr << " (" << symbol << ")\n";
    }
}

void signal_handler(int signal_num, siginfo_t *info, void *context) {
    if (!g_crashLogPath.empty()) {
        std::ofstream log_file(g_crashLogPath, std::ios::app);
        if (log_file.is_open()) {
            log_file << "\n\n===== NATIVE CRASH DETECTED =====\n";
            log_file << "Signal: " << signal_num << " (" << strsignal(signal_num) << ")\n";
            log_file << "Fault Address: " << info->si_addr << "\n";
            log_file << "Stack Trace:\n";
            capture_backtrace(log_file);
            log_file << "===================================\n";
            log_file.close();
        }
    }
    signal(signal_num, SIG_DFL);
    raise(signal_num);
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_JavaVM = vm;

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sigemptyset(&sa.sa_mask);
    sa.sa_sigaction = signal_handler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;

    sigaction(SIGSEGV, &sa, nullptr);
    sigaction(SIGABRT, &sa, nullptr);

    return JNI_VERSION_1_6;
}

#ifdef __aarch64__
#include "include/python3.12/Python.h"

#define JNI_PYTHON_FUNCTION(name) Java_org_catrobat_catroid_python_PythonEngine_##name
static std::atomic<PyThreadState*> g_worker_thread_state(nullptr);
static std::atomic<unsigned long> g_worker_thread_id(0);

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeForceStopPythonScript)(JNIEnv* env, jobject /* this */) {
    if (!Py_IsInitialized()) {
        LOGD("Python is not initialized or is finalizing. Skipping force stop.");
        return;
    }

    unsigned long thread_id = g_worker_thread_id.load();
    if (thread_id != 0) {
        LOGD("Attempting to inject SystemExit exception into thread ID: %lu", thread_id);

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
        jobjectArray modulePaths) {

    PyConfig config;
    PyConfig_InitPythonConfig(&config);
    config.install_signal_handlers = 0;
    config.module_search_paths_set = 1;

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

    PyStatus status = Py_InitializeFromConfig(&config);
    if (PyStatus_Exception(status)) {
        LOGD("FATAL: Py_InitializeFromConfig failed.");
        Py_ExitStatusException(status);
    } else {
        LOGD("!!! SUCCESS: Python has been initialized correctly !!!");
    }

    PyConfig_Clear(&config);
}

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

        config.module_search_paths_set = 1;

        std::wstring path_stdlib(pyHomeW);
        std::wstring path_pylibs(pyLibsPathW);
        std::wstring path_dynload = std::wstring(pyHomeW) + L"/lib-dynload";

        status = PyWideStringList_Append(&config.module_search_paths, path_stdlib.c_str());
        if (!PyStatus_Exception(status)) {
            status = PyWideStringList_Append(&config.module_search_paths, path_pylibs.c_str());
        }
        if (!PyStatus_Exception(status)) {
            status = PyWideStringList_Append(&config.module_search_paths, path_dynload.c_str());
        }

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
            PyObject *pType, *pValue, *pTraceback;
            PyErr_Fetch(&pType, &pValue, &pTraceback);
            PyErr_NormalizeException(&pType, &pValue, &pTraceback);

            PyErr_Restore(pType, pValue, pTraceback);

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
        errorMessage = env->NewStringUTF("Unknown Python error, and traceback module failed.");
    }

    PyGILState_Release(gstate);
    return errorMessage;
}

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

    PyObject* main_module = PyImport_AddModule("__main__");
    PyObject* main_dict = PyModule_GetDict(main_module);

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

    PyRun_String(capture_script, Py_file_input, main_dict, main_dict);

    PyObject* capture_func = PyDict_GetItemString(main_dict, "__run_and_capture");

    jstring result_string = NULL;

    if (capture_func && PyCallable_Check(capture_func)) {
        PyObject* pArgs = PyTuple_New(1);
        const char* script_cstr = env->GetStringUTFChars(script, 0);
        PyObject* pScript = PyUnicode_FromString(script_cstr);
        PyTuple_SetItem(pArgs, 0, pScript);
        env->ReleaseStringUTFChars(script, script_cstr);

        PyObject* pResult = PyObject_CallObject(capture_func, pArgs);
        Py_DECREF(pArgs);

        if (pResult != NULL) {
            const char* result_cstr = PyUnicode_AsUTF8(pResult);
            if (result_cstr) {
                result_string = env->NewStringUTF(result_cstr);
            }
            Py_DECREF(pResult);
        } else {
            PyErr_Print();
            result_string = env->NewStringUTF("FATAL: The C++ capture function itself failed.");
        }
    } else {
        result_string = env->NewStringUTF("FATAL: Could not find the __run_and_capture helper function.");
    }
     g_worker_thread_id = 0;

    PyGILState_Release(gstate);
    return result_string;
}

#else

#include <android/log.h>
#define JNI_PYTHON_FUNCTION(name) Java_org_catrobat_catroid_python_PythonEngine_##name

static std::atomic<unsigned long> g_worker_thread_id(0);

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeForceStopPythonScript)(JNIEnv* env, jobject /* this */) {
    LOGD("Python is not supported on this device architecture.");
}

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeInitPython)(JNIEnv*, jobject, jobjectArray) {
    __android_log_print(ANDROID_LOG_WARN, "PythonEngine", "nativeInitPython called on unsupported architecture. Doing nothing.");
}

extern "C" JNIEXPORT jstring JNICALL
JNI_PYTHON_FUNCTION(nativeRunScript)(JNIEnv* env, jobject, jstring) {
    return env->NewStringUTF("Python is not supported on this device architecture.");
}

extern "C" JNIEXPORT void JNICALL
JNI_PYTHON_FUNCTION(nativeFinalizePython)(JNIEnv* env, jobject /* this */) {
    LOGD("Python is not supported on this device architecture.");
}

#endif

#define JNI_FUNCTION(name) Java_org_catrobat_catroid_NN_OnnxSessionManager_##name

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

extern "C" JNIEXPORT jfloatArray JNICALL
JNI_FUNCTION(runInferenceJNI)(JNIEnv* env, jobject /* this */, jfloatArray inputData) {
    if (!session) { return nullptr; }

    if (session.GetInputCount() == 0) { return nullptr; }
    Ort::AllocatedStringPtr input_name_ptr = session.GetInputNameAllocated(0, allocator);
    const char* input_name_chars[] = { input_name_ptr.get() };


    Ort::TypeInfo input_type_info = session.GetInputTypeInfo(0);
    auto tensor_info = input_type_info.GetTensorTypeAndShapeInfo();
    std::vector<int64_t> input_shape = tensor_info.GetShape();

    for (int64_t &dim : input_shape) {
        if (dim < 1) {
            dim = 1;
        }
    }

    jsize userInputSize = env->GetArrayLength(inputData);
    size_t expectedInputSize = std::accumulate(input_shape.begin(), input_shape.end(), 1LL, std::multiplies<int64_t>());

    if (userInputSize != expectedInputSize) {
        return nullptr;
    }

    jfloat* input_floats = env->GetFloatArrayElements(inputData, nullptr);
    std::vector<float> input_vec(input_floats, input_floats + userInputSize);

    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
            memory_info, input_vec.data(), input_vec.size(), input_shape.data(), input_shape.size());

    if (session.GetOutputCount() == 0) { return nullptr; }
    Ort::AllocatedStringPtr output_name_ptr = session.GetOutputNameAllocated(0, allocator);
    const char* output_name_chars[] = { output_name_ptr.get() };


    auto output_tensors = session.Run(Ort::RunOptions{nullptr}, input_name_chars, &input_tensor, 1, output_name_chars, 1);

    const auto& output_tensor = output_tensors[0];
    auto output_shape_info = output_tensor.GetTensorTypeAndShapeInfo();
    size_t output_size = output_shape_info.GetElementCount();
    const float* output_data = output_tensor.GetTensorData<float>();

    jfloatArray resultArray = env->NewFloatArray(output_size);
    env->SetFloatArrayRegion(resultArray, 0, output_size, output_data);

    env->ReleaseFloatArrayElements(inputData, input_floats, 0);
    return resultArray;
}

extern "C" JNIEXPORT void JNICALL
JNI_FUNCTION(unloadModelJNI)(JNIEnv* env, jobject /* this */) {
    session = Ort::Session(nullptr);
}

struct Transform {
    float x, y;
    float scaleX, scaleY;
    float rotation;
    float originX, originY;
};

std::vector<float> transform_vertices(const std::vector<float>& vertices, const Transform& transform) {
    std::vector<float> transformed_vertices;
    transformed_vertices.reserve(vertices.size());

    float rotation_rad = transform.rotation * (M_PI / 180.0f);
    float cos_r = cos(rotation_rad);
    float sin_r = sin(rotation_rad);

    for (size_t i = 0; i < vertices.size(); i += 2) {
        float vx = vertices[i] - transform.originX;
        float vy = vertices[i+1] - transform.originY;

        vx *= transform.scaleX;
        vy *= transform.scaleY;

        float rotated_x = vx * cos_r - vy * sin_r;
        float rotated_y = vx * sin_r + vy * cos_r;

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

    jsize len = env->GetArrayLength(jvertices);
    jfloat* vertex_elements = env->GetFloatArrayElements(jvertices, nullptr);
    std::vector<float> vertices_vec(vertex_elements, vertex_elements + len);
    env->ReleaseFloatArrayElements(jvertices, vertex_elements, JNI_ABORT); // JNI_ABORT, т.к. мы не меняли исходные данные

    Transform t = {x, y, scaleX, scaleY, rotation, originX, originY};

    std::vector<float> transformed_vertices = transform_vertices(vertices_vec, t);

    jfloatArray result_array = env->NewFloatArray(transformed_vertices.size());
    env->SetFloatArrayRegion(result_array, 0, transformed_vertices.size(), transformed_vertices.data());

    return result_array;
}
std::vector<float> calculate_aabb(const Transform& transform, float width, float height) {
    float local_coords[8] = {
            0, 0,
            width, 0,
            width, height,
            0, height
    };

    float rotation_rad = transform.rotation * (M_PI / 180.0f);
    float cos_r = cos(rotation_rad);
    float sin_r = sin(rotation_rad);

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

    float min_x = *std::min_element(transformed_x.begin(), transformed_x.end());
    float max_x = *std::max_element(transformed_x.begin(), transformed_x.end());
    float min_y = *std::min_element(transformed_y.begin(), transformed_y.end());
    float max_y = *std::max_element(transformed_y.begin(), transformed_y.end());

    return {min_x, min_y, max_x - min_x, max_y - min_y};
}

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
    std::vector<float> aabb = calculate_aabb(t, width / scaleX, height / scaleY);

    jfloatArray result_array = env->NewFloatArray(4);
    env->SetFloatArrayRegion(result_array, 0, 4, aabb.data());
    return result_array;
}

struct AABB {
    double minX, minY, maxX, maxY;
};

bool aabbs_overlap(const AABB& a, const AABB& b) {
    if (a.maxX < b.minX || b.maxX < a.minX) return false;
    if (a.maxY < b.minY || b.maxY < a.minY) return false;
    return true;
}

std::vector<uint32_t> triangulate(const std::vector<double>& vertices) {
    if (vertices.size() < 6) {
        return {};
    }
    std::vector<std::vector<std::array<double, 2>>> polygon_data;
    std::vector<std::array<double, 2>> ring;
    ring.reserve(vertices.size() / 2);
    for (size_t i = 0; i < vertices.size(); i += 2) {
        ring.push_back({vertices[i], vertices[i+1]});
    }
    polygon_data.push_back(ring);

    return mapbox::earcut<uint32_t>(polygon_data);
}

bool polygons_overlap(const double* vertsA, int countA, const double* vertsB, int countB) {
    for (int i = 0; i < countA; i += 2) {
        double p1x = vertsA[i];
        double p1y = vertsA[i + 1];
        double p2x = vertsA[(i + 2) % countA];
        double p2y = vertsA[(i + 3) % countA];
        double axisX = -(p2y - p1y);
        double axisY = p2x - p1x;

        double len = std::sqrt(axisX * axisX + axisY * axisY);
        if (len > 1e-8) {
            axisX /= len;
            axisY /= len;
        }

        double minA = 1e18, maxA = -1e18, minB = 1e18, maxB = -1e18;
        for (int j = 0; j < countA; j += 2) {
            double dot = vertsA[j] * axisX + vertsA[j + 1] * axisY;
            minA = std::min(minA, dot);
            maxA = std::max(maxA, dot);
        }
        for (int j = 0; j < countB; j += 2) {
            double dot = vertsB[j] * axisX + vertsB[j + 1] * axisY;
            minB = std::min(minB, dot);
            maxB = std::max(maxB, dot);
        }
        if (maxA < minB || maxB < minA) return false;
    }

    for (int i = 0; i < countB; i += 2) {
        double p1x = vertsB[i];
        double p1y = vertsB[i + 1];
        double p2x = vertsB[(i + 2) % countB];
        double p2y = vertsB[(i + 3) % countB];
        double axisX = -(p2y - p1y);
        double axisY = p2x - p1x;

        double len = std::sqrt(axisX * axisX + axisY * axisY);
        if (len > 1e-8) {
            axisX /= len;
            axisY /= len;
        }

        double minA = 1e18, maxA = -1e18, minB = 1e18, maxB = -1e18;
        for (int j = 0; j < countA; j += 2) {
            double dot = vertsA[j] * axisX + vertsA[j+1] * axisY;
            minA = std::min(minA, dot);
            maxA = std::max(maxA, dot);
        }
        for (int j = 0; j < countB; j += 2) {
            double dot = vertsB[j] * axisX + vertsB[j+1] * axisY;
            minB = std::min(minB, dot);
            maxB = std::max(maxB, dot);
        }
        if (maxA < minB || maxB < minA) return false;
    }
    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_catrobat_catroid_utils_NativeLookOptimizer_checkSingleCollision(
        JNIEnv* env,
        jclass /* this */,
        jobjectArray firstLookPolygons,
        jobjectArray secondLookPolygons
) {
    int firstPolygonCount = env->GetArrayLength(firstLookPolygons);
    std::vector<std::vector<double>> firstPolys(firstPolygonCount);
    for (int i = 0; i < firstPolygonCount; ++i) {
        auto poly_jfloatArray = (jfloatArray)env->GetObjectArrayElement(firstLookPolygons, i);
        jfloat* verts_float = env->GetFloatArrayElements(poly_jfloatArray, nullptr);
        int count = env->GetArrayLength(poly_jfloatArray);
        firstPolys[i].assign(verts_float, verts_float + count);
        env->ReleaseFloatArrayElements(poly_jfloatArray, verts_float, JNI_ABORT);
        env->DeleteLocalRef(poly_jfloatArray);
    }

    int secondPolygonCount = env->GetArrayLength(secondLookPolygons);
    std::vector<std::vector<double>> secondPolys(secondPolygonCount);
    for (int i = 0; i < secondPolygonCount; ++i) {
        auto poly_jfloatArray = (jfloatArray)env->GetObjectArrayElement(secondLookPolygons, i);
        jfloat* verts_float = env->GetFloatArrayElements(poly_jfloatArray, nullptr);
        int count = env->GetArrayLength(poly_jfloatArray);
        secondPolys[i].assign(verts_float, verts_float + count);
        env->ReleaseFloatArrayElements(poly_jfloatArray, verts_float, JNI_ABORT);
        env->DeleteLocalRef(poly_jfloatArray);
    }

    for (const auto& polyA_verts : firstPolys) {
        std::vector<uint32_t> trianglesA_indices = triangulate(polyA_verts);

        for (const auto& polyB_verts : secondPolys) {
            std::vector<uint32_t> trianglesB_indices = triangulate(polyB_verts);

            for (size_t i = 0; i < trianglesA_indices.size(); i += 3) {
                double triangleA[6] = {
                        polyA_verts[trianglesA_indices[i] * 2], polyA_verts[trianglesA_indices[i] * 2 + 1],
                        polyA_verts[trianglesA_indices[i+1] * 2], polyA_verts[trianglesA_indices[i+1] * 2 + 1],
                        polyA_verts[trianglesA_indices[i+2] * 2], polyA_verts[trianglesA_indices[i+2] * 2 + 1]
                };

                for (size_t j = 0; j < trianglesB_indices.size(); j += 3) {
                    double triangleB[6] = {
                            polyB_verts[trianglesB_indices[j] * 2], polyB_verts[trianglesB_indices[j] * 2 + 1],
                            polyB_verts[trianglesB_indices[j+1] * 2], polyB_verts[trianglesB_indices[j+1] * 2 + 1],
                            polyB_verts[trianglesB_indices[j+2] * 2], polyB_verts[trianglesB_indices[j+2] * 2 + 1]
                    };

                    if (polygons_overlap(triangleA, 6, triangleB, 6)) {
                        return JNI_TRUE;
                    }
                }
            }
        }
    }

    return JNI_FALSE;
}


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

    std::vector<std::vector<std::vector<double>>> spritesData(spriteCount);
    std::vector<AABB> spriteAABBs(spriteCount);

    for (int i = 0; i < spriteCount; ++i) {
        auto polygonsArray = (jobjectArray)env->GetObjectArrayElement(allSpritesPolygons, i);
        int polygonCount = env->GetArrayLength(polygonsArray);
        spritesData[i].resize(polygonCount);

        double spriteMinX = 1e18, spriteMinY = 1e18, spriteMaxX = -1e18, spriteMaxY = -1e18;

        for (int j = 0; j < polygonCount; ++j) {
            auto verticesArray = (jfloatArray)env->GetObjectArrayElement(polygonsArray, j);
            jfloat* vertices_float = env->GetFloatArrayElements(verticesArray, nullptr);
            int vertexCount = env->GetArrayLength(verticesArray);

            spritesData[i][j].assign(vertices_float, vertices_float + vertexCount);

            for (int k = 0; k < vertexCount; k += 2) {
                spriteMinX = std::min(spriteMinX, (double)vertices_float[k]);
                spriteMinY = std::min(spriteMinY, (double)vertices_float[k+1]);
                spriteMaxX = std::max(spriteMaxX, (double)vertices_float[k]);
                spriteMaxY = std::max(spriteMaxY, (double)vertices_float[k+1]);
            }
            env->ReleaseFloatArrayElements(verticesArray, vertices_float, JNI_ABORT);
            env->DeleteLocalRef(verticesArray);
        }
        spriteAABBs[i] = {spriteMinX, spriteMinY, spriteMaxX, spriteMaxY};
        env->DeleteLocalRef(polygonsArray);
    }

    std::vector<int> collidingPairs;
    for (int i = 0; i < spriteCount; ++i) {
        for (int j = i + 1; j < spriteCount; ++j) {
            if (aabbs_overlap(spriteAABBs[i], spriteAABBs[j])) {
                bool collisionFound = false;
                for (const auto& polyA : spritesData[i]) {
                    std::vector<uint32_t> trianglesA = triangulate(polyA);

                    for (const auto& polyB : spritesData[j]) {
                        std::vector<uint32_t> trianglesB = triangulate(polyB);

                        for (size_t ti_a = 0; ti_a < trianglesA.size(); ti_a += 3) {
                            double triangleA_verts[6] = {
                                    polyA[trianglesA[ti_a] * 2], polyA[trianglesA[ti_a] * 2 + 1],
                                    polyA[trianglesA[ti_a+1] * 2], polyA[trianglesA[ti_a+1] * 2 + 1],
                                    polyA[trianglesA[ti_a+2] * 2], polyA[trianglesA[ti_a+2] * 2 + 1]
                            };

                            for (size_t ti_b = 0; ti_b < trianglesB.size(); ti_b += 3) {
                                double triangleB_verts[6] = {
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

    jintArray resultArray = env->NewIntArray(collidingPairs.size());
    if (!collidingPairs.empty()) {
        env->SetIntArrayRegion(resultArray, 0, collidingPairs.size(), collidingPairs.data());
    }
    return resultArray;
}

struct CoreAPI {
    void (*initialize)(ResolvePathCallback);
    void (*on_surface_created)(const char*, ANativeWindow*);
    void (*on_surface_changed)(const char*, int, int);
    void (*on_surface_destroyed)(const char*);
    void (*on_touch_event)(const char*, int, float, float, int);
    void (*shutdown)();
};

struct GLInstance {
    void*               so_handle;
    CoreAPI             api;
    ANativeWindow*      window;
    std::string         view_name;
};

std::map<std::string, GLInstance> g_GlInstances;
std::mutex g_Mutex;

#define JNI_GL_FUNCTION(name) Java_org_catrobat_catroid_utils_NativeBridge_##name

extern "C" {

const char* resolve_project_file_path(const char* fileName) {
    static std::string result_path;

    JNIEnv* env = nullptr;
    if (g_JavaVM->AttachCurrentThreadAsDaemon(&env, nullptr) != JNI_OK) {
        return nullptr;
    }
    if (!env) return nullptr;

    jclass bridgeClass = env->FindClass("org/catrobat/catroid/utils/NativeBridge");
    if (!bridgeClass) return nullptr;

    jmethodID methodId = env->GetStaticMethodID(bridgeClass, "getProjectFilePath", "(Ljava/lang/String;)Ljava/lang/String;");
    if (!methodId) return nullptr;

    jstring jFileName = env->NewStringUTF(fileName);
    auto jPath = (jstring)env->CallStaticObjectMethod(bridgeClass, methodId, jFileName);

    result_path.clear();
    if (jPath) {
        const char* path_chars = env->GetStringUTFChars(jPath, nullptr);
        result_path = path_chars;
        env->ReleaseStringUTFChars(jPath, path_chars);
        env->DeleteLocalRef(jPath);
    }

    env->DeleteLocalRef(jFileName);

    return result_path.empty() ? nullptr : result_path.c_str();
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(attachSoToView)(JNIEnv *env, jobject thiz, jstring view_name_j, jstring path_to_so_j) {
    const char* viewName = env->GetStringUTFChars(view_name_j, nullptr);
    const char* pathToSo = env->GetStringUTFChars(path_to_so_j, nullptr);

    std::lock_guard<std::mutex> lock(g_Mutex);

    void* handle = dlopen(pathToSo, RTLD_LAZY);
    if (!handle) {
        __android_log_print(ANDROID_LOG_ERROR, "NativeBridge", "Failed to load .so from '%s': %s", pathToSo, dlerror());
        env->ReleaseStringUTFChars(view_name_j, viewName);
        env->ReleaseStringUTFChars(path_to_so_j, pathToSo);
        return;
    }

    GLInstance instance;
    instance.so_handle = handle;
    instance.window = nullptr;
    instance.view_name = viewName;

    instance.api.initialize = (void (*)(ResolvePathCallback))dlsym(handle, "core_initialize");
    instance.api.on_surface_created = (void (*)(const char*, ANativeWindow*))dlsym(handle, "core_on_surface_created");
    instance.api.on_surface_changed = (void (*)(const char*, int, int))dlsym(handle, "core_on_surface_changed");
    instance.api.on_surface_destroyed = (void (*)(const char*))dlsym(handle, "core_on_surface_destroyed");
    instance.api.shutdown = (void (*)())dlsym(handle, "core_shutdown");
    instance.api.on_touch_event = (void (*)(const char*, int, float, float, int))dlsym(handle, "core_on_touch_event");

    if (instance.api.initialize) {
        try {
            instance.api.initialize(resolve_project_file_path);
        } catch (const std::exception& e) {
            std::ofstream log_file(g_crashLogPath, std::ios::app);
            log_file << "C++ Exception in initialize: " << e.what() << "\n";
        } catch (...) {
            std::ofstream log_file(g_crashLogPath, std::ios::app);
            log_file << "Unknown C++ Exception in initialize\n";
        }
    }

    g_GlInstances[viewName] = instance;
    __android_log_print(ANDROID_LOG_INFO, "NativeBridge", "Attached .so to view '%s'", viewName);

    env->ReleaseStringUTFChars(view_name_j, viewName);
    env->ReleaseStringUTFChars(path_to_so_j, pathToSo);
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(onSurfaceCreated)(JNIEnv *env, jobject thiz, jstring view_name_j, jobject surface) {
    const char* viewName = env->GetStringUTFChars(view_name_j, nullptr);
    std::lock_guard<std::mutex> lock(g_Mutex);

    if (g_GlInstances.count(viewName)) {
        GLInstance& instance = g_GlInstances[viewName];
        instance.window = ANativeWindow_fromSurface(env, surface);
        if (instance.api.on_surface_created) {
            try {
                instance.api.on_surface_created(viewName, instance.window);
            } catch (const std::exception& e) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "C++ Exception in on_surface_created: " << e.what() << "\n";
            } catch (...) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "Unknown C++ Exception in on_surface_created\n";
            }
        }
    }
    env->ReleaseStringUTFChars(view_name_j, viewName);
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(onSurfaceChanged)(JNIEnv *env, jobject thiz, jstring view_name_j, jint width, jint height) {
    const char* viewName = env->GetStringUTFChars(view_name_j, nullptr);
    std::lock_guard<std::mutex> lock(g_Mutex);
    if (g_GlInstances.count(viewName)) {
        GLInstance& instance = g_GlInstances[viewName];
        if (instance.api.on_surface_changed) {
            try {
                instance.api.on_surface_changed(viewName, width, height);
            } catch (const std::exception& e) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "C++ Exception in on_surface_changed: " << e.what() << "\n";
            } catch (...) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "Unknown C++ Exception in on_surface_changed\n";
            }
        }
    }
    env->ReleaseStringUTFChars(view_name_j, viewName);
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(onSurfaceDestroyed)(JNIEnv *env, jobject thiz, jstring view_name_j) {
    const char* viewName = env->GetStringUTFChars(view_name_j, nullptr);
    std::lock_guard<std::mutex> lock(g_Mutex);
    if (g_GlInstances.count(viewName)) {
        GLInstance& instance = g_GlInstances[viewName];
        if (instance.api.on_surface_destroyed) {
            try {
                instance.api.on_surface_destroyed(viewName);
            } catch (const std::exception& e) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "C++ Exception in on_surface_destroyed: " << e.what() << "\n";
            } catch (...) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "Unknown C++ Exception in on_surface_destroyed\n";
            }
        }
        if (instance.window) {
            ANativeWindow_release(instance.window);
            instance.window = nullptr;
        }
    }
    env->ReleaseStringUTFChars(view_name_j, viewName);
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(cleanupInstance)(JNIEnv *env, jobject thiz, jstring view_name_j) {
    const char* viewName = env->GetStringUTFChars(view_name_j, nullptr);
    std::lock_guard<std::mutex> lock(g_Mutex);

    auto it = g_GlInstances.find(viewName);
    if (it != g_GlInstances.end()) {
        GLInstance& instance = it->second;

        if (instance.window) {
            ANativeWindow_release(instance.window);
            instance.window = nullptr;
        }

        if (instance.api.shutdown) {
            try {
                instance.api.shutdown();
            } catch (const std::exception& e) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "C++ Exception in shutdown: " << e.what() << "\n";
            } catch (...) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "Unknown C++ Exception in shutdown\n";
            }
        }
        if (instance.so_handle) {
            dlclose(instance.so_handle);
        }
        g_GlInstances.erase(it);
        __android_log_print(ANDROID_LOG_INFO, "NativeBridge", "Cleaned up instance '%s'", viewName);
    }
    env->ReleaseStringUTFChars(view_name_j, viewName);
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(cleanupAllInstances)(JNIEnv *env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_Mutex);
    for (auto const& [name, instance] : g_GlInstances) {
        if (instance.window) {
            ANativeWindow_release(instance.window);
        }

        if (instance.api.shutdown) {
            try {
                instance.api.shutdown();
            } catch (const std::exception& e) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "C++ Exception in shutdown: " << e.what() << "\n";
            } catch (...) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "Unknown C++ Exception in shutdown\n";
            }
        }
        if (instance.so_handle) {
            dlclose(instance.so_handle);
        }
    }
    g_GlInstances.clear();
    __android_log_print(ANDROID_LOG_INFO, "NativeBridge", "Cleaned up ALL instances.");
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(onTouchEvent)(JNIEnv *env, jobject thiz, jstring view_name_j, jint action, jfloat x, jfloat y, jint pointerId) {
    const char* viewName = env->GetStringUTFChars(view_name_j, nullptr);
    std::lock_guard<std::mutex> lock(g_Mutex);
    if (g_GlInstances.count(viewName)) {
        GLInstance& instance = g_GlInstances[viewName];
        if (instance.api.on_touch_event) {
            try {
                instance.api.on_touch_event(viewName, action, x, y, pointerId);
            } catch (const std::exception& e) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "C++ Exception in on_touch_event: " << e.what() << "\n";
            } catch (...) {
                std::ofstream log_file(g_crashLogPath, std::ios::app);
                log_file << "Unknown C++ Exception in on_touch_event\n";
            }
        }
    }
    env->ReleaseStringUTFChars(view_name_j, viewName);
}

JNIEXPORT void JNICALL
JNI_GL_FUNCTION(setCrashLogPath)(JNIEnv* env, jobject thiz, jstring path_j) {
    const char* path_c = env->GetStringUTFChars(path_j, nullptr);
    if (path_c) {
        g_crashLogPath = path_c;
        __android_log_print(ANDROID_LOG_INFO, "NativeBridge", "Crash log path set to: %s", g_crashLogPath.c_str());
    }
    env->ReleaseStringUTFChars(path_j, path_c);
}
} // extern "C"

#define JNI_VM_FUNCTION(name) Java_org_catrobat_catroid_virtualmachine_VirtualMachineManager_##name

extern "C" {
void log_pipe_thread(int read_fd) {
    char buffer[256];
    ssize_t len;
    while ((len = read(read_fd, buffer, sizeof(buffer) - 1)) > 0) {
        buffer[len] = '\0';
        __android_log_print(ANDROID_LOG_INFO, "QEMU_LOG", "%s", buffer);
    }
    close(read_fd);
    __android_log_print(ANDROID_LOG_INFO, "QEMU_LOG", "Log pipe closed. QEMU process terminated.");
}


JNIEXPORT jint JNICALL
JNI_VM_FUNCTION(nativeCreateAndRunVM)(JNIEnv *env, jclass, jstring vmName_j, jobjectArray command_j, jstring dataPath_j) {
    const char* vmName = env->GetStringUTFChars(vmName_j, nullptr);
    const char* dataPath_c = env->GetStringUTFChars(dataPath_j, nullptr);

    const std::string libPath = std::string(dataPath_c) + "/lib";
    const std::string romPath = std::string(dataPath_c) + "/share/qemu";
    const char* linkerPath = "/system/bin/linker64";

    std::vector<std::string> commandVec;

    commandVec.push_back(linkerPath);

    int original_argc = env->GetArrayLength(command_j);

    jstring exe_j = (jstring) env->GetObjectArrayElement(command_j, 0);
    const char* exe_c = env->GetStringUTFChars(exe_j, nullptr);
    commandVec.push_back(exe_c);
    env->ReleaseStringUTFChars(exe_j, exe_c);
    env->DeleteLocalRef(exe_j);

    commandVec.push_back("-L");
    commandVec.push_back(romPath);

    for (int i = 1; i < original_argc; i++) {
        jstring string_j = (jstring) env->GetObjectArrayElement(command_j, i);
        const char* string_c = env->GetStringUTFChars(string_j, nullptr);
        commandVec.push_back(string_c);
        env->ReleaseStringUTFChars(string_j, string_c);
        env->DeleteLocalRef(string_j);
    }

    char** argv = new char*[commandVec.size() + 1];
    for (size_t i = 0; i < commandVec.size(); ++i) {
        argv[i] = strdup(commandVec[i].c_str());
    }
    argv[commandVec.size()] = NULL;

    int pipe_fds[2];
    if (pipe(pipe_fds) == -1) { return -1; }
    pid_t pid = fork();

    if (pid == -1) {
        return -1;
    } else if (pid == 0) {
        close(pipe_fds[0]);
        dup2(pipe_fds[1], STDOUT_FILENO);
        dup2(pipe_fds[1], STDERR_FILENO);
        close(pipe_fds[1]);

        setenv("LD_LIBRARY_PATH", libPath.c_str(), 1);

        execv(linkerPath, argv);
        _exit(127);
    } else {
        close(pipe_fds[1]);
        __android_log_print(ANDROID_LOG_INFO, "VMManager", "Successfully forked process for VM '%s' with PID %d", vmName, pid);
        std::lock_guard<std::mutex> lock(g_VmMutex);
        g_RunningVMs[vmName] = pid;
        std::thread log_thread(log_pipe_thread, pipe_fds[0]);
        log_thread.detach();

        for (size_t i = 0; i < commandVec.size(); ++i) {
            free(argv[i]);
        }
        delete[] argv;
        env->ReleaseStringUTFChars(vmName_j, vmName);
        env->ReleaseStringUTFChars(dataPath_j, dataPath_c);

        return pid;
    }
}

JNIEXPORT jint JNICALL
JNI_VM_FUNCTION(nativeStopVM)(JNIEnv *env, jclass, jstring vmName_j) {
    const char* vmName = env->GetStringUTFChars(vmName_j, nullptr);
    std::lock_guard<std::mutex> lock(g_VmMutex);

    auto it = g_RunningVMs.find(vmName);
    if (it != g_RunningVMs.end()) {
        pid_t pid = it->second;
        g_RunningVMs.erase(it);
        env->ReleaseStringUTFChars(vmName_j, vmName);

        int result = kill(pid, SIGTERM);
        if (result != 0) {
            __android_log_print(ANDROID_LOG_WARN, "VMManager", "Failed to send SIGTERM to PID %d. Trying SIGKILL.", pid);
            return kill(pid, SIGKILL);
        }
        return result;
    } else {
        __android_log_print(ANDROID_LOG_WARN, "VMManager", "VM '%s' not found in running processes map.", vmName);
        env->ReleaseStringUTFChars(vmName_j, vmName);
        return -1;
    }
}

} // extern "C"