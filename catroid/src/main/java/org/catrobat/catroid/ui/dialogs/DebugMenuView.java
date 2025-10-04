// В пакете .../ui/dialogs/ (или в другом подходящем месте)
package org.catrobat.catroid.ui.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class DebugMenuView extends FrameLayout {

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;

    private float dX, dY; // Для отслеживания смещения при перетаскивании

    private final LinearLayout variablesContainer;

    private final Map<String, TextView> entryViewMap = new HashMap<>();

    public DebugMenuView(Context context) {
        super(context);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.params = createLayoutParams();

        LayoutInflater.from(context).inflate(R.layout.dialog_debug_menu, this, true);

        this.variablesContainer = findViewById(R.id.variables_container);
        if (this.variablesContainer == null) {
            throw new IllegalStateException("Could not find variables_container in dialog_debug_menu.xml");
        }

        setupWindowControls();
        buildInitialLayout();
    }

    @SuppressLint("DefaultLocale")
    public void update() {
        post(() -> {
            Project project = ProjectManager.getInstance().getCurrentProject();
            if (project == null) return;

            // Обновляем глобальные переменные
            for (UserVariable var : project.getUserVariables()) {
                TextView tv = entryViewMap.get("global." + var.getName());
                if (tv != null) {
                    tv.setText(String.format("  • %s: %s", var.getName(), var.getValue().toString()));
                }
            }
            // Обновляем глобальные списки
            for (UserList list : project.getUserLists()) {
                TextView tv = entryViewMap.get("global." + list.getName());
                ListState state = (ListState) tv.getTag();
                if (tv != null && !state.isExpanded) { // Не трогаем заголовок, если список развернут
                    tv.setText(String.format("  ▶ %s: [%d items]", list.getName(), list.getValue().size()));
                }
                // Если список развернут, обновляем его элементы
                if (state.isExpanded) {
                    tv.setText(String.format("  ▶ %s: [%d items]", list.getName(), list.getValue().size()));
                    updateListItems(list, state, variablesContainer, tv);
                }
            }

            // Обновляем данные спрайтов (аналогично)
            List<Sprite> spritesCopy;
            try { spritesCopy = new ArrayList<>(project.getSpriteListWithClones()); }
            catch (Exception e) { return; }

            for (Sprite sprite : spritesCopy) {
                for (UserVariable var : sprite.getUserVariables()) {
                    TextView tv = entryViewMap.get(sprite.getName() + "." + var.getName());
                    if (tv != null) {
                        tv.setText(String.format("  • %s: %s", var.getName(), var.getValue().toString()));
                    }
                }
                for (UserList list : sprite.getUserLists()) {
                    TextView tv = entryViewMap.get(sprite.getName() + "." + list.getName());
                    if (tv != null) {
                        ListState state = (ListState) tv.getTag();
                        if (!state.isExpanded) {
                            tv.setText(String.format("  ▶ %s: [%d items]", list.getName(), list.getValue().size()));
                        }
                        if (state.isExpanded) {
                            tv.setText(String.format("  ▶ %s: [%d items]", list.getName(), list.getValue().size()));
                            updateListItems(list, state, variablesContainer, tv);
                        }
                    }
                }
            }
        });
    }

    private void updateListItems(UserList listData, ListState state, ViewGroup container, TextView headerView) {
        /*for (int i = 0; i < state.childViews.size(); i++) {
            TextView itemView = (TextView) state.childViews.get(i);
            if (i < listData.getValue().size()) {
                Object item = listData.getValue().get(i);
                itemView.setText(String.format("      %d: %s", i + 1, item.toString()));
                itemView.setVisibility(View.VISIBLE);
            } else {
                itemView.setVisibility(View.GONE); // Скрываем лишние View, если список уменьшился
            }
        }
        // Если список увеличился, нужно добавить новые View (для простоты можно перестроить только этот список)
        if (listData.getValue().size() > state.childViews.size()) {
            // (здесь можно добавить логику досоздания View, но пока пропустим для простоты)
        }*/
        int headerIndex = container.indexOfChild(headerView);

        for (View child : state.childViews) {
            container.removeView(child);
        }
        state.childViews.clear();

        // Добавляем элементы списка ПОСЛЕ заголовка
        for (int i = 0; i < listData.getValue().size(); i++) {
            Object item = listData.getValue().get(i);
            TextView itemView = new TextView(getContext());
            itemView.setText(String.format("      %d: %s", i + 1, item.toString()));
            itemView.setTextSize(12);
            itemView.setTextColor(Color.GRAY);

            // Добавляем View в контейнер и в наш список для отслеживания
            container.addView(itemView, headerIndex + 1 + i);
            state.childViews.add(itemView);
        }
    }

    private void buildInitialLayout() {
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return;

        // Очищаем все перед построением
        variablesContainer.removeAllViews();
        entryViewMap.clear();

        // Глобальные данные
        addHeader(variablesContainer, "Global Data");
        for (UserVariable var : project.getUserVariables()) {
            addDebugEntry(variablesContainer, "global." + var.getName(), var.getName(), var.getValue().toString(), false, null);
        }
        for (UserList list : project.getUserLists()) {
            addDebugEntry(variablesContainer, "global." + list.getName(), list.getName(), String.format("[%d items]", list.getValue().size()), true, list);
        }

        // Данные спрайтов
        for (Sprite sprite : project.getSpriteListWithClones()) {
            if (sprite != null && (!sprite.getUserVariables().isEmpty() || !sprite.getUserLists().isEmpty())) {
                addHeader(variablesContainer, "Sprite: " + sprite.getName());
                for (UserVariable var : sprite.getUserVariables()) {
                    addDebugEntry(variablesContainer, sprite.getName() + "." + var.getName(), var.getName(), var.getValue().toString(), false, null);
                }
                for (UserList list : sprite.getUserLists()) {
                    addDebugEntry(variablesContainer, sprite.getName() + "." + list.getName(), list.getName(), String.format("[%d items]", list.getValue().size()), true, list);
                }
            }
        }
    }

    private WindowManager.LayoutParams createLayoutParams() {
        // ▼▼▼ НАЧАЛО ФИНАЛЬНОГО ИСПРАВЛЕНИЯ ▼▼▼

        // Убираем FLAG_NOT_FOCUSABLE. Окно ДОЛЖНО быть фокусируемым, чтобы
        // его дочерние элементы могли получать клики.
        // Оставляем FLAG_NOT_TOUCH_MODAL, чтобы клики за пределами окна
        // уходили в игру.
        final int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                flags, // <-- Используем флаг без NOT_FOCUSABLE
                android.graphics.PixelFormat.TRANSLUCENT
        );

        // ▲▲▲ КОНЕЦ ФИНАЛЬНОГО ИСПРАВЛЕНИЯ ▲▲▲

        params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        params.x = 100;
        params.y = 100;
        return params;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWindowControls() {
        View titleBar = findViewById(R.id.title_bar);
        View closeButton = findViewById(R.id.close_button);

        closeButton.setOnClickListener(v -> DebugMenuManager.getInstance().hide());

        titleBar.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = params.x - event.getRawX();
                    dY = params.y - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (event.getRawX() + dX);
                    params.y = (int) (event.getRawY() + dY);
                    windowManager.updateViewLayout(this, params);
                    return true;
            }
            return false;
        });
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return params;
    }

    // Метод populateVariables и его хелперы остаются такими же, как были в DialogFragment
    private void populateVariables(View view) {
        LinearLayout container = view.findViewById(R.id.variables_container);
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return;

        // --- Глобальные переменные ---
        addHeader(container, "Global Variables");
        for (UserVariable var : project.getUserVariables()) {
            addVariableView(container, var.getName(), var.getValue().toString());
        }

        // --- Переменные спрайтов ---
        for (Sprite sprite : project.getSpriteListWithClones()) {
            if (!sprite.getUserVariables().isEmpty()) {
                addHeader(container, "Sprite: " + sprite.getName());
                for (UserVariable var : sprite.getUserVariables()) {
                    addVariableView(container, var.getName(), var.getValue().toString());
                }
            }
        }
    }

    private void addHeader(ViewGroup container, String text) {
        TextView header = new TextView(getContext());
        header.setText(text);
        header.setTextSize(16);
        header.setTextColor(Color.WHITE);
        header.setPadding(0, 16, 0, 4);
        container.addView(header);
    }

    private void addVariableView(ViewGroup container, String name, String value) {
        TextView varView = new TextView(getContext());
        varView.setText(String.format("  • %s: %s", name, value));
        varView.setTextSize(14);
        varView.setTextColor(Color.LTGRAY);
        container.addView(varView);
    }

    /**
     * Универсальный метод для добавления записи в дебаг-меню.
     * @param container     ViewGroup, куда добавлять View.
     * @param name          Имя переменной или списка.
     * @param valueOrInfo   Значение переменной или информация о списке (кол-во элементов).
     * @param isExpandable  true, если это список, который можно развернуть.
     * @param listData      Сам объект UserList, если это список (иначе null).
     */
    private void addDebugEntry(ViewGroup container, String uniqueId, String name, String valueOrInfo, boolean isExpandable, @Nullable UserList listData) {
        TextView entryView = new TextView(getContext());
        String prefix = isExpandable ? "▶ " : "• ";
        entryView.setText(String.format("  %s%s: %s", prefix, name, valueOrInfo));
        entryView.setTextSize(14);
        entryView.setTextColor(Color.LTGRAY);

        if (isExpandable) {
            entryView.setTag(new ListState(false, new ArrayList<>()));
            entryView.setOnClickListener(v -> toggleList(container, entryView, listData));
        }

        container.addView(entryView);
        // --- НОВОЕ: Сохраняем ссылку на View в карту ---
        entryViewMap.put(uniqueId, entryView);
    }

    @SuppressLint("DefaultLocale")
    private void toggleList(ViewGroup container, TextView headerView, UserList listData) {
        ListState state = (ListState) headerView.getTag();
        int headerIndex = container.indexOfChild(headerView);
        String listName = listData.getName();

        if (state.isExpanded) {
            // --- СВОРАЧИВАЕМ СПИСОК ---
            // Удаляем все дочерние View, которые мы ранее добавили
            for (View child : state.childViews) {
                container.removeView(child);
            }
            state.childViews.clear();
            headerView.setText(String.format("  ▶ %s: [%d items]", listName, listData.getValue().size()));
            state.isExpanded = false;
        } else {
            // --- РАЗВОРАЧИВАЕМ СПИСОК ---
            headerView.setText(String.format("  ▶ %s: [%d items]", listName, listData.getValue().size()));
            state.isExpanded = true;

            // Добавляем элементы списка ПОСЛЕ заголовка
            for (int i = 0; i < listData.getValue().size(); i++) {
                Object item = listData.getValue().get(i);
                TextView itemView = new TextView(getContext());
                itemView.setText(String.format("      %d: %s", i + 1, item.toString()));
                itemView.setTextSize(12);
                itemView.setTextColor(Color.GRAY);

                // Добавляем View в контейнер и в наш список для отслеживания
                container.addView(itemView, headerIndex + 1 + i);
                state.childViews.add(itemView);
            }
        }
    }

    /** Вспомогательный класс для хранения состояния раскрывающегося списка. */
    private static class ListState {
        boolean isExpanded;
        List<View> childViews;

        ListState(boolean isExpanded, List<View> childViews) {
            this.isExpanded = isExpanded;
            this.childViews = childViews;
        }
    }
}