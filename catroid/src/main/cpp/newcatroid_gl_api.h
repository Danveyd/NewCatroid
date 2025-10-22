#ifndef NEWCATROID_GL_API_H
#define NEWCATROID_GL_API_H

#include <android/native_window.h>

typedef const char* (*ResolvePathCallback)(const char* fileName);

#ifdef __cplusplus
extern "C" {
#endif


/**
 * Вызывается один раз при загрузке библиотеки (.so).
 * @param resolve_path_callback Указатель на функцию, которая может найти
 *                              полный путь к файлу проекта по его имени.
 */
void core_initialize(ResolvePathCallback resolve_path_callback);

/**
 * Вызывается, когда GLSurfaceView создал свою поверхность для рисования.
 * @param view_name Имя GLSurfaceView, которое задал пользователь.
 * @param window Указатель на нативное окно, в котором можно рисовать.
 *               Здесь нужно инициализировать EGL и OpenGL.
 */
void core_on_surface_created(const char* view_name, ANativeWindow* window);

/**
 * Вызывается при изменении размеров поверхности.
 * @param view_name Имя GLSurfaceView.
 * @param width Новая ширина в пикселях.
 * @param height Новая высота в пикселях.
 */
void core_on_surface_changed(const char* view_name, int width, int height);

/**
 * Вызывается, когда поверхность для рисования вот-вот будет уничтожена.
 * Здесь нужно освободить все ресурсы, связанные с EGL/OpenGL для этого окна.
 * @param view_name Имя GLSurfaceView.
 */
void core_on_surface_destroyed(const char* view_name);

/**
 * Вызывается перед выгрузкой библиотеки (.so).
 * Здесь нужно освободить все остальные ресурсы.
 */
void core_shutdown();

/**
 * Вызывается при событии касания на SurfaceView.
 * @param view_name Имя GLSurfaceView.
 * @param action Тип события (0=DOWN, 1=UP, 2=MOVE).
 * @param x Координата X касания.
 * @param y Координата Y касания.
 * @param pointerId ID пальца (для мультитача).
 */
void core_on_touch_event(const char* view_name, int action, float x, float y, int pointerId);

typedef const char* (*ResolvePathCallback)(const char* fileName);

#ifdef __cplusplus
}
#endif

#endif