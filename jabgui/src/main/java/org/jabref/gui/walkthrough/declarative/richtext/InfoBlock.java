package org.jabref.gui.walkthrough.declarative.richtext;

import org.jspecify.annotations.NonNull;

public record InfoBlock(
        @NonNull String text) implements WalkthroughRichTextBlock {
}
