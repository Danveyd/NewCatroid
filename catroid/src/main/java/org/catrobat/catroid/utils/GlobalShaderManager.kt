package org.catrobat.catroid.utils // или любой другой ваш пакет утилит

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

object GlobalShaderManager {
    // Здесь будет храниться скомпилированный шейдер пользователя
    var customSceneShader: ShaderProgram? = null
        private set

    // Здесь храним исходный код, чтобы можно было его редактировать
    var fragmentShaderCode: String? = null
        private set

    var vertexShaderCode: String? = null
        private set

    // Стандартный Vertex Shader, который почти никогда не меняется для 2D пост-эффектов
    val DEFAULT_VERTEX_SHADER = """
    attribute vec4 a_position;
    attribute vec2 a_texCoord0;
    uniform mat4 u_projTrans; // <--- ЭТО ВАЖНО
    varying vec2 v_texCoords;

    void main() {
        v_texCoords = a_texCoord0;
        gl_Position = u_projTrans * a_position;
    }
""".trimIndent()

    /*"""
    attribute vec4 a_position;
    attribute vec2 a_texCoord0;
    
    // ДОБАВЬТЕ ЭТУ СТРОКУ
    uniform mat4 u_projTrans; 

    varying vec2 v_texCoords;

    void main() {
        v_texCoords = a_texCoord0;
        // И ИЗМЕНИТЕ ЭТУ СТРОКУ
        gl_Position = u_projTrans * a_position;
    }
""".trimIndent()*/



    val DEFAULT_TEST_SHADER = """// Код для вашего фрагментного шейдера
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    // 1. Пытаемся получить цвет из текстуры FBO
    vec4 sceneColor = texture2D(u_texture, v_texCoords);

    // 2. Генерируем "контрольный" цвет-градиент, который не зависит от текстуры.
    // Он доказывает, что сам шейдер и его координаты работают.
    vec4 gradientColor = vec4(v_texCoords.x, v_texCoords.y, 0.0, 1.0);

    // 3. Смешиваем их. 50% от сцены, 50% от градиента.
    gl_FragColor = mix(sceneColor, gradientColor, 0.5);
}"""

    // Пытаемся установить новый шейдер
    // Возвращает null в случае успеха или строку с ошибкой в случае неудачи
    fun setCustomShader(fragmentCode: String, vertexCode: String): String? {
        android.util.Log.d("ShaderDebug", "--- GlobalShaderManager.setCustomShader CALLED ---")
        // Сначала уничтожаем старый шейдер, если он был
        customSceneShader?.dispose()
        customSceneShader = null

        // Если код пустой, просто сбрасываем шейдер
        if (fragmentCode.isBlank()) {
            fragmentShaderCode = null
            android.util.Log.d("ShaderDebug", "Fragment code is blank. Resetting shader.")
            return null
        }

        if (vertexCode.isBlank()) {
            vertexShaderCode = DEFAULT_VERTEX_SHADER
        } else {
            vertexShaderCode = vertexCode
        }

        android.util.Log.d("ShaderDebug", "Attempting to compile shader.")
        val newShader = ShaderProgram(vertexShaderCode, fragmentCode)
        if (!newShader.isCompiled) {
            val log = newShader.log
            android.util.Log.e("ShaderDebug", "SHADER COMPILATION FAILED! Log: $log")
            newShader.dispose() // Очищаем мусор
            vertexShaderCode = null
            return log // Возвращаем ошибку компиляции
        }

        // Успех!
        android.util.Log.d("ShaderDebug", "SHADER COMPILED SUCCESSFULLY!")
        customSceneShader = newShader
        fragmentShaderCode = fragmentCode
        return null // Нет ошибок
    }

    // Сброс к стандартному рендерингу
    fun resetToDefault() {
        customSceneShader?.dispose()
        customSceneShader = null
        fragmentShaderCode = null
        vertexShaderCode = null
    }

    // Важно вызывать при закрытии приложения, чтобы избежать утечек памяти GPU
    fun dispose() {
        customSceneShader?.dispose()
        customSceneShader = null
    }

    fun clear() {
        customSceneShader?.dispose()
        customSceneShader = null
    }
}