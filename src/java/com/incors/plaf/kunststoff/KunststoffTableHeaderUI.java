package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 *
 * This class was originally contributed by Jens Niemeyer, jens@jensn.de
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class KunststoffTableHeaderUI extends BasicTableHeaderUI {

  public static ComponentUI createUI(JComponent h) {
    return new KunststoffTableHeaderUI();
  }

  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    if (!c.isOpaque()) {
      return; // we only draw the gradients if the component is opaque
    }

    // paint reflection
    Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
    if (colorReflection != null) {
      Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
      Rectangle rect = new Rectangle(0, 0, c.getWidth(), c.getHeight()/2);
      KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rect, true);
    }

    // paint shadow
    Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
    if (colorShadow != null) {
      Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
      Rectangle rect = new Rectangle(0, c.getHeight()/2, c.getWidth(), c.getHeight()/2);
      KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rect, true);
    }
  }
}

