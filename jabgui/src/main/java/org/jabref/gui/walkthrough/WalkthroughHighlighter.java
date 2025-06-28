package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;
import org.jabref.gui.walkthrough.effects.BackdropHighlight;
import org.jabref.gui.walkthrough.effects.FullScreenDarken;
import org.jabref.gui.walkthrough.effects.PulseAnimateIndicator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages highlight effects across multiple windows for walkthrough steps.
 */
public class WalkthroughHighlighter {
    // backdropHighlights, pulseIndicators, and fullScreenDarkens represents the current state of walkthrough GUI effects
    private final Map<Window, BackdropHighlight> backdropHighlights = new HashMap<>();
    private final Map<Window, PulseAnimateIndicator> pulseIndicators = new HashMap<>();
    private final Map<Window, FullScreenDarken> fullScreenDarkens = new HashMap<>();

    /**
     * Applies the specified highlight configuration.
     *
     * @param config         The highlight configuration to apply. Default to
     *                       BackdropHighlight on the primary windows if null.
     * @param fallbackWindow The primary scene to apply the highlight to.
     * @param fallbackTarget The fallback target node to use if no highlight
     *                       configuration is provided.
     */
    public void applyHighlight(@Nullable MultiWindowHighlight config, @NonNull Scene fallbackWindow,
                               @Nullable Node fallbackTarget) {
        detachAll();

        if (config == null) {
            if (fallbackTarget != null) {
                applyBackdropHighlight(fallbackWindow.getWindow(), fallbackTarget);
            }
            return;
        }

        if (config.windowEffects().isEmpty() && config.fallbackEffect().isPresent()) {
            applyEffect(fallbackWindow.getWindow(), config.fallbackEffect().get(), fallbackTarget);
            return;
        }

        config.windowEffects().forEach(effect -> {
            Window window = effect.windowResolver().flatMap(WindowResolver::resolve).orElse(fallbackWindow.getWindow());
            Node targetNode = effect
                    .targetNodeResolver()
                    .flatMap(resolver -> resolver.resolve(window.getScene() != null ? window.getScene() : fallbackWindow))
                    .orElse(fallbackTarget);
            applyEffect(window, effect.effect(), targetNode);
        });
    }

    /**
     * Detaches all active highlight effects.
     */
    public void detachAll() {
        backdropHighlights.values().forEach(BackdropHighlight::detach);
        backdropHighlights.clear();

        pulseIndicators.values().forEach(PulseAnimateIndicator::detach);
        pulseIndicators.clear();

        fullScreenDarkens.values().forEach(FullScreenDarken::detach);
        fullScreenDarkens.clear();
    }

    private void applyEffect(@NonNull Window window, @NonNull HighlightEffect effect, @Nullable Node targetNode) {
        switch (effect) {
            case BACKDROP_HIGHLIGHT -> {
                if (targetNode != null) {
                    applyBackdropHighlight(window, targetNode);
                }
            }
            case ANIMATED_PULSE -> {
                if (targetNode != null) {
                    applyPulseAnimation(window, targetNode);
                }
            }
            case FULL_SCREEN_DARKEN ->
                    applyFullScreenDarken(window);
            case NONE -> {
                backdropHighlights.computeIfPresent(window, (_, highlight) -> {
                    highlight.detach();
                    return null;
                });
                pulseIndicators.computeIfPresent(window, (_, indicator) -> {
                    indicator.detach();
                    return null;
                });
                fullScreenDarkens.computeIfPresent(window, (_, darken) -> {
                    darken.detach();
                    return null;
                });
            }
        }
    }

    private void applyBackdropHighlight(@NonNull Window window, @NonNull Node targetNode) {
        Scene scene = window.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane pane)) {
            return;
        }

        BackdropHighlight backdrop = backdropHighlights.computeIfAbsent(window, _ -> new BackdropHighlight(pane));
        backdrop.attach(targetNode);
    }

    private void applyPulseAnimation(@NonNull Window window, @NonNull Node targetNode) {
        Scene scene = window.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane pane)) {
            return;
        }

        PulseAnimateIndicator pulse = pulseIndicators.computeIfAbsent(window, _ -> new PulseAnimateIndicator(pane));
        pulse.attach(targetNode);
    }

    private void applyFullScreenDarken(@NonNull Window window) {
        Scene scene = window.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane pane)) {
            return;
        }

        FullScreenDarken fullDarken = fullScreenDarkens.computeIfAbsent(window, _ -> new FullScreenDarken(pane));
        fullDarken.attach();
    }
}
