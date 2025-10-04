// В пакете .../ui/dialogs/
package org.catrobat.catroid.ui.dialogs;

import android.content.Context;
import android.view.WindowManager;

public class DebugMenuManager {
    private static final DebugMenuManager INSTANCE = new DebugMenuManager();
    private DebugMenuView debugMenuView;

    private DebugMenuManager() {}

    public static DebugMenuManager getInstance() {
        return INSTANCE;
    }

    public void updateIfVisible() {
        if (debugMenuView != null) {
            debugMenuView.update();
        }
    }

    public void show(Context context) {
        if (debugMenuView == null) {
            debugMenuView = new DebugMenuView(context.getApplicationContext());
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.addView(debugMenuView, debugMenuView.getLayoutParams()); // Используем LayoutParams из View
        }
    }

    public void hide() {
        if (debugMenuView != null) {
            WindowManager windowManager = (WindowManager) debugMenuView.getContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(debugMenuView);
            debugMenuView = null;
        }
    }

    // Добавим метод для получения LayoutParams из View
    private WindowManager.LayoutParams getLayoutParamsFromView() {
        if (debugMenuView != null) {
            // Убедимся, что возвращаем копию, а не сам объект, на всякий случай
            return new WindowManager.LayoutParams(
                    debugMenuView.getLayoutParams().width,
                    debugMenuView.getLayoutParams().height,
                    debugMenuView.getLayoutParams().type,
                    debugMenuView.getLayoutParams().flags,
                    debugMenuView.getLayoutParams().format
            );
        }
        return null;
    }
}