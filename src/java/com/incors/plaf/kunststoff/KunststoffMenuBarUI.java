package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class KunststoffMenuBarUI extends BasicMenuBarUI {

  public static ComponentUI createUI(JComponent x) {
    return new KunststoffMenuBarUI();
  }

  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);
    // paint upper gradient
    Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
    if (colorReflection != null) {
      Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
      Rectangle rect = new Rectangle(0, 0, c.getWidth(), c.getHeight()/2);
      KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rect, true);
    }

    // paint lower gradient
    Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
    if (colorShadow != null) {
      Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
      Rectangle rect = new Rectangle(0, c.getHeight()/2, c.getWidth(), c.getHeight()/2);
      KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rect, true);
    }
  }

}