cmake_minimum_required(VERSION 3.22.1)
project("catroid")

include_directories(src/main/cpp/onnx)

add_library(onnxruntime SHARED IMPORTED)

set_target_properties(onnxruntime PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libonnxruntime.so)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    message(STATUS "Python Support: ENABLED for ${ANDROID_ABI}")
    include_directories(src/main/cpp/include)
    add_library(libpython SHARED IMPORTED)
    set_target_properties(libpython PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libpython3.12.so)
    set(PYTHON_LINK_LIBS libpython)
else()
    message(STATUS "Python Support: DISABLED for ${ANDROID_ABI}")
    set(PYTHON_LINK_LIBS "")
endif()

add_library(${CMAKE_PROJECT_NAME} SHARED catroid.cpp)

find_library(log-lib log)
find_library(android-lib android)
find_library(dl-lib dl)
find_library(egl-lib EGL)
find_library(glesv2-lib GLESv2)

add_library(
        example_core
        SHARED
        example_core.cpp
)

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        onnxruntime
        ${PYTHON_LINK_LIBS}
        ${log-lib}
        ${android-lib}
        ${dl-lib}
        unwind
)

target_link_libraries(
        example_core
        log
        android
        EGL
        GLESv2
)