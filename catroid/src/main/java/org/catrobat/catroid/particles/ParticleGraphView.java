package org.catrobat.catroid.particles.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.catrobat.catroid.content.EasingFunctions;
import org.catrobat.catroid.particles.ParticleCurve;

import java.util.List;

public class ParticleGraphView extends View {

    public interface OnCurveChangeListener {
        void onCurveChanged(ParticleCurve curve);
        void onPointSelected(int index, ParticleCurve.Keyframe keyframe);
    }

    private ParticleCurve curve;
    private float minValue = -100f;
    private float maxValue = 100f;

    private float zoomScaleY = 1.0f;
    private float panOffsetY = 0f;

    private int selectedPointIndex = -1;
    private boolean isDraggingPoint = false;

    private OnCurveChangeListener listener;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zeroLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint variancePathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path curvePath = new Path();
    private final Path variancePath = new Path();

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;

    public ParticleGraphView(Context context) {
        this(context, null);
    }

    public ParticleGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initPaints();

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoomScaleY = Math.max(0.2f, Math.min(5.0f, zoomScaleY * detector.getScaleFactor()));
                invalidate();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                addNewPointAt(e.getX(), e.getY());
                return true;
            }
        });
    }

    private void initPaints() {
        gridPaint.setColor(0xFF1E2D3D);
        gridPaint.setStrokeWidth(dpToPx(1f));
        gridPaint.setStyle(Paint.Style.STROKE);

        zeroLinePaint.setColor(0xFFFF5252);
        zeroLinePaint.setStrokeWidth(dpToPx(1.5f));
        zeroLinePaint.setStyle(Paint.Style.STROKE);
        zeroLinePaint.setPathEffect(new DashPathEffect(new float[]{12f, 8f}, 0));

        curvePaint.setColor(0xFF4FC3F7);
        curvePaint.setStrokeWidth(dpToPx(2.5f));
        curvePaint.setStyle(Paint.Style.STROKE);

        variancePathPaint.setColor(0x3000ACD2);
        variancePathPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(0xFF4FC3F7);
        pointPaint.setStyle(Paint.Style.FILL);

        pointStrokePaint.setColor(Color.WHITE);
        pointStrokePaint.setStrokeWidth(dpToPx(2f));
        pointStrokePaint.setStyle(Paint.Style.STROKE);

        tooltipBgPaint.setColor(0xEE1E293B);
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(spToPx(10f));

        axisTextPaint.setColor(0xFF94A3B8);
        axisTextPaint.setTextSize(spToPx(9f));
    }

    public void setCurve(ParticleCurve curve, float minValue, float maxValue) {
        this.curve = curve;
        this.minValue = minValue;
        this.maxValue = Math.max(minValue + 0.01f, maxValue);
        this.selectedPointIndex = -1;
        this.zoomScaleY = 1.0f;
        this.panOffsetY = 0f;
        invalidate();
    }

    public void setOnCurveChangeListener(OnCurveChangeListener listener) {
        this.listener = listener;
    }

    public void zoomIn() { zoomScaleY = Math.min(5.0f, zoomScaleY * 1.25f); invalidate(); }
    public void zoomOut() { zoomScaleY = Math.max(0.2f, zoomScaleY / 1.25f); invalidate(); }
    public void resetZoom() { zoomScaleY = 1.0f; panOffsetY = 0f; invalidate(); }

    public ParticleCurve.Keyframe getSelectedKeyframe() {
        if (curve != null && selectedPointIndex >= 0 && selectedPointIndex < curve.getKeyframes().size()) {
            return curve.getKeyframes().get(selectedPointIndex);
        }
        return null;
    }

    public void deleteSelectedKeyframe() {
        if (curve != null && selectedPointIndex > 0 && selectedPointIndex < curve.getKeyframes().size() - 1) {
            curve.removeKeyframe(selectedPointIndex);
            selectedPointIndex = -1;
            notifyChanged();
            invalidate();
        }
    }

    public void addPointAtCenter() {
        if (curve == null) return;
        ParticleCurve.Keyframe kf = new ParticleCurve.Keyframe(0.5f, (minValue + maxValue) / 2f, 0f, EasingFunctions.EasingType.LINEAR);
        curve.addKeyframe(kf);
        selectedPointIndex = curve.getKeyframes().indexOf(kf);
        notifyChanged();
        invalidate();
    }

    public void setSelectedEasingType(EasingFunctions.EasingType easingType) {
        ParticleCurve.Keyframe kf = getSelectedKeyframe();
        if (kf != null) {
            kf.easingType = easingType;
            notifyChanged();
            invalidate();
        }
    }

    public void setSelectedVariance(float variance) {
        ParticleCurve.Keyframe kf = getSelectedKeyframe();
        if (kf != null) {
            kf.variance = Math.max(0f, variance);
            notifyChanged();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float paddingLeft = dpToPx(30f);
        float paddingTop = dpToPx(20f);
        float width = getWidth() - paddingLeft - dpToPx(16f);
        float height = getHeight() - paddingTop - dpToPx(20f);

        if (width <= 0 || height <= 0) return;

        float range = (maxValue - minValue) / zoomScaleY;
        float effectiveMin = minValue + panOffsetY;
        float effectiveMax = effectiveMin + range;

        for (int i = 0; i <= 4; i++) {
            float x = paddingLeft + (width / 4f) * i;
            canvas.drawLine(x, paddingTop, x, paddingTop + height, gridPaint);
            canvas.drawText(String.format("%.2f", i * 0.25f), x - dpToPx(8f), paddingTop + height + dpToPx(14f), axisTextPaint);
        }

        if (effectiveMin <= 0f && effectiveMax >= 0f) {
            float zeroY = paddingTop + height - ((0f - effectiveMin) / range) * height;
            canvas.drawLine(paddingLeft, zeroY, paddingLeft + width, zeroY, zeroLinePaint);
            canvas.drawText("0", paddingLeft - dpToPx(16f), zeroY + dpToPx(4f), axisTextPaint);
        }

        if (curve == null) return;
        List<ParticleCurve.Keyframe> keyframes = curve.getKeyframes();
        if (keyframes.isEmpty()) return;

        variancePath.reset();
        int steps = 60;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float baseVal = curve.evaluate(t, 0f, 0f);
            float var = getInterpolatedVariance(t);
            float topVal = baseVal + var;

            float screenX = paddingLeft + t * width;
            float screenY = paddingTop + height - ((topVal - effectiveMin) / range) * height;

            if (i == 0) variancePath.moveTo(screenX, screenY);
            else variancePath.lineTo(screenX, screenY);
        }

        for (int i = steps; i >= 0; i--) {
            float t = (float) i / steps;
            float baseVal = curve.evaluate(t, 0f, 0f);
            float var = getInterpolatedVariance(t);
            float botVal = baseVal - var;

            float screenX = paddingLeft + t * width;
            float screenY = paddingTop + height - ((botVal - effectiveMin) / range) * height;

            variancePath.lineTo(screenX, screenY);
        }
        variancePath.close();
        canvas.drawPath(variancePath, variancePathPaint);

        curvePath.reset();
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float val = curve.evaluate(t, 0f, 0f);

            float screenX = paddingLeft + t * width;
            float screenY = paddingTop + height - ((val - effectiveMin) / range) * height;

            if (i == 0) curvePath.moveTo(screenX, screenY);
            else curvePath.lineTo(screenX, screenY);
        }
        canvas.drawPath(curvePath, curvePaint);

        float pointRadius = dpToPx(8f);
        for (int i = 0; i < keyframes.size(); i++) {
            ParticleCurve.Keyframe kf = keyframes.get(i);

            float px = paddingLeft + kf.time * width;
            float py = paddingTop + height - ((kf.value - effectiveMin) / range) * height;

            if (i == selectedPointIndex) {
                canvas.drawCircle(px, py, pointRadius + dpToPx(4f), pointStrokePaint);

                String tooltip = String.format("t:%.2f | v:%.1f (±%.1f)", kf.time, kf.value, kf.variance);
                float textWidth = tooltipTextPaint.measureText(tooltip);
                RectF bgRect = new RectF(px - textWidth / 2f - dpToPx(6f), py - dpToPx(32f), px + textWidth / 2f + dpToPx(6f), py - dpToPx(12f));
                canvas.drawRoundRect(bgRect, dpToPx(4f), dpToPx(4f), tooltipBgPaint);
                canvas.drawText(tooltip, px - textWidth / 2f, py - dpToPx(18f), tooltipTextPaint);
            }

            canvas.drawCircle(px, py, pointRadius, pointPaint);
        }
    }

    private float getInterpolatedVariance(float t) {
        if (curve == null || curve.getKeyframes().isEmpty()) return 0f;
        List<ParticleCurve.Keyframe> keyframes = curve.getKeyframes();
        if (keyframes.size() == 1) return keyframes.get(0).variance;

        if (t <= keyframes.get(0).time) return keyframes.get(0).variance;
        if (t >= keyframes.get(keyframes.size() - 1).time) return keyframes.get(keyframes.size() - 1).variance;

        for (int i = 0; i < keyframes.size() - 1; i++) {
            ParticleCurve.Keyframe k1 = keyframes.get(i);
            ParticleCurve.Keyframe k2 = keyframes.get(i + 1);
            if (t >= k1.time && t <= k2.time) {
                float factor = (t - k1.time) / (k2.time - k1.time);
                return k1.variance + factor * (k2.variance - k1.variance);
            }
        }
        return 0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (scaleGestureDetector.isInProgress()) {
            return true;
        }

        float paddingLeft = dpToPx(30f);
        float paddingTop = dpToPx(20f);
        float width = getWidth() - paddingLeft - dpToPx(16f);
        float height = getHeight() - paddingTop - dpToPx(20f);

        float touchX = event.getX();
        float touchY = event.getY();

        float range = (maxValue - minValue) / zoomScaleY;
        float effectiveMin = minValue + panOffsetY;
        float effectiveMax = effectiveMin + range;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectedPointIndex = findClosestPointIndex(touchX, touchY, paddingLeft, paddingTop, width, height, effectiveMin, effectiveMax);
                if (selectedPointIndex != -1) {
                    isDraggingPoint = true;
                    if (listener != null) {
                        listener.onPointSelected(selectedPointIndex, curve.getKeyframes().get(selectedPointIndex));
                    }
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDraggingPoint && selectedPointIndex != -1 && curve != null) {
                    List<ParticleCurve.Keyframe> keyframes = curve.getKeyframes();
                    ParticleCurve.Keyframe kf = keyframes.get(selectedPointIndex);

                    float newTime = (touchX - paddingLeft) / width;
                    float newValue = effectiveMax - ((touchY - paddingTop) / height) * range;

                    if (selectedPointIndex == 0) {
                        kf.time = 0f;
                    } else if (selectedPointIndex == keyframes.size() - 1) {
                        kf.time = 1f;
                    } else {
                        float prevTime = keyframes.get(selectedPointIndex - 1).time + 0.01f;
                        float nextTime = keyframes.get(selectedPointIndex + 1).time - 0.01f;
                        kf.time = Math.max(prevTime, Math.min(nextTime, newTime));
                    }

                    kf.value = newValue;
                    notifyChanged();
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDraggingPoint = false;
                break;
        }

        return true;
    }

    private int findClosestPointIndex(float tx, float ty, float paddingLeft, float paddingTop, float width, float height, float effMin, float effMax) {
        if (curve == null) return -1;

        List<ParticleCurve.Keyframe> keyframes = curve.getKeyframes();
        float touchRadius = dpToPx(24f);

        for (int i = 0; i < keyframes.size(); i++) {
            ParticleCurve.Keyframe kf = keyframes.get(i);
            float px = paddingLeft + kf.time * width;
            float py = paddingTop + height - ((kf.value - effMin) / (effMax - effMin)) * height;

            float dist = (float) Math.hypot(tx - px, ty - py);
            if (dist <= touchRadius) {
                return i;
            }
        }
        return -1;
    }

    private void addNewPointAt(float tx, float ty) {
        if (curve == null) return;

        float paddingLeft = dpToPx(30f);
        float paddingTop = dpToPx(20f);
        float width = getWidth() - paddingLeft - dpToPx(16f);
        float height = getHeight() - paddingTop - dpToPx(20f);

        float time = (tx - paddingLeft) / width;
        float range = (maxValue - minValue) / zoomScaleY;
        float effectiveMin = minValue + panOffsetY;
        float value = effectiveMin + (1f - (ty - paddingTop) / height) * range;

        if (time > 0.02f && time < 0.98f) {
            ParticleCurve.Keyframe newKf = new ParticleCurve.Keyframe(time, value, 0f, EasingFunctions.EasingType.LINEAR);
            curve.addKeyframe(newKf);
            selectedPointIndex = curve.getKeyframes().indexOf(newKf);
            notifyChanged();
            invalidate();
        }
    }

    private void notifyChanged() {
        if (listener != null && curve != null) {
            listener.onCurveChanged(curve);
        }
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
