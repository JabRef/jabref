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

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.mergeentries.MergeEntries;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;

// created by : ?
//
// modified : r.nagel 2.09.2004
//            - insert close button

public class DuplicateResolverDialog extends JDialog {

    private static final int NOT_CHOSEN = -1;
    public static final int KEEP_BOTH = 0;
    public static final int KEEP_UPPER = 1;
    public static final int KEEP_LOWER = 2;
    public static final int AUTOREMOVE_EXACT = 3;
    public static final int KEEP_MERGE = 4;
    public static final int BREAK = 5; // close
    public static final int IMPORT_AND_DELETE_OLD = 1;
    public static final int IMPORT_AND_KEEP_OLD = 0;
    public static final int DO_NOT_IMPORT = 2;
    public static final int DUPLICATE_SEARCH = 1;
    public static final int IMPORT_CHECK = 2;
    public static final int INSPECTION = 3;
    public static final int DUPLICATE_SEARCH_WITH_EXACT = 4;

    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JButton merge = new JButton(Localization.lang("Keep merged entry only"));
    private final JabRefFrame frame;
    private JButton removeExact;
    private final JPanel options = new JPanel();
    private int status = DuplicateResolverDialog.NOT_CHOSEN;
    private MergeEntries me;
    private PositionWindow pw;

    public DuplicateResolverDialog(JabRefFrame frame, BibEntry one, BibEntry two, int type) {
        super(frame, Localization.lang("Possible duplicate entries"), true);
        this.frame = frame;
        init(one, two, type);
    }

    public DuplicateResolverDialog(ImportInspectionDialog frame, BibEntry one, BibEntry two, int type) {
        super(frame, Localization.lang("Possible duplicate entries"), true);
        this.frame = frame.frame;
        init(one, two, type);
    }

    private void init(BibEntry one, BibEntry two, int type) {
        JButton both;
        JButton second;
        JButton first;
        switch (type) {
        case DUPLICATE_SEARCH:
            first = new JButton(Localization.lang("Keep left"));
            second = new JButton(Localization.lang("Keep right"));
            both = new JButton(Localization.lang("Keep both"));
            me = new MergeEntries(one, two, frame.getCurrentBasePanel().getLoadedDatabase().getMode());
            break;
        case INSPECTION:
            first = new JButton(Localization.lang("Remove old entry"));
            second = new JButton(Localization.lang("Remove entry from import"));
            both = new JButton(Localization.lang("Keep both"));
            me = new MergeEntries(one, two, Localization.lang("Old entry"),
                    Localization.lang("From import"), frame.getCurrentBasePanel().getLoadedDatabase().getMode());
            break;
        case DUPLICATE_SEARCH_WITH_EXACT:
            first = new JButton(Localization.lang("Keep left"));
            second = new JButton(Localization.lang("Keep right"));
            both = new JButton(Localization.lang("Keep both"));
            removeExact = new JButton(Localization.lang("Automatically remove exact duplicates"));
            me = new MergeEntries(one, two, frame.getCurrentBasePanel().getLoadedDatabase().getMode());
            break;
        default:
            first = new JButton(Localization.lang("Import and remove old entry"));
            second = new JButton(Localization.lang("Do not import entry"));
            both = new JButton(Localization.lang("Import and keep old entry"));
            me = new MergeEntries(one, two, Localization.lang("Old entry"),
                    Localization.lang("From import"), frame.getCurrentBasePanel().getLoadedDatabase().getMode());
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

        first.addActionListener(e -> buttonPressed(KEEP_UPPER));
        second.addActionListener(e -> buttonPressed(KEEP_LOWER));
        both.addActionListener(e -> buttonPressed(KEEP_BOTH));
        merge.addActionListener(e -> buttonPressed(KEEP_MERGE));
        if (removeExact != null) {
            removeExact.addActionListener(e -> buttonPressed(AUTOREMOVE_EXACT));
        }
        cancel.addActionListener(e -> buttonPressed(BREAK));

        getContentPane().add(me.getMergeEntryPanel());
        getContentPane().add(options, BorderLayout.SOUTH);
        pack();

        pw = new PositionWindow(this, JabRefPreferences.DUPLICATES_POS_X, JabRefPreferences.DUPLICATES_POS_Y,
                JabRefPreferences.DUPLICATES_SIZE_X, JabRefPreferences.DUPLICATES_SIZE_Y);
        pw.setWindowPosition();

        // Set up a ComponentListener that saves the last size and position of the dialog
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // Save dialog position
                pw.storeWindowPosition();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // Save dialog position
                pw.storeWindowPosition();
            }
        });

        both.requestFocus();

    }


    private void buttonPressed(int button) {
        status = button;
        dispose();
    }

    public int getSelected() {
        return status;
    }

    public BibEntry getMergedEntry() {
        return me.getMergeEntry();
    }

}
