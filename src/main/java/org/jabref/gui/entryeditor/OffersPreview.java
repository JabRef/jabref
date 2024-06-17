package org.jabref.gui.entryeditor;

public interface OffersPreview {

    /**
     * Switch to next Preview style - should be overriden if a EntryEditorTab is actually showing a preview
     */
    void nextPreviewStyle();

    /**
     * Switch to previous Preview style - should be overriden if a EntryEditorTab is actually showing a preview
     */
    void previousPreviewStyle();
}
