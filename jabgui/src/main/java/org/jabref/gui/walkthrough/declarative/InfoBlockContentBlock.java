package org.jabref.gui.walkthrough.declarative;

// FIXME: Use a record class
/**
 * An info block content block for displaying highlighted information with icon in walkthrough steps.
 */
public class InfoBlockContentBlock extends WalkthroughContentBlock {
    private final String text;

    public InfoBlockContentBlock(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.INFO_BLOCK;
    }
}
