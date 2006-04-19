package net.sf.jabref.external;

import net.sf.jabref.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 4, 2006
 * Time: 7:31:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToApplicationAction extends AbstractAction implements Runnable {
    private PushToApplication operation;
    private JabRefFrame frame;
    private BasePanel panel;
    private BibtexEntry[] entries;

    public PushToApplicationAction(JabRefFrame frame, PushToApplication operation) {
        this.frame = frame;
        putValue(SMALL_ICON, operation.getIcon());
        putValue(NAME, operation.getName());
        putValue(SHORT_DESCRIPTION, operation.getTooltip());
        putValue(ACCELERATOR_KEY, Globals.prefs.getKey(operation.getKeyStrokeName()));
        this.operation = operation;
    }

    public void actionPerformed(ActionEvent e) {
        panel = frame.basePanel();

        // Check if a BasePanel exists:
        if (panel == null)
            return;

        // Check if any entries are selected:
        entries = panel.getSelectedEntries();
        if (entries.length == 0) {
            JOptionPane.showMessageDialog(frame, Globals.lang("This operation requires one or more entries to be selected."),
                    (String)getValue(NAME), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // If required, check that all entries have BibTeX keys defined:
        if (operation.requiresBibtexKeys())
            for (int i=0; i<entries.length; i++) {
                if ((entries[i].getCiteKey() == null) || (entries[i].getCiteKey().trim().length() == 0)) {
                    JOptionPane.showMessageDialog(frame, Globals.lang("This operation requires all selected entries to have BibTex keys defined."),
                        (String)getValue(NAME), JOptionPane.ERROR_MESSAGE);
                    return;
                }
        }

        // All set, call the operation in a new thread:
        Thread t = new Thread(this);
        t.start();

    }

    public void run() {
        // Do the operation:
        operation.pushEntries(entries, getKeyString(entries));

        // Call the operationCompleted() method on the event dispatch thread:
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                operation.operationCompleted(panel);
            }
        });
    }

    protected String getKeyString(BibtexEntry[] entries) {
        StringBuffer result = new StringBuffer();
        String citeKey = "";//, message = "";
        boolean first = true;
        for (int i=0; i<entries.length; i++) {
            BibtexEntry bes = entries[i];
            citeKey = (String) bes.getField(BibtexFields.KEY_FIELD);
            // if the key is empty we give a warning and ignore this entry
            if (citeKey == null || citeKey.equals(""))
                continue;
            if (first) {
                result.append(citeKey);
                first = false;
            } else {
                result.append(",").append(citeKey);
            }
        }
        return result.toString();
    }
}
