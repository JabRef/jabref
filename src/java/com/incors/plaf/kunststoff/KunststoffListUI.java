package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 * From version 2.0 the KunststoffListUI works with the DefaultListCellRenderer,
 * which makes it really plug and play!
 */
public class KunststoffListUI extends BasicListUI {
  private boolean isToolkitTrueColor = false;
  private Color colBg;

  public KunststoffListUI(JComponent list) {
    // this will be needed for the decision if a big gradient or a small shadow
    // should be painted. On 16-bit colors the big gradient looks awkward, therefore
    // we will then paint a small shadow instead
    isToolkitTrueColor = KunststoffUtilities.isToolkitTrueColor(list);
  }

  public static ComponentUI createUI(JComponent list) {
    return new KunststoffListUI(list);
  }

  public void update(Graphics g, JComponent c) {
    if (c.isOpaque()) {
      Graphics2D g2D = (Graphics2D) g;
      Color colorBackground = c.getBackground();
      int shadow = KunststoffLookAndFeel.getBackgroundGradientShadow();
      // we will only paint the background if the background color is not null
      if (colorBackground != null) {
        Rectangle clipBounds = g.getClipBounds();
        if (shadow == 0) {
          // paint the background without gradient
          g2D.setColor(colorBackground);
          g2D.fill(clipBounds);
        } else {
          // create the shadow color
          int red = colorBackground.getRed();
          int green = colorBackground.getGreen();
          int blue = colorBackground.getBlue();
          Color colorShadow = new Color(red>=shadow?red-shadow:0, green>=shadow?green-shadow:0, blue>=shadow?blue-shadow:0);

          if (isToolkitTrueColor) {
            // paint big horizontal gradient
            Rectangle rect = new Rectangle(0, 0, list.getWidth(), list.getHeight());
            KunststoffUtilities.drawGradient(g, colorBackground, colorShadow, rect, clipBounds, false);
          } else {
            g2D.setColor(colorBackground);
            g2D.fill(clipBounds);
            // create faded shadow color
            Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
            // paint shadow at top
            GradientPaint gradientTop = new GradientPaint(0f, 0f, colorShadow, 0f, 5f, colorShadowFaded);
            g2D.setPaint(gradientTop);
            g2D.fill(new Rectangle(clipBounds.x, clipBounds.y, clipBounds.width, 20));
            // paint shadow at left
            GradientPaint gradientLeft = new GradientPaint(0f, 0f, colorShadow, 5f, 0f, colorShadowFaded);
            g2D.setPaint(gradientLeft);
            g2D.fill(new Rectangle(clipBounds.x, clipBounds.y, 20, clipBounds.height));
          }
        }
      }
    }
    paint(g, c);
  }

  // We temporarily make the renderer transparent if the row is not selected
  // and the renderer is a JComponent (like DefaultListCellRenderer) and the
  // background color is a ColorUIResource (which means it probably has the
  // original color assigned by the Look&Feel).
  protected void paintCell(Graphics g, int row, Rectangle rowBounds,
                            ListCellRenderer cellRenderer, ListModel dataModel,
                            ListSelectionModel selModel, int leadIndex) {
    if (cellRenderer instanceof JComponent && !selModel.isSelectedIndex(row)) {
      JComponent renderer = ((JComponent) cellRenderer);
      if (renderer.getBackground() instanceof ColorUIResource && renderer.isOpaque()) {
        renderer.setOpaque(false);
        super.paintCell(g, row, rowBounds, cellRenderer, dataModel, selModel, leadIndex);
        renderer.setOpaque(true);
        return;
      }
    }
    super.paintCell(g, row, rowBounds, cellRenderer, dataModel, selModel, leadIndex);
  }


}