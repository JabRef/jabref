package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.text.*;

public class KunststoffPasswordFieldUI extends KunststoffTextFieldUI {

  KunststoffPasswordFieldUI(JComponent c) {
    super(c);
  }

  public static ComponentUI createUI(JComponent c) {
    return new KunststoffPasswordFieldUI(c);
  }

  protected String getPropertyPrefix() {
    return "PasswordField";
  }


  public View create(Element elem) {
    return new PasswordView(elem);
  }
}