package org.catrobat.catroid.raptor;

import android.app.Activity;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.editor.EditorActivity;
import org.catrobat.catroid.pocketmusic.note.Project;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.LightComponent;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.RenderComponent;
import org.catrobat.catroid.raptor.TransformComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Высокоуровневый менеджер сцены. Оперирует GameObject'ами и Компонентами,
 * делегируя всю низкоуровневую работу классу ThreeDManager.
 * Это основная точка взаимодействия для редактора и системы загрузки сцен.
 */
public class SceneManager {

    public final ThreeDManager engine;
    private final Map<String, GameObject> gameObjects = new HashMap<>();
    private final BoundingBox tempBoundingBox = new BoundingBox();

    public SceneManager(ThreeDManager lowLevelEngine) {
        this.engine = lowLevelEngine;
    }

    /**
     * Полностью очищает текущую сцену и загружает новую из файла.
     * Предназначен для использования из игрового движка.
     * @param fileHandle Файл сцены .rscene для загрузки.
     */
    public void loadAndReplaceScene(FileHandle fileHandle) {
        Gdx.app.postRunnable(() -> {
            clearScene_internal();

            if (fileHandle == null || !fileHandle.exists()) {
                Gdx.app.error("SceneManager", "Scene file handle is null or does not exist.");
                return;
            }
            String sceneJson = fileHandle.readString();
            json.setUsePrototypes(false);
            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);

            if (sceneData == null) { return; }

            setBackgroundLightIntensity(sceneData.ambientIntensity);
            setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);

            if (sceneData.gameObjects == null) { return; }

            for (GameObject go : sceneData.gameObjects) {
                // ВАЖНО: Устанавливаем ID равным имени из файла
                go.id = go.name;
                gameObjects.put(go.id, go);
                rebuildGameObject_internal(go);
            }
            Gdx.app.log("SceneManager", "Scene '" + fileHandle.name() + "' loaded and replaced current scene.");
        });
    }

    // --- УПРАВЛЕНИЕ GameObject ---

    /**
     * Создает новый пустой GameObject в сцене.
     * @param baseName Имя объекта для отображения в иерархии.
     * @return Созданный GameObject.
     */
    public GameObject createGameObject(String baseName) {
        String finalName = baseName;
        int counter = 1;
        // Гарантируем уникальность имени (и, следовательно, ID)
        while (gameObjects.containsKey(finalName)) {
            finalName = baseName + " (" + counter + ")";
            counter++;
        }

        GameObject go = new GameObject(finalName);
        gameObjects.put(go.id, go);
        return go;
    }

    /**
     * Безопасно переименовывает GameObject, обновляя его ID и все ссылки в движке.
     * @param go Объект для переименования.
     * @param newName Новое уникальное имя.
     * @return true, если переименование успешно.
     */
    public boolean renameGameObject(GameObject go, String newName) {
        if (go == null || newName == null || newName.isEmpty() || gameObjects.containsKey(newName)) {
            return false; // Нельзя переименовать в пустое или уже существующее имя
        }
        String oldId = go.id;

        gameObjects.remove(oldId);

        go.id = newName;
        go.name = newName;

        gameObjects.put(go.id, go);

        engine.renameObject(oldId, newName);

        return true;
    }

    /**
     * Создает и возвращает точную копию существующего GameObject.
     * @param original Объект для дублирования.
     * @return Новый, добавленный в сцену GameObject, или null в случае ошибки.
     */
    public GameObject duplicateGameObject(GameObject original) {
        if (original == null) return null;

        String objectAsJson = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, objectAsJson);

        String baseName = original.name.replaceAll(" \\(\\d+\\)$", ""); // Убираем старый номер, если он был
        String finalName = baseName;
        int counter = 1;
        while (gameObjects.containsKey(finalName)) {
            finalName = baseName + " (" + counter + ")";
            counter++;
        }
        copy.name = finalName;
        copy.id = finalName; // ID = Имя

        gameObjects.put(copy.id, copy);
        rebuildGameObject(copy); // Этот метод уже postRunnable

        return copy;
    }

    /**
     * Находит ближайший объект, пересекаемый лучом,
     * используя геометрический тест (Bounding Box), а не физический.
     * Идеально подходит для выбора объектов в редакторе.
     * @param ray Луч, выпущенный из камеры.
     * @return Найденный GameObject или null.
     */
    public GameObject getObjectByRaycast(Ray ray) {
        List<GameObject> candidates = new ArrayList<>();
        Vector3 intersectionPoint = new Vector3();

        // Этап 1: Проверяем обычные видимые объекты
        for (GameObject go : gameObjects.values()) {
            if (!go.hasComponent(RenderComponent.class)) continue;
            ModelInstance instance = engine.getModelInstance(go.id);
            if (instance == null) continue;

            instance.calculateBoundingBox(tempBoundingBox).mul(instance.transform);
            if (Intersector.intersectRayBounds(ray, tempBoundingBox, intersectionPoint)) {
                candidates.add(go);
            }
        }

        // Этап 2: Проверяем прокси-объекты редактора
        for (Map.Entry<String, ModelInstance> entry : engine.getEditorProxies().entrySet()) {
            String ownerId = entry.getKey();
            ModelInstance proxyInstance = entry.getValue();
            GameObject owner = findGameObject(ownerId);
            if (owner == null) continue;

            proxyInstance.transform.set(owner.transform.position, owner.transform.rotation);
            proxyInstance.calculateBoundingBox(tempBoundingBox).mul(proxyInstance.transform);

            if (Intersector.intersectRayBounds(ray, tempBoundingBox, intersectionPoint)) {
                candidates.add(owner);
            }
        }

        // Если никого не нашли, выходим
        if (candidates.isEmpty()) {
            return null;
        }

        // Если нашли только один объект, сразу возвращаем его
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // Этап 3: Точная фаза. Находим самый маленький объект среди кандидатов.
        GameObject bestCandidate = null;
        float smallestVolume = Float.MAX_VALUE;

        for (GameObject candidate : candidates) {
            ModelInstance instance = engine.getModelInstance(candidate.id);
            // Если это прокси-объект (свет), то его ModelInstance нет в основной карте, берем из прокси
            if (instance == null) {
                if (candidate.hasComponent(LightComponent.class) || candidate.hasComponent(CameraComponent.class)) {
                    instance = engine.getEditorProxies().get(candidate.id);
                }
            }
            if (instance == null) continue;

            instance.calculateBoundingBox(tempBoundingBox); // Получаем локальный Bounding Box
            float volume = tempBoundingBox.getWidth() * tempBoundingBox.getHeight() * tempBoundingBox.getDepth();

            if (volume < smallestVolume) {
                smallestVolume = volume;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    /**
     * Безопасно применяет относительный поворот к объекту, избегая проблем
     * с неравномерным масштабированием.
     * @param go Объект для вращения.
     * @param deltaRotation Кватернион, представляющий поворот, который нужно добавить.
     */
    public void rotate(GameObject go, Quaternion deltaRotation) {
        if (go == null) return;

        TransformComponent transform = go.transform;

        Vector3 originalScale = transform.scale.cpy();

        engine.setScale(go.id, 1, 1, 1);

        transform.rotation.mulLeft(deltaRotation);
        engine.setRotation(go.id, transform.rotation);

        engine.setScale(go.id, originalScale.x, originalScale.y, originalScale.z);

        transform.scale.set(originalScale);

        applyLightAndTransform(go);
    }

    /**
     * Собирает текущее состояние сцены в объект SceneData.
     * @return объект SceneData, готовый к кэшированию или сохранению.
     */
    public SceneData getCurrentSceneData() {
        SceneData sceneData = new SceneData();
        for (GameObject go : gameObjects.values()) {
            sceneData.gameObjects.add(go);
        }
        sceneData.skyR = this.skyR;
        sceneData.skyG = this.skyG;
        sceneData.skyB = this.skyB;
        sceneData.ambientIntensity = this.ambientIntensity;
        return sceneData;
    }

    /**
     * Загружает сцену из объекта SceneData. Используется для восстановления из кэша.
     */
    public void loadSceneFromData(SceneData sceneData) {
        if (sceneData == null) return;

        clearScene_internal();

        setBackgroundLightIntensity(sceneData.ambientIntensity);
        setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);

        if (sceneData.gameObjects == null) return;

        for (GameObject go : sceneData.gameObjects) {
            gameObjects.put(go.id, go);
            rebuildGameObject_internal(go);
        }
        Gdx.app.log("SceneManager", "Scene loaded from cache.");
    }

    /**
     * Удаляет GameObject и все связанные с ним ресурсы из сцены.
     * @param go GameObject для удаления.
     */
    public void removeGameObject(GameObject go) {
        if (go == null) return;
        gameObjects.remove(go.id);

        engine.removeObject(go.id);

        if (go.hasComponent(LightComponent.class)) {
            engine.removePBRLight(go.id);
            engine.removeEditorProxy(go.id);
        }
    }

    public GameObject findGameObject(String id) {
        return gameObjects.get(id);
    }

    public Map<String, GameObject> getAllGameObjects() {
        return gameObjects;
    }

    // --- РАБОТА С КОМПОНЕНТАМИ ---

    /**
     * Добавляет или обновляет RenderComponent, создавая видимую 3D-модель.
     */
    public void setRenderComponent(GameObject go, String modelFileName) {
        Gdx.app.postRunnable(() -> {
            File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelFileName);
            if (modelFile == null || !modelFile.exists()) {
                Gdx.app.error("SceneManager", "Model file not found in project: " + modelFileName);
            }
            String absolutePath = modelFile.getAbsolutePath();

            if (go.hasComponent(RenderComponent.class)) {
                engine.removeObject(go.id);
            }
            RenderComponent render = go.getComponent(RenderComponent.class);

            if (render == null) {
                render = new RenderComponent();
                go.addComponent(render);
            }
            render.modelFileName = modelFileName;

            boolean success = engine.createObject(go.id, absolutePath);
            if (!success) { return; }

            applyTransform(go);

            if (go.hasComponent(PhysicsComponent.class)) {
                PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
                setPhysicsComponent(go, physics.state, physics.shape, physics.mass);
            }
        });
    }

    public void setAnimationComponent(GameObject go, AnimationComponent animComponent) {
        if (go == null) return;

        go.components.removeIf(c -> c instanceof AnimationComponent);

        if (animComponent != null) {
            Log.d("SceneManager", "Is added: " + go.addComponent(animComponent));
        }

        playAnimationFromComponent(go);
    }


    /**
     * Устанавливает простое физическое состояние объекта.
     * ЭТОТ МЕТОД ПРЕДНАЗНАЧЕН ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ С БЛОКАМИ.
     * Он полностью перезаписывает любые сложные настройки коллайдеров,
     * заменяя их одним примитивом (Box, Sphere, Capsule).
     *
     * @param go    GameObject для изменения.
     * @param state Тип физического тела (Static, Dynamic и т.д.).
     * @param shape Форма единственного коллайдера.
     * @param mass  Масса (используется только для Dynamic).
     */
    public void setPhysicsComponent(GameObject go, ThreeDManager.PhysicsState state, ThreeDManager.PhysicsShape shape, float mass) {
        Gdx.app.postRunnable(() -> {
            PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
            if (physics == null) {
                physics = new PhysicsComponent();
                go.addComponent(physics);
            }
            // Обновляем компонент, чтобы инспектор показал правильное состояние
            physics.state = state;
            physics.mass = mass;
            physics.colliders.clear(); // Очищаем кастомные коллайдеры
            if (state == ThreeDManager.PhysicsState.STATIC || state == ThreeDManager.PhysicsState.DYNAMIC) {
                ColliderShapeData singleCollider = new ColliderShapeData();
                switch (shape) {
                    case SPHERE:
                        singleCollider.type = ColliderShapeData.ShapeType.SPHERE;
                        break;
                    case CAPSULE:
                        singleCollider.type = ColliderShapeData.ShapeType.CAPSULE;
                        break;
                    case BOX:
                    default:
                        singleCollider.type = ColliderShapeData.ShapeType.BOX;
                        break;
                }
                // Добавляем этот единственный коллайдер в список.
                // Размеры и смещение оставляем по умолчанию (0),
                // движок ThreeDManager сам использует габариты модели, если они не заданы.
                physics.colliders.add(singleCollider);
            }

            // 4. Вызываем ОСНОВНОЙ метод движка, передавая ему полностью
            //    сконфигурированный компонент.
            engine.setPhysicsState(go.id, state, shape, mass);
        });
    }

    /**
     * Обновляет физическое состояние объекта, используя данные напрямую из компонента.
     * Предназначен для использования из редактора, где компонент уже содержит
     * все нужные данные (включая список коллайдеров).
     * @param go        GameObject для обновления.
     * @param component Полностью настроенный PhysicsComponent.
     */
    public void setPhysicsComponent(GameObject go, PhysicsComponent component) {
        Gdx.app.postRunnable(() -> {
            go.components.removeIf(c -> c instanceof PhysicsComponent);
            go.addComponent(component);

            // Решаем, какой метод движка вызвать, точно так же, как при загрузке
            if (component.colliders != null && !component.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, component);
            } else {
                engine.setPhysicsState(go.id, component.state, component.shape, component.mass);
            }
        });
    }

    /**
     * Добавляет или обновляет LightComponent.
     */
    public void setLightComponent(GameObject go, LightComponent lightData) {
        Gdx.app.postRunnable(() -> {
            boolean wasLightBefore = go.hasComponent(LightComponent.class);
            go.components.removeIf(c -> c instanceof LightComponent);
            go.addComponent(lightData);

            if (!wasLightBefore) {
                engine.createEditorProxy(go.id);
            }

            applyLightAndTransform(go);
        });
    }

    public void setCameraComponent(GameObject go, CameraComponent cameraData) {
        Gdx.app.postRunnable(() -> {
            boolean wasCameraBefore = go.hasComponent(CameraComponent.class);
            go.components.removeIf(c -> c instanceof CameraComponent);
            go.addComponent(cameraData);

            if (!wasCameraBefore) {
                engine.createCameraProxy(go.id);
            }

            // Если эта камера помечена как главная, применяем ее настройки
            if (cameraData.isMainCamera) {
                applyCameraComponentToEngine(go, cameraData);
            }
        });
    }

    // --- НОВЫЙ ВСПОМОГАТЕЛЬНЫЙ МЕТОД ---
    private void applyCameraComponentToEngine(GameObject go, CameraComponent camComp) {
        engine.setCameraFov(camComp.fieldOfView, camComp.nearPlane, camComp.farPlane); // Нужен новый метод в ThreeDManager
        engine.setCameraPosition(go.transform.position.x, go.transform.position.y, go.transform.position.z);
        engine.setCameraRotation(go.transform.rotation);  // Нужен новый метод в ThreeDManager
    }

    // --- УПРАВЛЕНИЕ ТРАНСФОРМАЦИЕЙ ---

    public void setPosition(GameObject go, Vector3 position) {
        go.transform.position.set(position);
        applyLightAndTransform(go);
    }

    public void setRotation(GameObject go, Quaternion rotation) {
        go.transform.rotation.set(rotation);
        applyLightAndTransform(go);
    }

    public void setScale(GameObject go, Vector3 scale) {
        go.transform.scale.set(scale);
        applyTransform(go);
    }

    // --- УПРАВЛЕНИЕ АНИМАЦИЯМИ ---

    public void playAnimation(GameObject go, String animationName, int loops, float speed, float transitionTime) {
        engine.playAnimation(go.id, animationName, loops, speed, transitionTime);
    }

    public void stopAnimation(GameObject go) {
        engine.stopAnimation(go.id);
    }

    // --- ПРОЧИЕ СИСТЕМЫ ДВИЖКА ---

    public void castRay(String rayName, Vector3 from, Vector3 direction) {
        engine.castRay(rayName, from, direction);
    }

    public GameObject getRaycastHitObject(String rayName) {
        String hitId = engine.getRaycastHitObjectId(rayName);
        if (hitId != null && !hitId.isEmpty()) {
            return findGameObject(hitId);
        }
        return null;
    }

    public float getRaycastDistance(String rayName) {
        return engine.getRaycastDistance(rayName);
    }

    public float skyR = 0.1f;
    public float skyG = 0.2f;
    public float skyB = 0.3f;
    public float ambientIntensity = 1f;

    public void setBackgroundLightIntensity(float ambientIntensity) {
        this.ambientIntensity = ambientIntensity;
        engine.setBackgroundLightIntensity(ambientIntensity);
    }

    public void setSkyColor(float r, float g, float b) {
        skyR = r;
        skyG = g;
        skyB = b;
        engine.setSkyColor(skyR, skyG, skyB);
    }

    /**
     * Находит ПЕРВЫЙ GameObject с указанным именем.
     * Этот метод следует использовать в игровых скриптах для поиска объектов.
     * Поиск не чувствителен к регистру.
     * @param name Имя объекта, заданное в редакторе.
     * @return GameObject или null, если объект с таким именем не найден.
     */
    public GameObject findObjectByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GameObject go : gameObjects.values()) {
            if (name.equalsIgnoreCase(go.name)) {
                return go; // Возвращаем первый совпавший
            }
        }
        return null; // Ничего не найдено
    }

    /**
     * Находит ВСЕ GameObject'ы с указанным именем.
     * Полезно для поиска групп объектов (например, всех врагов с именем "Goblin").
     * @param name Имя объектов.
     * @return Список (может быть пустым) всех найденных GameObjects.
     */
    public List<GameObject> findObjectsByName(String name) {
        List<GameObject> foundObjects = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            return foundObjects;
        }
        for (GameObject go : gameObjects.values()) {
            if (name.equalsIgnoreCase(go.name)) {
                foundObjects.add(go);
            }
        }
        return foundObjects;
    }

    // --- УПРАВЛЕНИЕ СЦЕНОЙ ---

    private final Json json = new Json();

    /**
     * Сохраняет текущее состояние сцены в файл.
     * @param fileHandle Файл для сохранения (например, Gdx.files.local("scenes/level1.json")).
     */
    public void saveScene(FileHandle fileHandle) {
        SceneData sceneData = new SceneData();
        for (GameObject go : gameObjects.values()) {
            sceneData.gameObjects.add(go);
        }
        sceneData.skyR = skyR;
        sceneData.skyG = skyG;
        sceneData.skyB = skyB;
        sceneData.ambientIntensity = ambientIntensity;

        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);

        String sceneJson = json.prettyPrint(sceneData);

        fileHandle.writeString(sceneJson, false);
        Gdx.app.log("SceneManager", "Scene saved to " + fileHandle.path());
    }

    /**
     * Загружает сцену из файла, полностью заменяя текущую.
     * @param fileHandle Файл для загрузки.
     */
    public void loadScene(FileHandle fileHandle) {
        if (fileHandle == null || !fileHandle.exists()) {
            Gdx.app.error("SceneManager", "Scene file handle is null or does not exist.");
            return;
        }

        String sceneJson = fileHandle.readString();
        json.setUsePrototypes(false);
        SceneData sceneData = json.fromJson(SceneData.class, sceneJson);


        setBackgroundLightIntensity(sceneData.ambientIntensity);
        setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);

        if (sceneData == null || sceneData.gameObjects == null) { return; }

        for (GameObject go : sceneData.gameObjects) {
            gameObjects.put(go.id, go);
            rebuildGameObject(go);
        }

        Gdx.app.log("SceneManager", "Scene build commands issued.");
    }

    private void rebuildGameObject_internal(GameObject go) {
        RenderComponent render = go.getComponent(RenderComponent.class);
        if (render != null && render.modelFileName != null && !render.modelFileName.isEmpty()) {
            String absolutePath;
            if (render.modelFileName.startsWith("assets://")) {
                absolutePath = render.modelFileName.substring("assets://".length());
            } else {
                File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(render.modelFileName);
                if (modelFile != null && modelFile.exists()) {
                    absolutePath = modelFile.getAbsolutePath();
                } else {
                    Gdx.app.error("SceneManager", "Rebuild failed: Model file not found: " + render.modelFileName);
                    return;
                }
            }
            if (!engine.createObject(go.id, absolutePath)) {
                Gdx.app.error("SceneManager", "Rebuild failed: Could not create render object for " + go.id);
                return;
            }
        }

        applyTransform(go);

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null) {
            // --- УМНЫЙ ВЫБОР МЕТОДА ---
            // Если список colliders НЕ пустой (т.е. настроен в редакторе),
            // то используем НОВЫЙ метод.
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics);
            }
            // ИНАЧЕ, используем СТАРЫЙ, надежный метод.
            else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass);
            }

            engine.setFriction(go.id, physics.friction);
            engine.setRestitution(go.id, physics.restitution);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light != null) {
            engine.createEditorProxy(go.id);
            applyLightAndTransform(go);
        }

        CameraComponent camera = go.getComponent(CameraComponent.class);
        if (camera != null) {
            engine.createCameraProxy(go.id);
            if (camera.isMainCamera) {
                // Ищем первую же камеру, помеченную как главная, и применяем ее
                Gdx.app.log("SceneManager", "MainCamera found: " + go.name + ". Applying its transform.");
                applyCameraComponentToEngine(go, camera);
            }
        }

        playAnimationFromComponent(go);
    }


    /**
     * Вспомогательный метод, который воссоздает объект в движке на основе его компонентов.
     * @param go GameObject, загруженный из JSON.
     */
    public void rebuildGameObject(GameObject go) {
        Gdx.app.postRunnable(() -> rebuildGameObject_internal(go));
    }

    private void clearScene_internal() {
        engine.clearScene();
        gameObjects.clear();
    }

    /**
     * Полностью очищает текущую сцену, готовя ее к новой загрузке.
     */
    public void clearScene() {
        Gdx.app.postRunnable(this::clearScene_internal);
    }

    public void playAnimationFromComponent(GameObject go) {
        AnimationComponent anim = go.getComponent(AnimationComponent.class);
        if (anim == null || anim.animationName == null) {
            Gdx.app.postRunnable(() -> engine.stopAnimation(go.id));
            return;
        }
        Gdx.app.postRunnable(() -> {
            engine.playAnimation(go.id, anim.animationName, anim.loops, anim.speed, anim.transitionTime);
        });
    }

    public Array<String> getAnimationNames(GameObject go) {
        return engine.getAnimationNames(go.id);
    }

    // --- ПРИВАТНЫЕ МЕТОДЫ-ПОМОЩНИКИ ---

    /**
     * Применяет все трансформации из TransformComponent к объекту в движке.
     */
    private void applyTransform(GameObject go) {
        if (go == null) return;
        TransformComponent t = go.transform;

        // Убедимся, что объект существует в движке (у него должен быть RenderComponent)
        if (!engine.objectExists(go.id)) return;

        engine.setPosition(go.id, t.position.x, t.position.y, t.position.z);

        engine.setRotation(go.id, t.rotation);

        engine.setScale(go.id, t.scale.x, t.scale.y, t.scale.z);
    }

    /**
     * Удаляет RenderComponent с объекта.
     * Это делает объект невидимым, но сохраняет его в иерархии.
     */
    public void removeRenderComponent(GameObject go) {
        if (go == null || !go.hasComponent(RenderComponent.class)) return;

        go.components.removeIf(c -> c instanceof RenderComponent);
        engine.removeObject(go.id);
    }

    /**
     * Удаляет PhysicsComponent с объекта.
     */
    public void removePhysicsComponent(GameObject go) {
        if (go == null || !go.hasComponent(PhysicsComponent.class)) return;

        go.components.removeIf(c -> c instanceof PhysicsComponent);
        engine.removePhysicsBody(go.id);
    }

    /**
     * Удаляет LightComponent с объекта.
     */
    public void removeLightComponent(GameObject go) {
        if (go == null) return;

        go.components.removeIf(c -> c instanceof LightComponent);
        engine.removePBRLight(go.id);
        engine.removeEditorProxy(go.id);
    }


    /**
     * Применяет параметры света и трансформации.
     */
    private void applyLightAndTransform(GameObject go) {
        if (go == null) return;
        applyTransform(go);

        if (go.hasComponent(LightComponent.class)) {
            engine.updateEditorProxyPosition(go.id, go.transform.position);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light == null) return;

        Vector3 pos = go.transform.position;
        float r = light.color.r, g = light.color.g, b = light.color.b;

        switch (light.type) {
            case SPOT:
                Vector3 spotDir = new Vector3(0, 0, -1);
                go.transform.rotation.transform(spotDir);
                engine.setSpotLight(go.id, pos.x, pos.y, pos.z, spotDir.x, spotDir.y, spotDir.z,
                        r, g, b, light.intensity, light.cutoffAngle, light.exponent, light.range);
                break;
            case POINT:
                engine.setPointLight(go.id, pos.x, pos.y, pos.z, r, g, b, light.intensity, light.range);
                break;
            case DIRECTIONAL:
                Vector3 sunDir = new Vector3(0, 0, -1);
                // Мы просто берем поворот объекта и применяем его к вектору (0, 0, -1)
                go.transform.rotation.transform(sunDir);

                engine.setRealisticSunLight(sunDir.x, sunDir.y, sunDir.z, light.intensity);
                engine.setSunLightColor(r, g, b);

                // Сохраняем это вычисленное направление обратно в компонент,
                // чтобы оно корректно записалось в JSON при сохранении.
                light.direction.set(sunDir);
                break;
        }
    }

    public void setRestitution(String id, float restitution) {
        engine.setRestitution(id, restitution);
    }

    public void setFriction(String id, float friction) {
        engine.setFriction(id, friction);
    }

    public Json getJson() {
        return json;
    }
}