package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 *
 * Thanks to Christoph Wilhelms for providing a fic for a bug that screwed up
 * the menu bar's ui after setting a menu disabled.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class KunststoffMenuUI extends BasicMenuUI {

  public static ComponentUI createUI(JComponent x) {
    return new KunststoffMenuUI();
  }

  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);
    Container parent = menuItem.getParent();
    if (c.isOpaque() && parent != null && parent instanceof JMenuBar) {
      Point loc = c.getLocation();
      Graphics2D g2D = (Graphics2D) g;

      // paint upper gradient
      Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
      if (colorReflection != null) {
        Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
        Rectangle drawRect = new Rectangle(0, 0, parent.getWidth(), parent.getHeight()/2);
        Rectangle gradRect = new Rectangle(0, -loc.y, parent.getWidth(), parent.getHeight()/2);
        KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, drawRect, gradRect, true);
      }

      // paint lower gradient
      Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
      if (colorShadow != null) {
        Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
        Rectangle drawRect = new Rectangle(0, parent.getHeight()/2, parent.getWidth(), parent.getHeight()/2);
        Rectangle gradRect = new Rectangle(0, parent.getHeight()/2-loc.y, parent.getWidth(), parent.getHeight()/2);
        KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, drawRect, gradRect, true);
      }
    }
  }

}