package org.catrobat.catroid.raptor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.utils.ModelPathProcessor;

import java.util.HashMap;
import java.util.Map;

@LunoClass
public class ThreeDManager implements Disposable {

    public enum PhysicsState {
        NONE,    // Без физики
        STATIC,  // Статичное тело
        DYNAMIC,  // Динамическое тело
        MESH_STATIC
    }

    // Внутри класса ThreeDManager
    private static class RayCastResult {
        public boolean hasHit = false;
        public String hitObjectId = "";
        public float hitDistance = -1.0f;
    }

    // В поля класса ThreeDManager
    private com.badlogic.gdx.graphics.Color skyColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0); // По умолчанию - черный

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    // Храним загруженные модели, чтобы не загружать одну и ту же модель дважды
    private Map<String, Model> loadedModels = new HashMap<>();
    // Храним загруженные текстуры, чтобы не загружать одну и ту же текстуру дважды
    private Map<String, Texture> loadedTextures = new HashMap<>();
    // Храним экземпляры объектов на сцене по их ID
    private Map<String, ModelInstance> sceneObjects = new HashMap<>();
    // Храним экземпляры объектов на сцене по их ID
    // Храним источники света
    private Map<String, DirectionalLight> directionalLights = new HashMap<>();

    private final BoundingBox bounds1 = new BoundingBox();
    private final BoundingBox bounds2 = new BoundingBox();

    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    private btBroadphaseInterface broadphase;
    private btConstraintSolver solver;
    private btDiscreteDynamicsWorld dynamicsWorld;

    private com.badlogic.gdx.graphics.g3d.utils.ModelBuilder modelBuilder;

    private CollisionCallback collisionCallback; // Наш обработчик

    private Map<String, btRigidBody> physicsBodies = new HashMap<>();
    // Новая карта для хранения результатов пускания лучей
    private Map<String, RayCastResult> rayCastResults = new HashMap<>();

    private ShaderProvider defaultShaderProvider;
    private ShaderProvider customShaderProvider;

    private String currentVertexShader;
    private String currentFragmentShader;

    // Карта для хранения пользовательских uniform'ов
    private final Map<String, Object> customUniforms = new HashMap<>();
    private float time = 0f;

    // Добавьте эти поля к остальным полям класса
    private ModelBatch shadowBatch;
    private com.badlogic.gdx.graphics.glutils.FrameBuffer shadowFBO;
    private PerspectiveCamera lightCamera; // Используем Perspective для лучшего качества
    private com.badlogic.gdx.math.Matrix4 lightSpaceMatrix = new com.badlogic.gdx.math.Matrix4();
    private final int SHADOW_MAP_SIZE = 2048; // Размер карты теней, можно менять (1024, 2048, 4096)
    private ShaderProvider depthShaderProvider; // Специальный шейдер для рендера глубины

    public void init() {
        modelBuilder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
        defaultShaderProvider = new DefaultShaderProvider();
        modelBatch = new ModelBatch(defaultShaderProvider);
        environment = new Environment();
        setAmbientLight(0.4f, 0.4f, 0.4f); // Установим базовый свет

        //setDirectionalLight("sun", 0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setCameraPosition(100f, 100f, 100f);
        cameraLookAt(0, 0, 0);

        camera.near = 0.1f;      // Минимальное расстояние
        camera.far = 2500f;    // Максимальное расстояние (увеличили с 100 до 1000)

        // 1. Создаем FrameBuffer, куда будем рисовать карту теней
        shadowFBO = new com.badlogic.gdx.graphics.glutils.FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, true);

        // 2. Создаем "камеру" для источника света
        lightCamera = new PerspectiveCamera(90, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        lightCamera.near = 1f;
        lightCamera.far = 500f; // Дальность "прожектора" тени

        // 3. Создаем шейдер для рендера глубины (он очень простой)
        String depthVertexShader = "attribute vec3 a_position; uniform mat4 u_projViewTrans; void main() { gl_Position = u_projViewTrans * vec4(a_position, 1.0); }";
        String depthFragmentShader = "#ifdef GL_ES\nprecision mediump float;\n#endif\nvoid main() { gl_FragColor = vec4(gl_FragCoord.z, 0.0, 0.0, 1.0); }";
        depthShaderProvider = new DefaultShaderProvider(depthVertexShader, depthFragmentShader);
        shadowBatch = new ModelBatch(depthShaderProvider);

        Bullet.init();
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -9.81f, 0)); // Стандартная гравитация

        collisionCallback = new CollisionCallback();
    }

    /**
     * Устанавливает экспоненциальный туман в сцене.
     * @param r, g, b Цвет тумана (0-1).
     * @param density Плотность тумана. Хорошие значения от 0.001 (очень легкий) до 0.1 (плотный).
     *                Установите в 0 или меньше, чтобы выключить туман.
     */
    public void setFog(float r, float g, float b, float density) {
        if (density > 0) {
            environment.set(new ColorAttribute(ColorAttribute.Fog, r, g, b, 1f));
            // Плотность тумана контролируется через дальность камеры. Это хак, но он работает.
            // Чем меньше far, тем плотнее туман.
            camera.far = 1f / density;
        } else {
            // Убираем туман
            environment.remove(ColorAttribute.Fog);
            camera.far = 1000f; // Возвращаем стандартное значение
        }
        camera.update();
    }

    /**
     * Устанавливает цвет фона (неба).
     */
    public void setSkyColor(float r, float g, float b) {
        skyColor.set(r, g, b, 1f);
    }

    /**
     * Возвращает текущую позицию камеры.
     * @return Vector3 с координатами (x, y, z).
     */
    public Vector3 getCameraPosition() {
        return camera.position;
    }

    /**
     * Возвращает текущий вектор направления камеры.
     * ВАЖНО: Этот вектор уже нормализован (его длина равна 1).
     * @return Vector3 с направлением (x, y, z).
     */
    public Vector3 getCameraDirection() {
        return camera.direction;
    }

    public void update(float delta) {
        time += delta; // Обновляем время для u_time
        // Обновляем uniform'ы только если кастомный шейдер активен
        if (customShaderProvider != null) {
            customUniforms.put("u_time", time);
            customUniforms.put("u_resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            customUniforms.put("u_cameraPosition", camera.position);
            customUniforms.put("u_cameraDirection", camera.direction);
            customUniforms.put("u_viewMatrix", camera.view);
            customUniforms.put("u_projectionMatrix", camera.projection);
        }
        // 1. Обновляем физический мир, как и раньше
        dynamicsWorld.stepSimulation(delta, 5, 1f/60f);

        // 2. Новый, правильный цикл синхронизации
        for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
            ModelInstance instance = sceneObjects.get(entry.getKey());
            btRigidBody body = entry.getValue();

            if (instance != null && body.getMotionState() != null) {
                // А. Получаем матрицу трансформации из физического тела.
                //    Она содержит ПРАВИЛЬНЫЕ позицию и вращение, но НЕПРАВИЛЬНЫЙ масштаб (всегда 1,1,1).
                com.badlogic.gdx.math.Matrix4 bodyTransform = body.getWorldTransform();

                // Б. Извлекаем из нее только то, что нам нужно: позицию и вращение.
                Vector3 position = new Vector3();
                bodyTransform.getTranslation(position);
                Quaternion rotation = new Quaternion();
                bodyTransform.getRotation(rotation);

                // В. Получаем ПРАВИЛЬНЫЙ масштаб из нашей графической модели.
                Vector3 scale = new Vector3();
                instance.transform.getScale(scale);

                // Г. Собираем финальную трансформацию для графической модели из трех частей
                //    и устанавливаем ее.
                instance.transform.set(position, rotation, scale);
            }
        }
    }

    /**
     * Задает линейную скорость физического объекта.
     * Работает только для динамических объектов (Dynamic).
     * @param objectId ID объекта.
     * @param vx Скорость по оси X.
     * @param vy Скорость по оси Y.
     * @param vz Скорость по оси Z.
     */
    public void setVelocity(String objectId, float vx, float vy, float vz) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) { // Проверяем, что тело динамическое
            body.activate(); // "Будим" тело, если оно уснуло
            body.setLinearVelocity(new Vector3(vx, vy, vz));
        }
    }

    public void setGravity(float x, float y, float z) {
        dynamicsWorld.setGravity(new Vector3(x, y, z));
    }

    public void render() {
        Vector3 lightPosition = new Vector3(150, 200, 100);
        Vector3 lightLookAt = new Vector3(0, 0, 0);
        lightCamera.position.set(lightPosition);
        lightCamera.lookAt(lightLookAt);
        lightCamera.update();

        // 2. Рассчитываем матрицу, которая будет переводить мировые координаты в пространство света
        lightSpaceMatrix.set(lightCamera.combined);

        // 3. Начинаем рисовать в наш FrameBuffer
        shadowFBO.begin();
        Gdx.gl.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);

        // 4. Рендерим все объекты с помощью простого depth-шейдера
        shadowBatch.begin(lightCamera);
        for (ModelInstance instance : sceneObjects.values()) {
            shadowBatch.render(instance);
        }
        shadowBatch.end();

        // 5. Заканчиваем рисовать в FrameBuffer
        shadowFBO.end();

        // --- ПРОХОД 2: ОСНОВНОЙ РЕНДЕР СЦЕНЫ ---

        // 1. Возвращаем viewport к размеру экрана
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // 2. Очищаем основной экран
        if (skyColor.a != 0) {
            Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
            Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
        }


        camera.update();
        modelBatch.begin(camera);

        if (customShaderProvider != null) {
            // Привязываем текстуру с картой теней к текстурному юниту, например, 5
            int shadowMapTextureUnit = 5;
            shadowFBO.getColorBufferTexture().bind(shadowMapTextureUnit);

            // Передаем в шейдер, какой юнит использовать и матрицу
            setShaderUniform("shadowMap", shadowMapTextureUnit);
            customUniforms.put("u_lightSpaceMatrix", lightSpaceMatrix);

            // VVV ВОТ ИСПРАВЛЕНИЕ VVV
            // Передаем размер нашей карты теней в шейдер
            customUniforms.put("u_shadowMapSize", new Vector2(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE));
        }

        for (ModelInstance instance : sceneObjects.values()) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();
    }

    // --- НАЧАЛО НАШЕГО API ДЛЯ БЛОКОВ ---

    /**
     * Задает сплошной цвет для всех материалов объекта.
     * Примечание: Это удалит текстуру, если она была установлена.
     * @param objectId ID объекта.
     * @param r Красный компонент (0.0 - 1.0).
     * @param g Зеленый компонент (0.0 - 1.0).
     * @param b Синий компонент (0.0 - 1.0).
     */
    public void setObjectColor(String objectId, float r, float g, float b) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);
            material.set(ColorAttribute.createDiffuse(r, g, b, 1.0f));
        }
    }

    /**
     * Задает текстуру для всех материалов объекта из файла проекта.
     * @param objectId ID объекта.
     * @param texturePath Абсолютный путь к файлу текстуры (PNG/JPG).
     */
    public void setObjectTexture(String objectId, String texturePath) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null || texturePath == null || texturePath.isEmpty()) return;

        // Проверяем, кеширована ли текстура
        Texture texture = loadedTextures.get(texturePath);

        if (texture == null) {
            try {
                FileHandle textureFile = Gdx.files.absolute(texturePath);
                if (textureFile.exists()) {
                    texture = new com.badlogic.gdx.graphics.Texture(textureFile);
                    texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat); // Позволяет текстуре повторяться
                    loadedTextures.put(texturePath, texture);
                } else {
                    Gdx.app.error("3DManager", "Texture file not found: " + texturePath);
                    return;
                }
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Could not load texture: " + texturePath, e);
                return;
            }
        }

        // Применяем текстуру ко всем материалам объекта
        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(texture));

            material.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(
                    com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
            ));
        }
    }

    /**
     * Создает СЛОЖНОЕ физическое тело, основанное на геометрии модели.
     * Только для статичных объектов!
     */
    private void createMeshPhysicsBody(String objectId, ModelInstance instance) {
        // 1. Создаем "вершинный массив" из геометрии оригинальной, неотмасштабированной модели.
        com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray vertexArray =
                new com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray(instance.model.meshParts);

        // 2. Создаем сложную форму столкновения на основе этого массива.
        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);

        // 3. --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ ---
        //    Получаем текущий масштаб из графической модели...
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        //    ...и применяем его НАПРЯМУЮ к форме столкновения.
        meshShape.setLocalScaling(scale);

        // 4. Создаем "чистую" матрицу трансформации для самого RigidBody.
        //    Она содержит ТОЛЬКО позицию и вращение, БЕЗ МАСШТАБА.
        com.badlogic.gdx.math.Matrix4 bodyTransform = new com.badlogic.gdx.math.Matrix4();
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);
        bodyTransform.set(position, rotation); // Собираем матрицу БЕЗ масштаба

        // 5. Создаем RigidBody с массой 0 (статичный)
        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        float mass = 0f;
        Vector3 localInertia = new Vector3(0, 0, 0);

        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, meshShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        bodyInfo.dispose();
    }



    /**
     * Проверяет, есть ли физический контакт между двумя объектами.
     * @param objectId1 ID первого объекта.
     * @param objectId2 ID второго объекта.
     * @return true, если объекты касаются, иначе false.
     */
    public boolean checkCollision(String objectId1, String objectId2) {
        btRigidBody body1 = physicsBodies.get(objectId1);
        btRigidBody body2 = physicsBodies.get(objectId2);

        if (body1 == null || body2 == null) {
            return false;
        }

        // Сбрасываем флаг перед каждой проверкой
        collisionCallback.collided = false;

        // Запускаем тест на контакт между двумя телами
        dynamicsWorld.contactPairTest(body1, body2, collisionCallback);

        return collisionCallback.collided;
    }

    /**
     * Пускает луч в физическом мире и сохраняет результат.
     * @param rayName Имя для этого луча, чтобы потом получить результат.
     * @param from Начальная точка луча в мировых координатах.
     * @param direction Нормализованный вектор направления луча.
     */
    public void castRay(String rayName, Vector3 from, Vector3 direction) {
        // Рассчитываем конечную точку луча (очень далеко)
        Vector3 to = new Vector3(from).add(direction.scl(camera.far));

        // Создаем специальный обработчик, который найдет САМОЕ БЛИЗКОЕ пересечение
        com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback callback =
                new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(from, to);

        // Пускаем луч в физический мир
        dynamicsWorld.rayTest(from, to, callback);

        RayCastResult result = new RayCastResult();
        if (callback.hasHit()) {
            result.hasHit = true;
            // Получаем объект, в который попали
            com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = callback.getCollisionObject();

            // Ищем ID этого объекта в нашей карте physicsBodies (немного медленно, но надежно)
            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                if (entry.getValue().equals(hitObject)) {
                    result.hitObjectId = entry.getKey();
                    break;
                }
            }

            // Рассчитываем реальную дистанцию до точки столкновения
            float rayLength = from.dst(to);
            result.hitDistance = rayLength * callback.getClosestHitFraction();

        } else {
            result.hasHit = false;
            result.hitObjectId = "";
            result.hitDistance = -1.0f;
        }

        // Сохраняем результат (или его отсутствие) в нашу карту
        rayCastResults.put(rayName, result);

        // Важно! Освобождаем память, занимаемую колбэком
        callback.dispose();
    }

    /**
     * Возвращает дистанцию последнего столкновения для именованного луча.
     * @param rayName Имя луча.
     * @return Дистанция или -1, если луч ни во что не попал или не существует.
     */
    public float getRaycastDistance(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null) ? result.hitDistance : -1.0f;
    }

    /**
     * Возвращает ID объекта, с которым столкнулся именованный луч.
     * @param rayName Имя луча.
     * @return ID объекта или пустая строка, если луч ни во что не попал или не существует.
     */
    public String getRaycastHitObjectId(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null) ? result.hitObjectId : "";
    }

// --- КОНЕЦ API ---

    /**
     * Создает 3D-объект из файла модели.
     * Автоматически определяет, является ли путь абсолютным (начинается с "/")
     * или внутренним (относительно папки assets/models).
     * @param objectId Уникальный ID для нового объекта.
     * @param modelPath Путь к файлу модели (.obj).
     *                  Может быть абсолютным (e.g., "/storage/emulated/0/Download/car.obj")
     *                  или относительным (e.g., "car.obj").
     * @return true в случае успеха, false если модель не найдена или ID занят.
     */
    public boolean createObject(String objectId, String modelPath) {
        if (sceneObjects.containsKey(objectId)) return false; // ID уже занят

        // ▼▼▼ НАЧАЛО ИЗМЕНЕНИЙ ▼▼▼

        // Используем полный путь как ключ, чтобы избежать коллизий
        Model model = loadedModels.get(modelPath);

        if (model == null) {
            try {
                FileHandle modelFileHandle;

                // 1. Определяем тип пути и получаем FileHandle
                if (modelPath.startsWith("/")) {
                    // Это абсолютный путь в файловой системе устройства
                    modelFileHandle = Gdx.files.absolute(modelPath);
                } else {
                    // Это внутренний путь, ищем в assets/models/
                    modelFileHandle = Gdx.files.internal("models/" + modelPath);
                }

                // Проверяем, существует ли файл, прежде чем пытаться загрузить
                if (!modelFileHandle.exists()) {
                    Gdx.app.error("3DManager", "Model file does not exist: " + modelPath);
                    return false;
                }

                // 2. Используем наш обработчик путей (он работает с любым FileHandle)
                // Это по-прежнему важно, так как скачанный файл может иметь "плохие" пути
                FileHandle patchedModelHandle = ModelPathProcessor.process(modelFileHandle);

                Gdx.app.log("3DManager", "--- Verification before loading model ---");
                FileHandle textureToVerify = Gdx.files.local("Lowpoly_Laptop_Nor_2.jpg");
                Gdx.app.log("3DManager", "Verifying texture path: " + textureToVerify.path());
                Gdx.app.log("3DManager", "Does texture exist at path? -> " + textureToVerify.exists());
                Gdx.app.log("3DManager", "--- Verification finished ---");

                // 3. Загружаем модель
                FileHandleResolver resolver = new FileHandleResolver() {
                    @Override
                    public FileHandle resolve(String fileName) {
                        return patchedModelHandle.parent().child(fileName);
                    }
                };

                // 2. Создаем загрузчик с этим поисковиком.
                ObjLoader loader = new ObjLoader(resolver);

                // 3. Загружаем модель с помощью нашего нового загрузчика.
                model = loader.loadModel(patchedModelHandle, true);
                loadedModels.put(modelPath, model);

            } catch (Exception e) {
                Gdx.app.error("3DManager", "Could not load model: " + modelPath, e);
                return false;
            }
        }

        ModelInstance instance = new ModelInstance(model);
        sceneObjects.put(objectId, instance);

        // ▲▲▲ КОНЕЦ ИЗМЕНЕНИЙ ▲▲▲
        return true;
    }

    public void applyForce(String objectId, float forceX, float forceY, float forceZ) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.activate(); // "Будим" объект, если он "уснул"
            body.applyCentralForce(new Vector3(forceX, forceY, forceZ));
        }
    }

    /**
     * Задает или изменяет физическое состояние объекта.
     * @param objectId ID объекта.
     * @param state Тип физики (NONE, STATIC, DYNAMIC).
     * @param mass Масса объекта (имеет значение только для DYNAMIC, должна быть > 0).
     */
    public void setPhysicsState(String objectId, PhysicsState state, float mass) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        removePhysicsBody(objectId);

        switch (state) {
            case NONE:
                return; // Просто выходим

            case STATIC:
            case DYNAMIC:
                if (state == PhysicsState.DYNAMIC && mass <= 0) {
                    Gdx.app.error("3DManager", "Dynamic body must have mass > 0.");
                    return;
                }
                float bodyMass = (state == PhysicsState.DYNAMIC) ? mass : 0f;
                // Вызываем старый, проверенный метод для создания примитивов
                createPhysicsBody(objectId, instance, bodyMass);
                break;

            case MESH_STATIC:
                // Вызываем новый метод для создания сложной сетки
                createMeshPhysicsBody(objectId, instance);
                break;
        }
    }

    private Boolean objectExists(String id) {
        return sceneObjects.containsKey(id);
    }

    /**
     * Возвращает текущую линейную скорость физического объекта.
     * @param objectId ID объекта.
     * @return Vector3 со скоростью. Возвращает (0,0,0), если у объекта нет физики.
     */
    public Vector3 getVelocity(String objectId) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) {
            return body.getLinearVelocity();
        }
        return Vector3.Zero; // Возвращаем нулевой вектор, если тело не динамическое
    }

    /**
     * Возвращает текущий поворот камеры в виде углов Эйлера (в градусах).
     * @return Vector3, где: x = тангаж, y = рыскание, z = крен.
     */
    public Vector3 getCameraRotation() {
        Quaternion q = new Quaternion();
        // Получаем кватернион вращения из матрицы вида камеры
        q.setFromMatrix(camera.view);
        // Инвертируем его, так как матрица вида - это обратная трансформация
        q.conjugate();

        return new Vector3(q.getPitch(), q.getYaw(), q.getRoll());
    }

    /**
     * Создает простую 3D-сферу.
     * @param objectId Уникальный ID для нового объекта.
     * @return true в случае успеха, false если ID уже занят.
     */
    public boolean createSphere(String objectId) {
        if (sceneObjects.containsKey(objectId)) return false;

        // Используем уникальный ключ для кеширования модели сферы
        final String SPHERE_MODEL_KEY = "__PRIMITIVE_SPHERE__";
        Model sphereModel = loadedModels.get(SPHERE_MODEL_KEY);

        if (sphereModel == null) {
            // Создаем модель сферы с диаметром 50 (радиус 25),
            // с достаточным количеством полигонов (16x16), чтобы она выглядела гладкой.
            sphereModel = modelBuilder.createSphere(50f, 50f, 50f, 16, 16,
                    new com.badlogic.gdx.graphics.g3d.Material(ColorAttribute.createDiffuse(com.badlogic.gdx.graphics.Color.WHITE)),
                    com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal | com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates);
            loadedModels.put(SPHERE_MODEL_KEY, sphereModel);
        }

        ModelInstance instance = new ModelInstance(sphereModel);
        sceneObjects.put(objectId, instance);
        return true;
    }

    private void createPhysicsBody(String objectId, ModelInstance instance, float mass) {
        // --- НАЧАЛО ФИНАЛЬНОГО ИСПРАВЛЕНИЯ ---

        // 1. Получаем НЕОТМАСШТАБИРОВАННУЮ ограничивающую коробку из оригинальной модели.
        BoundingBox bbox = new BoundingBox();
        instance.model.calculateBoundingBox(bbox); // ВАЖНО: используем instance.model, а не instance
        Vector3 dimensions = new Vector3();
        bbox.getDimensions(dimensions);
        Vector3 center = new Vector3();
        bbox.getCenter(center);

        // 2. Создаем базовую физическую форму (масштаб 1,1,1).
        btBoxShape boxShape = new btBoxShape(dimensions.scl(0.5f));

        // 3. Создаем CompoundShape для правильного смещения центра.
        btCompoundShape compoundShape = new btCompoundShape();
        com.badlogic.gdx.math.Matrix4 shapeOffsetTransform = new com.badlogic.gdx.math.Matrix4();
        shapeOffsetTransform.setToTranslation(center);
        compoundShape.addChildShape(shapeOffsetTransform, boxShape);

        // 4. Применяем ТЕКУЩИЙ масштаб из ModelInstance к CompoundShape.
        // Это и есть недостающее звено!
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        compoundShape.setLocalScaling(scale);

        // --- КОНЕЦ ФИНАЛЬНОГО ИСПРАВЛЕНИЯ ---


        // 5. Дальше все как обычно, но с compoundShape
        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        com.badlogic.gdx.math.Matrix4 bodyTransform = new com.badlogic.gdx.math.Matrix4();
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);
        bodyTransform.set(position, rotation);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        bodyInfo.dispose();
    }

    /**
     * Полностью удаляет физическое тело, связанное с ID.
     */
    private void removePhysicsBody(String objectId) {
        btRigidBody body = physicsBodies.remove(objectId);
        if (body != null) {
            dynamicsWorld.removeRigidBody(body);
            if (body.getMotionState() != null) body.getMotionState().dispose();
            if (body.getCollisionShape() != null) body.getCollisionShape().dispose();
            body.dispose();
        }
    }

    /**
     * Проверяет, пересекаются ли ограничивающие коробки двух 3D-объектов.
     * @param objectId1 ID первого объекта.
     * @param objectId2 ID второго объекта.
     * @return true, если объекты пересекаются, иначе false.
     */
    public boolean checkIntersection(String objectId1, String objectId2) {
        ModelInstance instance1 = sceneObjects.get(objectId1);
        ModelInstance instance2 = sceneObjects.get(objectId2);

        if (instance1 == null || instance2 == null) {
            return false; // Если хотя бы одного объекта нет, пересечения нет
        }

        // 1. Получаем ограничивающую коробку для первого объекта
        // Сначала вычисляем "локальную" коробку модели
        instance1.calculateBoundingBox(bounds1);
        // Затем трансформируем ее в мировые координаты
        bounds1.mul(instance1.transform);

        // 2. Делаем то же самое для второго объекта
        instance2.calculateBoundingBox(bounds2);
        bounds2.mul(instance2.transform);

        // 3. Используем встроенный метод для проверки пересечения
        return bounds1.intersects(bounds2);
    }

    public boolean removeObject(String objectId) {
        ModelInstance instance = sceneObjects.remove(objectId);
        if (instance != null) {
            removePhysicsBody(objectId); // Удаляем физику, если она была
            // Тут можно добавить логику для выгрузки модели из loadedModels, если она больше не используется
            return true;
        }
        return false;
    }

    public boolean removeLight(String id) {
        DirectionalLight light = directionalLights.remove(id);
        return light != null;
    }

    public void setPosition(String objectId, float x, float y, float z) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) {
            return;
        }

        // <<< НАЧАЛО ИЗМЕНЕНИЙ
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            // Если есть физическое тело, мы должны "телепортировать" его.
            // Это работает и для STATIC, и для DYNAMIC тел.
            com.badlogic.gdx.math.Matrix4 worldTransform = body.getWorldTransform();
            worldTransform.setTranslation(x, y, z); // Меняем только позицию в его матрице
            body.setWorldTransform(worldTransform); // Устанавливаем новую матрицу
            body.getMotionState().setWorldTransform(worldTransform); // Синхронизируем и motion state
            body.activate(); // "Будим" тело, если оно уснуло
        } else {
            // Если физики нет, работаем как и раньше.
            instance.transform.setTranslation(x, y, z);
        }
        // <<< КОНЕЦ ИЗМЕНЕНИЙ
    }

    public void setRotation(String objectId, float yaw, float pitch, float roll) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Quaternion newRotation = new Quaternion().setEulerAngles(yaw, pitch, roll);

        // Сначала всегда обновляем графическую модель, сохраняя все компоненты
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        instance.transform.set(position, newRotation, scale);

        // Теперь, если есть физическое тело, обновляем ТОЛЬКО его позицию и вращение
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();
            // Берем позицию из самой физики, чтобы быть точным
            transform.getTranslation(position);
            // Устанавливаем позицию и НОВОЕ вращение (масштаб будет 1,1,1)
            transform.set(position, newRotation);

            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
        }
    }

    public void setCameraPosition(float x, float y, float z) {
        camera.position.set(x, y, z);
        camera.update();
    }

    public void cameraLookAt(float x, float y, float z) {
        camera.lookAt(x, y, z);
        camera.update();
    }

    public void objectLookAt(String id, float x, float y, float z) {
        ModelInstance instance = sceneObjects.get(id);
        if (instance == null) return;

        // 1. Сохраняем текущие позицию и масштаб ПЕРЕД разрушительной операцией
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);

        // 2. Выполняем setToLookAt, чтобы вычислить нужное вращение
        instance.transform.setToLookAt(new Vector3(x, y, z), Vector3.Y);
        Quaternion newRotation = new Quaternion();
        instance.transform.getRotation(newRotation); // Извлекаем вычисленное вращение

        // 3. Восстанавливаем полную трансформацию графической модели
        instance.transform.set(position, newRotation, scale);

        // 4. Обновляем физическое тело, используя ту же безопасную логику, что и в setRotation
        btRigidBody body = physicsBodies.get(id);
        if (body != null) {
            com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();
            transform.getTranslation(position); // Убедимся, что используем самую свежую позицию из физики
            transform.set(position, newRotation); // Устанавливаем позицию и вращение

            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
        }
    }

    /**
     * Устанавливает вращение камеры напрямую через углы Эйлера.
     * Этот метод полностью перезаписывает текущее направление камеры.
     * @param yaw Рыскание (поворот вокруг оси Y, "влево-вправо").
     * @param pitch Тангаж (поворот вокруг оси X, "вверх-вниз").
     * @param roll Крен (поворот вокруг оси Z, "наклон головы").
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        // 1. Создаем кватернион, представляющий желаемое вращение.
        Quaternion rotation = new Quaternion();
        rotation.setEulerAngles(yaw, pitch, roll);

        // 2. Устанавливаем направление камеры.
        // Начинаем со стандартного направления "вперед" (вдоль отрицательной оси Z)
        // и поворачиваем его согласно созданному кватерниону.
        camera.direction.set(0, 0, -1);
        rotation.transform(camera.direction);

        // 3. Устанавливаем "верх" камеры.
        // Начинаем со стандартного направления "вверх" (вдоль оси Y)
        // и поворачиваем его так же.
        camera.up.set(0, 1, 0);
        rotation.transform(camera.up);

        // 4. Важно! Обновляем камеру, чтобы она пересчитала свою матрицу вида (view matrix)
        // на основе новых векторов direction и up.
        camera.update();
    }

    public void setAmbientLight(float r, float g, float b) {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, r, g, b, 1f));
    }

    public void setDirectionalLight(String lightId, float r, float g, float b, float dirX, float dirY, float dirZ) {
        DirectionalLight light = directionalLights.get(lightId);
        if (light == null) {
            light = new DirectionalLight();
            directionalLights.put(lightId, light);
            environment.add(light);
        }
        light.set(r, g, b, dirX, dirY, dirZ);
    }

    public Camera getCamera() {
        return camera;
    }

    /**
     * Устанавливает масштаб объекта.
     * @param objectId ID объекта.
     * @param scaleX Масштаб по оси X.
     * @param scaleY Масштаб по оси Y.
     * @param scaleZ Масштаб по оси Z.
     */
    public void setScale(String objectId, float scaleX, float scaleY, float scaleZ) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        // --- Сохраняем текущее вращение ПЕРЕД всеми операциями ---
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);

        // 1. Устанавливаем масштаб графической модели
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        instance.transform.set(position, rotation, new Vector3(scaleX, scaleY, scaleZ));

        // 2. Пересоздаем физическое тело
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            // Сохраняем важные свойства
            float mass = (body.getInvMass() > 0f) ? 1f / body.getInvMass() : 0f;
            float friction = body.getFriction();
            float restitution = body.getRestitution();
            Vector3 velocity = body.getLinearVelocity();
            PhysicsState state = (mass > 0f) ? PhysicsState.DYNAMIC : PhysicsState.STATIC;

            // Пересоздаем тело с новой трансформацией
            setPhysicsState(objectId, state, mass);

            // Возвращаем сохраненные свойства новому телу
            btRigidBody newBody = physicsBodies.get(objectId);
            if (newBody != null) {
                // --- ВОЗВРАЩАЕМ ВРАЩЕНИЕ ---
                com.badlogic.gdx.math.Matrix4 transform = newBody.getWorldTransform();
                transform.getTranslation(position); // Берем позицию нового тела
                transform.set(position, rotation); // Устанавливаем сохраненное вращение
                newBody.setWorldTransform(transform);
                newBody.getMotionState().setWorldTransform(transform);
                // --- КОНЕЦ ---

                newBody.setFriction(friction);
                newBody.setRestitution(restitution);
                if (state == PhysicsState.DYNAMIC) {
                    newBody.setLinearVelocity(velocity);
                }
            }
        }
    }

    /**
     * Создает простой 3D-куб.
     * @param objectId Уникальный ID для нового объекта.
     * @return true в случае успеха, false если ID уже занят.
     */
    public boolean createCube(String objectId) {
        if (sceneObjects.containsKey(objectId)) return false;

        // Используем специальный ключ, чтобы кешировать модель куба
        final String CUBE_MODEL_KEY = "__PRIMITIVE_CUBE__";
        Model cubeModel = loadedModels.get(CUBE_MODEL_KEY);

        if (cubeModel == null) {
            // Создаем модель куба размером 50x50x50 с базовым материалом и текстурными координатами
            cubeModel = modelBuilder.createBox(50f, 50f, 50f,
                    new com.badlogic.gdx.graphics.g3d.Material(ColorAttribute.createDiffuse(com.badlogic.gdx.graphics.Color.WHITE)),
                    com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal | com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates);
            loadedModels.put(CUBE_MODEL_KEY, cubeModel);
        }

        ModelInstance instance = new ModelInstance(cubeModel);
        sceneObjects.put(objectId, instance);
        return true;
    }

    /**
     * Возвращает текущую позицию объекта.
     * @param objectId ID объекта.
     * @return Vector3 с координатами (x, y, z) или null, если объект не найден.
     */
    public Vector3 getPosition(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 position = new Vector3();
            instance.transform.getTranslation(position);
            return position;
        }
        return null; // Объект не найден
    }

    /**
     * Возвращает текущий поворот объекта в виде углов Эйлера (в градусах).
     * @param objectId ID объекта.
     * @return Vector3, где:
     *         x = наклон (pitch),
     *         y = рыскание (yaw),
     *         z = крен (roll).
     *         Возвращает null, если объект не найден.
     */
    public Vector3 getRotation(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Quaternion q = new Quaternion();
            instance.transform.getRotation(q);
            // Методы getPitch/Yaw/Roll возвращают углы в градусах
            return new Vector3(q.getPitch(), q.getYaw(), q.getRoll());
        }
        return null; // Объект не найден
    }

    /**
     * Возвращает текущий масштаб объекта.
     * @param objectId ID объекта.
     * @return Vector3 с масштабом по осям (x, y, z) или null, если объект не найден.
     */
    public Vector3 getScale(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 scale = new Vector3();
            instance.transform.getScale(scale);
            return scale;
        }
        return null; // Объект не найден
    }

    public Float getDistance(String id1, String id2) {
        if (getPosition(id1) == null || getPosition(id2) == null) return -1f;
        return getPosition(id1).dst(getPosition(id2));
    }

    /**
     * Задает коэффициент трения для физического объекта.
     * @param objectId ID объекта.
     * @param friction Коэффициент трения (обычно от 0.0 до 1.0).
     */
    // В файле ThreeDManager.java

    /**
     * Задает коэффициент трения для физического объекта.
     * @param objectId ID объекта.
     * @param friction Коэффициент трения (обычно от 0.0 до 1.0).
     */
    public void setFriction(String objectId, float friction) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.setFriction(friction);
            // --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ ---
            // Принудительно "пробуждаем" тело, чтобы физический мир
            // обновил информацию о его поверхности.
            body.activate();
            // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
        }
    }

    /**
     * Задает коэффициент упругости (отскока) для физического объекта.
     * @param objectId ID объекта.
     * @param restitution Коэффициент упругости (0.0 - нет отскока, 1.0 - идеальный отскок).
     */
    public void setRestitution(String objectId, float restitution) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.setRestitution(restitution);
            // --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ ---
            // Здесь это тоже важно, чтобы отскок начал работать немедленно.
            body.activate();
            // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
        }
    }

    /**
     * Включает или выключает Continuous Collision Detection (CCD) для объекта.
     * Помогает предотвратить "туннелирование" (проход сквозь стены).
     * @param objectId ID объекта.
     * @param enabled true - включить, false - выключить.
     */
    public void setContinuousCollisionDetection(String objectId, boolean enabled) {
        // (Этот метод был корректным, оставляем как есть)
        btRigidBody body = physicsBodies.get(objectId);
        ModelInstance instance = sceneObjects.get(objectId);
        if (body == null || instance == null || body.isStaticObject()) {
            return;
        }
        if (enabled) {
            BoundingBox bbox = new BoundingBox();
            instance.model.calculateBoundingBox(bbox);
            Vector3 dimensions = new Vector3();
            bbox.getDimensions(dimensions);
            float minSize = Math.min(Math.min(dimensions.x, dimensions.y), dimensions.z);
            body.setCcdMotionThreshold(minSize * 0.5f);
            body.setCcdSweptSphereRadius(minSize * 0.5f);
        } else {
            body.setCcdMotionThreshold(0);
            body.setCcdSweptSphereRadius(0);
        }
    }

    /**
     * Устанавливает кастомный GLSL шейдер из текстовых строк.
     * @param vertexCode   Строка с кодом вершинного шейдера.
     * @param fragmentCode Строка с кодом фрагментного шейдера.
     */
    public void setShaderCode(String vertexCode, String fragmentCode) {
        Gdx.app.postRunnable(() -> {
            Gdx.app.log("ShaderDebug", "--- setShaderCode CALLED ---");
            if (vertexCode == null || vertexCode.isEmpty() || fragmentCode == null || fragmentCode.isEmpty()) {
                resetSceneShader();
                return;
            }

            try {
                // --- НАЧАЛО ГЛАВНЫХ ИЗМЕНЕНИЙ ---

                // 1. Создаем Config, как и раньше
                DefaultShader.Config config = new DefaultShader.Config(vertexCode, fragmentCode);

                // 2. Чтобы проверить компиляцию, создаем временный ShaderProgram
                ShaderProgram tempProgram = new ShaderProgram(vertexCode, fragmentCode);
                if (!tempProgram.isCompiled()) {
                    throw new Exception("Shader compilation failed: " + tempProgram.getLog());
                }
                tempProgram.dispose(); // Не забываем очистить

                // 3. Освобождаем старые ресурсы
                if (customShaderProvider != null) customShaderProvider.dispose();
                if (modelBatch != null) modelBatch.dispose();

                // 4. Создаем НАШ кастомный провайдер
                customShaderProvider = new CustomShaderProvider(config, customUniforms);

                // 5. Создаем новый ModelBatch с нашим провайдером
                modelBatch = new ModelBatch(customShaderProvider);

                // --- КОНЕЦ ГЛАВНЫХ ИЗМЕНЕНИЙ ---
                Gdx.app.log("ShaderDebug", "--- CustomShaderProvider and new ModelBatch CREATED successfully ---");

            } catch (Exception e) {
                Gdx.app.error("3DManager", "Shader setup failed: " + e.getMessage());
                resetSceneShader(); // В случае ошибки сбрасываем на стандартный
            }
        });
    }

    /**
     * Сбрасывает шейдер к стандартному.
     */
    public void resetSceneShader() {
        Gdx.app.postRunnable(() -> {
            // --- НАЧАЛО ИЗМЕНЕНИЙ ---

            // 1. Если был кастомный провайдер, очищаем его
            if (customShaderProvider != null) {
                customShaderProvider.dispose();
                customShaderProvider = null;
            }

            // 2. Очищаем старый ModelBatch (если он был кастомным)
            if (modelBatch != null) modelBatch.dispose();

            customUniforms.clear();

            // 3. Создаем новый ModelBatch со СТАНДАРТНЫМ провайдером
            modelBatch = new ModelBatch(defaultShaderProvider);

            // --- КОНЕЦ ИЗМЕНЕНИЙ ---
        });
    }

    /**
     * Устанавливает uniform-переменную (float) по имени.
     */
    public void setShaderUniform(String name, float value) {
        // Добавляем префикс "u_" здесь, чтобы в LunoScript было чистое имя
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, value);
        }
    }

    /**
     * Устанавливает uniform-переменную (vec3) по имени.
     */
    public void setShaderUniform(String name, float x, float y, float z) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector3(x, y, z));
        }
    }

    public void setShaderUniform(String name, int value) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, value);
        }
    }

    /**
     * Устанавливает uniform-переменную (vec2).
     */
    public void setShaderUniform(String name, float x, float y) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector2(x, y));
        }
    }

    /**
     * Устанавливает uniform-переменную (vec4 или цвет с альфа-каналом).
     */
    public void setShaderUniform(String name, float x, float y, float z, float w) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector4(x, y, z, w));
        }
    }


    // --- КОНЕЦ API ---

    @Override
    public void dispose() {
        // 1. Очистка графики (у вас это было правильно)
        if (modelBatch != null) {
            modelBatch.dispose();
        }
        for (Model model : loadedModels.values()) {
            model.dispose();
        }
        loadedModels.clear();
        sceneObjects.clear();
        directionalLights.clear(); // Тоже очищаем для порядка

        for (Texture texture : loadedTextures.values()) {
            texture.dispose();
        }
        loadedTextures.clear();

        // 2. Очистка ВСЕХ физических тел, которые остались в мире
        // Это самый важный пропущенный шаг.
        // Мы не можем просто вызывать removePhysicsBody, так как это изменит коллекцию, по которой мы идем.
        // Вместо этого проходим по всем телам и удаляем их компоненты вручную.
        for (btRigidBody body : physicsBodies.values()) {
            // Убираем тело из физического мира
            if (dynamicsWorld != null) {
                dynamicsWorld.removeRigidBody(body);
            }
            // Освобождаем память, занимаемую его компонентами
            if (body.getMotionState() != null) {
                body.getMotionState().dispose();
            }
            if (body.getCollisionShape() != null) {
                body.getCollisionShape().dispose();
            }
            body.dispose();
        }
        physicsBodies.clear();

        // 3. Очистка самого физического мира (в порядке, обратном созданию)
        // Эти объекты должны быть удалены после того, как все тела из них убраны.
        if (dynamicsWorld != null) {
            dynamicsWorld.dispose();
        }
        if (solver != null) {
            solver.dispose();
        }
        if (broadphase != null) {
            broadphase.dispose();
        }
        if (dispatcher != null) {
            dispatcher.dispose();
        }
        if (collisionConfig != null) {
            collisionConfig.dispose();
        }
        if (collisionCallback != null) {
            collisionCallback.dispose();
        }
        if (defaultShaderProvider != null) ((DefaultShaderProvider) defaultShaderProvider).dispose();
        if (customShaderProvider != null) customShaderProvider.dispose();

        if (shadowBatch != null) shadowBatch.dispose();
        if (shadowFBO != null) shadowFBO.dispose();
        if (depthShaderProvider != null) depthShaderProvider.dispose();

        customUniforms.clear();

        // Обнуляем ссылки, чтобы избежать случайного использования после очистки
        modelBatch = null;
        dynamicsWorld = null;
        solver = null;
        broadphase = null;
        dispatcher = null;
        collisionConfig = null;
        collisionCallback = null;
    }

    // Внутри класса ThreeDManager
    private class CollisionCallback extends com.badlogic.gdx.physics.bullet.collision.ContactResultCallback {
        public boolean collided = false;

        @Override
        public float addSingleResult(com.badlogic.gdx.physics.bullet.collision.btManifoldPoint cp,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {
            collided = true;
            return 0; // Возвращаем 0, чтобы остановить дальнейшую проверку
        }
    }
}
