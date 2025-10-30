package org.jabref.gui.walkthrough.declarative.effect;

import java.util.Optional;

import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;

import org.jspecify.annotations.NonNull;

public record WindowEffect(
        @NonNull Optional<WindowResolver> windowResolver,
        @NonNull HighlightEffect effect,
        @NonNull Optional<NodeResolver> targetNodeResolver
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
