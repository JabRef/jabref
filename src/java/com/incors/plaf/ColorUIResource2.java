package com.incors.plaf;


/**
 * This class had to be created because <code>ColorUIResouce</code> does not allow
 * transparency. Hopefully one day support for transparency will be added to we
 * ColorUIResouce and we can get rid of this class. Wrapping a <code>Color</color>
 * object to make a pseudo subclass is very ugly.
 */
import java.awt.Color;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

import javax.swing.plaf.ColorUIResource;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class ColorUIResource2 extends ColorUIResource
{
    //~ Instance fields ////////////////////////////////////////////////////////

    private Color myColor;

    //~ Constructors ///////////////////////////////////////////////////////////

    // constructors
    public ColorUIResource2(Color c)
    {
        super(0, 0, 0);
        myColor = c;
    }

    public ColorUIResource2(int r, int g, int b)
    {
        super(0, 0, 0);
        myColor = new Color(r, g, b);
    }

    public ColorUIResource2(int r, int g, int b, int a)
    {
        super(0, 0, 0);
        myColor = new Color(r, g, b, a);
    }

    public ColorUIResource2(int rgb)
    {
        super(0, 0, 0);
        myColor = new Color(rgb);
    }

    public ColorUIResource2(int rgba, boolean hasalpha)
    {
        super(0, 0, 0);
        myColor = new Color(rgba, hasalpha);
    }

    public ColorUIResource2(float r, float g, float b)
    {
        super(0, 0, 0);
        myColor = new Color(r, g, b);
    }

    public ColorUIResource2(float r, float g, float b, float alpha)
    {
        super(0, 0, 0);
        myColor = new Color(r, g, b, alpha);
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public int getAlpha()
    {
        return myColor.getAlpha();
    }

    public int getBlue()
    {
        return myColor.getBlue();
    }

    public float[] getColorComponents(float[] compArray)
    {
        return myColor.getColorComponents(compArray);
    }

    public float[] getColorComponents(ColorSpace cspace, float[] compArray)
    {
        return myColor.getColorComponents(cspace, compArray);
    }

    public ColorSpace getColorSpace()
    {
        return myColor.getColorSpace();
    }

    public float[] getComponents(float[] compArray)
    {
        return myColor.getComponents(compArray);
    }

    public float[] getComponents(ColorSpace cspace, float[] compArray)
    {
        return myColor.getComponents(cspace, compArray);
    }

    public int getGreen()
    {
        return myColor.getGreen();
    }

    public int getRGB()
    {
        return myColor.getRGB();
    }

    public float[] getRGBColorComponents(float[] compArray)
    {
        return myColor.getRGBColorComponents(compArray);
    }

    public float[] getRGBComponents(float[] compArray)
    {
        return myColor.getRGBComponents(compArray);
    }

    // non static methods
    public int getRed()
    {
        return myColor.getRed();
    }

    public int getTransparency()
    {
        return myColor.getTransparency();
    }

    public Color brighter()
    {
        return myColor.brighter();
    }

    public PaintContext createContext(ColorModel cm, Rectangle r,
        Rectangle2D r2d, AffineTransform xform, RenderingHints hints)
    {
        return myColor.createContext(cm, r, r2d, xform, hints);
    }

    public Color darker()
    {
        return myColor.darker();
    }

    public boolean equals(Object obj)
    {
        return myColor.equals(obj);
    }

    public int hashCode()
    {
        return myColor.hashCode();
    }

    public String toString()
    {
        return myColor.toString();
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
