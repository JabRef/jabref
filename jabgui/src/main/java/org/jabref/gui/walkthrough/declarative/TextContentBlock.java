package org.jabref.gui.walkthrough.declarative;

/**
 * A text content block for displaying plain text in walkthrough steps.
 */
public class TextContentBlock extends WalkthroughContentBlock {
    private final String text;

    public TextContentBlock(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.TEXT;
    }
}
