package org.jabref.gui.documentviewer;

import javafx.scene.image.Image;

/**
 * Represents the view model for a page in the document viewer.
 */
public abstract class DocumentPageViewModel {

    /**
     * Renders this page and returns an image representation of itself.
     * @param width
     * @param height
     */
    public abstract Image render(int width, int height);

    /**
     * Get the page number of the current page in the document.
     */
    public abstract int getPageNumber();

    /**
     * Calculates the aspect ratio (width / height) of the page.
     */
    public abstract double getAspectRatio();
}
