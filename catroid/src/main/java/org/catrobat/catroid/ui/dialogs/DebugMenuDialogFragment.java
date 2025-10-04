package org.catrobat.catroid.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class DebugMenuDialogFragment extends DialogFragment {
    public static final String TAG = "DebugMenuDialogFragment";

    private float dX, dY; // Для отслеживания смещения при перетаскивании

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_debug_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Настраиваем перетаскивание и закрытие
        setupWindowControls(view);
        // Заполняем данными
        populateVariables(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWindowControls(View view) {
        View titleBar = view.findViewById(R.id.title_bar);
        View closeButton = view.findViewById(R.id.close_button);

        closeButton.setOnClickListener(v -> dismiss());

        titleBar.setOnTouchListener((dialogView, event) -> {
            Window window = getDialog().getWindow();
            if (window == null) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Запоминаем начальное смещение от левого верхнего угла окна до точки касания
                    dX = window.getAttributes().x - event.getRawX();
                    dY = window.getAttributes().y - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    // Обновляем позицию окна
                    window.getAttributes().x = (int) (event.getRawX() + dX);
                    window.getAttributes().y = (int) (event.getRawY() + dY);
                    window.getWindowManager().updateViewLayout(window.getDecorView(), window.getAttributes());
                    return true;
            }
            return false;
        });
    }

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // Убираем стандартный заголовок
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Убираем затемнение фона
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setDimAmount(0.0f);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}