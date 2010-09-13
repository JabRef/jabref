package net.sf.jabref.external;

import java.util.Vector;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.imports.PostOpenAction;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class defines the warning that can be offered when opening a pre-2.7
 * JabRef file into a later version. This warning mentions the new user-specific
 * file directory setting in this version of JabRef, and offers to:
 *
 * * upgrade old-style file directory setting into the new user specific setting
 */
public class FileDirectoryUpgradeWarning implements PostOpenAction {

    /**
     * This method should be performed if the major/minor versions recorded in the ParserResult
     * are less than or equal to 2.6.
     * @param pr
     * @return true if the file was written by a jabref version <=2.6
     */
    public boolean isActionNecessary(ParserResult pr) {
        // First check if this warning is disabled:
        if (!Globals.prefs.getBoolean("showFileDirUpgradeWarning"))
            return false;
        if (pr.getJabrefMajorVersion() < 0)
            return false; // non-JabRef file
        if (pr.getJabrefMajorVersion() < 2)
            return true; // old
        if (pr.getJabrefMajorVersion() > 2)
            return false; // wow, did we ever reach version 3?
        return (((float)pr.getJabrefMinorVersion() + ((float)pr.getJabrefMinor2Version()/10)) < ((float)6.1));
    }

    /**
     * This method presents a dialog box explaining and offering to make the
     * changes. If the user confirms, the changes are performed.
     * @param panel
     * @param pr
     */
    public void performAction(BasePanel panel, ParserResult pr) {
        // Find out which actions should be offered:
        // Only offer to change database properties if database already has a (legacy) GUIGlobals.FILE_FIELD+"Directory" setting
        boolean offerSetFileDir = !(panel.metaData().getData(GUIGlobals.FILE_FIELD + "Directory") == null) && (panel.metaData().getData(Globals.prefs.get("userFileDir")) == null);

        if (!offerSetFileDir) return; // Nothing to do, just return.
                
        JCheckBox setFileDir = new JCheckBox(Globals.lang("Set user specific file directory")+":", offerSetFileDir);
        JTextField fileDir = new JTextField(30);
        JCheckBox retainOldFileDir = new JCheckBox(Globals.lang("Retain legacy file directory setting (for older versions of JabRef)"), false);
        JCheckBox doNotShowDialog = new JCheckBox(Globals.lang("Do not show these options in the future"),
                false);

        StringBuilder sb = new StringBuilder("<html>");
        sb.append(Globals.lang("This database was written using an older version of JabRef."));
        sb.append("<br>");
        sb.append(Globals.lang("The current version features a new way of storing the file directory setting of<br>"
            +"a database. This enables multiple users of the same database file to keep their<br>"
            +"own personal setting for the path to the file directory.<br>"
            +"To take advantage of this, your file directory setting must be changed into the<br>"
            +"new format."));
        sb.append("<p>");
        sb.append(Globals.lang("Do you want JabRef to do the following operations?"));
        sb.append("</html>");

        JPanel message = new JPanel();
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref", ""), message);
        b.append(new JLabel(sb.toString()));
        b.nextLine();
        if (offerSetFileDir) {
	    if (panel.metaData().getData(GUIGlobals.FILE_FIELD + "Directory") != null)
		fileDir.setText(panel.metaData().getData(GUIGlobals.FILE_FIELD + "Directory").firstElement());
            JPanel pan = new JPanel();
            pan.add(setFileDir);
            pan.add(fileDir);
            //b.nextLine();
            JButton browse = new JButton(Globals.lang("Browse"));
            browse.addActionListener(new BrowseAction(null, fileDir, true));
            pan.add(browse);
            b.append(pan);
            b.nextLine();
            JPanel pan2 = new JPanel();
	    pan2.add(retainOldFileDir);
	    b.append(pan2);
	    b.nextLine();
        }
        b.append("");
        b.nextLine();
        b.append(doNotShowDialog);

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
						   message, Globals.lang("Upgrade file") + " - " + pr.getFile().getName(), JOptionPane.YES_NO_OPTION);
        if (doNotShowDialog.isSelected())
            Globals.prefs.putBoolean("showFileDirUpgradeWarning", false);

        if (answer == JOptionPane.YES_OPTION)
            makeChanges(panel, pr, setFileDir.isSelected() ? fileDir.getText() : null, retainOldFileDir.isSelected());
    }

    /**
     * This method performs the actual changes.
     * @param panel
     * @param pr
     * @param fileDir The path to the file directory to set, or null if it should not be set.
     */
	public void makeChanges(BasePanel panel, ParserResult pr, String fileDir, boolean retainOldFileLinks) {

        boolean changed = false;

        if (fileDir != null) {
	    Vector<String> vec = new Vector(1);
	    vec.add(fileDir);
            panel.metaData().putData(Globals.prefs.get("userFileDir"), vec);
	    changed = true;
        }

	if (!retainOldFileLinks) {
	    panel.metaData().remove(GUIGlobals.FILE_FIELD + "Directory");
	    changed = true;
        }

	if (changed)
	    panel.markNonUndoableBaseChanged();

    }

}
