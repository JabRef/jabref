package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.components.BackdropHighlight;
import org.jabref.gui.walkthrough.components.FullScreenDarken;
import org.jabref.gui.walkthrough.components.PulseAnimateIndicator;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;

import org.jspecify.annotations.NonNull;

/**
 * Manages highlight effects across multiple windows for walkthrough steps.
 */
public class HighlightManager {
    private final Map<Window, BackdropHighlight> backdropHighlights = new HashMap<>();
    private final Map<Window, PulseAnimateIndicator> pulseIndicators = new HashMap<>();
    private final Map<Window, FullScreenDarken> fullScreenDarkens = new HashMap<>();

    /**
     * Applies the specified highlight configuration.
     *
     * @param mainScene       The primary scene to apply the highlight to.
     * @param highlightConfig The optional highlight configuration to apply. Default to
     *                        BackdropHighlight on the primary windows if empty.
     * @param fallbackTarget  The optional fallback target node to use if no highlight
     *                        configuration is provided.
     */
    public void applyHighlight(@NonNull Scene mainScene,
                               Optional<MultiWindowHighlight> highlightConfig,
                               Optional<Node> fallbackTarget) {
        detachAll();

        highlightConfig.ifPresentOrElse(
                config -> {
                    if (config.windowEffects().isEmpty() && config.fallbackEffect().isPresent()) {
                        applyEffect(mainScene.getWindow(), config.fallbackEffect().get(), fallbackTarget);
                        return;
                    }
                    config.windowEffects().forEach(effect -> {
                        Window window = effect.windowResolver().resolve().orElse(mainScene.getWindow());
                        Optional<Node> targetNode = effect.targetNodeResolver()
                                                          .map(resolver ->
                                                                  resolver.resolve(Optional.ofNullable(window.getScene()).orElse(mainScene)))
                                                          .orElse(fallbackTarget);
                        applyEffect(window, effect.effect(), targetNode);
                    });
                },
                () -> fallbackTarget.ifPresent(node -> applyBackdropHighlight(mainScene.getWindow(), node))
        );
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

    private void applyEffect(Window window, HighlightEffect effect, Optional<Node> targetNode) {
        switch (effect) {
            case BACKDROP_HIGHLIGHT ->
                    targetNode.ifPresent(node -> applyBackdropHighlight(window, node));
            case ANIMATED_PULSE ->
                    targetNode.ifPresent(node -> applyPulseAnimation(window, node));
            case FULL_SCREEN_DARKEN -> applyFullScreenDarken(window);
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

    private void applyBackdropHighlight(Window window, Node targetNode) {
        Scene scene = window.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane pane)) {
            return;
        }

        BackdropHighlight backdrop = new BackdropHighlight(pane);
        backdrop.attach(targetNode);
        backdropHighlights.put(window, backdrop);
    }

    private void applyPulseAnimation(Window window, Node targetNode) {
        Scene scene = window.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane pane)) {
            return;
        }

        PulseAnimateIndicator pulse = new PulseAnimateIndicator(pane);
        pulse.attach(targetNode);
        pulseIndicators.put(window, pulse);
    }

    private void applyFullScreenDarken(Window window) {
        Scene scene = window.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane pane)) {
            return;
        }

        FullScreenDarken fullDarken = new FullScreenDarken(pane);
        fullDarken.attach();
        fullScreenDarkens.put(window, fullDarken);
    }
}
