package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

public class KunststoffScrollBarUI extends MetalScrollBarUI {

  public static ComponentUI createUI(JComponent c)    {
    return new KunststoffScrollBarUI();
  }

  protected JButton createDecreaseButton(int orientation) {
    decreaseButton = new KunststoffScrollButton(orientation, scrollBarWidth, isFreeStanding);
    return decreaseButton;
  }

  protected JButton createIncreaseButton(int orientation) {
    increaseButton =  new KunststoffScrollButton(orientation, scrollBarWidth, isFreeStanding);
    return increaseButton;
  }


  /**
   * Calls the super classes paint(Graphics g) method and then paints two gradients.
   * The direction of the gradients depends on the direction of the scrollbar.
   */
  protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
    super.paintThumb(g, c, thumbBounds);

    // colors for the reflection gradient
    Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
    Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);
    // colors for the shadow gradient
    Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
    Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);

    Rectangle rectReflection;  // rectangle for the reflection gradient
    Rectangle rectShadow;  // rectangle for the shadow gradient
    if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
      rectReflection = new Rectangle(thumbBounds.x+1, thumbBounds.y, thumbBounds.width/2, thumbBounds.height);
      rectShadow = new Rectangle(thumbBounds.x + thumbBounds.width/2, thumbBounds.y, thumbBounds.width/2+1, thumbBounds.height);
    } else {
      rectReflection = new Rectangle(thumbBounds.x, thumbBounds.y+1, thumbBounds.width, thumbBounds.height/2);
      rectShadow = new Rectangle(thumbBounds.x, thumbBounds.y + thumbBounds.height/2, thumbBounds.width, thumbBounds.height/2+1);
    }

    // the direction of the gradient is orthogonal to the direction of the scrollbar
    boolean isVertical = (scrollbar.getOrientation() == JScrollBar.HORIZONTAL);
    KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, rectReflection, isVertical);
    KunststoffUtilities.drawGradient(g, colorShadowFaded, colorShadow, rectShadow, isVertical);
  }

}