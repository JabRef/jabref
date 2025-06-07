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
    public static MultiWindowHighlight single(WindowEffect windowEffect) {
        return single(windowEffect, HighlightEffect.FULL_SCREEN_DARKEN);
    }

    public static MultiWindowHighlight single(WindowEffect windowEffect, HighlightEffect fallback) {
        return new MultiWindowHighlight(List.of(windowEffect), Optional.of(fallback));
    }

    public static MultiWindowHighlight multiple(WindowEffect... windowEffects) {
        return multiple(HighlightEffect.FULL_SCREEN_DARKEN, windowEffects);
    }

    public static MultiWindowHighlight multiple(HighlightEffect fallback, WindowEffect... windowEffects) {
        return new MultiWindowHighlight(List.of(windowEffects), Optional.of(fallback));
    }
}
