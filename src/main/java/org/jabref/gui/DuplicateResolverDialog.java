package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.gui.util.WindowLocation;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class DuplicateResolverDialog extends JabRefDialog {

    public enum DuplicateResolverType {
        DUPLICATE_SEARCH,
        IMPORT_CHECK,
        INSPECTION,
        DUPLICATE_SEARCH_WITH_EXACT
    }

    public enum DuplicateResolverResult {
        NOT_CHOSEN,
        KEEP_BOTH,
        KEEP_LEFT,
        KEEP_RIGHT,
        AUTOREMOVE_EXACT,
        KEEP_MERGE,
        BREAK
    }

    JButton helpButton = new HelpAction(Localization.lang("Help"), HelpFile.FIND_DUPLICATES).getHelpButton();
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JButton merge = new JButton(Localization.lang("Keep merged entry only"));
    private final JabRefFrame frame;
    private final JPanel options = new JPanel();
    private DuplicateResolverResult status = DuplicateResolverResult.NOT_CHOSEN;
    private MergeEntries me;

    public DuplicateResolverDialog(JabRefFrame frame, BibEntry one, BibEntry two, DuplicateResolverType type) {
        super(frame, Localization.lang("Possible duplicate entries"), true, DuplicateResolverDialog.class);
        this.frame = frame;
        init(one, two, type);
    }

    public DuplicateResolverDialog(ImportInspectionDialog dialog, BibEntry one, BibEntry two,
            DuplicateResolverType type) {
        super(dialog, Localization.lang("Possible duplicate entries"), true, DuplicateResolverDialog.class);
        this.frame = dialog.getFrame();
        init(one, two, type);
    }

    private void init(BibEntry one, BibEntry two, DuplicateResolverType type) {
        JButton both;
        JButton second;
        JButton first;
        JButton removeExact = null;
        switch (type) {
        case DUPLICATE_SEARCH:
            first = new JButton(Localization.lang("Keep left"));
            second = new JButton(Localization.lang("Keep right"));
            both = new JButton(Localization.lang("Keep both"));
            me = new MergeEntries(one, two, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
            break;
        case INSPECTION:
            first = new JButton(Localization.lang("Remove old entry"));
            second = new JButton(Localization.lang("Remove entry from import"));
            both = new JButton(Localization.lang("Keep both"));
            me = new MergeEntries(one, two, Localization.lang("Old entry"),
                    Localization.lang("From import"), frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
            break;
        case DUPLICATE_SEARCH_WITH_EXACT:
            first = new JButton(Localization.lang("Keep left"));
            second = new JButton(Localization.lang("Keep right"));
            both = new JButton(Localization.lang("Keep both"));
            removeExact = new JButton(Localization.lang("Automatically remove exact duplicates"));
            me = new MergeEntries(one, two, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
            break;
        default:
            first = new JButton(Localization.lang("Import and remove old entry"));
            second = new JButton(Localization.lang("Do not import entry"));
            both = new JButton(Localization.lang("Import and keep old entry"));
            me = new MergeEntries(one, two, Localization.lang("Old entry"),
                    Localization.lang("From import"), frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
            break;
        }

        if (removeExact != null) {
            options.add(removeExact);
        }
        options.add(first);
        options.add(second);
        options.add(both);
        options.add(merge);
        options.add(Box.createHorizontalStrut(5));
        options.add(cancel);
        options.add(helpButton);

        first.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_LEFT));
        second.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_RIGHT));
        both.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_BOTH));
        merge.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_MERGE));
        if (removeExact != null) {
            removeExact.addActionListener(e -> buttonPressed(DuplicateResolverResult.AUTOREMOVE_EXACT));
        }

        cancel.addActionListener(e -> buttonPressed(DuplicateResolverResult.BREAK));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                buttonPressed(DuplicateResolverResult.BREAK);
            }
        });

        getContentPane().add(me.getMergeEntryPanel());
        getContentPane().add(options, BorderLayout.SOUTH);
        pack();

        WindowLocation pw = new WindowLocation(this, JabRefPreferences.DUPLICATES_POS_X,
                JabRefPreferences.DUPLICATES_POS_Y, JabRefPreferences.DUPLICATES_SIZE_X,
                JabRefPreferences.DUPLICATES_SIZE_Y);
        pw.displayWindowAtStoredLocation();

        both.requestFocus();
    }

    private void buttonPressed(DuplicateResolverResult result) {
        status = result;
        dispose();
    }

    public DuplicateResolverResult getSelected() {
        return status;
    }

    public BibEntry getMergedEntry() {
        return me.getMergeEntry();
    }

}
