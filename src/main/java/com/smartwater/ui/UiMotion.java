package com.smartwater.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class UiMotion {
    private static final List<SpringState> SPRINGS = new ArrayList<>();
    private static final List<LiquidGlassSurface> GLASS_SURFACES = new ArrayList<>();
    private static long lastFrameNanos = 0L;
    // Track whether the timer is currently running
    private static boolean timerRunning = false;

    private static final AnimationTimer PHYSICS_TIMER = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastFrameNanos == 0L) {
                lastFrameNanos = now;
                return;
            }
            double dt = Math.min((now - lastFrameNanos) / 1_000_000_000.0, 0.032);
            lastFrameNanos = now;

            boolean anyActive = false;
            for (SpringState spring : SPRINGS) {
                if (spring.isActive()) {
                    spring.step(dt);
                    anyActive = true;
                }
            }
            for (LiquidGlassSurface surface : GLASS_SURFACES) {
                if (surface.isActive()) {
                    surface.step(dt);
                    anyActive = true;
                }
            }
            // Auto-pause timer when nothing is animating
            if (!anyActive) {
                PHYSICS_TIMER.stop();
                timerRunning = false;
                lastFrameNanos = 0L;
            }
        }
    };

    /** Wake the physics timer up — called whenever a spring or surface becomes active. */
    static void ensureTimerRunning() {
        if (!timerRunning) {
            timerRunning = true;
            lastFrameNanos = 0L;
            PHYSICS_TIMER.start();
        }
    }

    private UiMotion() {
    }

    public static Pane createAuroraBackdrop() {
        return createDarkAuroraBackdrop();
    }

    public static Pane createDarkAuroraBackdrop() {
        Pane backdrop = new Pane();
        backdrop.setMouseTransparent(true);
        backdrop.getStyleClass().add("liquid-aurora");
        backdrop.setStyle("-fx-background-color: transparent;");

        Circle glowOne = createGlowResponsive(220, Color.web("#0f172a"), 0.18, 0.10, 0.82, 0.045);
        Circle glowTwo = createGlowResponsive(180, Color.web("#2563eb"), 0.08, 0.72, 0.18, -0.065);
        Circle glowThree = createGlowResponsive(150, Color.web("#14b8a6"), 0.05, 0.88, 0.68, 0.08);

        backdrop.getChildren().addAll(glowOne, glowTwo, glowThree);
        attachResponsiveListeners(backdrop);
        animateFloat(glowOne, -12, 16, 7.5);
        animateFloat(glowTwo, 12, -16, 9.5);
        animateFloat(glowThree, -10, 12, 11.0);

        return backdrop;
    }

    public static Pane createLightAuroraBackdrop() {
        Pane backdrop = new Pane();
        backdrop.setMouseTransparent(true);
        backdrop.getStyleClass().add("liquid-aurora");
        backdrop.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #e2fff7 0%, #def4ff 43%, #efe7ff 100%);");

        // Reduced from 6 blobs to 3, radii halved to cut GPU overdraw
        Circle glowOne   = createGlowResponsive(280, Color.web("#38bdf8"), 0.28, 0.88, -0.10, -0.055);
        Circle glowTwo   = createGlowResponsive(230, Color.web("#2dd4bf"), 0.30, -0.08, 0.78,  0.075);
        Circle glowThree = createGlowResponsive(250, Color.web("#a78bfa"), 0.26,  1.04,  0.90, -0.045);

        backdrop.getChildren().addAll(glowOne, glowTwo, glowThree);
        attachResponsiveListeners(backdrop);
        animateFloat(glowOne,  -10,  12, 10.0);
        animateFloat(glowTwo,   14, -12, 12.0);
        animateFloat(glowThree, -8,  10, 11.0);

        return backdrop;
    }

    public static void installLiquidScene(Region sceneRoot, Pane backdrop, Node glassLayer, Node foregroundLayer) {
        SpringState glassX = spring(glassLayer, Axis.TRANSLATE_X, 52, 15);
        SpringState glassY = spring(glassLayer, Axis.TRANSLATE_Y, 52, 15);
        SpringState foregroundX = spring(foregroundLayer, Axis.TRANSLATE_X, 68, 18);
        SpringState foregroundY = spring(foregroundLayer, Axis.TRANSLATE_Y, 68, 18);

        // Throttle: only process a mouse-move event every 32 ms (~30 fps cap for parallax)
        final long[] lastParallaxNs = {0L};
        final long PARALLAX_INTERVAL_NS = 32_000_000L; // 32 ms

        sceneRoot.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            long now = System.nanoTime();
            if (now - lastParallaxNs[0] < PARALLAX_INTERVAL_NS) return;
            lastParallaxNs[0] = now;

            double normalizedX = normalize(event.getX(), sceneRoot.getWidth());
            double normalizedY = normalize(event.getY(), sceneRoot.getHeight());

            glassX.setTarget(normalizedX * 5.0);
            glassY.setTarget(normalizedY * 4.0);
            foregroundX.setTarget(normalizedX * 1.8);
            foregroundY.setTarget(normalizedY * 1.4);

            for (Node blob : backdrop.getChildren()) {
                Object depth = blob.getProperties().get("aurora-depth");
                if (depth instanceof Double) {
                    blob.setTranslateX(normalizedX * (Double) depth * sceneRoot.getWidth());
                }
            }
        });

        sceneRoot.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            glassX.setTarget(0);
            glassY.setTarget(0);
            foregroundX.setTarget(0);
            foregroundY.setTarget(0);
            for (Node blob : backdrop.getChildren()) {
                blob.setTranslateX(0);
            }
        });
    }

    public static void installLiquidGlass(Node node, Pane backdrop) {
        if (!(node instanceof Region region)) {
            return;
        }
        if (Boolean.TRUE.equals(region.getProperties().get("liquid-glass-installed"))) {
            return;
        }
        region.getProperties().put("liquid-glass-installed", true);
        GLASS_SURFACES.add(new LiquidGlassSurface(region, backdrop));
    }

    public static void fadeIn(Node node, double milliseconds) {
        FadeTransition ft = new FadeTransition(Duration.millis(milliseconds), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.play();
    }

    public static void slideUp(Node node, double milliseconds, double fromY) {
        node.setOpacity(0.0);
        node.setTranslateY(fromY);

        FadeTransition fade = new FadeTransition(Duration.millis(milliseconds), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(milliseconds), node);
        slide.setFromY(fromY);
        slide.setToY(0.0);

        fade.play();
        slide.play();
    }

    public static void reveal(Node node, double milliseconds, double delayMilliseconds) {
        reveal(node, milliseconds, delayMilliseconds, 14, 0.985);
    }

    public static void reveal(Node node, double milliseconds, double delayMilliseconds, double fromY,
            double fromScale) {
        node.setOpacity(0.0);
        node.setTranslateY(fromY);
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);

        PauseTransition pause = new PauseTransition(Duration.millis(delayMilliseconds));
        pause.setOnFinished(event -> {
            FadeTransition fade = new FadeTransition(Duration.millis(milliseconds), node);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slide = new TranslateTransition(Duration.millis(milliseconds), node);
            slide.setFromY(fromY);
            slide.setToY(0.0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(milliseconds), node);
            scale.setFromX(fromScale);
            scale.setFromY(fromScale);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(AnimationUtil.safeSpline(0.18, 0.85, 0.28, 1.0));

            new ParallelTransition(fade, slide, scale).play();
        });
        pause.play();
    }

    public static void staggerReveal(List<? extends Node> nodes, double milliseconds, double stepDelayMilliseconds) {
        for (int i = 0; i < nodes.size(); i++) {
            reveal(nodes.get(i), milliseconds, i * stepDelayMilliseconds);
        }
    }

    public static void staggerReveal(List<? extends Node> nodes, double milliseconds, double stepDelayMilliseconds,
            double fromY, double fromScale) {
        for (int i = 0; i < nodes.size(); i++) {
            reveal(nodes.get(i), milliseconds, i * stepDelayMilliseconds, fromY, fromScale);
        }
    }

    public static void applySpringPhysics(Node node, double target, double stiffness, double damping) {
        spring(node, Axis.TRANSLATE_Y, stiffness, damping).setTarget(target);
    }

    public static void applyParallax(Region container, Node node, double factor) {
        SpringState x = spring(node, Axis.TRANSLATE_X, 55, 15);
        SpringState y = spring(node, Axis.TRANSLATE_Y, 55, 15);
        container.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            x.setTarget((event.getX() - container.getWidth() / 2.0) * factor);
            y.setTarget((event.getY() - container.getHeight() / 2.0) * factor);
        });
        container.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            x.setTarget(0);
            y.setTarget(0);
        });
    }

    public static void applyLiquidRefraction(Region glass, Node highlight) {
        SpringState x = spring(highlight, Axis.TRANSLATE_X, 90, 18);
        SpringState y = spring(highlight, Axis.TRANSLATE_Y, 90, 18);
        SpringState opacity = spring(highlight, Axis.OPACITY, 75, 16);
        glass.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            x.setTarget(event.getX() - glass.getWidth() / 2.0);
            y.setTarget(event.getY() - glass.getHeight() / 2.0);
            opacity.setTarget(0.22);
        });
        glass.addEventHandler(MouseEvent.MOUSE_EXITED, event -> opacity.setTarget(0));
    }

    public static void installHoverLift(Node node) {
        installHoverLift(node, -4, 1.015);
    }

    public static void installHoverLift(Node node, double liftY, double scaleTo) {
        SpringState y = spring(node, Axis.TRANSLATE_Y, 110, 18);
        SpringState sx = spring(node, Axis.SCALE_X, 130, 20);
        SpringState sy = spring(node, Axis.SCALE_Y, 130, 20);
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            y.setTarget(liftY);
            sx.setTarget(scaleTo);
            sy.setTarget(scaleTo);
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            y.setTarget(0);
            sx.setTarget(1.0);
            sy.setTarget(1.0);
        });
    }

    public static void pulse(Node node, double seconds, double scaleTo) {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(seconds), node);
        pulse.setToX(scaleTo);
        pulse.setToY(scaleTo);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(ScaleTransition.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
    }

    private static SpringState spring(Node node, Axis axis, double stiffness, double damping) {
        SpringState spring = new SpringState(node, axis, stiffness, damping);
        SPRINGS.add(spring);
        return spring;
    }

    private static void attachResponsiveListeners(Pane backdrop) {
        backdrop.widthProperty().addListener((obs, oldW, newW) -> positionGlows(backdrop));
        backdrop.heightProperty().addListener((obs, oldH, newH) -> positionGlows(backdrop));
        positionGlows(backdrop);
    }

    private static void positionGlows(Pane backdrop) {
        for (Node n : backdrop.getChildren()) {
            if (n instanceof Circle c && n.getProperties().containsKey("aurora-x-ratio")) {
                double xr = (Double) c.getProperties().get("aurora-x-ratio");
                double yr = (Double) c.getProperties().get("aurora-y-ratio");
                c.setCenterX(backdrop.getWidth() * xr);
                c.setCenterY(backdrop.getHeight() * yr);
            }
        }
    }

    private static Circle createGlowResponsive(double radius, Color color, double opacity, double xRatio,
            double yRatio, double depth) {
        Circle glow = new Circle(radius);
        glow.setFill(color);
        glow.setOpacity(opacity);
        glow.setMouseTransparent(true);
        glow.setEffect(new GaussianBlur(radius * 0.12));
        glow.getProperties().put("aurora-x-ratio", xRatio);
        glow.getProperties().put("aurora-y-ratio", yRatio);
        glow.getProperties().put("aurora-depth", depth);
        return glow;
    }

    private static void animateFloat(Node node, double fromY, double toY, double seconds) {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(seconds), node);
        transition.setByY(toY - fromY);
        transition.setAutoReverse(true);
        transition.setCycleCount(TranslateTransition.INDEFINITE);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    private static double normalize(double value, double size) {
        if (size <= 0) {
            return 0;
        }
        return ((value / size) - 0.5) * 2.0;
    }

    private enum Axis {
        TRANSLATE_X,
        TRANSLATE_Y,
        SCALE_X,
        SCALE_Y,
        OPACITY
    }

    private static final class SpringState {
        private final Node node;
        private final Axis axis;
        private final double stiffness;
        private final double damping;
        private double position;
        private double velocity;
        private double target;
        private boolean initialized;

        private SpringState(Node node, Axis axis, double stiffness, double damping) {
            this.node = node;
            this.axis = axis;
            this.stiffness = stiffness;
            this.damping = damping;
            this.position = readValue();
            this.target = position;
            this.initialized = true;
        }

        private void setTarget(double target) {
            if (!initialized) {
                position = readValue();
                initialized = true;
            }
            this.target = target;
            UiMotion.ensureTimerRunning();
        }

        /** Returns true when the spring still has motion to simulate. */
        boolean isActive() {
            return Math.abs(target - position) >= 0.001 || Math.abs(velocity) >= 0.001;
        }

        private void step(double dt) {
            double displacement = position - target;
            double acceleration = -stiffness * displacement - damping * velocity;
            velocity += acceleration * dt;
            position += velocity * dt;

            if (Math.abs(target - position) < 0.001 && Math.abs(velocity) < 0.001) {
                position = target;
                velocity = 0;
            }
            writeValue(position);
        }

        private double readValue() {
            return switch (axis) {
                case TRANSLATE_X -> node.getTranslateX();
                case TRANSLATE_Y -> node.getTranslateY();
                case SCALE_X -> node.getScaleX();
                case SCALE_Y -> node.getScaleY();
                case OPACITY -> node.getOpacity();
            };
        }

        private void writeValue(double value) {
            switch (axis) {
                case TRANSLATE_X -> node.setTranslateX(value);
                case TRANSLATE_Y -> node.setTranslateY(value);
                case SCALE_X -> node.setScaleX(value);
                case SCALE_Y -> node.setScaleY(value);
                case OPACITY -> node.setOpacity(Math.max(0, Math.min(1, value)));
            }
        }
    }

    private static final class LiquidGlassSurface {
        private final Region region;
        private final Pane backdrop;
        private final DropShadow shadow;
        private double mouseXRatio = 0.18;
        private double mouseYRatio = 0.0;
        private double targetXRatio = 0.18;
        private double targetYRatio = 0.0;
        private double shimmerEnergy = 0.0;
        private double targetEnergy = 0.0;

        // Dirty flag: true when mouse is active over this surface
        private boolean mouseActive = false;
        // Last rendered values for dirty-checking
        private double lastShineX = -1;
        private double lastShineY = -1;
        private double lastSpecular = -1;
        private double lastBlob = -1;

        private LiquidGlassSurface(Region region, Pane backdrop) {
            this.region = region;
            this.backdrop = backdrop;
            this.shadow = new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(15, 23, 42, 0.10), 24, 0.18, 0, 10);
            region.setEffect(shadow);
            region.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
            region.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
                targetXRatio = 0.18;
                targetYRatio = 0.0;
                targetEnergy = 0.0;
                mouseActive = true; // need to animate back to resting state
                UiMotion.ensureTimerRunning();
            });
        }

        private void onMouseMoved(MouseEvent event) {
            targetXRatio = Math.max(0.0, Math.min(1.0, event.getX() / Math.max(region.getWidth(), 1)));
            targetYRatio = Math.max(0.0, Math.min(1.0, event.getY() / Math.max(region.getHeight(), 1)));
            targetEnergy = 1.0;
            mouseActive = true;
            UiMotion.ensureTimerRunning();
        }

        /** True while there is still visible change to render. */
        boolean isActive() {
            if (!mouseActive) return false;
            boolean energySettled   = Math.abs(shimmerEnergy - targetEnergy) < 0.002;
            boolean xSettled        = Math.abs(mouseXRatio - targetXRatio) < 0.002;
            boolean ySettled        = Math.abs(mouseYRatio - targetYRatio) < 0.002;
            if (energySettled && xSettled && ySettled) {
                mouseActive = false; // fully at rest
                return false;
            }
            return true;
        }

        private void step(double dt) {
            mouseXRatio += (targetXRatio - mouseXRatio) * Math.min(1, dt * 9.5);
            mouseYRatio += (targetYRatio - mouseYRatio) * Math.min(1, dt * 9.5);
            shimmerEnergy += (targetEnergy - shimmerEnergy) * Math.min(1, dt * 6.0);

            double blobInfluence = calculateBlobInfluence();
            double specularOpacity = 0.10 + shimmerEnergy * 0.22 + blobInfluence * 0.18;
            double shineX = mouseXRatio * 100.0;
            double shineY = mouseYRatio * 100.0;

            // Only call setStyle() when values actually changed (avoids constant repaints)
            double delta = Math.abs(shineX - lastShineX) + Math.abs(shineY - lastShineY)
                    + Math.abs(specularOpacity - lastSpecular) + Math.abs(blobInfluence - lastBlob);
            if (delta > 0.3) {
                double refractionOpacity = 0.22 + blobInfluence * 0.24;
                String style = String.format(
                        "-fx-background-color: "
                                + "radial-gradient(center %.1f%% %.1f%%, radius 38%%, rgba(255,255,255,%.3f), rgba(255,255,255,0.00) 68%%), "
                                + "radial-gradient(center %.1f%% %.1f%%, radius 72%%, rgba(56,189,248,%.3f), rgba(167,139,250,%.3f) 42%%, rgba(255,255,255,0.00) 76%%), "
                                + "rgba(255,255,255,0.70);"
                                + "-fx-border-color: rgba(255,255,255,0.74);"
                                + "-fx-border-width: 1.5;"
                                + "-fx-background-radius: 24;"
                                + "-fx-border-radius: 24;",
                        shineX, shineY, specularOpacity,
                        100 - shineX * 0.42, 100 - shineY * 0.26, refractionOpacity, refractionOpacity * 0.62);
                region.setStyle(style);

                shadow.setRadius(22 + blobInfluence * 16);
                shadow.setSpread(0.12 + blobInfluence * 0.16);
                shadow.setOffsetY(8 + shimmerEnergy * 4);
                shadow.setColor(Color.rgb(14, 165, 233, 0.10 + blobInfluence * 0.18));

                lastShineX   = shineX;
                lastShineY   = shineY;
                lastSpecular = specularOpacity;
                lastBlob     = blobInfluence;
            }
        }

        private double calculateBlobInfluence() {
            if (region.getScene() == null || backdrop.getScene() == null || region.getWidth() <= 0 || region.getHeight() <= 0) {
                return 0;
            }
            Bounds cardBounds = region.localToScene(region.getBoundsInLocal());
            double cardCenterX = cardBounds.getMinX() + cardBounds.getWidth() / 2.0;
            double cardCenterY = cardBounds.getMinY() + cardBounds.getHeight() / 2.0;
            double strongest = 0;
            for (Node node : backdrop.getChildren()) {
                if (!(node instanceof Circle circle)) {
                    continue;
                }
                Bounds blobBounds = circle.localToScene(circle.getBoundsInLocal());
                double blobCenterX = blobBounds.getMinX() + blobBounds.getWidth() / 2.0;
                double blobCenterY = blobBounds.getMinY() + blobBounds.getHeight() / 2.0;
                double dx = cardCenterX - blobCenterX;
                double dy = cardCenterY - blobCenterY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double radius = Math.max(circle.getRadius(), 1);
                strongest = Math.max(strongest, Math.max(0, 1.0 - distance / (radius * 1.35)));
            }
            return Math.min(1, strongest);
        }
    }
}
