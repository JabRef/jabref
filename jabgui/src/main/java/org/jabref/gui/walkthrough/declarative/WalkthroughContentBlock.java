package org.jabref.gui.walkthrough.declarative;

/**
 * Base class for walkthrough content blocks that can be displayed in walkthrough steps.
 */
public abstract class WalkthroughContentBlock {
    public abstract ContentBlockType getType();

    public enum ContentBlockType {
        TEXT,
        INFO_BLOCK
    }
}
