package net.sf.pdfboxbridge;

import java.awt.geom.Rectangle2D;

public class AwtRectangleBridge {

    /**
     *
     * This method is a bridge to the awt.geom packages Rectangle2D.Float()
     * Note: This is a quick hack to enforce the architecture constraint that no awt classes are used in logic code
     *
     * @param upperLeftX
     * @param upperLeftY
     * @param width
     * @param height
     * @return a Rectangle2D.FLoat with the given rectangles dimensions
     */
    public static Rectangle2D PDFBoxRectangleToAwtRectangle2DFloat (final float upperLeftX, final float upperLeftY,
            final float width, final float height) {
        return new Rectangle2D.Float(upperLeftX, upperLeftY, width, height);
    }
}
