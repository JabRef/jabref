package com.incors.plaf.kunststoff;

/*
 * This code was developed by Matthew Philips and INCORS GmbH (www.incors.com).
 * It is published under the terms of the Lesser GNU Public License.
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

/**
 * The KunststoffComboBoxUI is a little bit tricky, but it should work fine in
 * most cases. It currently draws only correctly if the renderer of the combo
 * box is an instance of <code>JComponent</code> and the background color is an
 * instance of <code>ColorUIResource</code>. In a default <code>JComboBox</code>
 * with a default renderer this should be the case.
 */
public class KunststoffComboBoxUI extends MetalComboBoxUI {

  public static ComponentUI createUI(JComponent c) {
    return new KunststoffComboBoxUI();
  }

  /**
   * Installs MyMetalComboBoxButton
   */
  protected JButton createArrowButton() {
    JButton button = new MyMetalComboBoxButton (comboBox, new MetalComboBoxIcon(),
                                                comboBox.isEditable() ? true : false,
                                                currentValuePane, listBox);
    button.setMargin(new Insets(0, 1, 1, 3 ));
    return button;
  }

  /*
   * This inner class finally fixed a nasty bug with the combo box. Thanks to
   * Matthew Philips for providing the bugfix.
   * Thanks to Ingo Kegel for fixing two compiling issues for jikes.
   */
  private final class MyMetalComboBoxButton extends javax.swing.plaf.metal.MetalComboBoxButton {

    public MyMetalComboBoxButton(JComboBox cb, Icon i, boolean onlyIcon,
                                CellRendererPane pane, JList list) {
      super (cb, i, onlyIcon, pane, list);
    }

    public void paintComponent(Graphics g) {
      if (! iconOnly && MyMetalComboBoxButton.this.comboBox != null) {
        boolean isSetRendererOpaque = false;
        ListCellRenderer renderer = MyMetalComboBoxButton.this.comboBox.getRenderer();
        if (renderer instanceof JComponent) {
          JComponent jRenderer = (JComponent) renderer;
          if (jRenderer.isOpaque() && jRenderer.getBackground() instanceof ColorUIResource) {
            isSetRendererOpaque = true;  // remember to set the renderer opaque again
            jRenderer.setOpaque(false);
          }
        }
        super.paintComponent(g);
        if (isSetRendererOpaque) {
          ((JComponent) renderer).setOpaque(true);
        }
      } else {
        super.paintComponent(g);
      }
    }

  }
}
