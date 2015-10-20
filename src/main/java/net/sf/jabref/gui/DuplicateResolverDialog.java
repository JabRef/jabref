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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.bibtex.BibtexEntryWriter;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.gui.mergeentries.MergeEntries;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

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

    private final Dimension DIM = new Dimension(800, 600);

    private PreviewPanel p1;
    private PreviewPanel p2;
    private PreviewPanel p3;
    private JTextArea ta1;
    private JTextArea ta2;
    private JTextArea ta3;
    private final JTabbedPane tabbed = new JTabbedPane();
    private final GridBagLayout gbl = new GridBagLayout();
    private final GridBagConstraints con = new GridBagConstraints();
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JButton merge = new JButton(Localization.lang("Keep merged entry only"));
    private JButton removeExact;
    private final JPanel options = new JPanel();
    private final JPanel main = new JPanel();
    private final JPanel source = new JPanel();
    private int status = DuplicateResolverDialog.NOT_CHOSEN;
    private boolean block = true;
    private MergeEntries me;

    public DuplicateResolverDialog(JFrame frame, BibtexEntry one, BibtexEntry two, int type) {
        super(frame, Localization.lang("Possible duplicate entries"), true);
        init(one, two, type);
        Util.placeDialog(this, frame);
    }

    public DuplicateResolverDialog(JDialog frame, BibtexEntry one, BibtexEntry two, int type) {
        super(frame, Localization.lang("Possible duplicate entries"), true);
        init(one, two, type);
        Util.placeDialog(this, frame);
    }

    private void init(BibtexEntry one, BibtexEntry two, int type) {
        JButton both;
        JButton second;
        JButton first;
        switch (type) {
        case DUPLICATE_SEARCH:
            first = new JButton(Localization.lang("Keep entry 1"));
            second = new JButton(Localization.lang("Keep entry 2"));
            both = new JButton(Localization.lang("Keep both"));
            me = new MergeEntries(one, two, Localization.lang("Entry 1"),
                    Localization.lang("Entry 2"));
            break;
        case INSPECTION:
            first = new JButton(Localization.lang("Remove old entry"));
            second = new JButton(Localization.lang("Remove entry from import"));
            both = new JButton(Localization.lang("Keep both"));
            me = new MergeEntries(one, two, Localization.lang("Old entry"),
                    Localization.lang("From import"));
            break;
        case DUPLICATE_SEARCH_WITH_EXACT:
            first = new JButton(Localization.lang("Keep entry 1"));
            second = new JButton(Localization.lang("Keep entry 2"));
            both = new JButton(Localization.lang("Keep both"));
            removeExact = new JButton(Localization.lang("Automatically remove exact duplicates"));
            me = new MergeEntries(one, two, Localization.lang("Entry 1"),
                    Localization.lang("Entry 2"));
            break;
        default:
            first = new JButton(Localization.lang("Import and remove old entry"));
            second = new JButton(Localization.lang("Do not import entry"));
            both = new JButton(Localization.lang("Import and keep old entry"));
            me = new MergeEntries(one, two, Localization.lang("Old entry"),
                    Localization.lang("From import"));
        }

        String layout = Globals.prefs.get(JabRefPreferences.PREVIEW_0);
        p1 = new PreviewPanel(null, one, null, new MetaData(), layout);
        p2 = new PreviewPanel(null, two, null, new MetaData(), layout);
        p3 = new PreviewPanel(null, me.getMergeEntry(), null, new MetaData(), layout);
        ta1 = new JTextArea();
        ta2 = new JTextArea();
        ta3 = new JTextArea();
        ta1.setEditable(false);
        ta2.setEditable(false);
        ta3.setEditable(false);


        //ta1.setPreferredSize(dim);
        //ta2.setPreferredSize(dim);

        setSourceView(one, two, me.getMergeEntry());

        //getContentPane().setLayout();
        main.setLayout(gbl);
        source.setLayout(gbl);
        con.insets = new Insets(10, 10, 0, 10);
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 1;
        con.weighty = 0;

        // @formatter:off
        TitleLabel lab = new TitleLabel((type == DuplicateResolverDialog.DUPLICATE_SEARCH) ||
                (type == DuplicateResolverDialog.DUPLICATE_SEARCH_WITH_EXACT) ? 
                        Localization.lang("Entry 1") :
                        Localization.lang("Entry in current database"));
        // @formatter:on
        gbl.setConstraints(lab, con);
        main.add(lab);
        // @formatter:off
        lab = new TitleLabel((type == DuplicateResolverDialog.DUPLICATE_SEARCH) ||
                (type == DuplicateResolverDialog.DUPLICATE_SEARCH_WITH_EXACT) ? 
                        Localization.lang("Entry 1") :
                        Localization.lang("Entry in current database"));
        // @formatter:on
        gbl.setConstraints(lab, con);
        source.add(lab);
        con.weighty = 1;
        con.insets = new Insets(5, 10, 10, 10);
        JScrollPane sp = new JScrollPane(p1);
        gbl.setConstraints(sp, con);
        main.add(sp);
        sp = new JScrollPane(ta1);
        gbl.setConstraints(sp, con);
        source.add(sp);
        con.weighty = 0;
        con.insets = new Insets(10, 10, 0, 10);
        // @formatter:off
        lab = new TitleLabel((type == DuplicateResolverDialog.DUPLICATE_SEARCH) ||
                (type == DuplicateResolverDialog.DUPLICATE_SEARCH_WITH_EXACT) ?
                        Localization.lang("Entry 2") :
                        Localization.lang("Entry in import"));
        // @formatter:on
        gbl.setConstraints(lab, con);
        main.add(lab);
        // @formatter:off
        lab = new TitleLabel((type == DuplicateResolverDialog.DUPLICATE_SEARCH) ||
                (type == DuplicateResolverDialog.DUPLICATE_SEARCH_WITH_EXACT) ?
                        Localization.lang("Entry 2") :
                        Localization.lang("Entry in import"));
        // @formatter:on
        gbl.setConstraints(lab, con);
        source.add(lab);
        con.weighty = 1;
        con.insets = new Insets(5, 10, 10, 10);
        sp = new JScrollPane(p2);
        gbl.setConstraints(sp, con);
        main.add(sp);
        sp = new JScrollPane(ta2);
        gbl.setConstraints(sp, con);
        source.add(sp);
        con.weighty = 0;
        con.insets = new Insets(10, 10, 0, 10);
        lab = new TitleLabel(Localization.lang("Merged entry"));
        gbl.setConstraints(lab, con);
        main.add(lab);
        lab = new TitleLabel(Localization.lang("Merged entry"));
        gbl.setConstraints(lab, con);
        source.add(lab);
        con.weighty = 1;
        con.insets = new Insets(5, 10, 10, 10);
        sp = new JScrollPane(p3);
        gbl.setConstraints(sp, con);
        main.add(sp);
        sp = new JScrollPane(ta3);
        gbl.setConstraints(sp, con);
        source.add(sp);
        tabbed.add(Localization.lang("Short form"), main);
        tabbed.add(Localization.lang("Complete record"), source);
        tabbed.add(Localization.lang("Merged entry"), me.getMergeEntryPanel());
        if (removeExact != null) {
            options.add(removeExact);
        }
        options.add(first);
        options.add(second);
        options.add(both);
        options.add(merge);
        options.add(Box.createHorizontalStrut(5));
        options.add(cancel);

        first.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                status = DuplicateResolverDialog.KEEP_UPPER;
                block = false;
                dispose();
            }
        });

        second.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                status = DuplicateResolverDialog.KEEP_LOWER;
                block = false;
                dispose();
            }
        });

        both.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                status = DuplicateResolverDialog.KEEP_BOTH;
                block = false;
                dispose();
            }
        });

        merge.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                status = DuplicateResolverDialog.KEEP_MERGE;
                block = false;
                dispose();
            }
        });

        if (removeExact != null) {
            removeExact.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    status = DuplicateResolverDialog.AUTOREMOVE_EXACT;
                    block = false;
                    dispose();
                }
            });
        }

        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                status = DuplicateResolverDialog.BREAK;
                block = false;
                dispose();
            }
        });

        //
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                if (((JTabbedPane) changeEvent.getSource()).getSelectedIndex() == 1) {
                    StringWriter sw = new StringWriter();
                    try {
                        new BibtexEntryWriter(new LatexFieldFormatter(), false).write(me.getMergeEntry(), sw);
                    } catch (IOException ignored) {
                    }
                    ta3.setText(sw.getBuffer().toString());
                    ta3.setCaretPosition(0);
                }
            }
        };

        tabbed.addChangeListener(changeListener);

        getContentPane().add(tabbed, BorderLayout.CENTER);
        getContentPane().add(options, BorderLayout.SOUTH);
        pack();

        if (getHeight() > DIM.height) {
            setSize(new Dimension(getWidth(), DIM.height));
        }
        if (getWidth() > DIM.width) {
            setSize(new Dimension(DIM.width, getHeight()));
        }

        both.requestFocus();

    }

    private void setSourceView(BibtexEntry one, BibtexEntry two, BibtexEntry three) {
        try {
            StringWriter sw = new StringWriter();
            new BibtexEntryWriter(new LatexFieldFormatter(), false).write(one, sw);
            ta1.setText(sw.getBuffer().toString());
            ta1.setCaretPosition(0);
            sw = new StringWriter();
            new BibtexEntryWriter(new LatexFieldFormatter(), false).write(two, sw);
            ta2.setText(sw.getBuffer().toString());
            ta2.setCaretPosition(0);
            sw = new StringWriter();
            new BibtexEntryWriter(new LatexFieldFormatter(), false).write(three, sw);
            ta3.setText(sw.getBuffer().toString());
            ta3.setCaretPosition(0);
        } catch (IOException ignored) {
        }
    }

    public void setEntries(BibtexEntry newOne, BibtexEntry newTwo, BibtexEntry newThree) {
        setSourceView(newOne, newTwo, newThree);
        p1.setEntry(newOne);
        p2.setEntry(newTwo);
        p3.setEntry(newThree);
        status = DuplicateResolverDialog.NOT_CHOSEN;
        p1.revalidate();
        p1.repaint();
        block = true;
    }

    public boolean isBlocking() {
        return block;
    }

    public int getSelected() {
        return status;
    }

    public BibtexEntry getMergedEntry() {
        return me.getMergeEntry();
    }

    public static int resolveDuplicate(JFrame frame, BibtexEntry one, BibtexEntry two) {
        DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two, DuplicateResolverDialog.DUPLICATE_SEARCH);
        drd.setVisible(true); // drd.show(); -> deprecated since 1.5
        return drd.getSelected();
    }

    public static int resolveDuplicate(JDialog frame, BibtexEntry one, BibtexEntry two) {
        DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two, DuplicateResolverDialog.DUPLICATE_SEARCH);
        drd.setVisible(true); // drd.show(); -> deprecated since 1.5
        return drd.getSelected();
    }

    public static int resolveDuplicateInImport(JabRefFrame frame, BibtexEntry existing, BibtexEntry imported) {
        DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, existing, imported, DuplicateResolverDialog.IMPORT_CHECK);
        drd.setVisible(true); // drd.show(); -> deprecated since 1.5
        return drd.getSelected();
    }

}
