package com.incors.plaf.kunststoff;

import javax.swing.plaf.*;

import com.incors.plaf.*;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class KunststoffGradientTheme implements GradientTheme
{
    //~ Instance fields ////////////////////////////////////////////////////////

    // gradient colors
    private final ColorUIResource componentGradientColorReflection = new ColorUIResource2(255,
            255, 255, 96);
    private final ColorUIResource componentGradientColorShadow = new ColorUIResource2(0,
            0, 0, 48);
    private final ColorUIResource textComponentGradientColorReflection = new ColorUIResource2(0,
            0, 0, 32);
    private final ColorUIResource textComponentGradientColorShadow = null;
    private final int backgroundGradientShadow = 32;

    //~ Methods ////////////////////////////////////////////////////////////////

    public int getBackgroundGradientShadow()
    {
        return backgroundGradientShadow;
    }

    // methods for getting gradient colors
    public ColorUIResource getComponentGradientColorReflection()
    {
        return componentGradientColorReflection;
    }

    public ColorUIResource getComponentGradientColorShadow()
    {
        return componentGradientColorShadow;
    }

    // methods
    public String getName()
    {
        return "Default Kunststoff Gradient Theme";
    }

    public ColorUIResource getTextComponentGradientColorReflection()
    {
        return textComponentGradientColorReflection;
    }

    public ColorUIResource getTextComponentGradientColorShadow()
    {
        return textComponentGradientColorShadow;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
