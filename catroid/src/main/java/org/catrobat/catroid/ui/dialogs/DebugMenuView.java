package org.catrobat.catroid.ui.dialogs;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class DebugMenuView extends FrameLayout {

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;

    private float dX, dY;
    private final LinearLayout variablesContainer;
    private final Map<String, TextView> entryViewMap = new HashMap<>();

    private boolean isUpdatePending = false;
    private long lastUpdateTime = 0;
    private static final int UPDATE_INTERVAL_MS = 250;
    private static final int MAX_TEXT_DISPLAY_LENGTH = 60;
    private static final int MAX_LIST_ITEMS_DISPLAY = 50;

    private static final int COLOR_ACCENT = Color.parseColor("#A8DFF4");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#B0BEC5");
    private static final int COLOR_LIST_TITLE = Color.parseColor("#AADBF0");
    private static final int COLOR_INPUT_BG = Color.parseColor("#00476F");

    public DebugMenuView(Context context) {
        super(context);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.params = createLayoutParams();

        LayoutInflater.from(context).inflate(R.layout.dialog_debug_menu, this, true);

        View xmlRoot = getChildAt(0);
        if (xmlRoot != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) xmlRoot.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            xmlRoot.setLayoutParams(lp);
        }

        this.variablesContainer = findViewById(R.id.variables_container);
        if (this.variablesContainer == null) {
            throw new IllegalStateException("Could not find variables_container in dialog_debug_menu.xml");
        }

        setupWindowControls();
        setupResizer();
        buildInitialLayout();
        setupLogcatButton();
    }

    @SuppressLint("DefaultLocale")
    public void update() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateTime = currentTime;

        post(() -> {
            Project project = ProjectManager.getInstance().getCurrentProject();
            if (project == null) return;

            for (UserVariable var : project.getUserVariables()) {
                updateVariableView("global." + var.getName(), var.getName(), String.valueOf(var.getValue()));
            }

            for (UserList list : project.getUserLists()) {
                updateListView("global." + list.getName(), list, variablesContainer);
            }

            List<Sprite> spritesCopy;
            try { spritesCopy = new ArrayList<>(project.getSpriteListWithClones()); }
            catch (Exception e) { return; }

            for (Sprite sprite : spritesCopy) {
                for (UserVariable var : sprite.getUserVariables()) {
                    updateVariableView(sprite.getName() + "." + var.getName(), var.getName(), String.valueOf(var.getValue()));
                }
                for (UserList list : sprite.getUserLists()) {
                    updateListView(sprite.getName() + "." + list.getName(), list, variablesContainer);
                }
            }
        });
    }

    private void updateVariableView(String uniqueId, String name, String rawValue) {
        TextView tv = entryViewMap.get(uniqueId);
        if (tv != null) {
            String newText = formatVariableText(name, rawValue);
            if (!tv.getText().toString().equals(newText)) {
                tv.setText(newText);
            }
        }
    }

    private void updateListView(String uniqueId, UserList list, ViewGroup container) {
        TextView tv = entryViewMap.get(uniqueId);
        if (tv != null) {
            ListState state = (ListState) tv.getTag();
            String newText = formatListText(list.getName(), list.getValue().size(), state.isExpanded);
            if (!tv.getText().toString().equals(newText)) {
                tv.setText(newText);
            }
            if (state.isExpanded) {
                refreshListItems(list, state, container, tv);
            }
        }
    }

    private String formatVariableText(String name, String value) {
        String displayValue = value;
        if (value.length() > MAX_TEXT_DISPLAY_LENGTH) {
            displayValue = value.substring(0, MAX_TEXT_DISPLAY_LENGTH) + "…";
        }
        return "  • " + name + " = " + displayValue;
    }

    private String formatListText(String name, int size, boolean isExpanded) {
        String prefix = isExpanded ? "  ▼ " : "  ▶ ";
        return getContext().getString(R.string.debug_menu_list_format, prefix, name, size);
    }

    private void buildInitialLayout() {
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return;

        variablesContainer.removeAllViews();
        entryViewMap.clear();

        addHeader(variablesContainer, getContext().getString(R.string.debug_menu_global_data));
        for (UserVariable var : project.getUserVariables()) {
            addDebugVariable(variablesContainer, "global." + var.getName(), var);
        }
        for (UserList list : project.getUserLists()) {
            addDebugList(variablesContainer, "global." + list.getName(), list);
        }

        for (Sprite sprite : project.getSpriteListWithClones()) {
            if (sprite != null && (!sprite.getUserVariables().isEmpty() || !sprite.getUserLists().isEmpty())) {
                addHeader(variablesContainer, getContext().getString(R.string.debug_menu_sprite) + ": " + sprite.getName().toUpperCase());
                for (UserVariable var : sprite.getUserVariables()) {
                    addDebugVariable(variablesContainer, sprite.getName() + "." + var.getName(), var);
                }
                for (UserList list : sprite.getUserLists()) {
                    addDebugList(variablesContainer, sprite.getName() + "." + list.getName(), list);
                }
            }
        }
    }

    private void addHeader(ViewGroup container, String text) {
        TextView header = new TextView(getContext());
        header.setText(text);
        header.setTextSize(12);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.setTextColor(COLOR_ACCENT);
        header.setPadding(8, 24, 8, 4);
        container.addView(header);
    }

    private void addDebugVariable(ViewGroup container, String uniqueId, UserVariable var) {
        TextView entryView = new TextView(getContext());
        entryView.setText(formatVariableText(var.getName(), String.valueOf(var.getValue())));
        entryView.setTextSize(13);
        entryView.setTypeface(Typeface.MONOSPACE);
        entryView.setTextColor(COLOR_TEXT_PRIMARY);
        entryView.setPadding(8, 8, 8, 8);

        entryView.setOnClickListener(v -> showEditVariableDialog(var));
        entryView.setOnLongClickListener(v -> {
            copyToClipboard(var.getName(), String.valueOf(var.getValue()));
            return true;
        });

        container.addView(entryView);
        entryViewMap.put(uniqueId, entryView);
    }

    private void addDebugList(ViewGroup container, String uniqueId, UserList list) {
        TextView entryView = new TextView(getContext());
        entryView.setText(formatListText(list.getName(), list.getValue().size(), false));
        entryView.setTextSize(13);
        entryView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        entryView.setTextColor(COLOR_LIST_TITLE);
        entryView.setPadding(8, 8, 8, 8);

        entryView.setTag(new ListState(false, new ArrayList<>()));
        entryView.setOnClickListener(v -> toggleList(container, entryView, list));

        entryView.setOnLongClickListener(v -> {
            copyToClipboard(list.getName(), String.valueOf(list.getValue()));
            return true;
        });

        container.addView(entryView);
        entryViewMap.put(uniqueId, entryView);
    }

    private void toggleList(ViewGroup container, TextView headerView, UserList listData) {
        ListState state = (ListState) headerView.getTag();

        if (state.isExpanded) {
            for (View child : state.childViews) {
                container.removeView(child);
            }
            if (state.moreView != null) {
                container.removeView(state.moreView);
                state.moreView = null;
            }
            state.childViews.clear();
            state.isExpanded = false;
        } else {
            state.isExpanded = true;
            refreshListItems(listData, state, container, headerView);
        }
        headerView.setText(formatListText(listData.getName(), listData.getValue().size(), state.isExpanded));
    }

    @SuppressLint("DefaultLocale")
    private void refreshListItems(UserList listData, ListState state, ViewGroup container, TextView headerView) {
        List<Object> listCopy = new ArrayList<>();
        List<Object> originalList = listData.getValue();
        if (originalList != null) {
            int size = originalList.size();
            for (int j = 0; j < size; j++) {
                try {
                    listCopy.add(originalList.get(j));
                } catch (Exception ignored) {
                    break;
                }
            }
        }

        int headerIndex = container.indexOfChild(headerView);
        int itemsCount = listCopy.size();
        int displayLimit = Math.min(itemsCount, MAX_LIST_ITEMS_DISPLAY);

        if (state.moreView != null) {
            container.removeView(state.moreView);
            state.moreView = null;
        }

        while (state.childViews.size() > displayLimit) {
            View v = state.childViews.remove(state.childViews.size() - 1);
            container.removeView(v);
        }

        for (int i = 0; i < displayLimit; i++) {
            Object item = listCopy.get(i);
            String rawVal = String.valueOf(item);
            String displayVal = rawVal.length() > MAX_TEXT_DISPLAY_LENGTH ? rawVal.substring(0, MAX_TEXT_DISPLAY_LENGTH) + "…" : rawVal;
            String newText = String.format("      [%d] %s", i + 1, displayVal);

            if (i < state.childViews.size()) {
                TextView itemView = (TextView) state.childViews.get(i);
                if (!itemView.getText().toString().equals(newText)) {
                    itemView.setText(newText);
                }
                final int index = i;
                itemView.setOnClickListener(v -> showEditListItemDialog(listData, index, rawVal));
                itemView.setOnLongClickListener(v -> {
                    copyToClipboard("Item " + (index+1), rawVal);
                    return true;
                });
            } else {
                TextView itemView = new TextView(getContext());
                itemView.setText(newText);
                itemView.setTextSize(12);
                itemView.setTypeface(Typeface.MONOSPACE);
                itemView.setTextColor(COLOR_TEXT_SECONDARY);
                itemView.setPadding(8, 4, 8, 4);

                final int index = i;
                itemView.setOnClickListener(v -> showEditListItemDialog(listData, index, rawVal));
                itemView.setOnLongClickListener(v -> {
                    copyToClipboard("Item " + (index+1), rawVal);
                    return true;
                });

                container.addView(itemView, headerIndex + 1 + i);
                state.childViews.add(itemView);
            }
        }

        if (itemsCount > MAX_LIST_ITEMS_DISPLAY) {
            TextView moreView = new TextView(getContext());
            moreView.setText(String.format(getContext().getString(R.string.debug_menu_more_items), itemsCount - MAX_LIST_ITEMS_DISPLAY));
            moreView.setTextSize(12);
            moreView.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
            moreView.setTextColor(Color.GRAY);
            container.addView(moreView, headerIndex + 1 + displayLimit);
            state.moreView = moreView;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupResizer() {
        TextView resizer = new TextView(getContext());
        resizer.setText("↘");
        resizer.setTextColor(COLOR_ACCENT);
        resizer.setTextSize(22);
        resizer.setPadding(32, 32, 32, 32);

        FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END
        );
        addView(resizer, handleParams);

        resizer.setOnTouchListener(new View.OnTouchListener() {
            private int originalX, originalY;
            private int originalWidth, originalHeight;
            private float initialX, initialY;
            private View resizePreview;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        originalX = params.x;
                        originalY = params.y;
                        originalWidth = getWidth();
                        originalHeight = getHeight();
                        initialX = event.getRawX();
                        initialY = event.getRawY();

                        View dialogRoot = getChildAt(0);
                        if (dialogRoot != null) {
                            dialogRoot.setVisibility(View.GONE);
                        }

                        resizePreview = new View(getContext());
                        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                        gd.setColor(Color.argb(80, 255, 255, 255));
                        gd.setCornerRadius(32f);
                        gd.setStroke(6, COLOR_ACCENT);
                        resizePreview.setBackground(gd);

                        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(originalWidth, originalHeight);
                        previewParams.leftMargin = originalX;
                        previewParams.topMargin = originalY;
                        previewParams.gravity = Gravity.TOP | Gravity.START;
                        resizePreview.setLayoutParams(previewParams);
                        addView(resizePreview);

                        params.width = WindowManager.LayoutParams.MATCH_PARENT;
                        params.height = WindowManager.LayoutParams.MATCH_PARENT;
                        params.x = 0;
                        params.y = 0;
                        windowManager.updateViewLayout(DebugMenuView.this, params);

                        resizer.bringToFront();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialX);
                        int dy = (int) (event.getRawY() - initialY);

                        int minSize = (int)(200 * getResources().getDisplayMetrics().density);
                        int newWidth = Math.max(originalWidth + dx, minSize);
                        int newHeight = Math.max(originalHeight + dy, minSize);

                        if (resizePreview != null) {
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) resizePreview.getLayoutParams();
                            lp.width = newWidth;
                            lp.height = newHeight;
                            resizePreview.setLayoutParams(lp);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        int finalWidth = originalWidth;
                        int finalHeight = originalHeight;

                        if (resizePreview != null) {
                            finalWidth = resizePreview.getWidth();
                            finalHeight = resizePreview.getHeight();
                            removeView(resizePreview);
                            resizePreview = null;
                        }

                        View root = getChildAt(0);
                        if (root != null) {
                            root.setVisibility(View.VISIBLE);
                        }

                        params.width = finalWidth;
                        params.height = finalHeight;
                        params.x = originalX;
                        params.y = originalY;
                        windowManager.updateViewLayout(DebugMenuView.this, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void showEditVariableDialog(UserVariable var) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.Theme_ChEngine_Dialog);
        builder.setTitle(String.format(getContext().getString(R.string.debug_menu_edit_variable), var.getName()));

        View container = createStyledInputContainer(String.valueOf(var.getValue()));
        EditText input = container.findViewById(R.id.formula_editor_edit_field);

        builder.setView(container);
        builder.setPositiveButton(R.string.debug_menu_save, (dialog, which) -> {
            var.setValue(input.getText().toString());
            update();
        });
        builder.setNegativeButton(R.string.debug_menu_cancel, (dialog, which) -> dialog.cancel());
        builder.setNeutralButton(R.string.debug_menu_copy_full, (dialog, which) -> copyToClipboard(var.getName(), String.valueOf(var.getValue())));
        builder.show();
    }

    private void showEditListItemDialog(UserList list, int index, String currentValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.Theme_ChEngine_Dialog);
        builder.setTitle(String.format(getContext().getString(R.string.debug_menu_edit_list_item), index + 1, list.getName()));

        View container = createStyledInputContainer(currentValue);
        EditText input = container.findViewById(R.id.formula_editor_edit_field);

        builder.setView(container);
        builder.setPositiveButton(R.string.debug_menu_save, (dialog, which) -> {
            list.getValue().set(index, input.getText().toString());
            update();
        });
        builder.setNegativeButton(R.string.debug_menu_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private View createStyledInputContainer(String initialText) {
        float density = getResources().getDisplayMetrics().density;

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (16 * density);
        lp.setMargins(margin, margin, margin, margin);

        EditText input = new EditText(getContext());
        input.setId(R.id.formula_editor_edit_field);
        input.setText(initialText);
        input.setTextColor(COLOR_TEXT_PRIMARY);
        input.setHintTextColor(COLOR_TEXT_SECONDARY);
        int padding = (int) (12 * density);
        input.setPadding(padding, padding, padding, padding);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(COLOR_INPUT_BG);
        gd.setCornerRadius(12 * density);
        gd.setStroke((int)(2 * density), COLOR_ACCENT);
        input.setBackground(gd);

        if (initialText.length() > 30) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            input.setMaxLines(8);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        input.setLayoutParams(lp);
        container.addView(input);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(container);
        return scrollView;
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), R.string.debug_menu_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private WindowManager.LayoutParams createLayoutParams() {
        final int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        int widthPx = (int) (280 * getContext().getResources().getDisplayMetrics().density);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                widthPx,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;
        return params;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWindowControls() {
        View titleBar = findViewById(R.id.title_bar);
        View closeButton = findViewById(R.id.close_button);

        closeButton.setOnClickListener(v -> DebugMenuManager.getInstance().hide());

        if (titleBar instanceof ViewGroup) {
            ViewGroup titleGroup = (ViewGroup) titleBar;

            TextView consoleBtn = new TextView(getContext());
            consoleBtn.setText(">_");
            consoleBtn.setTextColor(COLOR_ACCENT);
            consoleBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            consoleBtn.setTextSize(20);
            consoleBtn.setPadding(24, 0, 24, 0);
            consoleBtn.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.gravity = android.view.Gravity.CENTER_VERTICAL;
            consoleBtn.setLayoutParams(lp);

            consoleBtn.setOnClickListener(v -> {
                DebugMenuManager.getInstance().hide();

                int currentOrientation = getContext().getResources().getConfiguration().orientation;
                int activityOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                if (currentOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                    activityOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }

                android.content.Intent intent = new android.content.Intent(getContext(), RuntimeConsoleActivity.class);
                intent.putExtra("orientation", activityOrientation);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            });

            titleGroup.addView(consoleBtn, titleGroup.getChildCount() - 1);
        }

        titleBar.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = params.x - event.getRawX();
                    dY = params.y - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (event.getRawX() + dX);
                    params.y = (int) (event.getRawY() + dY);

                    scheduleWindowUpdate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scheduleWindowUpdate();
                    return true;
            }
            return false;
        });
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return params;
    }

    private void scheduleWindowUpdate() {
        if (isUpdatePending) {
            return;
        }
        isUpdatePending = true;
        android.view.Choreographer.getInstance().postFrameCallback(frameTimeNanos -> {
            isUpdatePending = false;
            if (getParent() != null) {
                try {
                    windowManager.updateViewLayout(DebugMenuView.this, params);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static class ListState {
        boolean isExpanded;
        List<View> childViews;
        View moreView;

        ListState(boolean isExpanded, List<View> childViews) {
            this.isExpanded = isExpanded;
            this.childViews = childViews;
            this.moreView = null;
        }
    }

    private void setupLogcatButton() {
        // Находим нашу кнопку как ImageButton
        android.widget.ImageButton btnRecord = findViewById(R.id.btn_record_logs);
        if (btnRecord == null) return;

        LogcatRecorder recorder = LogcatRecorder.getInstance();
        updateRecordButtonUI(btnRecord, recorder.isRecording());

        btnRecord.setOnClickListener(v -> {
            boolean currentlyRecording = recorder.isRecording();
            if (!currentlyRecording) {
                recorder.startRecording();
                Toast.makeText(getContext(), R.string.log_recording_started, Toast.LENGTH_SHORT).show();
                updateRecordButtonUI(btnRecord, true);
            } else {
                recorder.stopAndExport(getContext());
                updateRecordButtonUI(btnRecord, false);
            }
        });
    }

    private void updateRecordButtonUI(android.widget.ImageButton btn, boolean isRecording) {
        if (isRecording) {
            btn.setImageResource(R.drawable.ic_debug_stop);
            btn.setColorFilter(Color.parseColor("#FF5252"));
        } else {
            btn.setImageResource(R.drawable.ic_debug_record);
            btn.setColorFilter(COLOR_ACCENT);
        }
    }

    public static class LogcatRecorder {
        private static LogcatRecorder instance;
        private boolean isRecording = false;
        private long startTime = 0;

        private LogcatRecorder() {}

        public static synchronized LogcatRecorder getInstance() {
            if (instance == null) {
                instance = new LogcatRecorder();
            }
            return instance;
        }

        public boolean isRecording() {
            return isRecording;
        }

        public void startRecording() {
            isRecording = true;
            startTime = System.currentTimeMillis();
        }

        public void stopAndExport(Context context) {
            isRecording = false;

            new Thread(() -> {
                String report = generateReport(context);
                File file = saveReportToCache(context, report);
                if (file != null) {
                    new android.os.Handler(context.getMainLooper()).post(() -> shareReportFile(context, file));
                }
            }).start();
        }

        private String generateReport(Context context) {
            StringBuilder sb = new StringBuilder();
            sb.append("=========================================\n");
            sb.append("         CHENGINE DEBUG REPORT         \n");
            sb.append("=========================================\n\n");

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            sb.append("Report Generated: ").append(sdf.format(new java.util.Date())).append("\n");
            sb.append("Recording Started: ").append(sdf.format(new java.util.Date(startTime))).append("\n");
            sb.append("-----------------------------------------\n\n");

            sb.append("--- DEVICE INFORMATION ---\n");
            sb.append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
            sb.append("Model: ").append(android.os.Build.MODEL).append("\n");
            sb.append("Brand: ").append(android.os.Build.BRAND).append("\n");
            sb.append("Device: ").append(android.os.Build.DEVICE).append("\n");
            sb.append("OS Version: ").append(android.os.Build.VERSION.RELEASE)
                    .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
            sb.append("CPU ABI: ").append(android.os.Build.SUPPORTED_ABIS[0]).append("\n");

            try {
                android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                android.app.ActivityManager activityManager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(mi);
                    sb.append("Total RAM: ").append(mi.totalMem / (1024 * 1024)).append(" MB\n");
                    sb.append("Available RAM: ").append(mi.availMem / (1024 * 1024)).append(" MB\n");
                }
            } catch (Exception ignored) {}

            try {
                android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                sb.append("Screen Resolution: ").append(metrics.widthPixels).append("x").append(metrics.heightPixels).append("\n");
                sb.append("Screen Density: ").append(metrics.densityDpi).append(" DPI\n");
            } catch (Exception ignored) {}

            sb.append("\n--- APPLICATION INFORMATION ---\n");
            sb.append("Package Name: ").append(context.getPackageName()).append("\n");

            Project currentProject = ProjectManager.getInstance().getCurrentProject();
            if (currentProject != null) {
                sb.append("\n--- ACTIVE PROJECT INFORMATION ---\n");
                sb.append("Project Name: ").append(currentProject.getName()).append("\n");
                sb.append("Language Version: ").append(currentProject.getCatrobatLanguageVersion()).append("\n");
            }

            sb.append("\n=========================================\n");
            sb.append("               LOGCAT LOGS               \n");
            sb.append("=========================================\n\n");

            try {
                Process process = Runtime.getRuntime().exec("logcat -d -v time");
                java.io.BufferedReader bufferedReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream())
                );
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                bufferedReader.close();
            } catch (Exception e) {
                sb.append("ERROR CAPTURING LOGS: ").append(e.getMessage()).append("\n");
            }

            return sb.toString();
        }

        private File saveReportToCache(Context context, String reportText) {
            try {
                File cacheFile = new File(context.getCacheDir(), "chengine_debug_report.txt");
                if (cacheFile.exists()) cacheFile.delete();

                java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile);
                fos.write(reportText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.close();
                return cacheFile;
            } catch (Exception e) {
                android.util.Log.e("LogcatRecorder", "Failed to write debug report to cache", e);
                return null;
            }
        }

        private void shareReportFile(Context context, File file) {
            try {
                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileProvider",
                        file
                );

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Ch-engine Debug Report");

                android.content.Intent chooser = android.content.Intent.createChooser(intent, context.getString(R.string.share_report_title));
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            } catch (Exception e) {
                Toast.makeText(context, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
