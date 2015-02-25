/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.external;

import java.util.List;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.imports.PostOpenAction;
import net.sf.jabref.undo.NamedCompound;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class defines the warning that can be offered when opening a pre-2.3
 * JabRef file into a later version. This warning mentions the new external file
 * link system in this version of JabRef, and offers to:
 *
 * * upgrade old-style PDF/PS links into the "file" field
 * * modify General fields to show "file" instead of "pdf" / "ps"
 * * modify table column settings to show "file" instead of "pdf" / "ps"
 */
public class FileLinksUpgradeWarning implements PostOpenAction {

    private static final String[] FIELDS_TO_LOOK_FOR = new String[] {"pdf", "ps"};

    /**
     * This method should be performed if the major/minor versions recorded in the ParserResult
     * are less than or equal to 2.2.
     * @param pr
     * @return true if the file was written by a jabref version <=2.2
     */
    public boolean isActionNecessary(ParserResult pr) {
        // First check if this warning is disabled:
        if (!Globals.prefs.getBoolean("showFileLinksUpgradeWarning"))
            return false;
        if (pr.getJabrefMajorVersion() < 0)
            return false; // non-JabRef file
        if (pr.getJabrefMajorVersion() < 2)
            return true; // old
        if (pr.getJabrefMajorVersion() > 2)
            return false; // wow, did we ever reach version 3?
        return (pr.getJabrefMinorVersion() <= 2);
    }

    /**
     * This method presents a dialog box explaining and offering to make the
     * changes. If the user confirms, the changes are performed.
     * @param panel
     * @param pr
     */
    public void performAction(BasePanel panel, ParserResult pr) {
        // Find out which actions should be offered:
        // Only offer to change Preferences if file column is not already visible:
        boolean offerChangeSettings = !Globals.prefs.getBoolean("fileColumn") || !showsFileInGenFields();
        // Only offer to upgrade links if the pdf/ps fields are used:
        boolean offerChangeDatabase = linksFound(pr.getDatabase(), FIELDS_TO_LOOK_FOR);
        // If the "file" directory is not set, offer to migrate pdf/ps dir:
        boolean offerSetFileDir = !Globals.prefs.hasKey(GUIGlobals.FILE_FIELD+"Directory")
                && (Globals.prefs.hasKey("pdfDirectory") || Globals.prefs.hasKey("psDirectory"));

        if (!offerChangeDatabase && !offerChangeSettings && !offerSetFileDir)
                    return; // Nothing to do, just return.
                
        JCheckBox changeSettings = new JCheckBox(Globals.lang("Change table column and General fields settings to use the new feature"),
                offerChangeSettings);
        JCheckBox changeDatabase = new JCheckBox(Globals.lang("Upgrade old external file links to use the new feature"),
                offerChangeDatabase);
        JCheckBox setFileDir = new JCheckBox(Globals.lang("Set main external file directory")+":", offerSetFileDir);
        JTextField fileDir = new JTextField(30);
        JCheckBox doNotShowDialog = new JCheckBox(Globals.lang("Do not show these options in the future"),
                false);

        JPanel message = new JPanel();
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref", ""), message);
        b.append(new JLabel("<html>" + Globals.lang("This database was written using an older version of JabRef.") + "<br>" + Globals.lang("The current version features a new way of handling links to external files.<br>"
                + "To take advantage of this, your links must be changed into the new format, and<br>"
                + "JabRef must be configured to show the new links.") + "<p>" + Globals.lang("Do you want JabRef to do the following operations?") + "</html>"));
        b.nextLine();
        if (offerChangeSettings) {
            b.append(changeSettings);
            b.nextLine();
        }
        if (offerChangeDatabase) {
            b.append(changeDatabase);
            b.nextLine();
        }
        if (offerSetFileDir) {
            if (Globals.prefs.hasKey("pdfDirectory"))
                fileDir.setText(Globals.prefs.get("pdfDirectory"));
            else
                fileDir.setText(Globals.prefs.get("psDirectory"));
            JPanel pan = new JPanel();
            pan.add(setFileDir);
            pan.add(fileDir);
            JButton browse = new JButton(Globals.lang("Browse"));
            browse.addActionListener(new BrowseAction(null, fileDir, true));
            pan.add(browse);
            b.append(pan);
            b.nextLine();
        }
        b.append("");
        b.nextLine();
        b.append(doNotShowDialog);

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                message, Globals.lang("Upgrade file"), JOptionPane.YES_NO_OPTION);
        if (doNotShowDialog.isSelected())
            Globals.prefs.putBoolean("showFileLinksUpgradeWarning", false);

        if (answer == JOptionPane.YES_OPTION)
            makeChanges(panel, pr, changeSettings.isSelected(), changeDatabase.isSelected(),
                    setFileDir.isSelected() ? fileDir.getText() : null);
    }

    /**
     * Check the database to find out whether any of a set of fields are used
     * for any of the entries.
     * @param database The bib database.
     * @param fields The set of fields to look for.
     * @return true if at least one of the given fields is set in at least one entry,
     *  false otherwise.
     */
    public boolean linksFound(BibtexDatabase database, String[] fields) {
        for (BibtexEntry entry : database.getEntries()){
            for (String field : fields) {
                if (entry.getField(field) != null)
                    return true;
            }
        }
        return false;
    }

    /**
     * This method performs the actual changes.
     * @param panel
     * @param pr
     * @param fileDir The path to the file directory to set, or null if it should not be set.
     */
    public void makeChanges(BasePanel panel, ParserResult pr, boolean upgradePrefs,
                            boolean upgradeDatabase, String fileDir) {

        if (upgradeDatabase) {
            // Update file links links in the database:
            NamedCompound ce = Util.upgradePdfPsToFile(pr.getDatabase(), FIELDS_TO_LOOK_FOR);
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }

        if (fileDir != null) {
            Globals.prefs.put(GUIGlobals.FILE_FIELD+"Directory", fileDir);
        }

        if (upgradePrefs) {
            // Exchange table columns:
            Globals.prefs.putBoolean("pdfColumn", Boolean.FALSE);
            Globals.prefs.putBoolean("fileColumn", Boolean.TRUE);

            // Modify General fields if necessary:
            // If we don't find the file field, insert it at the bottom of the first tab:
            if (!showsFileInGenFields()) {
                String gfs = Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_FIELDS +"0");
                //System.out.println(gfs);
                StringBuffer sb = new StringBuffer(gfs);
                if (gfs.length() > 0)
                    sb.append(";");
                sb.append(GUIGlobals.FILE_FIELD);
                Globals.prefs.put(JabRefPreferences.CUSTOM_TAB_FIELDS +"0", sb.toString());
                Globals.prefs.updateEntryEditorTabList();
                panel.frame().removeCachedEntryEditors();
            }
            panel.frame().setupAllTables();
        }
    }

    private boolean showsFileInGenFields() {
        boolean found = false;
        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        outer: for (int i=0; i<tabList.getTabCount(); i++) {
            List<String> fields = tabList.getTabFields(i);
            for (String field : fields) {
                if (field.equals(GUIGlobals.FILE_FIELD)) {
                    found = true;
                    break outer;
                }
            }
        }
        return found;
    }

}
