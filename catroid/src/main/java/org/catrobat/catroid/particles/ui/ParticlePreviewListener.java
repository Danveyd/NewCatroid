package org.catrobat.catroid.particles.ui;

import android.graphics.Bitmap;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.particles.ParticleEffectModel;
import org.catrobat.catroid.particles.ParticleInstance;

import java.io.File;

public class ParticlePreviewListener implements ApplicationListener, GestureDetector.GestureListener {

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    public ParticleEffectModel model;
    private ParticleInstance previewInstance;

    public boolean showGravityGizmo = true;
    public boolean showAngleGizmo = true;
    public boolean showPosVarGizmo = true;
    public boolean showRadiusGizmo = true;

    private Texture defaultTexture;
    private Texture particleCustomTexture;
    private Texture customBgTexture;

    private float initialZoom = 1.0f;

    public ParticlePreviewListener(ParticleEffectModel model) {
        this.model = model;
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        createDefaultTexture();

        previewInstance = new ParticleInstance("preview", model, new TextureRegion(defaultTexture));
        previewInstance.x = 0;
        previewInstance.y = 0;

        Gdx.input.setInputProcessor(new GestureDetector(this));

        reloadParticleTexture();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        initialZoom = camera.zoom;
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        if (count == 2) {
            camera.zoom = 1.0f;
            camera.position.set(0, 0, 0);
        }
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom);
        return true;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        float ratio = initialDistance / distance;
        camera.zoom = MathUtils.clamp(initialZoom * ratio, 0.2f, 4.0f);
        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {
    }

    public void reloadParticleTexture() {
        if (model == null) return;

        Gdx.app.postRunnable(() -> {
            try {
                String texName = model.textureFileName != null ? model.textureFileName.trim() : "";
                if (!texName.isEmpty()) {
                    Project project = ProjectManager.getInstance().getCurrentProject();
                    if (project != null) {
                        File file = project.getFile(texName);
                        if (file != null && file.exists()) {
                            if (particleCustomTexture != null) particleCustomTexture.dispose();
                            particleCustomTexture = new Texture(Gdx.files.absolute(file.getAbsolutePath()));
                            if (previewInstance != null) {
                                previewInstance.setTextureRegion(new TextureRegion(particleCustomTexture));
                            }
                            return;
                        }
                    }
                }

                if (previewInstance != null && defaultTexture != null) {
                    previewInstance.setTextureRegion(new TextureRegion(defaultTexture));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createDefaultTexture() {
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fillCircle(8, 8, 7);
        defaultTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public void setCustomBackground(Bitmap bitmap) {
        if (bitmap == null) return;
        Gdx.app.postRunnable(() -> {
            try {
                if (customBgTexture != null) customBgTexture.dispose();

                Pixmap pixmap = new Pixmap(bitmap.getWidth(), bitmap.getHeight(), Pixmap.Format.RGBA8888);
                int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                for (int y = 0; y < bitmap.getHeight(); y++) {
                    for (int x = 0; x < bitmap.getWidth(); x++) {
                        int pixel = pixels[y * bitmap.getWidth() + x];
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        int a = (pixel >> 24) & 0xFF;
                        pixmap.drawPixel(x, y, (r << 24) | (g << 16) | (b << 8) | a);
                    }
                }

                customBgTexture = new Texture(pixmap);
                pixmap.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.06f, 0.09f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (customBgTexture != null) {
            batch.setColor(1f, 1f, 1f, 0.6f);
            batch.draw(customBgTexture, -400, -300, 800, 600);
            batch.setColor(Color.WHITE);
        }

        if (previewInstance != null) {
            previewInstance.update(Gdx.graphics.getDeltaTime());
            previewInstance.draw(batch);
        }

        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        if (model != null) {
            if (model.mode == ParticleEffectModel.EmitterMode.GRAVITY) {
                if (showPosVarGizmo) {
                    shapeRenderer.setColor(Color.CYAN);
                    shapeRenderer.rect(-model.posVarX, -model.posVarY, model.posVarX * 2f, model.posVarY * 2f);
                }
                if (showGravityGizmo) {
                    shapeRenderer.setColor(Color.RED);
                    shapeRenderer.line(0, 0, model.gravityX * 0.5f, model.gravityY * 0.5f);
                }
                if (showAngleGizmo) {
                    shapeRenderer.setColor(Color.YELLOW);
                    float minA = model.angle - model.angleVariance;
                    float maxA = model.angle + model.angleVariance;
                    float len = Math.max(60f, model.speed * 0.5f);

                    shapeRenderer.line(0, 0, (float) Math.cos(Math.toRadians(minA)) * len, (float) Math.sin(Math.toRadians(minA)) * len);
                    shapeRenderer.line(0, 0, (float) Math.cos(Math.toRadians(maxA)) * len, (float) Math.sin(Math.toRadians(maxA)) * len);
                }
            } else {
                if (showRadiusGizmo) {
                    shapeRenderer.setColor(Color.MAGENTA);
                    shapeRenderer.circle(0, 0, model.startRadius);
                    shapeRenderer.setColor(Color.CORAL);
                    shapeRenderer.circle(0, 0, model.endRadius);
                }
            }
        }

        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, false);
        }
    }

    @Override
    public void pause() {
    }

    public void resetPreview() {
        if (previewInstance != null) {
            previewInstance.reset();
        }
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (defaultTexture != null) defaultTexture.dispose();
        if (particleCustomTexture != null) particleCustomTexture.dispose();
        if (customBgTexture != null) customBgTexture.dispose();
    }
}
