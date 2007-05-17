package net.sf.jabref.external;

import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.imports.PostOpenAction;
import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;
import java.util.Iterator;

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
        boolean offerChangeSettings = !Globals.prefs.getBoolean("fileColumn");
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

        StringBuilder sb = new StringBuilder("<html>");
        sb.append(Globals.lang("This database was written using an older version of JabRef."));
        sb.append("<br>");
        sb.append(Globals.lang("The current version features a new way of handling links to external files.<br>"
            +"To take advantage of this, your links must be changed into the new format, and<br>"
            +"JabRef must be configured to show the new links."));
        sb.append("<p>");
        sb.append(Globals.lang("Do you want JabRef to do the following operations?"));
        sb.append("</html>");

        JPanel message = new JPanel();
        DefaultFormBuilder b = new DefaultFormBuilder(message,
                new FormLayout("left:pref", ""));
        b.append(new JLabel(sb.toString()));
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
        for (Iterator iterator = database.getEntries().iterator(); iterator.hasNext();) {
            BibtexEntry entry = (BibtexEntry)iterator.next();
            for (int i = 0; i < fields.length; i++) {
                if (entry.getField(fields[i]) != null)
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

            // Modify General fields:
            String genF = Globals.prefs.get(Globals.prefs.CUSTOM_TAB_FIELDS+"_def0");
            if (!Pattern.compile("\\b"+GUIGlobals.FILE_FIELD+"\\b").matcher(genF)
                .matches()) {

                StringBuilder sb = new StringBuilder(GUIGlobals.FILE_FIELD).append(';').
                    append(genF);
                /*int index = sb.indexOf(":");
                if (index > 0)
                    sb.insert(index+1, GUIGlobals.FILE_FIELD+";");*/

                Globals.prefs.put(Globals.prefs.CUSTOM_TAB_FIELDS+"_def0", sb.toString());
                System.out.println(sb.toString());
                Globals.prefs.updateEntryEditorTabList();
                panel.frame().removeCachedEntryEditors();
            }

            /*Pattern p1 = Pattern.compile("\\bpdf\\b");
            Pattern p2 = Pattern.compile("\\bps\\b");
            boolean mp1 = p1.matcher(genF).matches();
            boolean mp2 = p2.matcher(genF).matches();
            // Unfinished...
            if (mp1 && mp2) {
                genF = genF.replaceAll("\\bpdf\\b", GUIGlobals.FILE_FIELD);
                genF = genF.replaceAll("\\bps\\b", "");
            }*/

            panel.frame().setupAllTables();
        }
    }

}
