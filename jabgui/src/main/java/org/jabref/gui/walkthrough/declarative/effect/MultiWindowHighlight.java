package org.jabref.gui.walkthrough.declarative.effect;

import java.util.List;
import java.util.Optional;

/**
 * Highlighting effects across multiple windows.
 */
public record MultiWindowHighlight(
        List<WindowEffect> windowEffects,
        Optional<HighlightEffect> fallbackEffect
) {
    public MultiWindowHighlight(HighlightEffect effect) {
        this(new WindowEffect(effect));
    }

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
