package org.jabref.gui.walkthrough.declarative.effect;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

public record MultiWindowHighlight(
        @NonNull List<WindowEffect> windowEffects,
        Optional<HighlightEffect> fallbackEffect
) {
    public MultiWindowHighlight(WindowEffect windowEffect) {
        this(windowEffect, HighlightEffect.FULL_SCREEN_DARKEN);
    }

    public MultiWindowHighlight(WindowEffect windowEffect, HighlightEffect fallback) {
        this(List.of(windowEffect), Optional.of(fallback));
    }

    public MultiWindowHighlight(WindowEffect... windowEffects) {
        this(HighlightEffect.FULL_SCREEN_DARKEN, windowEffects);
    }

    public MultiWindowHighlight(HighlightEffect fallback, WindowEffect... windowEffects) {
        this(List.of(windowEffects), Optional.of(fallback));
    }
}
