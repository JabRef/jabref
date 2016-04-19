/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.mergeentries.MergeEntries;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

// created by : ?
//
// modified : r.nagel 2.09.2004
//            - insert close button

public class DuplicateResolverDialog extends JDialog {

    enum DuplicateResolverType {
        DUPLICATE_SEARCH,
        IMPORT_CHECK,
        INSPECTION,
        DUPLICATE_SEARCH_WITH_EXACT
    }

    enum DuplicateResolverResult {
        NOT_CHOSEN,
        KEEP_BOTH,
        KEEP_UPPER,
        KEEP_LOWER,
        AUTOREMOVE_EXACT,
        KEEP_MERGE,
        BREAK
    }

    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JButton merge = new JButton(Localization.lang("Keep merged entry only"));
    private final JabRefFrame frame;
    private final JPanel options = new JPanel();
    private DuplicateResolverResult status = DuplicateResolverResult.NOT_CHOSEN;
    private MergeEntries me;

    public DuplicateResolverDialog(JabRefFrame frame, BibEntry one, BibEntry two, DuplicateResolverType type) {
        super(frame, Localization.lang("Possible duplicate entries"), true);
        this.frame = frame;
        init(one, two, type);
    }

    public DuplicateResolverDialog(ImportInspectionDialog dialog, BibEntry one, BibEntry two,
            DuplicateResolverType type) {
        super(dialog, Localization.lang("Possible duplicate entries"), true);
        this.frame = dialog.frame;
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

        first.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_UPPER));
        second.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_LOWER));
        both.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_BOTH));
        merge.addActionListener(e -> buttonPressed(DuplicateResolverResult.KEEP_MERGE));
        if (removeExact != null) {
            removeExact.addActionListener(e -> buttonPressed(DuplicateResolverResult.AUTOREMOVE_EXACT));
        }
        cancel.addActionListener(e -> buttonPressed(DuplicateResolverResult.BREAK));

        getContentPane().add(me.getMergeEntryPanel());
        getContentPane().add(options, BorderLayout.SOUTH);
        pack();

        PositionWindow pw = new PositionWindow(this, JabRefPreferences.DUPLICATES_POS_X,
                JabRefPreferences.DUPLICATES_POS_Y, JabRefPreferences.DUPLICATES_SIZE_X,
                JabRefPreferences.DUPLICATES_SIZE_Y);
        pw.setWindowPosition();

        both.requestFocus();

    }


    private void buttonPressed(DuplicateResolverResult button) {
        status = button;
        dispose();
    }

    public DuplicateResolverResult getSelected() {
        return status;
    }

    public BibEntry getMergedEntry() {
        return me.getMergeEntry();
    }

}
