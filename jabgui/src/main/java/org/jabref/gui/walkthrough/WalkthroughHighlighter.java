package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.components.BackdropHighlight;
import org.jabref.gui.walkthrough.components.FullScreenDarken;
import org.jabref.gui.walkthrough.components.PulseAnimateIndicator;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages highlight effects across multiple windows for walkthrough steps.
 */
public class WalkthroughHighlighter {
    private final Map<Window, BackdropHighlight> backdropHighlights = new HashMap<>();
    private final Map<Window, PulseAnimateIndicator> pulseIndicators = new HashMap<>();
    private final Map<Window, FullScreenDarken> fullScreenDarkens = new HashMap<>();

    /**
     * Applies the specified highlight configuration.
     *
     * @param mainScene       The primary scene to apply the highlight to.
     * @param highlightConfig The highlight configuration to apply. Default to
     *                        BackdropHighlight on the primary windows if null.
     * @param fallbackTarget  The fallback target node to use if no highlight
     *                        configuration is provided.
     */
    public void applyHighlight(@NonNull Scene mainScene,
                               @Nullable MultiWindowHighlight highlightConfig,
                               @Nullable Node fallbackTarget) {
        detachAll();

        if (highlightConfig != null) {
            if (highlightConfig.windowEffects().isEmpty() && highlightConfig.fallbackEffect().isPresent()) {
                applyEffect(mainScene.getWindow(), highlightConfig.fallbackEffect().get(), fallbackTarget);
                return;
            }
            highlightConfig.windowEffects().forEach(effect -> {
                Window window = effect.windowResolver().flatMap(WindowResolver::resolve).orElse(mainScene.getWindow());
                Node targetNode = effect
                        .targetNodeResolver()
                        .flatMap(resolver -> resolver.resolve(window.getScene() != null ? window.getScene() : mainScene))
                        .orElse(fallbackTarget);
                applyEffect(window, effect.effect(), targetNode);
            });
        } else {
            if (fallbackTarget != null) {
                applyBackdropHighlight(mainScene.getWindow(), fallbackTarget);
            }
        }
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
                if (backdropHighlights.containsKey(window)) {
                    backdropHighlights.get(window).detach();
                    backdropHighlights.remove(window);
                }
                if (pulseIndicators.containsKey(window)) {
                    pulseIndicators.get(window).detach();
                    pulseIndicators.remove(window);
                }
                if (fullScreenDarkens.containsKey(window)) {
                    fullScreenDarkens.get(window).detach();
                    fullScreenDarkens.remove(window);
                }
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
