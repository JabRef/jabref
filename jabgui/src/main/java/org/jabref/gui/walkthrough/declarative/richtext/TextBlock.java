package org.jabref.gui.walkthrough.declarative.richtext;

import org.jspecify.annotations.NonNull;

public record TextBlock(
        @NonNull String text) implements WalkthroughRichTextBlock {
}
