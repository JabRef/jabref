package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com)
 * based on a contribution by C.J. Kent
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

public class KunststoffToolBarUI extends MetalToolBarUI {
  private final static KunststoffToolBarUI toolBarUI = new KunststoffToolBarUI();

  public static ComponentUI createUI(JComponent c) {
    return toolBarUI;
  }

  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    int orientation = SwingConstants.HORIZONTAL;

    if (c instanceof JToolBar) {
      orientation = ((JToolBar) c).getOrientation();
    }

    if (orientation == SwingConstants.HORIZONTAL) {

      // paint upper (reflection) gradient
      Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
      if (colorReflection != null) {
        Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
        Rectangle rect = new Rectangle(0, 0, c.getWidth(), c.getHeight()/2);
        KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rect, true);
      }
      // paint lower (shadow) gradient
      Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
      if (colorShadow != null) {
        Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
        Rectangle rect = new Rectangle(0, c.getHeight()/2, c.getWidth(), c.getHeight()/2);
        KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rect, true);
      }

    } else { // is orientation is vertical

      // paint left (reflection) gradient
      Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
      if (colorReflection != null) {
        Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
        Rectangle rect = new Rectangle(0, 0, c.getWidth()/2, c.getHeight());
        KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rect, false);
      }
      // paint right (shadow) gradient
      Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
      if (colorShadow != null) {
        Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
        Rectangle rect = new Rectangle(c.getWidth()/2, 0, c.getWidth()/2, c.getHeight());
        KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rect, false);
      }
    }

  }

}
