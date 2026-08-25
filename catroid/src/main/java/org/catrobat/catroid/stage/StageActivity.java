/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.danvexteam.lunoscript_annotations.LunoClass;
import com.gaurav.avnc.vnc.UserCredential;
import com.gaurav.avnc.vnc.VncClient;
import com.google.common.collect.Multimap;
import com.google.firebase.FirebaseApp;

import org.catrobat.catroid.BuildConfig;
import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.bluetooth.base.BluetoothDeviceService;
import org.catrobat.catroid.camera.CameraManager;
import org.catrobat.catroid.common.CatroidService;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.common.ServiceProvider;
import org.catrobat.catroid.content.BackPressedScript;
import org.catrobat.catroid.content.GlobalManager;
import org.catrobat.catroid.content.MyActivityManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.SafeKeyboardHeightProvider;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenNotificationClickedScript;
import org.catrobat.catroid.content.actions.RunJSAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.WhenNotificationClickedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.devices.raspberrypi.RaspberryPiService;
import org.catrobat.catroid.exceptions.ProjectException;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.io.StageAudioFocus;
import org.catrobat.catroid.nfc.NfcHandler;
import org.catrobat.catroid.stage.event.EventManager;
import org.catrobat.catroid.ui.MarketingActivity;
import org.catrobat.catroid.ui.dialogs.StageDialog;
import org.catrobat.catroid.ui.recyclerview.dialog.PlaySceneDialog;
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog;
import org.catrobat.catroid.ui.runtimepermissions.BrickResourcesToRuntimePermissions;
import org.catrobat.catroid.ui.runtimepermissions.PermissionAdaptingActivity;
import org.catrobat.catroid.ui.runtimepermissions.PermissionHandlingActivity;
import org.catrobat.catroid.ui.runtimepermissions.PermissionRequestActivityExtension;
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask;
import org.catrobat.catroid.utils.NativeBridge;
import org.catrobat.catroid.utils.ProjectSecurityChecker;
import org.catrobat.catroid.utils.Resolution;
import org.catrobat.catroid.utils.ScreenValueHandler;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.VibrationManager;
import org.catrobat.catroid.virtualmachine.VirtualMachineManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.idling.CountingIdlingResource;

import static org.catrobat.catroid.common.Constants.SCREENSHOT_AUTOMATIC_FILE_NAME;
import static org.catrobat.catroid.stage.TestResult.TEST_RESULT_MESSAGE;
import static org.catrobat.catroid.ui.MainMenuActivity.surveyCampaign;
import static org.koin.java.KoinJavaComponent.get;

@LunoClass
public class StageActivity extends AndroidApplication implements ContextProvider,
		PermissionHandlingActivity,
		PermissionAdaptingActivity {

	public static final String TAG = StageActivity.class.getSimpleName();
	public StageListener stageListener;

	public static final int REQUEST_START_STAGE = 101;

	public static final String EXTRA_PROJECT_PATH = "EXTRA_PROJECT_PATH";

	private static final List<IntentListener> intentListeners2 = new ArrayList<>();

	public static final int REGISTER_INTENT = 0;
	private static final int PERFORM_INTENT = 1;
	public static final int SHOW_DIALOG = 2;
	public static final int SHOW_TOAST = 3;
	public static final int SHOW_LONG_TOAST = 4;

	private long backPressedTime = 0;
	private static final int BACK_PRESS_EXIT_TIMEOUT = 2000;

	StageAudioFocus stageAudioFocus;
	PendingIntent pendingIntent;
	NfcAdapter nfcAdapter;
	private static NdefMessage nfcTagMessage;
	StageDialog stageDialog;
	BrickDialogManager brickDialogManager;
	private boolean resizePossible;

	static int numberOfSpritesCloned;

	public static Handler messageHandler;
	CameraManager cameraManager;
	public VibrationManager vibrationManager;

	public static SparseArray<IntentListener> intentListeners = new SparseArray<>();
	public static Random randomGenerator = new Random();

	AndroidApplicationConfiguration configuration = null;

	public StageResourceHolder stageResourceHolder;

	private static Handler mainThreadHandler;
	public CountingIdlingResource idlingResource = new CountingIdlingResource("StageActivity");
	private PermissionRequestActivityExtension permissionRequestActivityExtension = new PermissionRequestActivityExtension();
	public static WeakReference<StageActivity> activeStageActivity;

	private FrameLayout rootLayout;
	private FrameLayout backgroundLayout;
	private FrameLayout foregroundLayout;
	private FrameLayout activeNativeLayer;
	private View gameView;

	private Map<String, View> dynamicViews = new HashMap<>();

	private FrameLayout cameraContainer;

	private Map<String, WebViewCallback> webViewCallbacks = new HashMap<>();

	private static final int EXPORT_FILE_REQUEST_CODE = 42;
	private String sourceFileToExportPath;

	public Map<String, VncClient> vncClients = new HashMap<>();
	public volatile boolean frameReadyToRender = false;

    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private android.webkit.ValueCallback<Uri[]> webViewFilePathCallback;

    public static String pendingNotificationId = null;

    public static boolean isAppPaused = false;

    public interface WebViewCallback {

		void onJavaScriptMessage(String message);
	}


	public class WebAppInterface {
		private final String viewId;

		WebAppInterface(String viewId) {
			this.viewId = viewId;
		}


		@JavascriptInterface
		public void postMessage(String message) {

			final WebViewCallback callback = webViewCallbacks.get(viewId);
			if (callback != null) {


				runOnMainThread(() -> callback.onJavaScriptMessage(message));
			}
		}
	}

	private Pixmap vmPixmap;
	private Texture vmTexture;
	private boolean newFrameAvailable = false;


    private org.catrobat.catroid.ui.workspace.WindowContainer window;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (getIntent().hasExtra(EXTRA_PROJECT_PATH)) {
			String projectPath = getIntent().getStringExtra(EXTRA_PROJECT_PATH);
			File projectDir = new File(projectPath);

			File initTxt = new File(projectDir, "init.luno.txt");
			File initBin = new File(projectDir, "init.bin");

			if (initTxt.exists() || initBin.exists()) {
				try {
					org.catrobat.catroid.utils.lunoscript.LunoScriptEngine engine =
							new org.catrobat.catroid.utils.lunoscript.LunoScriptEngine(this, null);

					engine.getInterpreter().getGlobals().define(
							"ROOT_PATH",
							new org.catrobat.catroid.utils.lunoscript.LunoValue.String(projectDir.getAbsolutePath())
					);

					String scriptCode = "";
					if (initTxt.exists()) {
						scriptCode = new String(java.nio.file.Files.readAllBytes(initTxt.toPath()));
					} else {
						scriptCode = new String(java.nio.file.Files.readAllBytes(initBin.toPath()));
					}

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        engine.execute(scriptCode);
                    }

					if (ProjectManager.getInstance().getCurrentProject() != null) {
						ProjectManager.getInstance().getCurrentProject().setDirectory(projectDir);
					}

				} catch (Exception e) {
					Log.e(TAG, "Failed to load baked project via LunoScript", e);
					Toast.makeText(this, "Error executing LunoScript: " + e.getMessage(), Toast.LENGTH_LONG).show();
					finish();
					return;
				}
			} else if (projectDir.exists() && projectDir.isDirectory()) {
				try {
					ProjectManager.getInstance().loadProject(projectDir);
				} catch (ProjectException e) {
				}
			}
		}

		super.onCreate(savedInstanceState);


		rootLayout = new FrameLayout(this);
		cameraContainer = new FrameLayout(this);
		backgroundLayout = new FrameLayout(this);
		foregroundLayout = new FrameLayout(this);


		StageLifeCycleController.stageCreate(this);
		activeStageActivity = new WeakReference<>(this);
		MyActivityManager.Companion.setStage_activity(this);

        boolean isFreeStageEnabled = this instanceof StageWorkspaceActivity;

        configuration = new AndroidApplicationConfiguration();
        configuration.r = 8;
        configuration.g = 8;
        configuration.b = 8;
        configuration.a = 8;

        gameView = initializeForView(getApplicationListener(), configuration);

        if (ProjectManager.getInstance().getCurrentProject() != null) {
            calculateScreenSizes();
        }

        injectSafeKeyboardProvider();

        if (gameView instanceof android.view.SurfaceView) {
            android.view.SurfaceView glView = (android.view.SurfaceView) gameView;

            if (isFreeStageEnabled) {
                glView.getHolder().setFormat(PixelFormat.OPAQUE);
            } else {
                glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            }
            glView.setZOrderMediaOverlay(true);
        }


		rootLayout.addView(cameraContainer);
		rootLayout.addView(backgroundLayout);
		rootLayout.addView(gameView);
		rootLayout.addView(foregroundLayout);

		activeNativeLayer = foregroundLayout;

        if (isFreeStageEnabled) {
            getWindow().setFormat(PixelFormat.TRANSLUCENT);

            FrameLayout canvas = new FrameLayout(this);
            canvas.setBackgroundColor(Color.TRANSPARENT);

            window = new org.catrobat.catroid.ui.workspace.WindowContainer(this);
            window.initWindow("StageGame", getString(R.string.stage_window_title));

            FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            window.contentFrame.addView(rootLayout, rootParams);

            float density = getResources().getDisplayMetrics().density;
            boolean isProjectLandscape = ProjectManager.getInstance().getCurrentProject().getXmlHeader().islandscapeMode();

            int width, height;
            if (isProjectLandscape) {
                width = (int) (580 * density);
                height = (int) (360 * density);
            } else {
                width = (int) (360 * density);
                height = (int) (580 * density);
            }

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int initialLeft = (screenWidth - width) / 2;
            int initialTop = (screenHeight - height) / 2;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
            params.gravity = Gravity.TOP | Gravity.START;
            params.leftMargin = initialLeft;
            params.topMargin = initialTop;
            window.setLayoutParams(params);

            window.setOnCloseClickListener(() -> {
                finish();
                return null;
            });

            canvas.addView(window);
            setContentView(canvas);

            updateStageSize(width, height);
        } else {
            setContentView(rootLayout);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    boolean visible = insets.isVisible(android.view.WindowInsets.Type.ime());
                    isKeyboardVisible = visible;
                    if (visible) {
                        int h = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
                        if (h > 200) realKeyboardHeight = h;
                    }
                } else {
                    int h = insets.getSystemWindowInsetBottom();
                    if (h > 200) {
                        isKeyboardVisible = true;
                        realKeyboardHeight = h;
                    } else if (h < 150) {
                        isKeyboardVisible = false;
                    }
                }
                return v.onApplyWindowInsets(insets);
            });
        }


        GlobalManager.Companion.setSaveScenes(true);
		GlobalManager.Companion.setStopSounds(true);
		mainThreadHandler = new Handler(Looper.getMainLooper());

		File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		if (downloadsDir.canWrite() && NativeBridge.INSTANCE.isWorking()) {
			File logFile = new File(downloadsDir, "NewCatroid_CPP_CrashLog.txt");
			NativeBridge.INSTANCE.setCrashLogPath(logFile.getAbsolutePath());
		}
		checkAndRequestPermissions();

        handleNotificationIntent(getIntent());
	}

    public static volatile boolean isKeyboardVisible = false;
    public static volatile int realKeyboardHeight = 0;

    private void handleNotificationIntent(android.content.Intent intent) {
        if (intent == null) return;

        if (intent.hasExtra("NOTIFICATION_CLICKED_ID")) {
            String clickedId = intent.getStringExtra("NOTIFICATION_CLICKED_ID");
            intent.removeExtra("NOTIFICATION_CLICKED_ID");

            if (StageActivity.getActiveStageListener() != null) {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                    StageActivity.getActiveStageListener().onNotificationClicked(clickedId);
                });
            } else {
                pendingNotificationId = clickedId;
            }
        }

        if (intent.hasExtra("NOTIFICATION_ACTION_ID")) {
            String actionId = intent.getStringExtra("NOTIFICATION_ACTION_ID");
            String draftId = intent.getStringExtra("NOTIF_DRAFT_ID");
            int notifId = intent.getIntExtra("NOTIF_ID", -1);
            boolean autoClose = intent.getBooleanExtra("AUTO_CLOSE", false);

            intent.removeExtra("NOTIFICATION_ACTION_ID");

            android.os.Bundle remoteInput = androidx.core.app.RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence("KEY_REPLY");
                if (replyText != null) {
                    org.catrobat.catroid.utils.NewCatroidNotificationManager.INSTANCE
                            .getSavedReplies().put(actionId, replyText.toString());
                }
            }

            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (autoClose) {
                if (notifId != -1) {
                    nm.cancel(notifId);
                }
            } else {
                org.catrobat.catroid.utils.NewCatroidNotificationManager.NotificationState state =
                        org.catrobat.catroid.utils.NewCatroidNotificationManager.INSTANCE.getActiveDrafts().get(draftId);
                if (state != null) {
                    org.catrobat.catroid.utils.NewCatroidNotificationManager.INSTANCE.buildAndShow(this, state);
                }
            }

            if (StageActivity.getActiveStageListener() != null) {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                    StageActivity.getActiveStageListener().onNotificationActionTriggered(actionId);
                    StageActivity.getActiveStageListener().onNotificationClicked(actionId);
                });
            } else {
                pendingNotificationId = actionId;
            }
        }
    }

	private float currentVmMouseX = 0f;
	private float currentVmMouseY = 0f;

	public static final String DEFAULT_VM_NAME = "default_vm";

	public void createAndRunVM(String memory, String cpuCores, String hdaPath, String cdromPath) {

		StringBuilder args = new StringBuilder();
		args.append("-m ").append(memory);
		args.append(" -smp ").append(cpuCores);
		args.append(" -display vnc=0.0.0.0:0 -vga std");


		if (hdaPath != null && !hdaPath.isEmpty()) {
			File hdaFile = ProjectManager.getInstance().getCurrentProject().getFile(hdaPath);
			if (hdaFile != null && hdaFile.exists()) {
				args.append(" -hda \"").append(hdaFile.getAbsolutePath()).append("\"");
			}
		}


		if (cdromPath != null && !cdromPath.isEmpty() && !cdromPath.equals("0")) {
			File cdromFile = ProjectManager.getInstance().getCurrentProject().getFile(cdromPath);
			if (cdromFile != null && cdromFile.exists()) {
				args.append(" -cdrom \"").append(cdromFile.getAbsolutePath()).append("\"");
			}
		}


		VirtualMachineManager.INSTANCE.createVM(getApplicationContext(), DEFAULT_VM_NAME, args.toString(), "", "");
	}

	public void moveVmMouseRelative(float dx, float dy, int buttonState) {
		currentVmMouseX += dx;
		currentVmMouseY += dy;

		if (stageListener != null) {
			float limitX = stageListener.getVirtualWidth() / 2f;
			float limitY = stageListener.getVirtualHeight() / 2f;

			if (currentVmMouseX > limitX) currentVmMouseX = limitX;
			if (currentVmMouseX < -limitX) currentVmMouseX = -limitX;
			if (currentVmMouseY > limitY) currentVmMouseY = limitY;
			if (currentVmMouseY < -limitY) currentVmMouseY = -limitY;

			sendVmMouseEvent(currentVmMouseX, currentVmMouseY, buttonState);
		}
	}

	public void resizeVmMonitor(int width, int height) {
		if (stageListener != null) {
			stageListener.setVmScreenSize(width, height);
		}
	}

	public int getKeysymByName(String keyName) {
		if (keyName == null) return 0;
		String key = keyName.toLowerCase().trim();

		switch (key) {
			case "esc": return 0xFF1B;
			case "enter": return 0xFF0D;
			case "backspace": return 0xFF08;
			case "tab": return 0xFF09;
			case "space": return 0x0020;

			case "up": case "arrow up": return 0xFF52;
			case "down": case "arrow down": return 0xFF54;
			case "left": case "arrow left": return 0xFF51;
			case "right": case "arrow right": return 0xFF53;

			case "f1": return 0xFFBE;
			case "f2": return 0xFFBF;
			case "f3": return 0xFFC0;
			case "f4": return 0xFFC1;
			case "f5": return 0xFFC2;
			case "f6": return 0xFFC3;
			case "f7": return 0xFFC4;
			case "f8": return 0xFFC5;
			case "f9": return 0xFFC6;
			case "f10": return 0xFFC7;
			case "f11": return 0xFFC8;
			case "f12": return 0xFFC9;

			case "shift": case "l shift": return 0xFFE1;
			case "r shift": return 0xFFE2;
			case "ctrl": case "l ctrl": return 0xFFE3;
			case "r ctrl": return 0xFFE4;
			case "alt": case "l alt": return 0xFFE9;
			case "r alt": return 0xFFEA;
			case "win": case "command": case "super": return 0xFFEB;

			case "caps lock": return 0xFFE5;
			case "num lock": return 0xFF7F;
			case "scroll lock": return 0xFF14;
			case "print screen": case "prt scr": return 0xFF61;
			case "pause": case "break": return 0xFF13;

			case "insert": case "ins": return 0xFF63;
			case "delete": case "del": return 0xFFFF;
			case "home": return 0xFF50;
			case "end": return 0xFF57;
			case "page up": case "pgup": return 0xFF55;
			case "page down": case "pgdn": return 0xFF56;

			default:
				if (key.length() == 1) return (int) key.charAt(0);
				return 0;
		}
	}


	public void reloadWithNewProject(final String newProjectPath) {

		runOnUiThread(() -> {

			try {
				ProjectManager.getInstance().loadProject(new File(newProjectPath));
			} catch (Exception e) {
				Log.e(TAG, "Failed to load project for reload: " + newProjectPath, e);
				Toast.makeText(this, "Error loading project: " + e.getMessage(), Toast.LENGTH_LONG).show();

				return;
			}



			if (stageListener != null) {
				stageListener.reloadProject(stageDialog);
			}
		});
	}

	public void stopVM() {
		VirtualMachineManager.INSTANCE.stopVM(DEFAULT_VM_NAME);
	}

	public void createHardDisk(String diskName, String diskSize) {
		if (diskName == null || diskName.isEmpty() || diskSize == null || diskSize.isEmpty()) return;


		String qemuBaseDir = new File(getFilesDir(), "qemu_x86_64").getAbsolutePath();
		File disksDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
		if (!disksDir.exists()) disksDir.mkdirs();
		String diskPath = new File(disksDir, diskName).getAbsolutePath();

		VirtualMachineManager.INSTANCE.createDiskIfNotExists(qemuBaseDir, diskPath, diskSize);
	}

	public void sendVmMouseEvent(float catroidX, float catroidY, int buttonState) {
		currentVmMouseX = catroidX;
		currentVmMouseY = catroidY;

		if(!VirtualMachineManager.INSTANCE.isWorking()) return;
		VncClient client = vncClients.get(DEFAULT_VM_NAME);
		if (client == null) return;
		if (stageListener == null) return;

		float virtualWidth = stageListener.getVirtualWidth();
		float virtualHeight = stageListener.getVirtualHeight();

		float screenX = catroidX + (virtualWidth / 2f);
		float screenY = -catroidY + (virtualHeight / 2f);

		int vmWidth = stageListener.getVmWidth();
		int vmHeight = stageListener.getVmHeight();

		int vmX = (int) ((screenX / virtualWidth) * vmWidth);
		int vmY = (int) ((screenY / virtualHeight) * vmHeight);

		vmX = Math.max(0, Math.min(vmWidth - 1, vmX));
		vmY = Math.max(0, Math.min(vmHeight - 1, vmY));

		client.sendPointerEvent(vmX, vmY, buttonState);
	}

	public void sendVmKeyEvent(int keysym, boolean isDown) {
		if(!VirtualMachineManager.INSTANCE.isWorking()) return;
		VncClient client = vncClients.get(DEFAULT_VM_NAME);
		if (client != null) {
			client.sendKeyEvent(keysym, 0, isDown);
		}
	}


	public void setVmDisplayVisible(boolean visible) {
		if (stageListener != null) {
			attachVMScreen(DEFAULT_VM_NAME);
			stageListener.setVmDisplayVisible(visible);
		}
	}


	public void launchExportFilePicker(String sourcePath, String defaultName) {

		this.sourceFileToExportPath = sourcePath;

		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_TITLE, defaultName);


		startActivityForResult(intent, EXPORT_FILE_REQUEST_CODE);
	}

	private boolean captureScheduled = false;

	public void attachVMScreen(String viewId) {
		if(!VirtualMachineManager.INSTANCE.isWorking()) return;
		runOnUiThread(() -> {



			VncClient.Observer vncObserver = new VncClient.Observer() {

				@Override
				public String onPasswordRequired() { return ""; }

				@Override
				public UserCredential onCredentialRequired() { return new UserCredential("",""); }

				@Override
				public boolean onVerifyCertificate(X509Certificate certificate) { return true; }

				@Override
				public void onGotXCutText(String text) { /* None */ }

				@Override
				public void onFramebufferUpdated() {



					frameReadyToRender = true;

					if (!captureScheduled && stageListener != null) {
						captureScheduled = true;
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
							Log.i("VNC_CAPTURE", "Requesting VM Texture capture now...");
							stageListener.captureAndSaveVmTexture();
						}, 3000);
					}
				}

				@Override
				public void onFramebufferSizeChanged(int width, int height) {
					Log.i(TAG, "VM screen size changed: " + width + "x" + height);

					if (stageListener != null) {
						stageListener.setVmScreenSize(width, height);
					}
				}

				@Override
				public void onPointerMoved(int x, int y) {  }
			};


			new Thread(() -> {
				try {


					Log.i(TAG, "Waiting for QEMU VNC server to start...");
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}

				VncClient vncClient = new VncClient(vncObserver);
				try {

					vncClients.put(viewId, vncClient);

					vncClient.configure(0, true, 5, false);
					vncClient.connect("127.0.0.1", 5900);


					while (vncClient.getConnected()) {
						vncClient.processServerMessage();
					}
				} catch (Exception e) {
					Log.e(TAG, "VNC Client thread failed", e);
				} finally {

					VncClient client = vncClients.remove(viewId);
					if (client != null) {
						client.cleanup();
					}
					Log.i(TAG, "VNC Client thread finished for viewId: " + viewId);
				}
			}).start();


		});
	}


	public void sendVMMouseEvent(String viewId, int x, int y, int buttonMask) {
		VncClient vncClient = vncClients.get(viewId);
		if (vncClient != null) {
			vncClient.sendPointerEvent(x, y, buttonMask);
		}
	}


	public void sendVMKeyEvent(String viewId, int keysym, boolean isDown) {
		VncClient vncClient = vncClients.get(viewId);
		if (vncClient != null) {

			vncClient.sendKeyEvent(keysym, 0, isDown);
		}
	}


	public void setNativesBackground() {
		runOnMainThread(() -> activeNativeLayer = backgroundLayout);
	}


	public void setNativesForeground() {
		runOnMainThread(() -> activeNativeLayer = foregroundLayout);
	}


	private void injectSafeKeyboardProvider() {
		try {

			java.lang.reflect.Field field = AndroidApplication.class.getDeclaredField("keyboardHeightProvider");


			field.setAccessible(true);


			field.set(this, new SafeKeyboardHeightProvider(this));

			Log.i(TAG, "Successfully injected SafeKeyboardHeightProvider via reflection.");

		} catch (Exception e) {


			Log.e(TAG, "Failed to inject SafeKeyboardHeightProvider via reflection. Keyboard-related crashes might occur.", e);
		}
	}



	public void createWebViewWithUrl(String viewId, String url, int x, int y, int width, int height) {

		WebView webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
		webView.addJavascriptInterface(new WebAppInterface(viewId), "Android");

		webView.setBackgroundColor(Color.TRANSPARENT);
		webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (webViewFilePathCallback != null) {
                    webViewFilePathCallback.onReceiveValue(null);
                }
                webViewFilePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (android.content.ActivityNotFoundException e) {
                    webViewFilePathCallback = null;
                    return false;
                }
                return true;
            }
        });
		webView.loadUrl(url);

        webView.setVisibility(org.catrobat.catroid.common.NativeViewBindingManager.getDefaultVisibility());


		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;


		addViewToStage(viewId, webView, params);
	}


	public void playVideo(final String viewId) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {
				((VideoView) view).start();
			}
		});
	}


	public void pauseVideo(final String viewId) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {
				if (((VideoView) view).isPlaying()) {
					((VideoView) view).pause();
				}
			}
		});
	}


	public void seekVideoTo(final String viewId, final int seconds) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {

				((VideoView) view).seekTo(seconds * 1000);
			}
		});
	}


	public float getVideoCurrentTime(final String viewId) {
		Callable<Integer> callable = () -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {
				return ((VideoView) view).getCurrentPosition();
			}
			return -1;
		};

		FutureTask<Integer> task = new FutureTask<>(callable);
		runOnUiThread(task);
		try {

			int milliseconds = task.get(1, TimeUnit.SECONDS);
			return milliseconds / 1000.0f;
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get video time for " + viewId, e);
			return -1.0f;
		}
	}


	public boolean isVideoPlaying(final String viewId) {
		Callable<Boolean> callable = () -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {
				return ((VideoView) view).isPlaying();
			}
			return false;
		};

		FutureTask<Boolean> task = new FutureTask<>(callable);
		runOnUiThread(task);
		try {
			return task.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get video playing state for " + viewId, e);
			return false;
		}
	}

    public void createContainer(String viewId, int x, int y, int width, int height) {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.TRANSPARENT);

        container.setVisibility(org.catrobat.catroid.common.NativeViewBindingManager.getDefaultVisibility());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = x;
        params.topMargin = y;

        addViewToStage(viewId, container, params);
    }

    public float getViewX(final String viewId) {
        View view = dynamicViews.get(viewId);
        if (view == null) return -1.0f;

        if (view.getLayoutParams() instanceof android.view.WindowManager.LayoutParams) {
            return (float) ((android.view.WindowManager.LayoutParams) view.getLayoutParams()).x;
        }

        if (view.getParent() != null) {
            View parent = (View) view.getParent();
            if (parent.getLayoutParams() instanceof android.view.WindowManager.LayoutParams) {
                float parentX = (float) ((android.view.WindowManager.LayoutParams) parent.getLayoutParams()).x;
                return parentX + view.getX();
            }
        }

        return view.getX();
    }

    public float getViewY(final String viewId) {
        Callable<Float> callable = () -> {
            View view = dynamicViews.get(viewId);
            if (view != null) {
                if (view.getLayoutParams() instanceof android.view.WindowManager.LayoutParams) {
                    return (float) ((android.view.WindowManager.LayoutParams) view.getLayoutParams()).y;
                }
                return view.getY();
            }
            return -1.0f;
        };

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try {
                return callable.call();
            } catch (Exception e) {
                return -1.0f;
            }
        }

        FutureTask<Float> task = new FutureTask<>(callable);
        runOnUiThread(task);
        try {
            return task.get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return -1.0f;
        }
    }

    public int getViewWidth(final String viewId) {
        View view = dynamicViews.get(viewId);
        return (view != null) ? view.getWidth() : -1;
    }

    public int getViewHeight(final String viewId) {
        View view = dynamicViews.get(viewId);
        return (view != null) ? view.getHeight() : -1;
    }


	public void createWebViewWithHtml(String viewId, String htmlContent, int x, int y, int width, int height) {

		WebView webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
		webView.addJavascriptInterface(new WebAppInterface(viewId), "Android");
		webView.getSettings().setDomStorageEnabled(true);
		webView.setBackgroundColor(Color.TRANSPARENT);
		webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (webViewFilePathCallback != null) {
                    webViewFilePathCallback.onReceiveValue(null);
                }
                webViewFilePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (android.content.ActivityNotFoundException e) {
                    webViewFilePathCallback = null;
                    return false;
                }
                return true;
            }
        });

		webView.loadDataWithBaseURL("https://", htmlContent, "text/html", "UTF-8", null);

        webView.setVisibility(org.catrobat.catroid.common.NativeViewBindingManager.getDefaultVisibility());

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;


		addViewToStage(viewId, webView, params);
	}
	public static final String STYLE_TEXT_SIZE = "textSize";
	public static final String STYLE_TEXT_COLOR = "textColor";
	public static final String STYLE_HINT_TEXT = "hintText";
	public static final String STYLE_HINT_TEXT_COLOR = "hintTextColor";
	public static final String STYLE_BACKGROUND_COLOR = "backgroundColor";
	public static final String STYLE_TEXT_ALIGNMENT = "textAlignment";
	public static final String STYLE_FONT_PATH = "fontPath";
	public static final String STYLE_INPUT_TYPE = "inputType";
	public static final String STYLE_IS_PASSWORD = "isPassword";
	public static final String STYLE_MAX_LENGTH = "maxLength";
	public static final String STYLE_CORNER_RADIUS = "cornerRadius";
	public static final String STYLE_IS_MULTI_LINE = "isMultiLine";

	public void createInputField(String viewId, UserVariable variable, String initialText, int x, int y, int width, int height, HashMap<String, String> styleOptions) {

		final EditText editText = new EditText(this);
		editText.setText(initialText);

		if (styleOptions != null) {
			GradientDrawable backgroundShape = new GradientDrawable();
			backgroundShape.setShape(GradientDrawable.RECTANGLE);

			if (styleOptions.containsKey(STYLE_CORNER_RADIUS)) {
				try {
					float radius = Float.parseFloat(styleOptions.get(STYLE_CORNER_RADIUS));
					backgroundShape.setCornerRadius(radius);
				} catch (NumberFormatException e) { /* Ignore */ }
			}



			if (styleOptions.containsKey(STYLE_BACKGROUND_COLOR)) {
				try {
					backgroundShape.setColor(Color.parseColor(styleOptions.get(STYLE_BACKGROUND_COLOR)));
				} catch (IllegalArgumentException e) { /* Ignore */ }
			} else {

				backgroundShape.setColor(Color.TRANSPARENT);
			}

			editText.setBackground(backgroundShape);


			if (styleOptions.containsKey(STYLE_TEXT_SIZE)) {
				try {
					float size = Float.parseFloat(styleOptions.get(STYLE_TEXT_SIZE));
					editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
				} catch (NumberFormatException e) { /* Ignore */ }
			}
			if (styleOptions.containsKey(STYLE_TEXT_COLOR)) {
				try {
					editText.setTextColor(Color.parseColor(styleOptions.get(STYLE_TEXT_COLOR)));
				} catch (IllegalArgumentException e) { /* Ignore */ }
			}
			if (styleOptions.containsKey(STYLE_HINT_TEXT)) {
				editText.setHint(styleOptions.get(STYLE_HINT_TEXT));
			}
			if (styleOptions.containsKey(STYLE_HINT_TEXT_COLOR)) {
				try {
					editText.setHintTextColor(Color.parseColor(styleOptions.get(STYLE_HINT_TEXT_COLOR)));
				} catch (IllegalArgumentException e) { /* Ignore */ }
			}
			if (styleOptions.containsKey(STYLE_TEXT_ALIGNMENT)) {
				String alignment = styleOptions.get(STYLE_TEXT_ALIGNMENT);
				if (alignment != null) {
					switch (alignment.toLowerCase()) {
						case "center": editText.setGravity(Gravity.CENTER); break;
						case "right": editText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); break;
						default: editText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); break;
					}
				}
			}


			if (styleOptions.containsKey(STYLE_MAX_LENGTH)) {
				try {
					int maxLength = Integer.parseInt(styleOptions.get(STYLE_MAX_LENGTH));
					if (maxLength > 0) {
						editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
					}
				} catch (NumberFormatException e) { /* Игнорируем */ }
			}



			if (Boolean.parseBoolean(styleOptions.get(STYLE_IS_PASSWORD))) {
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			} else if (styleOptions.containsKey(STYLE_INPUT_TYPE)) {
				String inputType = styleOptions.get(STYLE_INPUT_TYPE);
				if ("number".equalsIgnoreCase(inputType)) {

					editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
				} else {

					editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);





					editText.setMaxLines(Integer.MAX_VALUE);


					int currentGravity = editText.getGravity();

					editText.setGravity((currentGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
				}
			}


			if (styleOptions.containsKey(STYLE_FONT_PATH)) {
				try {

					Typeface customFont = Typeface.createFromFile(styleOptions.get(STYLE_FONT_PATH));
					editText.setTypeface(customFont);
				} catch (Exception e) {

					Log.e("StageActivity", "Failed to load font from path: " + styleOptions.get(STYLE_FONT_PATH), e);
				}
			}
		}


		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				Project project = ProjectManager.getInstance().getCurrentProject();
				if (project != null) {

					if (variable != null) {

						variable.setValue(s.toString());
					}
				}
			}
		});

        editText.setVisibility(org.catrobat.catroid.common.NativeViewBindingManager.getDefaultVisibility());

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, editText, params);
	}


	public void setWebViewCallback(String viewId, WebViewCallback callback) {
		if (callback == null) {
			webViewCallbacks.remove(viewId);
		} else {
			webViewCallbacks.put(viewId, callback);
		}
	}


	public void createGLSurfaceView(String viewId, int x, int y, int width, int height) {
		if (!NativeBridge.INSTANCE.isWorking()) return;
		if (dynamicViews.containsKey(viewId)) {
			Log.w(TAG, "View with id '" + viewId + "' already exists. Removing old one.");
			removeView(viewId);
		}




		SurfaceView glView = new SurfaceView(this);

		glView.setOnTouchListener((v, event) -> {

			NativeBridge.INSTANCE.onTouchEvent(
					viewId,
					event.getActionMasked(),
					event.getX(),
					event.getY(),
					event.getPointerId(0)
			);
			return true;
		});

		glView.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				NativeBridge.INSTANCE.onSurfaceCreated(viewId, holder.getSurface());
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				NativeBridge.INSTANCE.onSurfaceChanged(viewId, width, height);
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				NativeBridge.INSTANCE.onSurfaceDestroyed(viewId);
			}
		});


		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, glView, params);
	}


	public void attachSoToView(String viewId, String soPath) {
		if (!NativeBridge.INSTANCE.isWorking()) return;
		if (!dynamicViews.containsKey(viewId)) {
			Log.e(TAG, "Cannot attach .so: View with id '" + viewId + "' not found.");
			return;
		}
		NativeBridge.INSTANCE.attachSoToView(viewId, soPath);
	}


	public void destroyGLView(String viewId) {
		if (!NativeBridge.INSTANCE.isWorking()) return;
		removeViewFromStage(viewId);
		NativeBridge.INSTANCE.cleanupInstance(viewId);
	}


	public void removeAllNativeViews() {

		runOnUiThread(() -> {
			if (NativeBridge.INSTANCE.isWorking()) NativeBridge.INSTANCE.cleanupAllInstances();

			for (View viewToRemove : dynamicViews.values()) {
				rootLayout.removeView(viewToRemove);
				if (viewToRemove != null && viewToRemove.getParent() instanceof ViewGroup) {
					((ViewGroup) viewToRemove.getParent()).removeView(viewToRemove);
				}
			}

			dynamicViews.clear();

			webViewCallbacks.clear();
		});
	}


	public void executeJavaScript(final String viewId, final String javascriptCode) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof WebView) {
				WebView webView = (WebView) view;


				webView.evaluateJavascript(javascriptCode, null);
			} else {
				Log.w(TAG, "View with id '" + viewId + "' is not a WebView. Cannot execute JavaScript.");
			}
		});
	}



	public void createDebugView(String viewId, int color, int x, int y, int width, int height) {
		View debugView = new View(this);
		debugView.setBackgroundColor(color);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, debugView, params);
	}



	public void debugLayoutHierarchy() {
		runOnUiThread(() -> {
			String TAG = "LayoutDebugger";
			Log.d(TAG, "=============================================");
			Log.d(TAG, "====== ЗАПУСК ДИАГНОСТИКИ ИЕРАРХИИ СЛОЕВ ======");
			Log.d(TAG, "=============================================");

			if (rootLayout == null) {
				Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: rootLayout == null!");
				return;
			}

			Log.d(TAG, "Иерархия в rootLayout:");
			for (int i = 0; i < rootLayout.getChildCount(); i++) {
				View child = rootLayout.getChildAt(i);
				String childName = child.getClass().getSimpleName();
				if (child == cameraContainer) childName = "cameraContainer";
				if (child == backgroundLayout) childName = "backgroundLayout";
				if (child == gameView) childName = "gameView (LibGDX)";
				if (child == foregroundLayout) childName = "foregroundLayout";
				Log.d(TAG, "  -> Слой " + i + ": " + childName);
			}

			Log.d(TAG, "---------------------------------------------");
			Log.d(TAG, "АНАЛИЗ СЛОЯ backgroundLayout:");
			if (backgroundLayout != null) {
				Log.d(TAG, "  - Видимость: " + (backgroundLayout.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
				Log.d(TAG, "  - Размеры (ШхВ): " + backgroundLayout.getWidth() + "x" + backgroundLayout.getHeight());
				Log.d(TAG, "  - Альфа: " + backgroundLayout.getAlpha());
				Log.d(TAG, "  - Количество дочерних View: " + backgroundLayout.getChildCount());
				if (backgroundLayout.getChildCount() > 0) {
					View child = backgroundLayout.getChildAt(0);
					Log.d(TAG, "    -> Дочерний View[0]: " + child.getClass().getSimpleName());
					Log.d(TAG, "       - Размеры (ШхВ): " + child.getWidth() + "x" + child.getHeight());
				}
			} else {
				Log.e(TAG, "  - ОШИБКА: backgroundLayout == null!");
			}

			Log.d(TAG, "---------------------------------------------");
			Log.d(TAG, "АНАЛИЗ СЛОЯ foregroundLayout:");
			if (foregroundLayout != null) {
				Log.d(TAG, "  - Количество дочерних View: " + foregroundLayout.getChildCount());
				if (foregroundLayout.getChildCount() > 0) {
					View child = foregroundLayout.getChildAt(0);
					Log.d(TAG, "    -> Дочерний View[0]: " + child.getClass().getSimpleName());
				}
			}

			Log.d(TAG, "=============================================");
			Log.d(TAG, "=============== ДИАГНОСТИКА ЗАВЕРШЕНА ===============");
			Log.d(TAG, "=============================================");
		});
	}


    public void setViewPosition(final String viewId, final int x, final int y) {
        runOnUiThread(() -> {
            View view = dynamicViews.get(viewId);
            if (view != null) {
                if (view.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                    params.leftMargin = x;
                    params.topMargin = y;
                    view.setLayoutParams(params);
                } else if (view.getLayoutParams() instanceof android.view.WindowManager.LayoutParams) {
                    org.catrobat.catroid.utils.OverlayViewManager.updateOverlayPosition(viewId, x, y);
                }
            } else {
                Log.w(TAG, "Cannot set position for view '" + viewId + "'. View not found.");
            }
        });
    }


	public void createVideoPlayer(String viewId, String videoPath, int x, int y, int width, int height, boolean showControls, final boolean loopVideo, boolean isTransparent) {
		final VideoView videoView = new VideoView(this);


		if (isTransparent) {
			videoView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		}

        videoView.setZOrderOnTop(activeNativeLayer == foregroundLayout);

		if (showControls) {
			MediaController mediaController = new MediaController(this);

			videoView.setMediaController(mediaController);

			mediaController.setAnchorView(videoView);
		}

		videoView.setVideoPath(videoPath);


		videoView.setOnPreparedListener(mediaPlayer -> {
			mediaPlayer.start();
			if (loopVideo) {
				mediaPlayer.setLooping(true);
			}
		});

        videoView.setVisibility(org.catrobat.catroid.common.NativeViewBindingManager.getDefaultVisibility());

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, videoView, params);
		videoView.requestFocus();
	}


	public void removeView(String viewId) {

		removeViewFromStage(viewId);
	}



	public void addViewToStage(final String viewId, final View view, final FrameLayout.LayoutParams params) {
		removeViewFromStage(viewId);

		runOnUiThread(() -> {
			view.setLayoutParams(params);
			dynamicViews.put(viewId, view);

			activeNativeLayer.addView(view);
		});
	}



	public void removeViewFromStage(final String viewId) {
		if (dynamicViews.containsKey(viewId)) {
			runOnUiThread(() -> {
				View viewToRemove = dynamicViews.get(viewId);
				if (viewToRemove != null && viewToRemove.getParent() instanceof ViewGroup) {


					((ViewGroup) viewToRemove.getParent()).removeView(viewToRemove);
				}
				dynamicViews.remove(viewId);
			});
		}
	}

	public FrameLayout getCameraContainer() {
		return cameraContainer;
	}


	public View getViewFromStage(String viewId) {
		return dynamicViews.get(viewId);
	}


	public static void runOnMainThread(Runnable runnable) {
		if (mainThreadHandler != null) {
			mainThreadHandler.post(runnable);
		}
	}


	private void checkAndRequestPermissions() {
		List<String> permissionsNeeded = new ArrayList<>();


		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded.add(Manifest.permission.CAMERA);
		}


		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
		}


		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
			}
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
			}
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
			}

		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}
		}


		if (!permissionsNeeded.isEmpty()) {
			ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 100);
		}
	}

	@Override
	public Context getContext() {
		return this;
	}

	@Override
	public void onPause() {
        isAppPaused = true;
		StageLifeCycleController.stagePause(this);
		super.onPause();

		if (surveyCampaign != null) {
			surveyCampaign.endStageTime();

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            try {
                if (isApplicationSentToBackground(this) || !pm.isInteractive()) {
                    surveyCampaign.endAppTime(this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
		}
	}

	private boolean isApplicationSentToBackground(final Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();

        if (runningProcesses == null) {
            return false;
        }

		for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
			if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				for (String activeProcess : processInfo.pkgList) {
					if (activeProcess.equals(context.getPackageName())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public void onResume() {
        isAppPaused = false;
		StageLifeCycleController.stageResume(this);
		super.onResume();
		activeStageActivity = new WeakReference<>(this);

		if (surveyCampaign != null) {
			surveyCampaign.startAppTime(this);
			surveyCampaign.startStageTime();
		}
	}

    @Override
    public void finish() {
        ProjectManager.popProjectHistory();
        super.finish();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (NativeBridge.INSTANCE.isWorking()) NativeBridge.INSTANCE.cleanupAllInstances();


		if (ProjectManager.getInstance().getCurrentProject() != null) {
			StageLifeCycleController.stageDestroy(this);
		}

        org.catrobat.catroid.utils.OverlayViewManager.removeAllOverlays();

		RunJSAction.Companion.destroyWebView();
	}

	AndroidGraphics getGdxGraphics() {
		return graphics;
	}



	void setupAskHandler() {
		final StageActivity currentStage = this;
		messageHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message message) {
				List<Object> params = (ArrayList<Object>) message.obj;

				switch (message.what) {
					case REGISTER_INTENT:
						currentStage.queueIntent((IntentListener) params.get(0));
						break;
					case PERFORM_INTENT:
						currentStage.startQueuedIntent((Integer) params.get(0));
						break;
					case SHOW_DIALOG:
						brickDialogManager.showDialog((BrickDialogManager.DialogType) params.get(0),
								(Action) params.get(1), (String) params.get(2), (String) params.get(3), (String) params.get(4), (String) params.get(5), (String) params.get(6));
						break;
					case SHOW_TOAST:
						showToastMessage((String) params.get(0));
						break;
					case SHOW_LONG_TOAST:
						showLongToastMessage((String) params.get(0));
						break;
					default:
						Log.e(TAG, "Unhandled message in messagehandler, case " + message.what);
				}
			}
		};
	}

	public boolean dialogIsShowing() {
		return (stageDialog.isShowing() || brickDialogManager.dialogIsShowing());
	}

	private void showToastMessage(String message) {
		ToastUtil.showError(this, message);
	}

	private void showLongToastMessage(String message) {
		ToastUtil.showInfoLong(this, message);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		NfcHandler.processIntent(intent);
        setIntent(intent);

		if (nfcTagMessage != null) {
			Tag currentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			synchronized (StageActivity.class) {
				NfcHandler.writeTag(currentTag, nfcTagMessage);
				setNfcTagMessage(null);
			}
		}

        handleNotificationIntent(intent);
	}


	private boolean projectHasScriptOfType(Class<? extends Script> scriptClass) {
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project == null || scriptClass == null) {
			return false;
		}
		for (Scene scene : project.getSceneList()) {
			for (Sprite sprite : scene.getSpriteList()) {
				for (Script script : sprite.getScriptList()) {
					if (scriptClass.isInstance(script)) {
						return true;
					}
				}
			}
		}
		return false;
	}


	private void broadcastEventToAllSprites(EventId eventId) {
		Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
		if (scene == null) {
			return;
		}

		for (Sprite sprite : scene.getSpriteList()) {
			Multimap<EventId, ScriptSequenceAction> eventMap = sprite.getIdToEventThreadMap();
			if (eventMap != null && eventMap.containsKey(eventId)) {

				for (ScriptSequenceAction sequence : eventMap.get(eventId)) {
					sequence.restart();
					sprite.look.addAction(sequence);
				}
			}
		}
	}

    public void captureScreenWithNativeViews(final String screenshotPath, final String filename, final ScreenshotSaverCallback callback) {
        if (!(gameView instanceof SurfaceView)) {
            if (callback != null) callback.screenshotSaved(false);
            return;
        }

        final SurfaceView surfaceView = (SurfaceView) gameView;
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();

        if (width <= 0 || height <= 0) {
            if (callback != null) callback.screenshotSaved(false);
            return;
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.os.HandlerThread handlerThread = new android.os.HandlerThread("PixelCopyThread");
            handlerThread.start();

            android.view.PixelCopy.request(surfaceView, bitmap, copyResult -> {
                handlerThread.quitSafely();
                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    runOnUiThread(() -> {
                        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

                        if (backgroundLayout != null) {
                            backgroundLayout.draw(canvas);
                        }
                        if (foregroundLayout != null) {
                            foregroundLayout.draw(canvas);
                        }

                        new Thread(() -> {
                            boolean success = saveBitmapToPng(bitmap, new File(screenshotPath, filename));
                            bitmap.recycle();
                            if (callback != null) {
                                runOnUiThread(() -> callback.screenshotSaved(success));
                            }
                        }).start();
                    });
                } else {
                    bitmap.recycle();
                    Log.e("StageActivity", "PixelCopy failed with error code: " + copyResult);
                    if (callback != null) {
                        runOnUiThread(() -> callback.screenshotSaved(false));
                    }
                }
            }, new android.os.Handler(handlerThread.getLooper()));
        } else {
            if (stageListener != null) {
                stageListener.requestTakingScreenshot(filename, callback);
            } else if (callback != null) {
                callback.screenshotSaved(false);
            }
        }
    }

    private boolean saveBitmapToPng(Bitmap bitmap, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                return true;
            }
        } catch (Exception e) {
            Log.e("StageActivity", "Failed to save bitmap to PNG", e);
            return false;
        }
    }


	public static StageListener getActiveStageListener() {
		if (activeStageActivity == null) {
			return null;
		}

		StageActivity currentStage = activeStageActivity.get();

		if (currentStage != null && currentStage.stageListener != null) {
			return currentStage.stageListener;
		}

		return null;
	}

	@Override
	public void onBackPressed() {
        if (this instanceof StageWorkspaceActivity && window != null && window.isMaximized()) {
            window.toggleMaximize();
            return;
        }

		Project currentProject = ProjectManager.getInstance().getCurrentProject();

		boolean backPressedScriptExists = EventManager.projectHasScriptOfType(
				currentProject, BackPressedScript.class);

		if (backPressedScriptExists) {
			if (backPressedTime + BACK_PRESS_EXIT_TIMEOUT > System.currentTimeMillis()) {
				handleBack();
			} else {
				broadcastEventToAllSprites(new EventId(EventId.BACK_PRESSED));
				Toast.makeText(this, getString(R.string.stage_press_again_for_menu), Toast.LENGTH_SHORT).show();
				backPressedTime = System.currentTimeMillis();
			}
		} else {
			handleBack();
		}
	}

	private void handleBack() {
		if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
			//BluetoothDeviceService service = ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE);


			//TextToSpeechHolder.getInstance().deleteSpeechFiles();
			//Intent marketingIntent = new Intent(this, MarketingActivity.class);
			//startActivity(marketingIntent);
			//finish();
		} else {
			StageLifeCycleController.stagePause(this);
			idlingResource.increment();
			stageListener.requestTakingScreenshot(SCREENSHOT_AUTOMATIC_FILE_NAME,
					success -> runOnUiThread(() -> idlingResource.decrement()));
			stageDialog.show();
		}
	}

	public void manageLoadAndFinish() {
		stageListener.pause();
		stageListener.finish();

		TextToSpeechHolder.getInstance().shutDownTextToSpeech();
		get(SpeechRecognitionHolderFactory.class).getInstance().destroy();

		BluetoothDeviceService service = ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE);
		if (service != null) {
			service.pause();
		}

		RaspberryPiService.getInstance().disconnect();
	}

	public static CameraManager getActiveCameraManager() {
		if (activeStageActivity != null) {
			return activeStageActivity.get().cameraManager;
		}
		return null;
	}

	public static VibrationManager getActiveVibrationManager() {
		if (activeStageActivity != null) {
			return activeStageActivity.get().vibrationManager;
		}
		return null;
	}

	public boolean isResizePossible() {
		return resizePossible;
	}

    void calculateScreenSizes() {
        ScreenValueHandler.updateScreenWidthAndHeight(getContext());

        Resolution projectResolution = new Resolution(
                ProjectManager.getInstance().getCurrentProject().getXmlHeader().getVirtualScreenWidth(),
                ProjectManager.getInstance().getCurrentProject().getXmlHeader().getVirtualScreenHeight());

        ScreenValues.currentScreenResolution =
                ScreenValues.currentScreenResolution.flipToFit(projectResolution);

        resizePossible = !ScreenValues.currentScreenResolution.sameRatioOrMeasurements(projectResolution) &&
                !ProjectManager.getInstance().getCurrentProject().isCastProject();

        if (resizePossible) {
            stageListener.setMaxViewPort(projectResolution.resizeToFit(ScreenValues.currentScreenResolution));
        } else {
            stageListener.setMaxViewPort(ScreenValues.currentScreenResolution);
        }
    }

    @Override
	public ApplicationListener getApplicationListener() {

		if (this.stageListener == null) {


			this.stageListener = new StageListener();
		}
		return this.stageListener;
	}

	@Override
	public void log(String tag, String message, Throwable exception) {
		Log.d(tag, message, exception);
	}

	@Override
	public int getLogLevel() {
		return 0;
	}

	//for running Asynchronous Tasks from the stage
	public void post(Runnable r) {
		handler.post(r);
	}

	public void jsDestroy() {
		stageListener.finish();
		manageLoadAndFinish();
		exit();
	}

	public static int getAndIncrementNumberOfClonedSprites() {
		return ++numberOfSpritesCloned;
	}

	public static void resetNumberOfClonedSprites() {
		numberOfSpritesCloned = 0;
	}

	public static void setNfcTagMessage(NdefMessage message) {
		nfcTagMessage = message;
	}

	public static NdefMessage getNfcTagMessage() {
		return nfcTagMessage;
	}

	public synchronized void queueIntent(IntentListener asker) {
		if (StageActivity.messageHandler == null) {
			return;
		}
		int newIdentId;
		do {
			newIdentId = StageActivity.randomGenerator.nextInt(Integer.MAX_VALUE);
		} while (intentListeners.indexOfKey(newIdentId) >= 0);

		intentListeners.put(newIdentId, asker);
		ArrayList<Object> params = new ArrayList<>();
		params.add(newIdentId);
		Message message = StageActivity.messageHandler.obtainMessage(StageActivity.PERFORM_INTENT, params);
		message.sendToTarget();
	}

	private void startQueuedIntent(int intentKey) {
		if (intentListeners.indexOfKey(intentKey) < 0) {
			return;
		}
		Intent queuedIntent = intentListeners.get(intentKey).getTargetIntent();
		if (queuedIntent == null) {
			return;
		}
		Package pack = this.getClass().getPackage();
		if (pack != null) {
			queuedIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, pack.getName());
		}
		this.startActivityForResult(queuedIntent, intentKey);
	}

	public static void addIntentListener(IntentListener listener) {
		intentListeners2.add(listener);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == EXPORT_FILE_REQUEST_CODE) {

			if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
				Uri destinationUri = data.getData();
				File sourceFile = new File(sourceFileToExportPath);


				try (InputStream in = new FileInputStream(sourceFile);
					 OutputStream out = getContentResolver().openOutputStream(destinationUri)) {

					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
                        assert out != null;
                        out.write(buf, 0, len);
					}

				} catch (IOException e) {
					Log.e(TAG, "Ошибка экспорта файла", e);
					Toast.makeText(this, getString(R.string.stage_export_error, e.getMessage()), Toast.LENGTH_SHORT).show();
				}
			}

			sourceFileToExportPath = null;
		}

		for (IntentListener listener : intentListeners2) {
			if (listener.onIntentResult(requestCode, resultCode, data)) {
				return;
			}
		}

		if (resultCode == TestResult.STAGE_ACTIVITY_TEST_SUCCESS
				|| resultCode == TestResult.STAGE_ACTIVITY_TEST_FAIL) {
			String message = data.getStringExtra(TEST_RESULT_MESSAGE);
			ToastUtil.showError(this, message);
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			ClipData testResult = ClipData.newPlainText("TestResult",
					ProjectManager.getInstance().getCurrentProject().getName() + "\n" + message);
			clipboard.setPrimaryClip(testResult);
		}

		if (intentListeners.indexOfKey(requestCode) >= 0) {
			IntentListener asker = intentListeners.get(requestCode);
			if (data != null) {
				asker.onIntentResult(requestCode, resultCode, data);
			}
			intentListeners.remove(requestCode);
		} else {
			stageResourceHolder.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void adaptToDeniedPermissions(List<String> deniedPermissions) {
		Brick.ResourcesSet requiredResources = new Brick.ResourcesSet();
		Project project = ProjectManager.getInstance().getCurrentProject();

		for (Scene scene: project.getSceneList()) {
			for (Sprite sprite : scene.getSpriteList()) {
				for (Brick brick : sprite.getAllBricks()) {
					brick.addRequiredResources(requiredResources);
					List<String> requiredPermissions = BrickResourcesToRuntimePermissions.translate(requiredResources);
					requiredPermissions.retainAll(deniedPermissions);

					if (!requiredPermissions.isEmpty()) {
						brick.setCommentedOut(true);
					}
					requiredResources.clear();
				}
			}
		}
	}

	public interface IntentListener {
		Intent getTargetIntent();
		boolean onIntentResult(int requestCode, int resultCode, Intent data);
	}

	@Override
	public void addToRequiresPermissionTaskList(RequiresPermissionTask task) {
		permissionRequestActivityExtension.addToRequiresPermissionTaskList(task);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 100) {
			boolean allGranted = true;

			for (int result : grantResults) {
				if (result != PackageManager.PERMISSION_GRANTED) {
					allGranted = false;
					break;
				}
			}


		} else {
			permissionRequestActivityExtension.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
		}
	}

	private static final String PREFS_NAME = "SecurityPreferences";
	private static final String PREFS_KEY_SUPPRESS_WARNING = "suppress_security_warning_new";

	public static void handlePlayButton(ProjectManager projectManager, final Activity activity) {
		Project project = projectManager.getCurrentProject();


		boolean isDangerous = ProjectSecurityChecker.projectContainsDangerousBricks(project);


		SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		boolean shouldSuppressWarning = prefs.getBoolean(PREFS_KEY_SUPPRESS_WARNING, false);


		if (isDangerous && !shouldSuppressWarning) {
			showSecurityWarningDialog(projectManager, activity);
		} else {

			launchProject(projectManager, activity);
		}
	}

	private static void showSecurityWarningDialog(ProjectManager projectManager, Activity activity) {
		new AlertDialog.Builder(activity)
				.setTitle(R.string.stage_warning_title)
				.setMessage(R.string.stage_warning_msg)
				.setCancelable(false)
				.setIcon(android.R.drawable.ic_dialog_alert)


				.setPositiveButton(R.string.stage_run, (dialog, which) -> {
					dialog.dismiss();
					launchProject(projectManager, activity);
				})


				.setNegativeButton(R.string.cancel, (dialog, which) -> {
					dialog.dismiss();
				})


				.setNeutralButton(R.string.stage_dont_remind_again, (dialog, which) -> {

					SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(PREFS_KEY_SUPPRESS_WARNING, true);
					editor.apply();

					dialog.dismiss();
					launchProject(projectManager, activity);
				})
				.show();
	}

	private static void launchProject(ProjectManager projectManager, final Activity activity) {
        Project project = projectManager.getCurrentProject();
        if (project == null) return;

        Scene currentScene = projectManager.getCurrentlyEditedScene();
        Scene defaultScene = project.getDefaultScene();

        if (currentScene == null || defaultScene == null) {
            return;
        }

		if (currentScene.getName().equals(defaultScene.getName())) {
			projectManager.setCurrentlyPlayingScene(defaultScene);
			projectManager.setStartScene(defaultScene);
			startStageActivity(activity);
		} else {
			new PlaySceneDialog.Builder(activity)
					.setPositiveButton(R.string.play, (dialog, which) -> startStageActivity(activity))
					.create()
					.show();
		}
	}

    public static void handleAiButton() {
        StageActivity stageActivity = activeStageActivity.get();
        if (stageActivity == null || stageActivity.isFinishing()) {
            return;
        }

        View view = View.inflate(stageActivity, R.layout.dialog_ai_assist, null);

        TextInputDialog.Builder builder = new TextInputDialog.Builder(stageActivity);
        builder.setPositiveButton("Ok", (TextInputDialog.OnClickListener) (dialog, textInput) -> {
            Log.d("ab", textInput);
        });

        final AlertDialog alertDialog = builder.setTitle(R.string.ai_assist)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        alertDialog.show();
    }

	private static void startStageActivity(Activity activity) {
        boolean isFreeStageEnabled = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean("pref_workspace_stage", false);

        Intent intent;
        if (isFreeStageEnabled) {
            intent = new Intent(activity, StageWorkspaceActivity.class);
        } else {
            intent = new Intent(activity, StageActivity.class);
        }
        activity.startActivityForResult(intent, StageActivity.REQUEST_START_STAGE);
	}

	public static void finishStage() {
		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null && !stageActivity.isFinishing()) {
			stageActivity.finish();
		}
	}

	public static void finishTestWithResult(TestResult testResult) {
		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null && !stageActivity.isFinishing()) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra(TEST_RESULT_MESSAGE, testResult.getMessage());
			stageActivity.setResult(testResult.getResultCode(), resultIntent);
			stageActivity.finish();
		}
	}

    public void updateStageSize(int width, int height) {
        if (gameView instanceof android.view.SurfaceView) {
            android.view.SurfaceView glView = (android.view.SurfaceView) gameView;
        }
    }
}
