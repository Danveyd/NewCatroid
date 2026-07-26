package org.catrobat.catroid.particles;

import org.catrobat.catroid.content.EasingFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ParticleCurve implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Keyframe implements Serializable {
        private static final long serialVersionUID = 1L;

        public float time;
        public float value;
        public float variance;
        public EasingFunctions.EasingType easingType = EasingFunctions.EasingType.LINEAR;

        public Keyframe() {}

        public Keyframe(float time, float value, float variance, EasingFunctions.EasingType easingType) {
            this.time = Math.max(0f, Math.min(1f, time));
            this.value = value;
            this.variance = variance;
            this.easingType = easingType != null ? easingType : EasingFunctions.EasingType.LINEAR;
        }

        public float getRandomValue() {
            if (variance == 0f) return value;
            float randomOffset = (float) ((Math.random() * 2.0 - 1.0) * variance);
            return value + randomOffset;
        }
    }

    private List<Keyframe> keyframes = new ArrayList<>();

    public ParticleCurve() {
        keyframes.add(new Keyframe(0.0f, 1.0f, 0.0f, EasingFunctions.EasingType.LINEAR));
        keyframes.add(new Keyframe(1.0f, 1.0f, 0.0f, EasingFunctions.EasingType.LINEAR));
    }

    public ParticleCurve(float startValue, float endValue, float variance, EasingFunctions.EasingType easing) {
        keyframes.add(new Keyframe(0.0f, startValue, variance, easing));
        keyframes.add(new Keyframe(1.0f, endValue, variance, easing));
    }

    public List<Keyframe> getKeyframes() {
        return keyframes;
    }

    public void addKeyframe(Keyframe keyframe) {
        if (keyframe == null) return;
        keyframes.add(keyframe);
        sortKeyframes();
    }

    public void removeKeyframe(int index) {
        if (keyframes.size() > 2 && index >= 0 && index < keyframes.size()) {
            keyframes.remove(index);
        }
    }

    public void sortKeyframes() {
        Collections.sort(keyframes, Comparator.comparingDouble(k -> k.time));
    }

    public float evaluate(float t, float startRandomOffset, float endRandomOffset) {
        if (keyframes.isEmpty()) return 0f;
        if (keyframes.size() == 1) return keyframes.get(0).value + startRandomOffset;

        t = Math.max(0f, Math.min(1f, t));

        Keyframe left = keyframes.get(0);
        Keyframe right = keyframes.get(keyframes.size() - 1);

        if (t <= left.time) return left.value + startRandomOffset;
        if (t >= right.time) return right.value + endRandomOffset;

        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (t >= k1.time && t <= k2.time) {
                left = k1;
                right = k2;
                break;
            }
        }

        float segmentDuration = right.time - left.time;
        if (segmentDuration <= 0f) return left.value + startRandomOffset;

        float localTime = t - left.time;
        float startVal = left.value + startRandomOffset;
        float endVal = right.value + endRandomOffset;

        return EasingFunctions.calculate(left.easingType, localTime, segmentDuration, startVal, endVal);
    }
}
