package org.jabref.gui.walkthrough.declarative.effect;

import java.util.Optional;

import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;

/**
 * Represents a highlight effect configuration for a specific window.
 */
public record WindowEffect(
        Optional<WindowResolver> windowResolver,
        HighlightEffect effect,
        Optional<NodeResolver> targetNodeResolver
) {
    public WindowEffect(HighlightEffect effect) {
        this(Optional.empty(), effect, Optional.empty());
    }

    public WindowEffect(WindowResolver windowResolver, HighlightEffect effect) {
        this(Optional.of(windowResolver), effect, Optional.empty());
    }

    public WindowEffect(WindowResolver windowResolver, HighlightEffect effect, NodeResolver targetNodeResolver) {
        this(Optional.of(windowResolver), effect, Optional.of(targetNodeResolver));
    }
}
