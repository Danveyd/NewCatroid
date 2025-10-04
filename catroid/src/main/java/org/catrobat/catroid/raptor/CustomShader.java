package org.catrobat.catroid.raptor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;

import java.util.Map;

public class CustomShader extends DefaultShader {
    private final Map<String, Object> customUniforms;

    public CustomShader(Renderable renderable, Config config, Map<String, Object> customUniforms) {
        super(renderable, config);
        this.customUniforms = customUniforms;
    }

    @Override
    public void render(Renderable renderable) {
        Gdx.app.log("ShaderDebug", ">>> CustomShader is RENDERING! Time is: " + customUniforms.get("u_time"));
        // 1. Сначала вызываем стандартный рендер.
        super.render(renderable);

        // 2. Теперь устанавливаем наши кастомные uniform'ы.
        if (!customUniforms.isEmpty()) {
            for (Map.Entry<String, Object> entry : customUniforms.entrySet()) {
                String name = entry.getKey(); // Имя уже содержит "u_" префикс
                Object value = entry.getValue();

                // Проверяем тип и вызываем соответствующий setUniform
                if (value instanceof Float) {
                    program.setUniformf(name, (Float) value);
                } else if (value instanceof Integer) {
                    program.setUniformi(name, (Integer) value);
                } else if (value instanceof Vector2) {
                    program.setUniformf(name, (Vector2) value);
                } else if (value instanceof Vector3) {
                    program.setUniformf(name, (Vector3) value);
                } else if (value instanceof Vector4) {
                    program.setUniformf(name, (Vector4) value);
                } else if (value instanceof Matrix4) {
                    program.setUniformMatrix(name, (Matrix4) value);
                }
                // Можно добавить другие типы, например, массивы (float[], vec3[])
            }
        }
    }
}