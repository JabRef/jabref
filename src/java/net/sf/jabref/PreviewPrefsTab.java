
package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.io.File;

public class PreviewPrefsTab extends JPanel implements PrefsTab {

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JabRefPreferences _prefs;


    public PreviewPrefsTab(JabRefPreferences prefs, HelpDialog helpDiag) {
	_prefs = prefs;
 	setLayout(gbl);

    }


    public void storeSettings() {
	/*_prefs.putBoolean("autoOpenForm", autoOpenForm.isSelected());
	_prefs.putBoolean("backup", backup.isSelected());
	_prefs.putBoolean("openLastEdited", openLast.isSelected());
	_prefs.putBoolean("defaultShowSource", defSource.isSelected());
        _prefs.putBoolean("enableSourceEditing", editSource.isSelected());
        _prefs.putBoolean("disableOnMultipleSelection", disableOnMultiple.isSelected());
        _prefs.putBoolean("useOwner", useOwner.isSelected());
        _prefs.putBoolean("dialogWarningForDuplicateKey", keyWarningDialog.isSelected());
        _prefs.putBoolean("confirmDelete", confirmDelete.isSelected());
	*/
    }

}
