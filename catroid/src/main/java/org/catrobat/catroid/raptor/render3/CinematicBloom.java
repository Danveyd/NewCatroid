package org.catrobat.catroid.raptor.render3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

public class CinematicBloom implements Disposable {

    private static final int PASSES = 4;

    private final FrameBuffer[] downsampleFbos = new FrameBuffer[PASSES];
    private final FrameBuffer[] upsampleFbos = new FrameBuffer[PASSES];

    private ShaderProgram downsampleShader;
    private ShaderProgram upsampleShader;

    private final SpriteBatch batch;
    private final OrthographicCamera orthoCam;

    public float threshold = 0.85f;

    public CinematicBloom() {
        batch = new SpriteBatch();
        orthoCam = new OrthographicCamera();

        ShaderProgram.pedantic = false;

        String vsh = "attribute vec4 a_position;\n" +
                "attribute vec4 a_color;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    v_texCoords = a_texCoord0;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}";

        downsampleShader = new ShaderProgram(vsh, Gdx.files.internal("shaders/bloom_downsample.frag").readString());
        upsampleShader = new ShaderProgram(vsh, Gdx.files.internal("shaders/bloom_upsample.frag").readString());

        if (!downsampleShader.isCompiled()) Gdx.app.error("Bloom", "Downsample error: " + downsampleShader.getLog());
        if (!upsampleShader.isCompiled()) Gdx.app.error("Bloom", "Upsample error: " + upsampleShader.getLog());
    }

    public void resize(int width, int height) {
        disposeFbos();

        int curW = width / 2;
        int curH = height / 2;

        for (int i = 0; i < PASSES; i++) {
            curW = Math.max(2, curW);
            curH = Math.max(2, curH);

            downsampleFbos[i] = new FrameBuffer(Pixmap.Format.RGBA8888, curW, curH, false);
            downsampleFbos[i].getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            upsampleFbos[i] = new FrameBuffer(Pixmap.Format.RGBA8888, curW, curH, false);
            upsampleFbos[i].getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            curW /= 2;
            curH /= 2;
        }

        orthoCam.setToOrtho(false, 1, 1);
    }

    public Texture process(Texture inputHdrScene) {
        batch.setProjectionMatrix(orthoCam.combined);

        Texture currentInput = inputHdrScene;

        for (int i = 0; i < PASSES; i++) {
            FrameBuffer dst = downsampleFbos[i];
            dst.begin();
            Gdx.gl.glViewport(0, 0, dst.getWidth(), dst.getHeight());

            batch.setShader(downsampleShader);
            batch.begin();
            downsampleShader.setUniformf("u_texelSize", 1.0f / currentInput.getWidth(), 1.0f / currentInput.getHeight());
            downsampleShader.setUniformf("u_threshold", (i == 0) ? threshold : 0.0f);

            batch.draw(currentInput, 0, 0, 1, 1, 0, 0, currentInput.getWidth(), currentInput.getHeight(), false, true);
            batch.end();
            dst.end();

            currentInput = dst.getColorBufferTexture();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

        for (int i = PASSES - 1; i > 0; i--) {
            FrameBuffer src = (i == PASSES - 1) ? downsampleFbos[i] : upsampleFbos[i];
            FrameBuffer dst = upsampleFbos[i - 1];

            dst.begin();
            Gdx.gl.glViewport(0, 0, dst.getWidth(), dst.getHeight());

            batch.setShader(upsampleShader);
            batch.begin();
            upsampleShader.setUniformf("u_texelSize", 1.0f / src.getWidth(), 1.0f / src.getHeight());
            upsampleShader.setUniformf("u_sampleScale", 1.0f);

            Texture srcTex = src.getColorBufferTexture();
            batch.draw(srcTex, 0, 0, 1, 1, 0, 0, srcTex.getWidth(), srcTex.getHeight(), false, true);
            batch.end();
            dst.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
        return upsampleFbos[0].getColorBufferTexture();
    }

    private void disposeFbos() {
        for (int i = 0; i < PASSES; i++) {
            if (downsampleFbos[i] != null) downsampleFbos[i].dispose();
            if (upsampleFbos[i] != null) upsampleFbos[i].dispose();
        }
    }

    @Override
    public void dispose() {
        disposeFbos();
        if (downsampleShader != null) downsampleShader.dispose();
        if (upsampleShader != null) upsampleShader.dispose();
        batch.dispose();
    }
}
