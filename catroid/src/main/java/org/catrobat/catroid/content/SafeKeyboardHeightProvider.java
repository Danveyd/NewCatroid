// Полный путь: org/catrobat/catroid/content/SafeKeyboardHeightProvider.java
package org.catrobat.catroid.content; // Убедитесь, что пакет правильный

import android.util.Log;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.keyboardheight.AndroidXKeyboardHeightProvider;

// Наследуемся от проблемного класса
public class SafeKeyboardHeightProvider extends AndroidXKeyboardHeightProvider {

    public SafeKeyboardHeightProvider(AndroidApplication activity) {
        super(activity);
    }

    // Переопределяем ТОЛЬКО метод close(), который вызывает падение
    @Override
    public void close() {
        try {
            // Пытаемся вызвать оригинальный метод родительского класса
            super.close();
        } catch (Throwable t) {
            // Если он падает с ЛЮБОЙ ошибкой (NullPointerException или другой),
            // мы просто ловим ее и игнорируем. Это предотвратит крах приложения.
            Log.w("SafeKeyboardProvider", "Caught a throwable while closing KeyboardHeightProvider. " +
                    "This is a known issue and is being safely ignored.", t);
        }
    }
}