package org.jabref.gui.walkthrough.declarative.richtext;

import java.util.function.BiFunction;

import javafx.scene.Node;

import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NonNull;

public record ArbitraryJFXBlock(
        @NonNull BiFunction<Walkthrough, Runnable, Node> componentFactory)
        implements WalkthroughRichTextBlock {
}
