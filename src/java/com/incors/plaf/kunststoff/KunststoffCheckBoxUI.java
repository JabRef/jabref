package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

/*
 * The only difference to the MetalCheckBoxUI is the icon, which has a gradient.
 */
public class KunststoffCheckBoxUI extends MetalCheckBoxUI {
  private final static KunststoffCheckBoxUI checkBoxUI = new KunststoffCheckBoxUI();

  public KunststoffCheckBoxUI() {
    icon = new KunststoffCheckBoxIcon();
  }

  public static ComponentUI createUI(JComponent b) {
    return checkBoxUI;
  }

  public void installDefaults(AbstractButton b) {
    super.installDefaults(b);
    icon = new KunststoffCheckBoxIcon();
  }

}