package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import com.incors.plaf.*;

/**
 * Collection of methods often used in the Kunststoff Look&Feel
 */
public class KunststoffUtilities {

  /**
   * Convenience method to create a translucent <code>Color</color>.
   */
  public static Color getTranslucentColor(Color color, int alpha) {
    if (color == null) {
      return null;
    } else if (alpha == 255) {
      return color;
    } else {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
  }

  /**
   * Convenience method to create a translucent <code>ColorUIResource</code>.
   */
  public static Color getTranslucentColorUIResource(Color color, int alpha) {
    if (color == null) {
      return null;
    } else if (alpha == 255) {
      return color;
    } else {
      return new ColorUIResource2(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
  }

  /**
   * Convenience method to draw a gradient on the specified rectangle
   */
  public static void drawGradient(Graphics g, Color color1, Color color2, Rectangle rect, boolean isVertical) {
    Graphics2D g2D = (Graphics2D) g;
    Paint gradient = new FastGradientPaint(color1, color2, isVertical);
    g2D.setPaint(gradient);
    g2D.fill(rect);
  }

  /**
   * Convenience method to draw a gradient. The first rectangle defines the drawing region,
   * the second rectangle defines the size of the gradient.
   */
  public static void drawGradient(Graphics g, Color color1, Color color2, Rectangle rect, Rectangle rect2, boolean isVertical) {
    // We are currently not using the FastGradientPaint to render this gradient, because we have to decide how
    // we can use FastGradientPaint if rect and rect2 are different.
    if (isVertical) {
      Graphics2D g2D = (Graphics2D) g;
      GradientPaint gradient = new GradientPaint(0f, (float) rect.getY(), color1, 0f, (float) (rect.getHeight() + rect.getY()), color2);
      g2D.setPaint(gradient);
      g2D.fill(rect);
    } else {
      Graphics2D g2D = (Graphics2D) g;
      GradientPaint gradient = new GradientPaint((float) rect.getX(), 0f, color1, (float) (rect.getWidth() + rect.getX()), 0f, color2);
      g2D.setPaint(gradient);
      g2D.fill(rect);
    }
  }

  /**
   * Returns true if the display uses 24- or 32-bit color depth (= true color)
   */
  public static boolean isToolkitTrueColor(Component c) {
    int pixelsize = c.getToolkit().getColorModel().getPixelSize();
    return pixelsize >= 24;
  }
}
