package org.catrobat.catroid.content;

import android.util.Log;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.keyboardheight.AndroidXKeyboardHeightProvider;

public class SafeKeyboardHeightProvider extends AndroidXKeyboardHeightProvider {

    public SafeKeyboardHeightProvider(AndroidApplication activity) {
        super(activity);
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (Throwable t) {
            Log.w("SafeKeyboardProvider", "Caught a throwable while closing KeyboardHeightProvider. " +
                    "This is a known issue and is being safely ignored.", t);
        }
    }
}