// Создай новый файл org/catrobat/catroid/raptor/CustomShaderProvider.java
package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;
import java.util.Map;

public class CustomShaderProvider extends BaseShaderProvider {
    private final DefaultShader.Config config;
    private final Map<String, Object> customUniforms;

    public CustomShaderProvider(DefaultShader.Config config, Map<String, Object> customUniforms) {
        this.config = config;
        this.customUniforms = customUniforms;
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        return new CustomShader(renderable, config, customUniforms);
    }
}