package org.jabref.gui.documentviewer;

import javafx.scene.image.Image;

/**
 * Represents the view model for a page in the document viewer.
 */
public abstract class DocumentPageViewModel {

    /**
     * Renders this page and returns an image representation of itself.
     */
    public abstract Image render();
}
