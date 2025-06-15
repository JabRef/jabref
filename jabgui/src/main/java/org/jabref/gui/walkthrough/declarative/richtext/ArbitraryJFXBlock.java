package org.jabref.gui.walkthrough.declarative.richtext;

import java.util.function.Function;

import javafx.scene.Node;

import org.jabref.gui.walkthrough.Walkthrough;

public record ArbitraryJFXBlock(Function<Walkthrough, Node> componentFactory)
        implements WalkthroughRichTextBlock {
}
