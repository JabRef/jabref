package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com)
 * with contributions by Jamie LaScolea.
 *
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class KunststoffTabbedPaneUI extends BasicTabbedPaneUI {
  private static final int SHADOW_WIDTH = 5;

  public static ComponentUI createUI(JComponent c) {
    return new KunststoffTabbedPaneUI();
  }

  protected void installDefaults() {
    super.installDefaults();
  }

  /*
   * Thanks to a contribution by Jamie LaScolea this method now works with
   * multiple rows of tabs.
   */
  protected void paintTab(Graphics g, int tabPlacement,
                            Rectangle[] rects, int tabIndex,
                            Rectangle iconRect, Rectangle textRect) {
    super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);
    Graphics2D g2D = (Graphics2D) g;

    Rectangle tabRect = rects[tabIndex];

    Color colorShadow = KunststoffLookAndFeel.getComponentGradientColorShadow();
    Color colorShadowFaded = KunststoffUtilities.getTranslucentColor(colorShadow, 0);
    Color colorReflection = KunststoffLookAndFeel.getComponentGradientColorReflection();
    Color colorReflectionFaded = KunststoffUtilities.getTranslucentColor(colorReflection, 0);

    // paint shadow that the selected tab throws on the next tab
    int selectedIndex = tabPane.getSelectedIndex();
    // the following statement was added by Jamie LaScolea as a bug fix. Thanks Jamie!
    if (this.lastTabInRun(tabPane.getTabCount(), this.selectedRun) != selectedIndex ){
      if (tabPlacement == JTabbedPane.TOP || tabPlacement == JTabbedPane.BOTTOM) {
        if (tabIndex == selectedIndex+1) {
          Rectangle gradientRect = new Rectangle((int) tabRect.getX(), (int) tabRect.getY(), SHADOW_WIDTH, (int) tabRect.getHeight());
          KunststoffUtilities.drawGradient(g, colorShadow, colorShadowFaded, gradientRect, false);
        }
      } else {
        if (tabIndex == selectedIndex+1) {
          Rectangle gradientRect = new Rectangle((int) tabRect.getX(), (int) tabRect.getY(), (int) tabRect.getWidth(), SHADOW_WIDTH);
          KunststoffUtilities.drawGradient(g, colorShadow, colorShadowFaded, gradientRect, true);
        }
      }
    }

    if (tabPlacement == JTabbedPane.TOP) {
      Rectangle gradientRect = new Rectangle((int) tabRect.getX(), (int) tabRect.getY(), (int) tabRect.getWidth(), (int) SHADOW_WIDTH);
      KunststoffUtilities.drawGradient(g, colorReflection, colorReflectionFaded, gradientRect, true);
    } else if (tabPlacement == JTabbedPane.BOTTOM) {
      if (tabIndex != selectedIndex) {
        Rectangle gradientRect = new Rectangle((int) tabRect.getX(), (int) tabRect.getY(), (int) tabRect.getWidth(), SHADOW_WIDTH);
        KunststoffUtilities.drawGradient(g, colorShadow, colorShadowFaded, gradientRect, true);
      }
    } else if (tabPlacement == JTabbedPane.RIGHT) {
      if (tabIndex != selectedIndex) {
        Rectangle gradientRect = new Rectangle((int) tabRect.getX(), (int) tabRect.getY(), (int) SHADOW_WIDTH, (int) tabRect.getHeight());
        KunststoffUtilities.drawGradient(g, colorShadow, colorShadowFaded, gradientRect, false);
      }
    }
  }

  protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected ) {
    if ( isSelected ) {
      g.setColor(UIManager.getColor("TabbedPane.selected"));
    } else {
      g.setColor(tabPane.getBackgroundAt(tabIndex));
    }
    switch(tabPlacement) {
      case LEFT:  g.fillRect(x+1, y+1, w-2, h-3);
      break;
      case RIGHT: g.fillRect(x, y+1, w-2, h-3);
      break;
      case BOTTOM: g.fillRect(x+1, y, w-3, h-1);
      break;
      case TOP: default: g.fillRect(x+1, y+1, w-3, h-1);
    }
  }

}