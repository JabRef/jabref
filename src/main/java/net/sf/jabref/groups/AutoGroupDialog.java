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
package net.sf.jabref.groups;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.groups.structure.ExplicitGroup;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;
import net.sf.jabref.gui.undo.NamedCompound;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class AutoGroupDialog extends JDialog implements CaretListener {

    private final JTextField remove = new JTextField(60);
    private final JTextField field = new JTextField(60);
    private final JTextField deliminator = new JTextField(60);
    JLabel nf = new JLabel(Localization.lang("Field to group by") + ":");
    JLabel nr = new JLabel(Localization.lang("Characters to ignore") + ":");
    private final JRadioButton
            keywords = new JRadioButton(Localization.lang("Generate groups from keywords in a BibTeX field"));
    private final JRadioButton authors = new JRadioButton(Localization.lang("Generate groups for author last names"));
    private final JRadioButton editors = new JRadioButton(Localization.lang("Generate groups for editor last names"));
    private final JCheckBox nd = new JCheckBox(Localization.lang("Use the following delimiter character(s):"));
    private final JButton ok = new JButton(Localization.lang("Ok"));
    private boolean ok_pressed;
    private final GroupTreeNode m_groupsRoot;
    private final JabRefFrame frame;
    private final BasePanel panel;
    private final GroupSelector gs;
    private String oldRemove;
    private String oldField;
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();


    /**
     * @param groupsRoot
     *            The original set of groups, which is required as undo
     *            information when all groups are cleared.
     */
    public AutoGroupDialog(JabRefFrame jabrefFrame, BasePanel basePanel,
            GroupSelector groupSelector, GroupTreeNode groupsRoot,
            String defaultField, String defaultRemove, String defaultDeliminator) {
        super(jabrefFrame, Localization.lang("Automatically create groups"), true);
        frame = jabrefFrame;
        gs = groupSelector;
        panel = basePanel;
        m_groupsRoot = groupsRoot;
        field.setText(defaultField);
        remove.setText(defaultRemove);
        deliminator.setText(defaultDeliminator);
        nd.setSelected(true);
        ActionListener okListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ok_pressed = true;
                dispose();

                GroupTreeNode autoGroupsRoot = new GroupTreeNode(
                        new ExplicitGroup(Localization.lang("Automatically created groups"),
                                GroupHierarchyType.INCLUDING));
                Set<String> hs = null;
                String field = field();
                if (keywords.isSelected()) {
                    if (nd.isSelected()) {
                        hs = Util
                                .findDeliminatedWordsInField(panel.getDatabase(),
                                        field().toLowerCase().trim(), deliminator
                                                .getText());
                    } else {
                        hs = Util.findAllWordsInField(panel.getDatabase(),
                                field().toLowerCase().trim(), remove());

                    }
                }
                else if (authors.isSelected()) {
                    List<String> fields = new ArrayList<String>(2);
                    fields.add("author");
                    hs = Util.findAuthorLastNames(panel.getDatabase(), fields);
                    field = "author";
                }
                else if (editors.isSelected()) {
                    List<String> fields = new ArrayList<String>(2);
                    fields.add("editor");
                    hs = Util.findAuthorLastNames(panel.getDatabase(), fields);
                    field = "editor";
                }

                for (String keyword : hs) {
                    KeywordGroup group = new KeywordGroup(keyword, field,
                            keyword, false, false, GroupHierarchyType.INDEPENDENT);
                    autoGroupsRoot.add(new GroupTreeNode(group));
                }

                m_groupsRoot.add(autoGroupsRoot);
                NamedCompound ce = new NamedCompound(Localization.lang("Autogenerate groups"));
                UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                        gs, m_groupsRoot, autoGroupsRoot,
                        UndoableAddOrRemoveGroup.ADD_NODE);
                undo.setRevalidate(true);
                ce.addEdit(undo);

                panel.markBaseChanged(); // a change always occurs
                gs.revalidateGroups();
                frame.output(Localization.lang("Created groups."));
                ce.end();
                panel.undoManager.addEdit(ce);
            }
        };
        remove.addActionListener(okListener);
        field.addActionListener(okListener);
        field.addCaretListener(this);
        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(cancelAction);
        ok.addActionListener(okListener);
        // Key bindings:
        JPanel main = new JPanel();
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(frame.prefs().getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        ButtonGroup bg = new ButtonGroup();
        bg.add(keywords);
        bg.add(authors);
        bg.add(editors);
        keywords.setSelected(true);
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout
                ("left:20dlu, 4dlu, left:pref, 4dlu, fill:60dlu, 4dlu, fill:0dlu", ""), main);
        b.append(keywords, 5);
        b.nextLine();
        b.append(new JPanel());
        b.append(Localization.lang("Field to group by") + ":");
        b.append(field);
        b.nextLine();
        b.append(new JPanel());
        b.append(Localization.lang("Characters to ignore") + ":");
        b.append(remove);
        b.nextLine();
        b.append(new JPanel());
        b.append(nd);
        b.append(deliminator);
        b.nextLine();
        b.append(authors, 5);
        b.nextLine();
        b.append(editors, 5);
        b.nextLine();

        JPanel opt = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(opt);
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        // Layout starts here.
        /*main.setLayout(gbl);
        opt.setLayout(gbl);
        main.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), Globals.lang("Group properties")));
        // Main panel:
        con.weightx = 0;
        con.gridwidth = 1;
        con.insets = new Insets(3, 5, 3, 5);
        con.anchor = GridBagConstraints.EAST;
        con.fill = GridBagConstraints.NONE;
        con.gridx = 0;
        con.gridy = 0;
        gbl.setConstraints(nf, con);
        main.add(nf);
        con.gridy = 1;
        gbl.setConstraints(nr, con);
        main.add(nr);
        con.gridy = 2;
        gbl.setConstraints(nd, con);
        main.add(nd);
        con.weightx = 1;
        con.anchor = GridBagConstraints.WEST;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.gridy = 0;
        con.gridx = 1;
        gbl.setConstraints(field, con);
        main.add(field);
        con.gridy = 1;
        gbl.setConstraints(remove, con);
        main.add(remove);
        con.gridy = 2;
        gbl.setConstraints(deliminator, con);
        main.add(deliminator);
        // Option buttons:
        con.gridx = GridBagConstraints.RELATIVE;
        con.gridy = GridBagConstraints.RELATIVE;
        con.weightx = 1;
        con.gridwidth = 1;
        con.anchor = GridBagConstraints.EAST;
        con.fill = GridBagConstraints.NONE;
        gbl.setConstraints(ok, con);
        opt.add(ok);
        con.anchor = GridBagConstraints.WEST;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(cancel, con);
        opt.add(cancel);*/
        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        opt.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(opt, BorderLayout.SOUTH);
        // pack();
        updateComponents();
        pack();
        Util.placeDialog(this, frame);
    }

    public boolean okPressed() {
        return ok_pressed;
    }

    public String oldField() {
        return oldField;
    }

    public String oldRemove() {
        return oldRemove;
    }

    private String field() {
        return field.getText();
    }

    private String remove() {
        return remove.getText();
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        updateComponents();
    }

    private void updateComponents() {
        String groupField = field.getText().trim();
        ok.setEnabled(groupField.matches("\\w+"));
    }
}
