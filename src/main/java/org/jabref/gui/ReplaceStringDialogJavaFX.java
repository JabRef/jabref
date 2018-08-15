/**
 * ReplaceStringDialogJavaFX.java.
 */

package org.jabref.gui;

import javax.swing.JFrame;

import org.jabref.logic.l10n.Localization;

/**
 * Dialog for replacing strings
 */
class ReplaceStringDialogJavaFX extends JabRefDialog {


    public ReplaceStringDialogJavaFX(JabRefFrame parent) {
        super((JFrame) null, Localization.lang("Replace string"), true, ReplaceStringDialog.class);

    }


}
