package org.catrobat.catroid.particles.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.catrobat.catroid.R;

public class MaterialSliderView extends View {

    public interface OnSliderChangeListener {
        void onValueChanged(float value);
        void onValueChangeFinished();
    }

    private String label = "";
    private float minValue = 0f;
    private float maxValue = 100f;
    private float currentValue = 50f;
    private String suffix = "";

    private boolean isPressed = false;
    private boolean isDragging = false;
    private float startX = 0f;
    private float startY = 0f;
    private final int touchSlop;

    private OnSliderChangeListener listener;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float trackHeightAnimProgress = 0f;
    private ValueAnimator pressAnimator;

    private final GestureDetector gestureDetector;

    public MaterialSliderView(Context context) {
        this(context, null);
    }

    public MaterialSliderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        initPaints(context);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                showDirectValueInputDialog();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                showMinMaxDialog();
            }
        });
    }

    private void initPaints(Context context) {
        bgPaint.setColor(ContextCompat.getColor(context, R.color.surface_card));
        bgPaint.setStyle(Paint.Style.FILL);

        fillPaint.setColor(ContextCompat.getColor(context, R.color.accent));
        fillPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(ContextCompat.getColor(context, R.color.separator));
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        borderPaint.setStyle(Paint.Style.STROKE);

        textWhitePaint.setColor(ContextCompat.getColor(context, R.color.solid_white));
        textWhitePaint.setTextSize(spToPx(11f));
        textWhitePaint.setFakeBoldText(true);

        textDarkPaint.setColor(ContextCompat.getColor(context, R.color.app_background_dark));
        textDarkPaint.setTextSize(spToPx(11f));
        textDarkPaint.setFakeBoldText(true);
    }

    public void setParams(String label, float minValue, float maxValue, float currentValue, String suffix) {
        this.label = label;
        this.minValue = minValue;
        this.maxValue = Math.max(minValue + 0.001f, maxValue);
        this.currentValue = Math.max(minValue, Math.min(maxValue, currentValue));
        this.suffix = suffix != null ? suffix : "";
        invalidate();
    }

    public void setOnSliderChangeListener(OnSliderChangeListener listener) {
        this.listener = listener;
    }

    public float getValue() {
        return currentValue;
    }

    public void setValue(float value) {
        setValueAnimated(Math.max(minValue, Math.min(maxValue, value)));
    }

    public void setValueAnimated(float targetValue) {
        ValueAnimator anim = ValueAnimator.ofFloat(currentValue, targetValue);
        anim.setDuration(150);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            currentValue = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void showMinMaxDialog() {
        Context context = getContext();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) dpToPx(16f);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_card));

        TextView tvInfo = new TextView(context);
        tvInfo.setText(context.getString(R.string.particle_slider_bounds_msg, label));
        tvInfo.setTextColor(ContextCompat.getColor(context, R.color.solid_white));
        tvInfo.setTextSize(14);

        EditText etMin = new EditText(context);
        etMin.setHint(R.string.particle_min_val);
        etMin.setHintTextColor(0x88FFFFFF);
        etMin.setTextColor(0xFFFFFFFF);
        etMin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etMin.setText(String.valueOf(minValue));

        EditText etMax = new EditText(context);
        etMax.setHint(R.string.particle_max_val);
        etMax.setHintTextColor(0x88FFFFFF);
        etMax.setTextColor(0xFFFFFFFF);
        etMax.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etMax.setText(String.valueOf(maxValue));

        layout.addView(tvInfo);
        layout.addView(etMin);
        layout.addView(etMax);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.particle_slider_bounds_title)
                .setView(layout)
                .setPositiveButton(R.string.particle_ok, (d, which) -> {
                    try {
                        float newMin = Float.parseFloat(etMin.getText().toString());
                        float newMax = Float.parseFloat(etMax.getText().toString());

                        if (newMin >= newMax) {
                            Toast.makeText(context, R.string.particle_invalid_range, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        this.minValue = newMin;
                        this.maxValue = newMax;
                        this.currentValue = Math.max(minValue, Math.min(maxValue, currentValue));

                        invalidate();
                        if (listener != null) {
                            listener.onValueChanged(currentValue);
                            listener.onValueChangeFinished();
                        }
                    } catch (Exception e) {
                        Toast.makeText(context, "Некорректный ввод!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.particle_cancel, null)
                .create();

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.accent));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.accent));
    }

    private void showDirectValueInputDialog() {
        Context context = getContext();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) dpToPx(16f);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_card));

        TextView tvInfo = new TextView(context);
        tvInfo.setText("Точный ввод для " + label + " (" + (int)minValue + ".." + (int)maxValue + "):");
        tvInfo.setTextColor(ContextCompat.getColor(context, R.color.solid_white));

        EditText etValue = new EditText(context);
        etValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etValue.setText(String.valueOf(currentValue));
        etValue.setTextColor(0xFFFFFFFF);

        layout.addView(tvInfo);
        layout.addView(etValue);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
                .setTitle("Точный ввод")
                .setView(layout)
                .setPositiveButton(R.string.particle_ok, (d, which) -> {
                    try {
                        float val = Float.parseFloat(etValue.getText().toString());
                        setValueAnimated(Math.max(minValue, Math.min(maxValue, val)));
                        if (listener != null) {
                            listener.onValueChanged(currentValue);
                            listener.onValueChangeFinished();
                        }
                    } catch (Exception ignored) {}
                })
                .setNegativeButton(R.string.particle_cancel, null)
                .create();

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.accent));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.accent));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dpToPx(38f);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), desiredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = dpToPx(2f);
        float width = getWidth() - padding * 2f;
        float height = getHeight();

        if (width <= 0 || height <= 0) return;

        float baseTrackHeight = dpToPx(26f);
        float expandedTrackHeight = dpToPx(32f);
        float trackHeight = baseTrackHeight + (expandedTrackHeight - baseTrackHeight) * trackHeightAnimProgress;

        float centerY = height / 2f;
        RectF trackRect = new RectF(padding, centerY - trackHeight / 2f, padding + width, centerY + trackHeight / 2f);
        float cornerRadius = trackHeight / 2f;

        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, bgPaint);
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, borderPaint);

        float progress = (currentValue - minValue) / (maxValue - minValue);
        float fillWidth = Math.max(trackHeight, width * progress);
        RectF fillRect = new RectF(padding, centerY - trackHeight / 2f, padding + fillWidth, centerY + trackHeight / 2f);

        canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint);

        String text = label + ": " + String.format("%.1f", currentValue) + suffix + "  [" + (int)minValue + ".." + (int)maxValue + "]";
        float textX = padding + dpToPx(12f);
        float textY = centerY + dpToPx(4f);

        canvas.drawText(text, textX, textY, textWhitePaint);

        canvas.save();
        canvas.clipRect(fillRect);
        canvas.drawText(text, textX, textY, textDarkPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        float padding = dpToPx(2f);
        float x = event.getX() - padding;
        float width = getWidth() - padding * 2f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                isPressed = true;
                isDragging = false;
                animatePressState(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - startX);
                float dy = Math.abs(event.getY() - startY);

                if (!isDragging && dx > touchSlop && dx > dy) {
                    isDragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                if (isDragging) {
                    updateValue(x, width);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && listener != null) {
                    listener.onValueChangeFinished();
                }
                isPressed = false;
                isDragging = false;
                animatePressState(false);
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void animatePressState(boolean pressed) {
        if (pressAnimator != null) pressAnimator.cancel();
        pressAnimator = ValueAnimator.ofFloat(trackHeightAnimProgress, pressed ? 1f : 0f);
        pressAnimator.setDuration(120);
        pressAnimator.addUpdateListener(a -> {
            trackHeightAnimProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
    }

    private void updateValue(float x, float width) {
        if (width <= 0) return;
        float factor = Math.max(0f, Math.min(1f, x / width));
        float rawValue = minValue + factor * (maxValue - minValue);

        float snapRadius = (maxValue - minValue) * 0.03f;
        if (Math.abs(rawValue - 15f) <= snapRadius) {
            currentValue = 15f;
        } else if (Math.abs(rawValue - 0f) <= snapRadius) {
            currentValue = 0f;
        } else {
            currentValue = rawValue;
        }

        if (listener != null) {
            listener.onValueChanged(currentValue);
        }
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
