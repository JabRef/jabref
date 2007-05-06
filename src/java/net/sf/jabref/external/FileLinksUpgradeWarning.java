package net.sf.jabref.external;

import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.imports.PostOpenAction;
import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.GUIGlobals;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;

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

    /**
     * This method should be performed if the major/minor versions recorded in the ParserResult
     * are less than or equal to 2.2.
     * @param pr
     * @return true if the file was written by a jabref version <=2.2
     */
    public boolean isActionNecessary(ParserResult pr) {
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
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(Globals.lang("This database was written using an older version of JabRef."));

        sb.append("</html>");

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                sb.toString());
        if (answer == JOptionPane.OK_OPTION)
            makeChanges(panel, pr);
    }

    /**
     * This method performs the actual changes.
     * @param panel
     * @param pr
     */
    public void makeChanges(BasePanel panel, ParserResult pr) {

        // Update file links links in the database:
        Util.upgradePdfPsToFile(pr.getDatabase(),
                new String[] {"pdf", "ps"});

        // Exchange table columns:
        Globals.prefs.putBoolean("pdfColumn", Boolean.FALSE);
        Globals.prefs.putBoolean("fileColumn", Boolean.TRUE);

        // Modify General fields:
        String genF = Globals.prefs.get("generalFields");
        Pattern p1 = Pattern.compile("\\bpdf\\b");
        Pattern p2 = Pattern.compile("\\bps\\b");
        boolean mp1 = p1.matcher(genF).matches();
        boolean mp2 = p2.matcher(genF).matches();
        // Unfinished...
        /*if (mp1 && mp2) {
            genF = genF.replaceAll("\\bpdf\\b", GUIGlobals.FILE_FIELD);
            genF = genF.replaceAll("\\bps\\b", "");
        }*/
    }

}
