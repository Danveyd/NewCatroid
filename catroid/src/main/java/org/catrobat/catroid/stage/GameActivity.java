package org.catrobat.catroid.stage;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.catrobat.catroid.R;

public class GameActivity extends AppCompatActivity {
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        myWebView = findViewById(R.id.myWebView);
        myWebView.setWebViewClient(new WebViewClient()); // Чтобы открывать ссылки в WebView

        myWebView.getLayoutParams().width = 300;  // Установи ширину (в пикселях)
        myWebView.getLayoutParams().height = 400; // Установи высоту (в пикселях)
        myWebView.requestLayout(); // Обновление компоновки
        // Загрузи URL
        myWebView.loadUrl("https://uchi.ru/");
    }

    @Override
    protected void onDestroy() {
        if (myWebView != null) {
            myWebView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Не вызываем super, чтобы предотвратить стандартное поведение
        // Здесь можно добавить логику, если необходимо
        onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false; // Пропускает касания
    }

}
