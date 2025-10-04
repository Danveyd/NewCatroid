# В файле Catroid/vncclient/src/main/cpp/CMakeLists.txt

cmake_minimum_required(VERSION 3.10)
project(VncClientBridge C CXX)

# --- НАЧАЛО ИСПРАВЛЕНИЯ ---
# Отключаем команды install и export для всех дочерних проектов.
# Это самый надежный способ избежать ошибок сборки.
function(install)
endfunction()
function(export)
endfunction()
# --- КОНЕЦ ИСПРАВЛЕНИЯ ---


# --- ШАГ 1: Собираем WOLFSSL ---
# Устанавливаем опции, чтобы отключить сборку тестов и примеров.
file(GLOB_RECURSE WOLFSSL_SOURCES
    "wolfssl/src/*.c"
    "wolfssl/wolfcrypt/src/*.c"
)

# 2. Создаем статическую библиотеку
add_library(wolfssl STATIC ${WOLFSSL_SOURCES})

# 3. Добавляем пути к заголовочным файлам
target_include_directories(wolfssl PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/wolfssl
)

# 4. Устанавливаем необходимые флаги компиляции
target_compile_definitions(wolfssl PRIVATE
    OPENSSL_ALL
    # HAVE_CRL           <-- УДАЛИТЕ ИЛИ ЗАКОММЕНТИРУЙТЕ ЭТУ СТРОКУ
    HAVE_TLS_EXTENSIONS
    HAVE_AESGCM
    WOLFSSL_DES_ECB
    WC_NO_HARDEN
)


# --- ШАГ 2: Собираем LIBJPEG-TURBO ---
# Отключаем сборку всего, кроме статической библиотеки
set(ENABLE_SHARED OFF CACHE BOOL "Build shared libraries" FORCE)
set(ENABLE_STATIC ON CACHE BOOL "Build static libraries" FORCE)
set(WITH_JPEG8 ON CACHE BOOL "Enable JPEG 8-bit support" FORCE)
# Теперь запускаем сборку
add_subdirectory(libjpeg-turbo)


# --- ШАГ 3: Собираем LIBVNCSERVER ---
include_directories(
    libvncserver/include
    ${CMAKE_CURRENT_BINARY_DIR}/libvncserver/include
)
add_subdirectory(libvncserver)


# --- ШАГ 4: Собираем наш JNI-мост (native-vnc.cpp) ---
add_library(native-vnc SHARED
    native-vnc.cpp
)


# --- ШАГ 5: Финальная линковка ---
target_link_libraries(native-vnc
    vncclient
    turbojpeg-static
    wolfssl
    log
    GLESv2
)