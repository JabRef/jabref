package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com)
 * based on a contribution by Timo Haberkern.
 * It is published under the terms of the Lesser GNU Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class KunststoffTreeUI extends BasicTreeUI {
  protected static ImageIcon m_iconExpanded;
  protected static ImageIcon m_iconCollapsed;


  public KunststoffTreeUI(JComponent tree) {
    try {
      m_iconExpanded = new ImageIcon(getClass().getResource("icons/treeex.gif"));
      m_iconCollapsed = new ImageIcon(getClass().getResource("icons/treecol.gif"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ComponentUI createUI(JComponent tree) {
    return new KunststoffTreeUI(tree);
  }

  // This method replaces the metal expand-/collaps-icons with some nicer ones.
  protected void paintExpandControl(Graphics g, Rectangle clipBounds,
                                Insets insets, Rectangle bounds,
                                TreePath path, int row, boolean isExpanded,
                                boolean hasBeenExpanded, boolean isLeaf) {
    //super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
    if (isExpanded == true) {
      if (null != m_iconExpanded) {
        //g.drawImage(m_iconExpanded.getImage(), (int)bounds.x-15, (int)bounds.y+5, null);
        g.drawImage(m_iconExpanded.getImage(), (int)bounds.x-17, (int)bounds.y+4, null);
      }
    } else {
      if (null != m_iconCollapsed) {
        //g.drawImage(m_iconCollapsed.getImage(), (int)bounds.x-15, (int)bounds.y+5, null);
        g.drawImage(m_iconCollapsed.getImage(), (int)bounds.x-17, (int)bounds.y+4, null);
      }
    }
  }


}