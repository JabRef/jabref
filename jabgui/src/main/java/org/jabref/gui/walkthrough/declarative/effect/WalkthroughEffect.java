package org.jabref.gui.walkthrough.declarative.effect;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

public record WalkthroughEffect(@NonNull List<WindowEffect> windowEffects,
                                Optional<HighlightEffect> fallbackEffect) {
    public WalkthroughEffect(WindowEffect windowEffect) {
        this(windowEffect, HighlightEffect.FULL_SCREEN_DARKEN);
    }

    public WalkthroughEffect(WindowEffect windowEffect, HighlightEffect fallback) {
        this(List.of(windowEffect), Optional.of(fallback));
    }

    public WalkthroughEffect(WindowEffect... windowEffects) {
        this(HighlightEffect.FULL_SCREEN_DARKEN, windowEffects);
    }

    public WalkthroughEffect(HighlightEffect fallback, WindowEffect... windowEffects) {
        this(List.of(windowEffects), Optional.of(fallback));
    }
}
