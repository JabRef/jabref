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
import javax.swing.plaf.metal.*;

public class KunststoffToggleButtonUI extends MetalToggleButtonUI {
  private final static KunststoffToggleButtonUI buttonUI = new KunststoffToggleButtonUI();

  public static ComponentUI createUI(JComponent c) {
    return buttonUI;
  }

  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    if (!c.isOpaque()) {
      return; // we only draw the gradients if the component is opaque
    }

    Component parent = c.getParent();

    if(parent instanceof JToolBar) {
      int orientation = ((JToolBar) parent).getOrientation();
      Point loc = c.getLocation();
      Rectangle rectReflection;
      Rectangle rectShadow;
      Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
      Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();

      if(orientation == SwingConstants.HORIZONTAL) {
        // paint upper gradient
        if (colorReflection != null) {
          Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
          rectReflection = new Rectangle(0, -loc.y, parent.getWidth(), parent.getHeight()/2);
          KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rectReflection, true);
        }
        // paint lower gradient
        if (colorShadow != null) {
          Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
          rectShadow = new Rectangle(0, parent.getHeight()/2 - loc.y, parent.getWidth(), parent.getHeight()/2);
          KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rectShadow, true);
        }
      } else {  // if tool bar orientation is vertical
        // paint left gradient
        if (colorReflection != null) {
          Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
          rectReflection = new Rectangle(0, 0, parent.getWidth()/2, parent.getHeight());
          KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rectReflection, false);
        }
        // paint right gradient
        if (colorShadow != null) {
          Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
          rectShadow = new Rectangle(parent.getWidth()/2 - loc.x, 0, parent.getWidth(), parent.getHeight());
          KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rectShadow, false);
        }
      }
    } else { // if not in tool bar
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

}