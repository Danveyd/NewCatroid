#include "newcatroid_gl_api.h"
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <thread>
#include <atomic>
#include <vector>
#include <random>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

static ResolvePathCallback g_resolve_path = nullptr;

struct Particle {
    float x, y;
    float vx, vy;
    float life;
};

static ANativeWindow* native_window = nullptr;
static EGLDisplay display = EGL_NO_DISPLAY;
static EGLContext context = EGL_NO_CONTEXT;
static EGLSurface surface = EGL_NO_SURFACE;

static std::thread render_thread;
static std::atomic<bool> should_render(false);

static std::vector<Particle> particles;
static std::atomic<float> touch_x(-1.0f), touch_y(-1.0f);
static int screen_width = 1, screen_height = 1;

static GLuint particle_texture_id = 0;

const char* VERTEX_SHADER = R"glsl(
#version 300 es
// 'in' вместо 'attribute' в GLES 3
in vec2 a_position;
void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    // Размер частиц теперь больше, чтобы текстура была видна
    gl_PointSize = 40.0;
}
)glsl";

const char* FRAGMENT_SHADER = R"glsl(
#version 300 es
precision mediump float;
// uniform для передачи текстуры в шейдер
uniform sampler2D u_texture;
// 'out' вместо 'gl_FragColor' в GLES 3
out vec4 fragColor;
void main() {
    // gl_PointCoord - специальная переменная, содержит UV-координаты внутри точки (0,0 - 1,1)
    // Мы просто берем цвет из текстуры в этих координатах
    fragColor = texture(u_texture, gl_PointCoord);
}
)glsl";
static GLuint shader_program;

void load_particle_texture() {
    if (!g_resolve_path) {
        __android_log_print(ANDROID_LOG_ERROR, "ExampleCore", "Path resolver is not available!");
        return;
    }

    const char* full_path = g_resolve_path("particle.png");
    if (!full_path) {
        __android_log_print(ANDROID_LOG_ERROR, "ExampleCore", "Could not find 'particle.png' in project files.");
        return;
    }

    int width, height, channels;
    unsigned char* image_data = stbi_load(full_path, &width, &height, &channels, 4);
    if (!image_data) {
        __android_log_print(ANDROID_LOG_ERROR, "ExampleCore", "Failed to load image data from: %s", full_path);
        return;
    }

    glGenTextures(1, &particle_texture_id);
    glBindTexture(GL_TEXTURE_2D, particle_texture_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image_data);

    stbi_image_free(image_data);

    __android_log_print(ANDROID_LOG_INFO, "ExampleCore", "Successfully loaded particle texture with ID %d", particle_texture_id);
}


void update_and_render() {
    for (auto& p : particles) {
        p.x += p.vx;
        p.y += p.vy;
        p.vy += 0.005f;
        p.life -= 0.01f;
    }
    particles.erase(std::remove_if(particles.begin(), particles.end(), [](const Particle& p){ return p.life <= 0; }), particles.end());

    float tx = touch_x.load();
    float ty = touch_y.load();
    if (tx > 0 && particles.size() < 500) {
        std::mt19937 rng(std::random_device{}());
        std::uniform_real_distribution<float> dist(-1.0, 1.0);
        for (int i = 0; i < 5; ++i) {
            particles.push_back({ (tx / screen_width) * 2.0f - 1.0f, -((ty / screen_height) * 2.0f - 1.0f), dist(rng) * 0.02f, dist(rng) * 0.02f - 0.05f, 1.0f });
        }
    }

    glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    if (!particles.empty() && particle_texture_id > 0) {
        std::vector<float> positions;
        positions.reserve(particles.size() * 2);
        for (const auto& p : particles) {
            positions.push_back(p.x);
            positions.push_back(p.y);
        }

        glUseProgram(shader_program);

        GLint texture_uniform_location = glGetUniformLocation(shader_program, "u_texture");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, particle_texture_id);
        glUniform1i(texture_uniform_location, 0);

        GLint pos_attrib = glGetAttribLocation(shader_program, "a_position");
        glEnableVertexAttribArray(pos_attrib);
        glVertexAttribPointer(pos_attrib, 2, GL_FLOAT, GL_FALSE, 0, positions.data());
        glDrawArrays(GL_POINTS, 0, particles.size());
    }
}

void render_loop() {
    eglMakeCurrent(display, surface, surface, context);
    while (should_render) {
        update_and_render();
        eglSwapBuffers(display, surface);
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
}

extern "C" {
void core_initialize(ResolvePathCallback resolve_path_callback) {
    g_resolve_path = resolve_path_callback;
}
void core_shutdown() {
    should_render = false;
    if (render_thread.joinable()) {
        render_thread.join();
    }
}

void core_on_surface_created(const char* view_name, ANativeWindow* window) {
    native_window = window;
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, 0, 0);
    const EGLint attribs[] = { EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL_SURFACE_TYPE, EGL_WINDOW_BIT, EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8, EGL_NONE };
    EGLint numConfigs;
    EGLConfig config;
    eglChooseConfig(display, attribs, &config, 1, &numConfigs);
    surface = eglCreateWindowSurface(display, config, native_window, NULL);
    const EGLint context_attribs[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    context = eglCreateContext(display, config, NULL, context_attribs);

    eglMakeCurrent(display, surface, surface, context);

    GLuint vs = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vs, 1, &VERTEX_SHADER, NULL);
    glCompileShader(vs);
    GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fs, 1, &FRAGMENT_SHADER, NULL);
    glCompileShader(fs);
    shader_program = glCreateProgram();
    glAttachShader(shader_program, vs);
    glAttachShader(shader_program, fs);
    glLinkProgram(shader_program);
    glDeleteShader(vs);
    glDeleteShader(fs);
    load_particle_texture();

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE);

    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

    should_render = true;
    render_thread = std::thread(render_loop);
}

void core_on_surface_changed(const char* view_name, int width, int height) {
    screen_width = width;
    screen_height = height;
    glViewport(0, 0, width, height);
}

void core_on_surface_destroyed(const char* view_name) {
    should_render = false;
    if (render_thread.joinable()) {
        render_thread.join();
    }

    eglMakeCurrent(display, surface, surface, context);
    if (particle_texture_id > 0) {
        glDeleteTextures(1, &particle_texture_id);
    }
    if (shader_program > 0) {
        glDeleteProgram(shader_program);
    }
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

    if (display != EGL_NO_DISPLAY) {
        eglDestroySurface(display, surface);
        eglDestroyContext(display, context);
        eglTerminate(display);
    }
    display = EGL_NO_DISPLAY;
    context = EGL_NO_CONTEXT;
    surface = EGL_NO_SURFACE;
}

void core_on_touch_event(const char* view_name, int action, float x, float y, int pointerId) {
    if (action == 0 || action == 2) { // ACTION_DOWN or ACTION_MOVE
        touch_x = x;
        touch_y = y;
    } else { // ACTION_UP
        touch_x = -1.0f;
    }
}
} // extern "C"