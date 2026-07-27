package org.catrobat.catroid.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import android.util.Log;

import com.google.gson.Gson;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleManager {
    private static final String TAG = "ParticleManager";
    private static ParticleManager instance;

    private final Map<String, ParticleEffectModel> effectTemplates = new ConcurrentHashMap<>();
    private final Map<String, ParticleInstance> activeInstances = new ConcurrentHashMap<>();
    private final Map<String, ParticleActor> activeActors = new ConcurrentHashMap<>();

    private final Map<String, Texture> textureCache = new ConcurrentHashMap<>();
    private final Map<String, TextureRegion> textureRegionCache = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    private Texture defaultTexture;
    private TextureRegion defaultTextureRegion;

    private ParticleManager() {
        createDefaultTexture();
    }

    public static synchronized ParticleManager getInstance() {
        if (instance == null) {
            instance = new ParticleManager();
        }
        return instance;
    }

    public synchronized void validateGLContext() {
        if (defaultTexture == null || !defaultTexture.isManaged() && Gdx.gl != null) {
            createDefaultTexture();
            for (ParticleInstance pInst : activeInstances.values()) {
                TextureRegion region = getTextureRegionForModel(pInst.model);
                pInst.setTextureRegion(region);
            }
        }
    }

    private void createDefaultTexture() {
        try {
            if (defaultTexture != null) {
                try { defaultTexture.dispose(); } catch (Exception ignored) {}
            }
            Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(8, 8, 7);
            defaultTexture = new Texture(pixmap);
            pixmap.dispose();
            defaultTextureRegion = new TextureRegion(defaultTexture);
        } catch (Exception e) {
            Log.e(TAG, "Error creating default particle texture", e);
        }
    }

    public void registerEffectTemplate(String effectId, ParticleEffectModel model) {
        if (effectId == null || model == null) return;
        String cleanId = effectId.trim().replace("'", "").replace("\"", "");
        effectTemplates.put(cleanId, model);
    }

    public void spawnInstance(String effectFile, String instanceId, float x, float y) {
        validateGLContext();

        String cleanFileName = effectFile.trim().replace("'", "").replace("\"", "");
        String cleanInstanceId = instanceId.trim().replace("'", "").replace("\"", "");

        ParticleEffectModel model = effectTemplates.get(cleanFileName);

        if (model == null) {
            model = loadModelFromProjectFile(cleanFileName);
            if (model != null) {
                effectTemplates.put(cleanFileName, model);
            }
        }

        if (model == null) {
            model = new ParticleEffectModel();
            model.effectId = cleanFileName;
            effectTemplates.put(cleanFileName, model);
        }

        ParticleEffectModel instanceModel = cloneModel(model);
        TextureRegion region = getTextureRegionForModel(instanceModel);

        ParticleInstance particleInstance = new ParticleInstance(cleanInstanceId, instanceModel, region);
        particleInstance.x = x;
        particleInstance.y = y;
        particleInstance.reset();

        activeInstances.put(cleanInstanceId, particleInstance);

        Gdx.app.postRunnable(() -> {
            ParticleActor actor = new ParticleActor(particleInstance);
            activeActors.put(cleanInstanceId, actor);

            StageListener stageListener = StageActivity.getActiveStageListener();
            if (stageListener != null && stageListener.getStage() != null) {
                stageListener.getStage().addActor(actor);
            }
        });
    }


    public void setZIndex(String instanceId, int zIndex) {
        if (instanceId == null) return;
        String cleanId = instanceId.trim().replace("'", "").replace("\"", "");
        ParticleActor actor = activeActors.get(cleanId);
        if (actor != null) {
            Gdx.app.postRunnable(() -> {
                StageListener stageListener = StageActivity.getActiveStageListener();
                if (stageListener != null) {
                    stageListener.setActorZIndexSafely(actor, zIndex);
                } else {
                    actor.setZIndex(zIndex);
                }
            });
        }
    }

    public void bringToFront(String instanceId) {
        if (instanceId == null) return;
        String cleanId = instanceId.trim().replace("'", "").replace("\"", "");
        ParticleActor actor = activeActors.get(cleanId);
        if (actor != null) {
            Gdx.app.postRunnable(actor::toFront);
        }
    }

    public void sendToBack(String instanceId) {
        if (instanceId == null) return;
        String cleanId = instanceId.trim().replace("'", "").replace("\"", "");
        ParticleActor actor = activeActors.get(cleanId);
        if (actor != null) {
            Gdx.app.postRunnable(actor::toBack);
        }
    }

    public synchronized TextureRegion getTextureRegionForModel(ParticleEffectModel model) {
        if (model != null && model.textureFileName != null) {
            String texName = model.textureFileName.replaceAll("[\"'\r\n\t]", "").trim();
            if (!texName.isEmpty()) {
                TextureRegion customRegion = getOrCreateProjectTextureRegion(texName);
                if (customRegion != null) {
                    return customRegion;
                }
            }
        }
        return defaultTextureRegion;
    }

    private synchronized TextureRegion getOrCreateProjectTextureRegion(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return defaultTextureRegion;

        String cleanName = fileName.replaceAll("[\"'\r\n\t]", "").trim();

        if (textureRegionCache.containsKey(cleanName)) {
            Texture cachedTex = textureCache.get(cleanName);
            if (cachedTex != null && cachedTex.isManaged()) {
                return textureRegionCache.get(cleanName);
            }
        }

        try {
            Project project = ProjectManager.getInstance().getCurrentProject();
            if (project != null) {
                File file = project.getFile(cleanName);
                if (file != null && file.exists() && file.length() > 0) {
                    Texture texture = new Texture(Gdx.files.absolute(file.getAbsolutePath()));
                    TextureRegion region = new TextureRegion(texture);

                    textureCache.put(cleanName, texture);
                    textureRegionCache.put(cleanName, region);

                    return region;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Texture creating error: " + cleanName, e);
        }

        return defaultTextureRegion;
    }

    public ParticleEffectModel loadModelFromProjectFile(String fileName) {
        try {
            Project project = ProjectManager.getInstance().getCurrentProject();
            if (project == null) return null;

            String cleanName = fileName.replaceAll("[\"'\r\n\t]", "").trim();
            File filesDir = project.getFilesDir();
            File targetFile = project.getFile(cleanName);

            if (!targetFile.exists()) {
                File[] allFiles = filesDir.listFiles();
                if (allFiles != null) {
                    for (File f : allFiles) {
                        if (f.isFile() && f.getName().trim().equalsIgnoreCase(cleanName)) {
                            targetFile = f;
                            break;
                        }
                    }
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0) return null;

            String json = com.google.common.io.Files.toString(targetFile, StandardCharsets.UTF_8);
            return gson.fromJson(json, ParticleEffectModel.class);
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + fileName, e);
        }
        return null;
    }

    private ParticleEffectModel cloneModel(ParticleEffectModel source) {
        try {
            String json = gson.toJson(source);
            return gson.fromJson(json, ParticleEffectModel.class);
        } catch (Exception e) {
            return source;
        }
    }

    public ParticleInstance getInstance(String instanceId) {
        if (instanceId == null) return null;
        String cleanId = instanceId.trim().replace("'", "").replace("\"", "");
        return activeInstances.get(cleanId);
    }

    public void updateAll(float delta) {
        validateGLContext();
        for (ParticleInstance pInstance : activeInstances.values()) {
            pInstance.update(delta);
        }
    }

    public void renderForBuffer(String bufferName, Batch batch) {
        if (bufferName == null || bufferName.trim().isEmpty()) return;
        String cleanBufferName = bufferName.trim();

        for (ParticleInstance pInstance : activeInstances.values()) {
            String targetBuffer = pInstance.targetBufferName;
            if (targetBuffer != null && targetBuffer.trim().equalsIgnoreCase(cleanBufferName)) {
                if (pInstance.bufferMode != ParticleInstance.BufferRenderMode.SCREEN_ONLY) {
                    pInstance.draw(batch);
                }
            }
        }
    }

    public void stopInstance(String instanceId, boolean immediate) {
        if (instanceId == null) return;
        String cleanId = instanceId.trim().replace("'", "").replace("\"", "");
        ParticleInstance pInstance = activeInstances.get(cleanId);
        if (pInstance != null) {
            pInstance.stop(immediate);
            if (immediate) {
                activeInstances.remove(cleanId);
                ParticleActor actor = activeActors.remove(cleanId);
                if (actor != null) {
                    Gdx.app.postRunnable(actor::remove);
                }
            }
        }
    }

    private synchronized void clearCustomTextures() {
        for (Texture tex : textureCache.values()) {
            if (tex != null) {
                try { tex.dispose(); } catch (Exception ignored) {}
            }
        }
        textureCache.clear();
        textureRegionCache.clear();
    }

    public void resetForNewScene() {
        for (ParticleActor actor : activeActors.values()) {
            if (actor != null) actor.remove();
        }
        activeActors.clear();
        activeInstances.clear();
        effectTemplates.clear();
        clearCustomTextures();
    }

    public void clearAll() {
        resetForNewScene();
    }

    public void dispose() {
        clearAll();
        if (defaultTexture != null) {
            try { defaultTexture.dispose(); } catch (Exception ignored) {}
            defaultTexture = null;
        }
    }
}
