// Создай новый файл org/catrobat/catroid/raptor/CustomShader.java
package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Vector3;
import java.util.Map;

public class CustomShader extends DefaultShader {
    private final Map<String, Object> customUniforms;

    public CustomShader(Renderable renderable, Config config, Map<String, Object> customUniforms) {
        super(renderable, config);
        this.customUniforms = customUniforms;
    }

    @Override
    public void render(Renderable renderable) {
        // 1. Сначала вызываем стандартный рендер. Он установит все
        //    встроенные uniform'ы (матрицы, позицию камеры и т.д.).
        super.render(renderable);

        // 2. Теперь устанавливаем наши кастомные uniform'ы.
        if (customUniforms != null && !customUniforms.isEmpty()) {
            for (Map.Entry<String, Object> entry : customUniforms.entrySet()) {
                String name = "u_" + entry.getKey(); // Добавляем префикс "u_"
                Object value = entry.getValue();

                if (value instanceof Float) {
                    program.setUniformf(name, (Float) value);
                } else if (value instanceof Vector3) {
                    program.setUniformf(name, (Vector3) value);
                } else if (value instanceof Integer) {
                    program.setUniformi(name, (Integer) value);
                }
                // Можно добавить другие типы, если нужно (vec2, vec4, etc.)
            }
        }
    }
}