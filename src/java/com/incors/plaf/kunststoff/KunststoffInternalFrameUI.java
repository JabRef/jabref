package com.incors.plaf.kunststoff;

/*
 * This code was developed by Jerason Banes (jbanes@techie.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

public class KunststoffInternalFrameUI extends MetalInternalFrameUI {
  private MetalInternalFrameTitlePane titlePane;
  private PropertyChangeListener paletteListener;

  private static String FRAME_TYPE     = "JInternalFrame.frameType";
  private static String NORMAL_FRAME   = "normal";
  private static String PALETTE_FRAME  = "palette";
  private static String OPTION_DIALOG  = "optionDialog";

  protected static String IS_PALETTE   = "JInternalFrame.isPalette"; // added by Thomas Auinger
                                                                     // to solve a compiling problem

  public KunststoffInternalFrameUI(JInternalFrame b) {
    super(b);
  }

  public static ComponentUI createUI(JComponent c) {
    return new KunststoffInternalFrameUI((JInternalFrame)c);
  }

  public void installUI(JComponent c) {
    paletteListener = new PaletteListener();
    c.addPropertyChangeListener(paletteListener);

    super.installUI(c);
  }

  public void uninstallUI(JComponent c) {
    c.removePropertyChangeListener(paletteListener);
    super.uninstallUI(c);
  }

  protected JComponent createNorthPane(JInternalFrame w) {
    super.createNorthPane(w);
    titlePane = new KunststoffInternalFrameTitlePane(w);
    return titlePane;
  }

  public void setPalette(boolean isPalette) {
    super.setPalette(isPalette);
    titlePane.setPalette(isPalette);
  }

  private void setFrameType(String frameType) {
    if (frameType.equals(OPTION_DIALOG)) {
      LookAndFeel.installBorder(frame, "InternalFrame.optionDialogBorder");
      titlePane.setPalette(false);
    } else if (frameType.equals(PALETTE_FRAME)) {
      LookAndFeel.installBorder(frame, "InternalFrame.paletteBorder");
      titlePane.setPalette(true);
    } else {
      LookAndFeel.installBorder(frame, "InternalFrame.border");
      titlePane.setPalette(false);
    }
  }

  class PaletteListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent e) {
      String name = e.getPropertyName();

      if(name.equals(FRAME_TYPE)) {
        if(e.getNewValue() instanceof String) setFrameType((String)e.getNewValue());
      } else if(name.equals(IS_PALETTE)) {
        if(e.getNewValue() != null) setPalette(((Boolean)e.getNewValue()).booleanValue());
        else setPalette(false);
      }
    }
  } // end class PaletteListener
}
