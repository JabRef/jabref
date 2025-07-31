package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.effects.BackdropHighlight;
import org.jabref.gui.walkthrough.effects.FullScreenDarken;
import org.jabref.gui.walkthrough.effects.PulseAnimateIndicator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class WalkthroughHighlighter {
    private final Map<Window, BackdropHighlight> backdropHighlights = new HashMap<>();
    private final Map<Window, PulseAnimateIndicator> pulseIndicators = new HashMap<>();
    private final Map<Window, FullScreenDarken> fullScreenDarkens = new HashMap<>();

    private final Map<Window, EffectState> currentEffects = new HashMap<>();
    private final WalkthroughPane.Supplier paneSupplier;
    private @Nullable Runnable onBackgroundClickHandler;

    public WalkthroughHighlighter(WalkthroughPane.Supplier paneSupplier) {
        this.paneSupplier = paneSupplier;
    }

    /// Applies the specified highlight configuration.
    ///
    /// @param config         The highlight configuration to apply. Default to
    ///                       BackdropHighlight on the primary windows if null.
    /// @param scene          The primary scene to apply the highlight to.
    /// @param fallbackTarget The fallback target node to use if no highlight
    ///                       configuration is provided.
    public synchronized void applyHighlight(@Nullable WalkthroughEffect config, @NonNull Scene scene, @Nullable Node fallbackTarget) {
        Map<Window, EffectState> newEffects = computeNewEffects(config, scene, fallbackTarget);

        Map<Window, EffectState> toUpdate = new HashMap<>();
        Map<Window, EffectState> toCreate = new HashMap<>();
        Map<Window, EffectState> toRemove = new HashMap<>();

        diffEffects(currentEffects, newEffects, toUpdate, toCreate, toRemove);

        toRemove.forEach((window, _) -> detach(window));

        toUpdate.forEach(this::updateExistingEffect);

        toCreate.forEach((window, newState) -> applyEffect(window, newState.effect, newState.targetNode));

        currentEffects.clear();
        currentEffects.putAll(newEffects);
    }

    /// Sets a handler to be called when the user clicks on backdrop or darkened areas.
    ///
    /// @param handler The handler to call when the background is clicked. If null, no
    ///                action will be taken on background clicks. Usually used to
    ///                support quit walkthrough on clicking the effects.
    public void setOnBackgroundClick(@Nullable Runnable handler) {
        this.onBackgroundClickHandler = handler;
    }

    /// Detaches the specified window from all effects. Restore the scene graph on this
    /// window to the state before any effects were applied.
    public void detach(@NonNull Window window) {
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

        currentEffects.remove(window);
    }

    /// Detaches all effects from all windows. See [WalkthroughHighlighter#detach].
    public void detachAll() {
        backdropHighlights.values().forEach(BackdropHighlight::detach);
        backdropHighlights.clear();

        pulseIndicators.values().forEach(PulseAnimateIndicator::detach);
        pulseIndicators.clear();

        fullScreenDarkens.values().forEach(FullScreenDarken::detach);
        fullScreenDarkens.clear();

        currentEffects.clear();
    }

    private Map<Window, EffectState> computeNewEffects(@Nullable WalkthroughEffect config, @NonNull Scene fallbackWindow, @Nullable Node fallbackTarget) {
        Map<Window, EffectState> newEffects = new HashMap<>();

        if (config == null) {
            if (fallbackTarget != null) {
                newEffects.put(fallbackWindow.getWindow(),
                        new EffectState(HighlightEffect.BACKDROP_HIGHLIGHT, fallbackTarget));
            }
            return newEffects;
        }

        if (config.windowEffects().isEmpty() && config.fallbackEffect().isPresent()) {
            newEffects.put(fallbackWindow.getWindow(),
                    new EffectState(config.fallbackEffect().get(), fallbackTarget));
            return newEffects;
        }

        config.windowEffects().forEach(effect -> {
            Window window = effect.windowResolver().flatMap(WindowResolver::resolve).orElse(fallbackWindow.getWindow());
            Node targetNode = effect
                    .targetNodeResolver()
                    .flatMap(resolver -> resolver.resolve(window.getScene() != null ? window.getScene() : fallbackWindow))
                    .orElse(fallbackTarget);

            if (targetNode != null || effect.effect() == HighlightEffect.FULL_SCREEN_DARKEN) {
                newEffects.put(window, new EffectState(effect.effect(), targetNode));
            }
        });

        return newEffects;
    }

    private void diffEffects(Map<Window, EffectState> current,
                             Map<Window, EffectState> desired,
                             Map<Window, EffectState> toUpdate,
                             Map<Window, EffectState> toCreate,
                             Map<Window, EffectState> toRemove) {
        current.forEach((window, currentState) -> {
            EffectState desiredState = desired.get(window);
            if (desiredState == null) {
                toRemove.put(window, currentState);
            } else if (currentState.canTransitionTo(desiredState)) {
                toUpdate.put(window, desiredState);
            } else {
                toRemove.put(window, currentState);
                toCreate.put(window, desiredState);
            }
        });

        desired.forEach((window, desiredState) -> {
            if (!current.containsKey(window)) {
                toCreate.put(window, desiredState);
            }
        });
    }

    private void updateExistingEffect(@NonNull Window window, @NonNull EffectState newState) {
        switch (newState.effect) {
            case BACKDROP_HIGHLIGHT -> {
                BackdropHighlight backdrop = backdropHighlights.get(window);
                if (backdrop != null && newState.targetNode != null) {
                    backdrop.transitionTo(newState.targetNode);
                }
            }
            case ANIMATED_PULSE -> {
                PulseAnimateIndicator pulse = pulseIndicators.get(window);
                if (pulse != null && newState.targetNode != null) {
                    pulse.transitionTo(newState.targetNode);
                }
            }
            case FULL_SCREEN_DARKEN -> {
                // FullScreenDarken doesn't need updates as it has no target node
            }
            case NONE -> detach(window);
        }
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
            case FULL_SCREEN_DARKEN -> applyFullScreenDarken(window);
            case NONE -> detach(window);
        }
    }

    private void applyBackdropHighlight(@NonNull Window window, @NonNull Node targetNode) {
        WalkthroughPane pane = paneSupplier.get(window);
        pane.attach();

        BackdropHighlight backdrop = getOrCreateBackdropHighlight(window, pane);
        backdrop.setOnClick(onBackgroundClickHandler);
        backdrop.attach(targetNode);
    }

    private void applyPulseAnimation(@NonNull Window window, @NonNull Node targetNode) {
        WalkthroughPane pane = paneSupplier.get(window);
        pane.attach();

        PulseAnimateIndicator pulse = getOrCreatePulseIndicator(window, pane);
        pulse.attach(targetNode);
    }

    private void applyFullScreenDarken(@NonNull Window window) {
        WalkthroughPane pane = paneSupplier.get(window);
        pane.attach();

        FullScreenDarken fullDarken = getOrCreateFullScreenDarken(window, pane);
        fullDarken.setOnClick(onBackgroundClickHandler);
        fullDarken.attach();
    }

    private BackdropHighlight getOrCreateBackdropHighlight(@NonNull Window window, @NonNull WalkthroughPane pane) {
        return backdropHighlights.computeIfAbsent(window, _ -> new BackdropHighlight(pane));
    }

    private PulseAnimateIndicator getOrCreatePulseIndicator(@NonNull Window window, @NonNull WalkthroughPane pane) {
        return pulseIndicators.computeIfAbsent(window, _ -> new PulseAnimateIndicator(pane));
    }

    private FullScreenDarken getOrCreateFullScreenDarken(@NonNull Window window, @NonNull WalkthroughPane pane) {
        return fullScreenDarkens.computeIfAbsent(window, _ -> new FullScreenDarken(pane));
    }

    private record EffectState(HighlightEffect effect, @Nullable Node targetNode) {
        boolean canTransitionTo(EffectState other) {
            return effect == other.effect;
        }
    }
}
