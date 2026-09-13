package org.catrobat.catroid.ide

import android.content.Context
import org.catrobat.catroid.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ProjectMetadata(
    val name: String,
    val path: String,
    val lastModified: Long
)

data class SigningConfig(
    val keystorePath: String,
    val keystorePass: String,
    val keyAlias: String,
    val keyPass: String
)

data class ProjectConfig(
    val name: String,
    val packageName: String,
    val version: String,
    val versionCode: Int,
    val orientation: String,
    val permissions: List<String>,
    val repositories: List<String>,
    val runConsole: Boolean,
    val signing: SigningConfig? = null,
    val isProtected: Boolean = false,
    val isObfuscated: Boolean = false,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
)

object ProjectManager {
    private const val PROJECTS_DIR_NAME = "NecatProjects"

    fun getProjectsFolder(filesDir: File): File {
        val folder = File(filesDir, PROJECTS_DIR_NAME)
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    fun getAllProjects(filesDir: File): List<ProjectMetadata> {
        val root = getProjectsFolder(filesDir)
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.map { dir ->
                ProjectMetadata(
                    name = dir.name,
                    path = dir.absolutePath,
                    lastModified = dir.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    fun createProject(filesDir: File, name: String): Boolean {
        val root = getProjectsFolder(filesDir)
        val projectDir = File(root, name)
        if (projectDir.exists()) return false

        if (projectDir.mkdirs()) {
            File(projectDir, "src").mkdirs()
            File(projectDir, "assets").mkdirs()


            val config = ProjectConfig(name, "com.example.$name", "1.0.0", 1, "sensor", emptyList(), emptyList(), true)
            saveConfig(projectDir.absolutePath, config)
            return true
        }
        return false
    }

    fun createProject(
        context: Context,
        name: String,
        packageName: String,
        type: String,
        onProgress: (String) -> Unit
    ): Boolean {

        onProgress(context.getString(R.string.ipm_creating_structure))
        val root = getProjectsFolder(context.filesDir)
        val projectDir = File(root, name)
        if (projectDir.exists()) {
            onProgress(context.getString(R.string.ipm_project_exists_error))
            return false
        }

        if (projectDir.mkdirs()) {
            val srcDir = File(projectDir, "src/game").apply { mkdirs() }
            File(projectDir, "assets").mkdirs()

            val config = ProjectConfig(
                name = name,
                packageName = packageName,
                version = "1.0.0",
                versionCode = 1,
                orientation = "portrait",
                permissions = listOf("android.permission.INTERNET"),
                repositories = emptyList(),
                runConsole = (type == "console" || type == "jsoup" || type == "empty" || type == "background_task" || type == "file_io" || type == "gson" || type == "zxing"),
                signing = null,
                isProtected = false
            )
            saveConfig(projectDir.absolutePath, config)

            val mainFile = File(srcDir, "Main.java")

            when (type) {
                "console" -> {
                    mainFile.writeText("""
                        package game;
                        public class Main {
                            public void onStart() {
                                System.out.println("Hello from Console!");
                            }
                        }
                    """.trimIndent())
                }

                "android_ui" -> {
                    mainFile.writeText("""
                        package game;
                        import android.content.Context;
                        import android.view.ViewGroup;
                        import android.widget.Button;
                        import android.widget.TextView;
                        import android.widget.LinearLayout;
                        import android.graphics.Color;
                        import android.view.View;
                        import android.view.Gravity;

                        public class Main {
                            public void onStart(final Context context, ViewGroup layout) {
                                layout.setBackgroundColor(Color.parseColor("#202020"));
                                
                                LinearLayout panel = new LinearLayout(context);
                                panel.setOrientation(LinearLayout.VERTICAL);
                                panel.setGravity(Gravity.CENTER);
                                
                                final TextView text = new TextView(context);
                                text.setText("Hello Android UI!");
                                text.setTextSize(32);
                                text.setTextColor(Color.CYAN);
                                
                                Button btn = new Button(context);
                                btn.setText("Tap Me");
                                btn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        text.setText("Tapped!");
                                        text.setTextColor(Color.GREEN);
                                    }
                                });
                                
                                panel.addView(text);
                                panel.addView(btn);
                                layout.addView(panel);
                            }
                        }
                    """.trimIndent())
                }

                "libgdx" -> {
                    val libs = listOf(
                        // LibGDX
                        "com.badlogicgames.gdx:gdx:1.12.1",
                        "com.badlogicgames.gdx:gdx-jnigen-loader:2.3.1",
                        "com.badlogicgames.gdx:gdx-backend-android:1.12.1",
                        "com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a",

                        // AndroidX Essentials
                        "androidx.fragment:fragment:1.3.6",

                        // Fragment
                        "androidx.activity:activity:1.2.3",
                        "androidx.loader:loader:1.0.0",
                        "androidx.viewpager:viewpager:1.0.0",
                        "androidx.customview:customview:1.0.0",
                        "androidx.collection:collection:1.1.0",
                        "androidx.core:core:1.6.0",
                        "androidx.versionedparcelable:versionedparcelable:1.1.1",

                        // Lifecycle
                        "androidx.lifecycle:lifecycle-common:2.3.1",
                        "androidx.lifecycle:lifecycle-viewmodel:2.3.1",
                        "androidx.lifecycle:lifecycle-livedata-core:2.3.1",
                        "androidx.lifecycle:lifecycle-viewmodel-savedstate:2.3.1",
                        "androidx.savedstate:savedstate:1.1.0",

                        "androidx.annotation:annotation:1.2.0"
                    )


                    for (lib in libs) {
                        onProgress(context.getString(R.string.ipm_downloading, lib.split(':')[1]))
                        val success = DependencyManager.downloadLibraryRecursive(
                            context,
                            projectDir.absolutePath,
                            lib,
                            recursive = false
                        ) { _, _ ->

                        }
                        if (!success) {
                            onProgress(context.getString(R.string.ipm_download_error, lib))


                            return true

                        }
                    }

                    onProgress(context.getString(R.string.ipm_generating_code))


                    mainFile.writeText("""
                        package game;
                        import android.content.Context;
                        import android.view.ViewGroup;
                        import android.view.View;
                        import androidx.fragment.app.FragmentActivity;
                        import androidx.fragment.app.FragmentManager;
                        import androidx.fragment.app.Fragment;
                
                        public class Main {
                            public void onStart(Context context, ViewGroup layout) {
                                FragmentActivity activity = (FragmentActivity) context;
                                FragmentManager fm = activity.getSupportFragmentManager();
                                String TAG = "libgdx_container_tag";
                
                                Fragment old = fm.findFragmentByTag(TAG);
                                if (old != null) {
                                    fm.beginTransaction().remove(old).commitNow();
                                }
                
                                int containerId = View.generateViewId();
                                layout.setId(containerId);
                
                                ContainerFragment containerFrag = new ContainerFragment();
                                
                                fm.beginTransaction()
                                  .add(containerId, containerFrag, TAG)
                                  .commitNow();
                            }
                        }
                    """.trimIndent())

                    File(srcDir, "ContainerFragment.java").writeText("""
                        package game;
                        import android.os.Bundle;
                        import android.view.LayoutInflater;
                        import android.view.View;
                        import android.view.ViewGroup;
                        import android.widget.FrameLayout;
                        import androidx.fragment.app.Fragment;
                        import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
                
                        public class ContainerFragment extends Fragment implements AndroidFragmentApplication.Callbacks {
                            
                            @Override
                            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                                FrameLayout layout = new FrameLayout(getContext());
                                layout.setId(View.generateViewId());
                                
                                GameFragment gameFragment = new GameFragment();
                                
                                getChildFragmentManager().beginTransaction()
                                    .add(layout.getId(), gameFragment)
                                    .commitNow();
                                    
                                return layout;
                            }
                
                            @Override
                            public void exit() {
                            }
                        }
                    """.trimIndent())

                    File(srcDir, "GameFragment.java").writeText("""
                        package game;
                        import android.os.Bundle;
                        import android.view.LayoutInflater;
                        import android.view.View;
                        import android.view.ViewGroup;
                        import com.badlogic.gdx.Gdx;
                        import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
                        import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
                
                        public class GameFragment extends AndroidFragmentApplication {
                            
                            @Override
                            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                                AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
                                config.useAccelerometer = false;
                                config.useCompass = false;
                                config.r = 8; config.g = 8; config.b = 8; config.a = 8;
                                return initializeForView(new MyGame(), config);
                            }
                
                            @Override
                            public void onPause() {
                                if (Gdx.graphics != null) {
                                    Gdx.graphics.setContinuousRendering(false);
                                    Gdx.graphics.requestRendering();
                                }
                                super.onPause();
                            }
                
                            @Override
                            public void onDestroy() {
                                try {
                                    if (Gdx.app != null) Gdx.app.exit();
                                } catch (Exception e) {}
                                
                                Gdx.app = null;
                                Gdx.graphics = null;
                                super.onDestroy();
                            }
                        }
                    """.trimIndent())

                    File(srcDir, "MyGame.java").writeText("""
                        package game;
                        import com.badlogic.gdx.ApplicationAdapter;
                        import com.badlogic.gdx.Gdx;
                        import com.badlogic.gdx.graphics.GL20;
                        public class MyGame extends ApplicationAdapter {
                            float r = 0;
                            @Override public void create() { Gdx.app.log("MyGame", "Game Created!"); }
                            @Override public void render() {
                                r += 0.01f;
                                Gdx.gl.glClearColor((float)Math.sin(r), 0, (float)Math.cos(r), 1);
                                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                            }
                        }
                    """.trimIndent())
                }

                "jsoup" -> {
                    val lib = "org.jsoup:jsoup:1.13.1"

                    onProgress(context.getString(R.string.ipm_downloading, lib.split(':')[1]))
                    val success = DependencyManager.downloadLibraryRecursive(
                        context,
                        projectDir.absolutePath,
                        lib
                    ) { _, _ ->

                    }
                    if (!success) {
                        onProgress(context.getString(R.string.ipm_download_error, lib))


                        return true

                    }

                    onProgress(context.getString(R.string.ipm_generating_code))

                    mainFile.writeText("""
                        package game;
                        import android.content.Context;
                        import org.jsoup.Jsoup;
                        import org.jsoup.nodes.Document;

                        public class Main {
                            public void onStart() {
                                System.out.println("Fetching Google title...");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Document doc = Jsoup.connect("https://www.google.com").get();
                                            System.out.println("Title: " + doc.title());
                                        } catch (Exception e) {
                                            System.out.println("Error: " + e.getMessage());
                                        }
                                    }
                                }).start();
                            }
                        }
                     """.trimIndent())
                }

                "background_task" -> {
                    mainFile.writeText("""
        package game;

        public class Main {
            private boolean running = true;

            public void onStart() {
                System.out.println("Background task started");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        while (running) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}

                            System.out.println("Tick " + i++);
                        }
                    }
                }).start();
            }

            public void onStop() {
                running = false;
            }
        }
    """.trimIndent())
                }

                "canvas_game" -> {
                    mainFile.writeText("""
        package game;

        import android.content.Context;
        import android.view.View;
        import android.view.ViewGroup;
        import android.graphics.Canvas;
        import android.graphics.Paint;
        import android.graphics.Color;

        public class Main {

            public void onStart(Context context, ViewGroup layout) {
                GameView view = new GameView(context);
                layout.addView(view);
            }

            static class GameView extends View {
                Paint paint = new Paint();
                float x = 100;
                float dx = 10;

                public GameView(Context c) {
                    super(c);
                    paint.setColor(Color.RED);
                }

                @Override
                protected void onDraw(Canvas canvas) {
                    canvas.drawColor(Color.BLACK);
                    canvas.drawCircle(x, 300, 50, paint);
                    x += dx;
                    if (x > getWidth() || x < 0) dx = -dx;
                    invalidate();
                }
            }
        }
    """.trimIndent())
                }

                "file_io" -> {
                    mainFile.writeText("""
        package game;

        import android.content.Context;
        import java.io.File;
        import java.io.FileWriter;
        import java.util.Scanner;

        public class Main {

            public void onStart(Context context) {
                try {
                    File f = new File(context.getFilesDir(), "test.txt");
                    FileWriter w = new FileWriter(f);
                    w.write("Hello Necat!");
                    w.close();

                    Scanner s = new Scanner(f);
                    System.out.println("File content: " + s.nextLine());
                    s.close();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    """.trimIndent())
                }

                "gson" -> {
                    val lib = "com.google.code.gson:gson:2.10.1"
                    DependencyManager.downloadLibraryRecursive(context, projectDir.absolutePath, lib) { _, _ -> }

                    mainFile.writeText("""
        package game;

        import com.google.gson.Gson;

        class User {
            String name;
            int age;
        }

        public class Main {
            public void onStart() {
                Gson gson = new Gson();
                User u = new User();
                u.name = "Necat";
                u.age = 1;

                String json = gson.toJson(u);
                System.out.println(json);

                User u2 = gson.fromJson(json, User.class);
                System.out.println(u2.name + " " + u2.age);
            }
        }
    """.trimIndent())
                }

                "zxing" -> {
                    val lib = "com.google.zxing:core:3.5.2"
                    DependencyManager.downloadLibraryRecursive(context, projectDir.absolutePath, lib) { _, _ -> }

                    mainFile.writeText("""
        package game;

        import com.google.zxing.qrcode.QRCodeWriter;
        import com.google.zxing.BarcodeFormat;
        import com.google.zxing.common.BitMatrix;

        public class Main {
            public void onStart() {
                try {
                    QRCodeWriter writer = new QRCodeWriter();
                    BitMatrix matrix = writer.encode("Hello Necat!", BarcodeFormat.QR_CODE, 10, 10);
                    System.out.println("QR generated: " + matrix.getWidth());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    """.trimIndent())
                }

                "empty" -> {
                    mainFile.writeText("""
        package game;

        public class Main {
            public void onStart() {
                
            }
        }
    """.trimIndent())
                }

                "cpp_jni" -> {

                    mainFile.writeText("""
        package game;
        import android.content.Context;
        import android.widget.TextView;
        import android.view.ViewGroup;
        import android.graphics.Color;
        import android.view.Gravity;

        public class Main {
            static {
                System.loadLibrary("native-lib");
            }

            public native String stringFromJNI();

            public void onStart(Context context, ViewGroup layout) {
                TextView tv = new TextView(context);
                tv.setText(stringFromJNI());
                tv.setTextSize(30);
                tv.setTextColor(Color.GREEN);
                tv.setGravity(Gravity.CENTER);
                layout.addView(tv);
            }
        }
    """.trimIndent())


                    val cppDir = File(projectDir, "src/main/cpp")
                    cppDir.mkdirs()


                    File(cppDir, "native-lib.cpp").writeText("""
        #include <jni.h>
        #include <string>

        extern "C" JNIEXPORT jstring JNICALL
        Java_game_Main_stringFromJNI(JNIEnv* env, jobject /* this */) {
            std::string hello = "Hello from C++!";
            return env->NewStringUTF(hello.c_str());
        }
    """.trimIndent())


                    File(cppDir, "CMakeLists.txt").writeText("""
        cmake_minimum_required(VERSION 3.10.2)
        project("necat_cpp")

        add_library( 
             native-lib
             SHARED
             native-lib.cpp
        )

        find_library( 
             log-lib
             log
        )

        target_link_libraries( 
             native-lib
             $\{log-lib}
        )
    """.trimIndent())
                }

            }
            return true
        }
        return false
    }

    fun deleteProject(filesDir: File, projectName: String): Boolean {
        val root = getProjectsFolder(filesDir)
        val projectDir = File(root, projectName)
        return if (projectDir.exists()) {
            projectDir.deleteRecursively()
        } else {
            false
        }
    }



    fun loadConfig(projectPath: String): ProjectConfig {
        val configFile = File(projectPath, "project.json")
        val defaultName = File(projectPath).name


        if (!configFile.exists()) {
            return ProjectConfig(
                name = defaultName,
                packageName = "com.example.${defaultName.lowercase().replace(" ", "")}",
                version = "1.0.0",
                versionCode = 1,
                orientation = "landscape",
                permissions = listOf("android.permission.INTERNET"),
                repositories = emptyList(),
                runConsole = true,
                signing = null
            )
        }

        return try {
            val json = JSONObject(configFile.readText())

            val repos = ArrayList<String>()
            json.optJSONArray("repositories")?.let { arr ->
                for (i in 0 until arr.length()) repos.add(arr.getString(i))
            }

            val perms = ArrayList<String>()
            json.optJSONArray("permissions")?.let { arr ->
                for (i in 0 until arr.length()) perms.add(arr.getString(i))
            }
            if (perms.isEmpty()) perms.add("android.permission.INTERNET")


            var signingConfig: SigningConfig? = null
            val signObj = json.optJSONObject("signing")
            if (signObj != null) {
                signingConfig = SigningConfig(
                    keystorePath = signObj.optString("path", "release.jks"),
                    keystorePass = signObj.optString("pass", ""),
                    keyAlias = signObj.optString("alias", "key0"),
                    keyPass = signObj.optString("keyPass", "")
                )
            }

            ProjectConfig(
                name = json.optString("name", defaultName),
                packageName = json.optString("packageName", "com.danvex.${defaultName.lowercase()}"),
                version = json.optString("version", "1.0.0"),
                versionCode = json.optInt("versionCode", 1),
                orientation = json.optString("orientation", "landscape"),
                permissions = perms,
                repositories = repos,
                runConsole = json.optBoolean("runConsole", true),
                signing = signingConfig,
                isProtected = json.optBoolean("isProtected", false),
                isObfuscated = json.optBoolean("isObfuscated", false),
                minSdk = json.optInt("minSdk", 21),
                targetSdk = json.optInt("targetSdk", 34)
            )
        } catch (e: Exception) {
            e.printStackTrace()

            ProjectConfig(defaultName, "com.example.game", "1.0", 1, "landscape", listOf("android.permission.INTERNET"), emptyList(), true, null)
        }
    }

    fun saveConfig(projectPath: String, config: ProjectConfig) {
        val configFile = File(projectPath, "project.json")
        val json = JSONObject()
        json.put("name", config.name)
        json.put("packageName", config.packageName)
        json.put("version", config.version)
        json.put("versionCode", config.versionCode)
        json.put("orientation", config.orientation)
        json.put("runConsole", config.runConsole)
        json.put("isProtected", config.isProtected)
        json.put("isObfuscated", config.isObfuscated)
        json.put("minSdk", config.minSdk)
        json.put("targetSdk", config.targetSdk)

        val reposArray = JSONArray()
        config.repositories.forEach { reposArray.put(it) }
        json.put("repositories", reposArray)

        val permsArray = JSONArray()
        config.permissions.forEach { permsArray.put(it) }
        json.put("permissions", permsArray)


        if (config.signing != null) {
            val signObj = JSONObject()
            signObj.put("path", config.signing.keystorePath)
            signObj.put("pass", config.signing.keystorePass)
            signObj.put("alias", config.signing.keyAlias)
            signObj.put("keyPass", config.signing.keyPass)
            json.put("signing", signObj)
        }

        configFile.writeText(json.toString(4))
    }
}
