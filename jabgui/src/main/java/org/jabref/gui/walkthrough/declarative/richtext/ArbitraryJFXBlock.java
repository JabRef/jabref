package org.jabref.gui.walkthrough.declarative.richtext;

import java.util.function.BiFunction;

import javafx.scene.Node;

import org.jabref.gui.walkthrough.Walkthrough;

public record ArbitraryJFXBlock(
        BiFunction<Walkthrough, Runnable, Node> componentFactory)
        implements WalkthroughRichTextBlock {
}
