package com.incors.plaf;


/*
 * This code was contributed by Sebastian Ferreyra (sebastianf@citycolor.net).
 * It is published under the terms of the GNU Lesser General Public License.
 */
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.ColorModel;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class FastGradientPaint implements Paint
{
    //~ Instance fields ////////////////////////////////////////////////////////

    boolean isVertical;
    int endColor;
    int startColor;

    //~ Constructors ///////////////////////////////////////////////////////////

    public FastGradientPaint(Color sc, Color ec, boolean isV)
    {
        startColor = sc.getRGB();
        endColor = ec.getRGB();
        isVertical = isV;
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public int getTransparency()
    {
        return ((((startColor & endColor) >> 24) & 0xFF) == 0xFF) ? OPAQUE
                                                                  : TRANSLUCENT;
    }

    public synchronized PaintContext createContext(ColorModel cm, Rectangle r,
        Rectangle2D r2d, AffineTransform xform, RenderingHints hints)
    {
        return new FastGradientPaintContext(cm, r, startColor, endColor,
            isVertical);
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
