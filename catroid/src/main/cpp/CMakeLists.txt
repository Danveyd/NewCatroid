
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("catroid")

include_directories(src/main/cpp/onnx)

add_library(onnxruntime SHARED IMPORTED)

set_target_properties(onnxruntime PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libonnxruntime.so)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    # Если это arm64-v8a, включаем полную поддержку Python
    message(STATUS "Python Support: ENABLED for ${ANDROID_ABI}")

    # Добавляем пути к заголовочным файлам Python
    include_directories(src/main/cpp/include)

    # Определяем библиотеку libpython
    add_library(libpython SHARED IMPORTED)
    set_target_properties(libpython PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libpython3.12.so) # Имя файла должно быть без версий

    # Создаем переменную, чтобы добавить libpython к списку линковки
    set(PYTHON_LINK_LIBS libpython)
else()
    # Для всех других архитектур (x86_64, x86, armeabi-v7a) просто выводим сообщение
    message(STATUS "Python Support: DISABLED for ${ANDROID_ABI}")

    # Оставляем переменную пустой
    set(PYTHON_LINK_LIBS "")
endif()

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED catroid.cpp)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
find_library(log-lib log)

# Линкуем нашу библиотеку с виртуальной библиотекой ONNX, которую мы создали.
target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        onnxruntime
        ${PYTHON_LINK_LIBS}
        ${log-lib}
)