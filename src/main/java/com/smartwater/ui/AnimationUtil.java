package com.smartwater.ui;

import javafx.animation.Interpolator;

public final class AnimationUtil {
    private AnimationUtil() {
    }

    public static Interpolator safeSpline(double x1, double y1, double x2, double y2) {
        try {
            return Interpolator.SPLINE(x1, y1, x2, y2);
        } catch (IllegalArgumentException ex) {
            return Interpolator.EASE_BOTH;
        }
    }
}