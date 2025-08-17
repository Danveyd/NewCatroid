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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
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
import com.badlogic.gdx.scenes.scene2d.Action;
import com.danvexteam.lunoscript_annotations.LunoClass;
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
import org.catrobat.catroid.content.actions.RunJSAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.devices.raspberrypi.RaspberryPiService;
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
import org.catrobat.catroid.utils.ProjectSecurityChecker;
import org.catrobat.catroid.utils.Resolution;
import org.catrobat.catroid.utils.ScreenValueHandler;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.VibrationManager;

import java.lang.ref.WeakReference;
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
	public static StageListener stageListener;

	public static final int REQUEST_START_STAGE = 101;

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

	private FrameLayout rootLayout;       // Главный контейнер для всего
	private FrameLayout backgroundLayout; // Слой для View ЗА сценой LibGDX
	private FrameLayout foregroundLayout; // Слой для View ПЕРЕД сценой LibGDX
	private FrameLayout activeNativeLayer; // Указывает, куда добавлять View сейчас
	private View gameView;         // View для LibGDX сцены
	// Карта для хранения всех динамически добавленных View по их ID
	private Map<String, View> dynamicViews = new HashMap<>();

	private FrameLayout cameraContainer;

	private Map<String, WebViewCallback> webViewCallbacks = new HashMap<>();

	/**
	 * Публичный интерфейс, который нужно реализовать для получения сообщений из WebView.
	 */
	public interface WebViewCallback {
		/**
		 * Вызывается, когда из JavaScript приходит сообщение через Android.postMessage().
		 * @param message Данные, переданные из JavaScript в виде строки.
		 */
		void onJavaScriptMessage(String message);
	}

	/**
	 * Это класс-"мост", экземпляр которого будет доступен в JavaScript под именем "Android".
	 */
	public class WebAppInterface {
		private final String viewId;

		WebAppInterface(String viewId) {
			this.viewId = viewId;
		}

		/**
		 * Метод, который можно будет вызывать из JavaScript: Android.postMessage("какие-то данные");
		 * @param message Строка данных из WebView.
		 */
		@JavascriptInterface
		public void postMessage(String message) {
			// Ищем, был ли для этого WebView установлен обработчик
			final WebViewCallback callback = webViewCallbacks.get(viewId);
			if (callback != null) {
				// Выполняем колбэк в основном потоке, чтобы избежать проблем
				// при работе с UI или переменными проекта.
				runOnMainThread(() -> callback.onJavaScriptMessage(message));
			}
		}
	}

	// ИЗМЕНИТЬ: Полностью заменяем onCreate
	// ИЗМЕНИТЬ: в StageActivity.java

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// --- Иерархия слоев ---
		rootLayout = new FrameLayout(this);
		cameraContainer = new FrameLayout(this);
		backgroundLayout = new FrameLayout(this);
		foregroundLayout = new FrameLayout(this);

		// --- Логика Catroid ---
		StageLifeCycleController.stageCreate(this);
		activeStageActivity = new WeakReference<>(this);
		MyActivityManager.Companion.setStage_activity(this);

		// --- Инициализация LibGDX ---
		configuration = new AndroidApplicationConfiguration();
		configuration.r = 8;
		configuration.g = 8;
		configuration.b = 8;
		configuration.a = 8;

		gameView = initializeForView(getApplicationListener(), configuration);

		injectSafeKeyboardProvider();

		// --- НАСТРОЙКА ПРОЗРАЧНОСТИ (КЛЮЧЕВОЙ МОМЕНТ) ---
		if (gameView instanceof android.view.SurfaceView) {
			android.view.SurfaceView glView = (android.view.SurfaceView) gameView;
			// Эта команда делает SurfaceView способным иметь прозрачные пиксели
			glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

			// УДАЛЯЕМ ЭТУ СТРОКУ! Она ломала композицию слоев.
			// glView.setZOrderMediaOverlay(true);
		}

		// --- Собираем финальную иерархию View ---
		rootLayout.addView(cameraContainer);
		rootLayout.addView(backgroundLayout);
		rootLayout.addView(gameView);
		rootLayout.addView(foregroundLayout);

		activeNativeLayer = foregroundLayout;

		// --- Устанавливаем контент ---
		setContentView(rootLayout);

		// --- Остальная логика ---
		GlobalManager.Companion.setSaveScenes(true);
		GlobalManager.Companion.setStopSounds(true);
		mainThreadHandler = new Handler(Looper.getMainLooper());
		checkAndRequestPermissions();
	}

	/**
	 * Устанавливает режим, при котором все последующие нативные View
	 * будут добавляться ЗА сценой LibGDX.
	 */
	public void setNativesBackground() {
		runOnMainThread(() -> activeNativeLayer = backgroundLayout);
	}

	/**
	 * Устанавливает режим, при котором все последующие нативные View
	 * будут добавляться ПЕРЕД сценой LibGDX (режим по умолчанию).
	 */
	public void setNativesForeground() {
		runOnMainThread(() -> activeNativeLayer = foregroundLayout);
	}

	// Поместите этот метод в любое место внутри класса StageActivity
	private void injectSafeKeyboardProvider() {
		try {
			// 1. Получаем доступ к полю `keyboardHeightProvider` родительского класса AndroidApplication
			java.lang.reflect.Field field = AndroidApplication.class.getDeclaredField("keyboardHeightProvider");

			// 2. Делаем его доступным для записи (обходя private/protected)
			field.setAccessible(true);

			// 3. Создаем наш безопасный объект и записываем его в это поле
			field.set(this, new SafeKeyboardHeightProvider(this));

			Log.i(TAG, "Successfully injected SafeKeyboardHeightProvider via reflection.");

		} catch (Exception e) {
			// Если что-то пошло не так (например, поле переименовали в другой версии LibGDX),
			// мы увидим это в логах, но приложение не упадет в этом месте.
			Log.e(TAG, "Failed to inject SafeKeyboardHeightProvider via reflection. Keyboard-related crashes might occur.", e);
		}
	}

	/**
	 * Создает и отображает WebView с загрузкой по URL.
	 *
	 * @param viewId Уникальный строковый ID для этого WebView (например, "wiki-page").
	 *               Используйте этот ID позже для удаления.
	 * @param url    URL-адрес, который нужно загрузить.
	 * @param x      Позиция по горизонтали от левого края экрана в пикселях.
	 * @param y      Позиция по вертикали от верхнего края экрана в пикселях.
	 * @param width  Ширина WebView в пикселях.
	 * @param height Высота WebView в пикселях.
	 */
	public void createWebViewWithUrl(String viewId, String url, int x, int y, int width, int height) {
		// Создаем WebView и настраиваем его
		WebView webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true); // Включаем JavaScript
		webView.addJavascriptInterface(new WebAppInterface(viewId), "Android");
		// Это важно, чтобы ссылки открывались внутри WebView, а не в браузере
		webView.setBackgroundColor(Color.TRANSPARENT);
		webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		webView.setWebViewClient(new WebViewClient());
		webView.loadUrl(url);

		// Создаем параметры макета для точного позиционирования
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START; // Устанавливаем точку отсчета в левый верхний угол
		params.leftMargin = x;
		params.topMargin = y;

		// Используем наш универсальный метод для добавления View на сцену
		addViewToStage(viewId, webView, params);
	}

	/**
	 * Запускает или возобновляет воспроизведение видео.
	 * @param viewId ID видеоплеера, который нужно запустить.
	 */
	public void playVideo(final String viewId) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {
				((VideoView) view).start();
			}
		});
	}

	/**
	 * Ставит видео на паузу.
	 * @param viewId ID видеоплеера, который нужно поставить на паузу.
	 */
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

	/**
	 * Перематывает видео на указанное время.
	 * @param viewId ID видеоплеера.
	 * @param seconds Время в секундах, на которое нужно перемотать.
	 */
	public void seekVideoTo(final String viewId, final int seconds) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof VideoView) {
				// VideoView принимает время в миллисекундах
				((VideoView) view).seekTo(seconds * 1000);
			}
		});
	}

	/**
	 * Возвращает текущее время воспроизведения видео в секундах.
	 * ВНИМАНИЕ: Этот метод блокирует текущий поток, пока не получит ответ от UI-потока.
	 * @param viewId ID видеоплеера.
	 * @return Текущее время в секундах (с плавающей точкой) или -1.0f, если плеер не найден.
	 */
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
			// Ждем результат от UI-потока, но не дольше 1 секунды
			int milliseconds = task.get(1, TimeUnit.SECONDS);
			return milliseconds / 1000.0f; // Преобразуем в секунды
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get video time for " + viewId, e);
			return -1.0f;
		}
	}

	/**
	 * Проверяет, проигрывается ли видео в данный момент.
	 * ВНИМАНИЕ: Блокирующий вызов.
	 * @param viewId ID видеоплеера.
	 * @return true, если видео играет, иначе false.
	 */
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


// --- ОБЩИЕ МЕТОДЫ ДЛЯ ЛЮБОГО View ---

	/**
	 * Возвращает X-координату View относительно левого края.
	 * ВНИМАНИЕ: Блокирующий вызов.
	 * @param viewId ID любого View на сцене.
	 * @return Координата X в пикселях или -1.0f, если View не найден.
	 */
	public float getViewX(final String viewId) {
		Callable<Float> callable = () -> {
			View view = dynamicViews.get(viewId);
			return (view != null) ? view.getX() : -1.0f;
		};

		FutureTask<Float> task = new FutureTask<>(callable);
		runOnUiThread(task);
		try {
			return task.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get X for " + viewId, e);
			return -1.0f;
		}
	}

	/**
	 * Возвращает Y-координату View относительно верхнего края.
	 * ВНИМАНИЕ: Блокирующий вызов.
	 * @param viewId ID любого View на сцене.
	 * @return Координата Y в пикселях или -1.0f, если View не найден.
	 */
	public float getViewY(final String viewId) {
		Callable<Float> callable = () -> {
			View view = dynamicViews.get(viewId);
			return (view != null) ? view.getY() : -1.0f;
		};

		FutureTask<Float> task = new FutureTask<>(callable);
		runOnUiThread(task);
		try {
			return task.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get Y for " + viewId, e);
			return -1.0f;
		}
	}

	/**
	 * Возвращает ширину View в пикселях.
	 * ВНИМАНИЕ: Блокирующий вызов.
	 * @param viewId ID любого View на сцене.
	 * @return Ширина в пикселях или -1, если View не найден.
	 */
	public int getViewWidth(final String viewId) {
		Callable<Integer> callable = () -> {
			View view = dynamicViews.get(viewId);
			return (view != null) ? view.getWidth() : -1;
		};

		FutureTask<Integer> task = new FutureTask<>(callable);
		runOnUiThread(task);
		try {
			return task.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get width for " + viewId, e);
			return -1;
		}
	}

	/**
	 * Возвращает высоту View в пикселях.
	 * ВНИМАНИЕ: Блокирующий вызов.
	 * @param viewId ID любого View на сцене.
	 * @return Высота в пикселях или -1, если View не найден.
	 */
	public int getViewHeight(final String viewId) {
		Callable<Integer> callable = () -> {
			View view = dynamicViews.get(viewId);
			return (view != null) ? view.getHeight() : -1;
		};

		FutureTask<Integer> task = new FutureTask<>(callable);
		runOnUiThread(task);
		try {
			return task.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			Log.e("StageActivity", "Failed to get height for " + viewId, e);
			return -1;
		}
	}

	/**
	 * Создает и отображает WebView с отображением HTML-кода из строки.
	 *
	 * @param viewId      Уникальный строковый ID для этого WebView (например, "welcome-message").
	 * @param htmlContent Строка, содержащая полный HTML-код для отображения.
	 * @param x           Позиция по горизонтали от левого края экрана в пикселях.
	 * @param y           Позиция по вертикали от верхнего края экрана в пикселях.
	 * @param width       Ширина WebView в пикселях.
	 * @param height      Высота WebView в пикселях.
	 */
	public void createWebViewWithHtml(String viewId, String htmlContent, int x, int y, int width, int height) {
		// Создаем и настраиваем WebView
		WebView webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true); // JavaScript все еще нужен
		webView.addJavascriptInterface(new WebAppInterface(viewId), "Android");
		webView.getSettings().setDomStorageEnabled(true); // Полезно для современных сайтов
		webView.setBackgroundColor(Color.TRANSPARENT);
		webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		// --- НАЧАЛО КЛЮЧЕВОГО ИЗМЕНЕНИЯ ---

		// БЫЛО:
		// webView.loadData(htmlContent, "text/html; charset=utf-8", "UTF-8");

		// СТАЛО:
		// Мы сообщаем WebView, что базовый URL для этого HTML - "https://".
		// Это дает ему разрешение загружать другие ресурсы (CSS, картинки, шрифты) из интернета.
		webView.loadDataWithBaseURL("https://", htmlContent, "text/html", "UTF-8", null);

		// --- КОНЕЦ КЛЮЧЕВОГО ИЗМЕНЕНИЯ ---


		// Создаем параметры макета
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		// Добавляем View на сцену
		addViewToStage(viewId, webView, params);
	}
	public static final String STYLE_TEXT_SIZE = "textSize";       // Размер текста в SP (например, "18")
	public static final String STYLE_TEXT_COLOR = "textColor";     // Цвет текста в HEX (например, "#FF0000")
	public static final String STYLE_HINT_TEXT = "hintText";       // Текст-подсказка (placeholder)
	public static final String STYLE_HINT_TEXT_COLOR = "hintTextColor"; // Цвет подсказки в HEX
	public static final String STYLE_BACKGROUND_COLOR = "backgroundColor"; // Цвет фона в HEX
	public static final String STYLE_TEXT_ALIGNMENT = "textAlignment";   // Выравнивание
	public static final String STYLE_FONT_PATH = "fontPath";             // Путь к файлу шрифта (*.ttf или *.otf)
	public static final String STYLE_INPUT_TYPE = "inputType";           // Тип ввода: "text" (по умолч.), "number"
	public static final String STYLE_IS_PASSWORD = "isPassword";         // "true", если поле для пароля
	public static final String STYLE_MAX_LENGTH = "maxLength";           // Макс. длина текста (например, "50")
	public static final String STYLE_CORNER_RADIUS = "cornerRadius";     // Радиус скругления углов в пикселях
	public static final String STYLE_IS_MULTI_LINE = "isMultiLine";
	/**
	 * Создает текстовое поле для ввода, связанное с переменной проекта.
	 *
	 * @param viewId        Уникальный ID для этого поля.
	 * @param variable      переменная проекта (UserVariable), которая будет обновляться.
	 * @param initialText   Начальный текст в поле.
	 * @param x             Позиция X.
	 * @param y             Позиция Y.
	 * @param width         Ширина.
	 * @param height        Высота.
	 * @param styleOptions  Карта (HashMap) с опциями для стилизации. Может быть null.
	 */
	public void createInputField(String viewId, UserVariable variable, String initialText, int x, int y, int width, int height, HashMap<String, String> styleOptions) {
		// Создаем EditText
		final EditText editText = new EditText(this);
		editText.setText(initialText);

		// --- НАСТРОЙКА СТИЛЕЙ ---
		if (styleOptions != null) {

			// --- НОВОЕ: Закругленные углы и фон ---
			// Создаем Drawable, который будем использовать для фона.
			// Это позволяет нам задать и цвет, и скругление углов.
			GradientDrawable backgroundShape = new GradientDrawable();
			backgroundShape.setShape(GradientDrawable.RECTANGLE);

			// Устанавливаем радиус скругления, если он указан
			if (styleOptions.containsKey(STYLE_CORNER_RADIUS)) {
				try {
					float radius = Float.parseFloat(styleOptions.get(STYLE_CORNER_RADIUS));
					backgroundShape.setCornerRadius(radius);
				} catch (NumberFormatException e) { /* Игнорируем */ }
			}

			// Устанавливаем цвет фона. Если есть скругление, цвет применится к фигуре,
			// иначе - будет просто заливка.
			if (styleOptions.containsKey(STYLE_BACKGROUND_COLOR)) {
				try {
					backgroundShape.setColor(Color.parseColor(styleOptions.get(STYLE_BACKGROUND_COLOR)));
				} catch (IllegalArgumentException e) { /* Игнорируем */ }
			} else {
				// Если цвет не указан, делаем фон прозрачным, чтобы фигура не имела цвета по умолчанию
				backgroundShape.setColor(Color.TRANSPARENT);
			}
			// Применяем наш созданный фон к EditText
			editText.setBackground(backgroundShape);

			// --- Стандартные стили ---
			if (styleOptions.containsKey(STYLE_TEXT_SIZE)) {
				try {
					float size = Float.parseFloat(styleOptions.get(STYLE_TEXT_SIZE));
					editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
				} catch (NumberFormatException e) { /* Игнорируем */ }
			}
			if (styleOptions.containsKey(STYLE_TEXT_COLOR)) {
				try {
					editText.setTextColor(Color.parseColor(styleOptions.get(STYLE_TEXT_COLOR)));
				} catch (IllegalArgumentException e) { /* Игнорируем */ }
			}
			if (styleOptions.containsKey(STYLE_HINT_TEXT)) {
				editText.setHint(styleOptions.get(STYLE_HINT_TEXT));
			}
			if (styleOptions.containsKey(STYLE_HINT_TEXT_COLOR)) {
				try {
					editText.setHintTextColor(Color.parseColor(styleOptions.get(STYLE_HINT_TEXT_COLOR)));
				} catch (IllegalArgumentException e) { /* Игнорируем */ }
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

			// --- НОВОЕ: Максимальная длина текста ---
			if (styleOptions.containsKey(STYLE_MAX_LENGTH)) {
				try {
					int maxLength = Integer.parseInt(styleOptions.get(STYLE_MAX_LENGTH));
					if (maxLength > 0) {
						editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
					}
				} catch (NumberFormatException e) { /* Игнорируем */ }
			}

			// --- НОВОЕ: Тип ввода и режим пароля ---
			// Режим пароля имеет приоритет над типом ввода
			if (Boolean.parseBoolean(styleOptions.get(STYLE_IS_PASSWORD))) {
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			} else if (styleOptions.containsKey(STYLE_INPUT_TYPE)) {
				String inputType = styleOptions.get(STYLE_INPUT_TYPE);
				if ("number".equalsIgnoreCase(inputType)) {
					// Разрешает ввод только цифр (включая знак и дробную часть)
					editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
				} else {
					// Стандартный текстовый ввод
					editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

					// 2. Убираем горизонтальную прокрутку, чтобы текст переносился
					//editText.setHorizontallyScrolling(false);

					// 3. Устанавливаем максимальное количество строк (можно большое число)
					editText.setMaxLines(Integer.MAX_VALUE);

					// 4. Важно! Выравниваем текст по верху для многострочного режима
					int currentGravity = editText.getGravity();
					// Убираем вертикальное центрирование и добавляем выравнивание по верху
					editText.setGravity((currentGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
				}
			}

			// --- НОВОЕ: Кастомный шрифт ---
			if (styleOptions.containsKey(STYLE_FONT_PATH)) {
				try {
					// Пытаемся создать шрифт из файла
					Typeface customFont = Typeface.createFromFile(styleOptions.get(STYLE_FONT_PATH));
					editText.setTypeface(customFont);
				} catch (Exception e) {
					// Если файл не найден или поврежден, ничего не делаем, будет использован шрифт по умолчанию
					Log.e("StageActivity", "Failed to load font from path: " + styleOptions.get(STYLE_FONT_PATH), e);
				}
			}
		}

		// --- СВЯЗЬ С ПЕРЕМЕННОЙ CATROID ---
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				Project project = ProjectManager.getInstance().getCurrentProject();
				if (project != null) {
					//UserVariable userVar = project.getUserVariable(variableName);
					if (variable != null) {
						// Обновляем значение переменной проекта текстом из поля ввода
						variable.setValue(s.toString());
					}
				}
			}
		});

		// --- ПОЗИЦИОНИРОВАНИЕ И ДОБАВЛЕНИЕ НА СЦЕНУ ---
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, editText, params);
	}

	/**
	 * Регистрирует или удаляет обработчик обратного вызова для конкретного WebView.
	 * @param viewId ID WebView, для которого устанавливается обработчик.
	 * @param callback Ваша реализация интерфейса WebViewCallback для обработки сообщений.
	 *                 Передайте null, чтобы удалить существующий обработчик.
	 */
	public void setWebViewCallback(String viewId, WebViewCallback callback) {
		if (callback == null) {
			webViewCallbacks.remove(viewId);
		} else {
			webViewCallbacks.put(viewId, callback);
		}
	}

	/**
	 * --- НОВОЕ ---
	 * Удаляет ВСЕ динамически добавленные View со сцены.
	 * Вызывается при перезапуске проекта, чтобы очистить интерфейс.
	 */
	public void removeAllNativeViews() {
		// Обязательно выполняем в UI-потоке
		runOnUiThread(() -> {
			// Проходим по всем значениям (View) в нашей карте
			for (View viewToRemove : dynamicViews.values()) {
				rootLayout.removeView(viewToRemove);
				if (viewToRemove != null && viewToRemove.getParent() instanceof ViewGroup) {
					((ViewGroup) viewToRemove.getParent()).removeView(viewToRemove);
				}
			}
			// Полностью очищаем карту, чтобы не было "утечек" ссылок
			dynamicViews.clear();
			// Также очищаем обработчики
			webViewCallbacks.clear();
		});
	}

	/**
	 * Выполняет JavaScript-код в указанном WebView.
	 * Это позволяет динамически изменять содержимое страницы без перезагрузки.
	 * @param viewId ID WebView, в котором нужно выполнить код.
	 * @param javascriptCode Строка с JavaScript-кодом для выполнения.
	 */
	public void executeJavaScript(final String viewId, final String javascriptCode) {
		runOnUiThread(() -> {
			View view = dynamicViews.get(viewId);
			if (view instanceof WebView) {
				WebView webView = (WebView) view;
				// evaluateJavascript - это современный и безопасный способ выполнения JS.
				// Он не блокирует UI-поток.
				webView.evaluateJavascript(javascriptCode, null); // Второй параметр - это колбэк для получения результата от JS, нам он здесь не нужен.
			} else {
				Log.w(TAG, "View with id '" + viewId + "' is not a WebView. Cannot execute JavaScript.");
			}
		});
	}

	// ДОБАВИТЬ В StageActivity.java
	/**
	 * Создает простой цветной прямоугольник для отладки слоев.
	 */
	public void createDebugView(String viewId, int color, int x, int y, int width, int height) {
		View debugView = new View(this);
		debugView.setBackgroundColor(color); // Устанавливаем яркий цвет

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, debugView, params);
	}

	// ДОБАВИТЬ ЭТОТ МЕТОД В StageActivity.java

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

	/**
	 * Создает и отображает видеоплеер на сцене.
	 *
	 * @param viewId        Уникальный строковый ID для этого плеера (например, "intro-video").
	 * @param videoPath     Полный путь к видеофайлу на устройстве.
	 * @param x             Позиция по горизонтали от левого края экрана в пикселях.
	 * @param y             Позиция по вертикали от верхнего края экрана в пикселях.
	 * @param width         Ширина плеера в пикселях.
	 * @param height        Высота плеера в пикселях.
	 * @param showControls  true, если нужно показать стандартные элементы управления (пауза, прокрутка).
	 *                      false, если нужно показывать только "чистое" видео.
	 * @param loopVideo     true, если видео должно начинаться заново после завершения.
	 */
	public void createVideoPlayer(String viewId, String videoPath, int x, int y, int width, int height, boolean showControls, final boolean loopVideo) {
		final VideoView videoView = new VideoView(this);

		// Настройки Z-Order и прозрачности (оставляем, как было)
		//videoView.setZOrderOnTop(true);
		//videoView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		// Настраиваем MediaController, если нужно (оставляем, как было)
		if (showControls) {
			MediaController mediaController = new MediaController(this);
			mediaController.setAnchorView(videoView);
			videoView.setMediaController(mediaController);
		}

		// Устанавливаем путь к видеофайлу
		videoView.setVideoPath(videoPath);

		// --- НАЧАЛО КЛЮЧЕВОГО ИЗМЕНЕНИЯ ---

		// Устанавливаем "слушатель готовности". Этот код выполнится только тогда,
		// когда видео будет полностью подготовлено к воспроизведению.
		videoView.setOnPreparedListener(mediaPlayer -> {
			// Теперь видео ГОТОВО. Это самое правильное место для вызова start().
			mediaPlayer.start();

			// Также, это лучшее место для установки зацикливания.
			// Это более прямой способ, чем OnCompletionListener.
			if (loopVideo) {
				mediaPlayer.setLooping(true);
			}
		});

		// --- КОНЕЦ КЛЮЧЕВОГО ИЗМЕНЕНИЯ ---


		// --- УДАЛЯЕМ СТАРЫЙ СПОСОБ ЗАПУСКА ---
		// Старые строки:
		// videoView.requestFocus();
		// videoView.start();
		// Больше не нужны здесь. Запуск произойдет в onPrepared.


		// Позиционирование и добавление на сцену (оставляем, как было)
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = Gravity.TOP | Gravity.START;
		params.leftMargin = x;
		params.topMargin = y;

		addViewToStage(viewId, videoView, params);

		// Запрос фокуса все еще полезен, чтобы MediaController мог работать сразу
		videoView.requestFocus();
	}

	/**
	 * Удаляет любой View (включая WebView), ранее добавленный на сцену, по его ID.
	 *
	 * @param viewId Уникальный ID, который вы использовали при создании View.
	 */
	public void removeView(String viewId) {
		// Просто вызываем наш уже существующий метод
		removeViewFromStage(viewId);
	}

	/**
	 * Добавляет любой View на сцену.
	 * Все операции с UI должны выполняться в главном потоке. Этот метод позаботится об этом.
	 *
	 * @param viewId Уникальный строковый идентификатор для этого View. Нужен для последующего доступа или удаления.
	 * @param view   Объект View, который нужно добавить (например, new Button(this), new WebView(this)).
	 * @param params Параметры макета, определяющие размер и положение View внутри FrameLayout.
	 */
	// ИЗМЕНИТЬ: Метод добавления теперь использует activeNativeLayer
	public void addViewToStage(final String viewId, final View view, final FrameLayout.LayoutParams params) {
		removeViewFromStage(viewId); // Сначала удаляем старый, если он есть

		runOnUiThread(() -> {
			view.setLayoutParams(params);
			dynamicViews.put(viewId, view);
			// Добавляем не в rootLayout, а в текущий активный слой!
			activeNativeLayer.addView(view);
		});
	}

	// ИЗМЕНИТЬ: Метод удаления стал умнее и удаляет View из любого родителя
	/**
	 * Удаляет View со сцены по его ID.
	 *
	 * @param viewId Уникальный ID View, которое нужно удалить.
	 */
	public void removeViewFromStage(final String viewId) {
		if (dynamicViews.containsKey(viewId)) {
			runOnUiThread(() -> {
				View viewToRemove = dynamicViews.get(viewId);
				if (viewToRemove != null && viewToRemove.getParent() instanceof ViewGroup) {
					// Удаляем View из его текущего родителя, будь то
					// backgroundLayout или foregroundLayout.
					((ViewGroup) viewToRemove.getParent()).removeView(viewToRemove);
				}
				dynamicViews.remove(viewId);
			});
		}
	}

	public FrameLayout getCameraContainer() {
		return cameraContainer;
	}

	/**
	 * Получает View по его ID для дальнейших манипуляций.
	 *
	 * @param viewId Уникальный ID View.
	 * @return Объект View или null, если не найден.
	 */
	public View getViewFromStage(String viewId) {
		return dynamicViews.get(viewId);
	}

	/**
	 * Гарантированно выполняет Runnable в главном потоке UI.
	 * @param runnable код для выполнения.
	 */
	public static void runOnMainThread(Runnable runnable) {
		if (mainThreadHandler != null) {
			mainThreadHandler.post(runnable);
		}
	}

	// В файле StageActivity.java
	private void checkAndRequestPermissions() {
		List<String> permissionsNeeded = new ArrayList<>();

		// --- Камера ---
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded.add(Manifest.permission.CAMERA);
		}

		// --- Микрофон ---
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
		}

		// --- Местоположение ---
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S = API 31
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
			}
		}

		// --- Уведомления (для Android 13+) ---
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
			}
		}

		// --- Хранилище (Ваш код уже был здесь, оставляем его) ---
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
			}
			// и т.д. для видео и аудио
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}
		}

		// --- Запускаем запрос, если что-то нужно ---
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
		StageLifeCycleController.stagePause(this);
		super.onPause();

		if (surveyCampaign != null) {
			surveyCampaign.endStageTime();

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

			if (isApplicationSentToBackground(this) || !pm.isInteractive()) {
				surveyCampaign.endAppTime(this);
			}
		}
	}

	private boolean isApplicationSentToBackground(final Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
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
		StageLifeCycleController.stageResume(this);
		super.onResume();
		activeStageActivity = new WeakReference<>(this);

		if (surveyCampaign != null) {
			surveyCampaign.startAppTime(this);
			surveyCampaign.startStageTime();
		}
	}

	@Override
	protected void onDestroy() {
		// 1. СНАЧАЛА вызываем родительский метод.
		// Он корректно очистит слушатели и ресурсы LibGDX, пока View еще существуют.
		super.onDestroy();

		// 2. ТЕПЕРЬ выполняем свою собственную очистку.
		if (ProjectManager.getInstance().getCurrentProject() != null) {
			StageLifeCycleController.stageDestroy(this);
		}

		// 3. И очистку WebView в конце.
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

		if (nfcTagMessage != null) {
			Tag currentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			synchronized (StageActivity.class) {
				NfcHandler.writeTag(currentTag, nfcTagMessage);
				setNfcTagMessage(null);
			}
		}
	}

	/**
	 * Проверяет, содержит ли текущий проект хотя бы один скрипт указанного типа.
	 * @param scriptClass Класс скрипта для поиска (например, BackPressedScript.class).
	 * @return true, если найден хотя бы один экземпляр, иначе false.
	 */
	private boolean projectHasScriptOfType(Class<? extends Script> scriptClass) {
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project == null || scriptClass == null) {
			return false;
		}
		for (Scene scene : project.getSceneList()) {
			for (Sprite sprite : scene.getSpriteList()) {
				for (Script script : sprite.getScriptList()) {
					if (scriptClass.isInstance(script)) {
						return true; // Нашли, дальше можно не искать
					}
				}
			}
		}
		return false; // Не нашли во всем проекте
	}

	/**
	 * "Транслирует" событие всем спрайтам на текущей сцене.
	 * @param eventId ID события для запуска.
	 */
	private void broadcastEventToAllSprites(EventId eventId) {
		Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
		if (scene == null) {
			return;
		}

		for (Sprite sprite : scene.getSpriteList()) {
			Multimap<EventId, ScriptSequenceAction> eventMap = sprite.getIdToEventThreadMap();
			if (eventMap != null && eventMap.containsKey(eventId)) {
				// Запускаем все скрипты, которые подписаны на это событие
				for (ScriptSequenceAction sequence : eventMap.get(eventId)) {
					sequence.restart(); // Перезапускаем экшн, чтобы его можно было использовать снова
					sprite.look.addAction(sequence); // look - это Actor из LibGDX, он выполняет действия
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		Project currentProject = ProjectManager.getInstance().getCurrentProject();

		// 1. ПРОВЕРЯЕМ, ЕСТЬ ЛИ В ПРОЕКТЕ НУЖНЫЙ СКРИПТ
		boolean backPressedScriptExists = EventManager.projectHasScriptOfType(
				currentProject, BackPressedScript.class);

		if (backPressedScriptExists) {
			// 2. ЛОГИКА ДЛЯ ПРОЕКТОВ С НОВЫМ СКРИПТОМ

			// Проверяем, было ли предыдущее нажатие менее 2 секунд назад
			if (backPressedTime + BACK_PRESS_EXIT_TIMEOUT > System.currentTimeMillis()) {
				handleBack();
			} else {
				// НЕТ, ЭТО ПЕРВОЕ НАЖАТИЕ
				// а) Запускаем событие для всех скриптов
				broadcastEventToAllSprites(new EventId(EventId.BACK_PRESSED));
				// б) Показываем подсказку пользователю
				Toast.makeText(this, "Нажмите еще раз для вызова меню", Toast.LENGTH_SHORT).show();

				// в) Запоминаем время этого нажатия
				backPressedTime = System.currentTimeMillis();
			}

		} else {
			handleBack();
		}
	}

	private void handleBack() {
		if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
			//BluetoothDeviceService service = ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE);
			/*if (service != null) {
				service.disconnectDevices();
			}*/

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
		return stageListener;
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

		for (IntentListener listener : intentListeners2) {
			if (listener.onIntentResult(requestCode, resultCode, data)) {
				return; // Если обработано, прекращаем вызовы
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
		boolean onIntentResult(int requestCode, int resultCode, Intent data); //don't do heavy processing here
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

			/*if (allGranted) {
				Toast.makeText(this, "Все разрешения предоставлены!", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Не все разрешения предоставлены. Некоторые функции могут быть недоступны.", Toast.LENGTH_LONG).show();
			}*/
		} else {
			permissionRequestActivityExtension.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
		}
	}

	private static final String PREFS_NAME = "SecurityPreferences";
	private static final String PREFS_KEY_SUPPRESS_WARNING = "suppress_security_warning";

	public static void handlePlayButton(ProjectManager projectManager, final Activity activity) {
		Project project = projectManager.getCurrentProject();

		// Проверяем, содержит ли проект опасные блоки
		boolean isDangerous = ProjectSecurityChecker.projectContainsDangerousBricks(project);

		// Проверяем, не отключал ли пользователь это предупреждение ранее
		SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		boolean shouldSuppressWarning = prefs.getBoolean(PREFS_KEY_SUPPRESS_WARNING, false);

		// Если проект опасен И пользователь не отключал предупреждение, показываем диалог
		if (isDangerous && !shouldSuppressWarning) {
			showSecurityWarningDialog(projectManager, activity);
		} else {
			// В противном случае (проект безопасен или предупреждение отключено) - запускаем как обычно
			launchProject(projectManager, activity);
		}
	}

	private static void showSecurityWarningDialog(ProjectManager projectManager, Activity activity) {
		new AlertDialog.Builder(activity)
				.setTitle("Проект может содержать вредоносный код")
				.setMessage("В проекте используется LunoScript, Python или Библиотеки, это может быть опасно. Запускайте его только если проверили код или доверяете источнику.")
				.setCancelable(false) // Запрещаем закрывать диалог кнопкой "назад"
				.setIcon(android.R.drawable.ic_dialog_alert)

				// Кнопка "Запуск" (Positive)
				.setPositiveButton("Запуск", (dialog, which) -> {
					dialog.dismiss();
					launchProject(projectManager, activity); // Запускаем проект
				})

				// Кнопка "Отмена" (Negative)
				.setNegativeButton("Отмена", (dialog, which) -> {
					dialog.dismiss(); // Просто закрываем диалог
				})

				// Кнопка "Больше не напоминать" (Neutral)
				.setNeutralButton("Больше не напоминать", (dialog, which) -> {
					// Сохраняем выбор пользователя
					SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(PREFS_KEY_SUPPRESS_WARNING, true);
					editor.apply();

					dialog.dismiss();
					launchProject(projectManager, activity); // И запускаем проект
				})
				.show();
	}

	private static void launchProject(ProjectManager projectManager, final Activity activity) {
		Scene currentScene = projectManager.getCurrentlyEditedScene();
		Scene defaultScene = projectManager.getCurrentProject().getDefaultScene();

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
		View view = View.inflate(CatroidApplication.getAppContext(), R.layout.dialog_ai_assist, null);

		TextInputDialog.Builder builder = new TextInputDialog.Builder(CatroidApplication.getAppContext());
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
		Intent intent = new Intent(activity, StageActivity.class);
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
}
