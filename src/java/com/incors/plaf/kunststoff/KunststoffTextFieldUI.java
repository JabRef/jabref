package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class KunststoffTextFieldUI extends BasicTextFieldUI{
  protected JComponent myComponent;

  public KunststoffTextFieldUI() {
    super();
  }

  KunststoffTextFieldUI(JComponent c) {
    super();
    myComponent = c;
  }

  public static ComponentUI createUI(JComponent c) {
    return new KunststoffTextFieldUI(c);
  }

  protected void paintBackground(Graphics g) {
    super.paintBackground(g);

    // paint upper gradient
    Color colorReflection = KunststoffLookAndFeel.getTextComponentGradientColorReflection();
    if (colorReflection != null) {
      Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
      Rectangle rect = new Rectangle(0, 0, myComponent.getWidth(), myComponent.getHeight()/2);
      KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rect, true);
    }

    // paint lower gradient
    Color colorShadow = KunststoffLookAndFeel.getTextComponentGradientColorShadow();
    if (colorShadow != null) {
      Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
      Rectangle rect = new Rectangle(0, myComponent.getHeight()/2, myComponent.getWidth(), myComponent.getHeight()/2);
      KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rect, true);
    }
  }
}