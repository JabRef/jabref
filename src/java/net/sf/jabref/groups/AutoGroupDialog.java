/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.groups;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.undo.NamedCompound;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class AutoGroupDialog extends JDialog implements CaretListener {
    JTextField remove = new JTextField(60), field = new JTextField(60),
            deliminator = new JTextField(60);
    JLabel nf = new JLabel(Globals.lang("Field to group by") + ":"),
            nr = new JLabel(Globals.lang("Characters to ignore") + ":");
    JRadioButton
        keywords = new JRadioButton(Globals.lang("Generate groups from keywords in a BibTeX field")),
        authors = new JRadioButton(Globals.lang("Generate groups for author last names")),
        editors = new JRadioButton(Globals.lang("Generate groups for editor last names"));
    JCheckBox nd = new JCheckBox(Globals.lang(
    		"Use the following delimiter character(s)")
            + ":"); // JZTODO lyrics
    JButton ok = new JButton(Globals.lang("Ok")), cancel = new JButton(Globals
            .lang("Cancel"));
    JPanel main = new JPanel(), opt = new JPanel();
    private boolean ok_pressed = false;
    private GroupTreeNode m_groupsRoot;
    private JabRefFrame frame;
    private BasePanel panel;
    private GroupSelector gs;
    private String oldRemove, oldField;
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
        super(jabrefFrame, Globals.lang("Automatically create groups"), true);
        frame = jabrefFrame;
        gs = groupSelector;
        panel = basePanel;
        m_groupsRoot = groupsRoot;
        field.setText(defaultField);
        remove.setText(defaultRemove);
        deliminator.setText(defaultDeliminator);
        nd.setSelected(true);
        ActionListener okListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok_pressed = true;
                dispose();

                GroupTreeNode autoGroupsRoot = new GroupTreeNode(
                        new ExplicitGroup(Globals.lang("Automatically created groups"),
                        		AbstractGroup.INCLUDING));
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

                for (String keyword : hs){
                    KeywordGroup group = new KeywordGroup(keyword, field,
                            keyword, false, false, AbstractGroup.INDEPENDENT);
                    autoGroupsRoot.add(new GroupTreeNode(group));
                }

                m_groupsRoot.add(autoGroupsRoot);
                NamedCompound ce = new NamedCompound(Globals
                        .lang("Autogenerate groups"));
                UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                        gs, m_groupsRoot, autoGroupsRoot,
                        UndoableAddOrRemoveGroup.ADD_NODE);
                undo.setRevalidate(true);
                ce.addEdit(undo);

                panel.markBaseChanged(); // a change always occurs
                gs.revalidateGroups();
                frame.output(Globals.lang("Created groups."));
                ce.end();
                panel.undoManager.addEdit(ce);
            }
        };
        remove.addActionListener(okListener);
        field.addActionListener(okListener);
        field.addCaretListener(this);
        AbstractAction cancelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        cancel.addActionListener(cancelAction);
        ok.addActionListener(okListener);
        // Key bindings:
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
        b.append(Globals.lang("Field to group by")+":");
        b.append(field);
        b.nextLine();
        b.append(new JPanel());
        b.append(Globals.lang("Characters to ignore")+":");
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
        
        ButtonBarBuilder bb = new ButtonBarBuilder(opt);
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
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
        main.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        opt.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
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

    public String field() {
        return field.getText();
    }

    public String remove() {
        return remove.getText();
    }

    public void caretUpdate(CaretEvent e) {
        updateComponents();
    }
    
    protected void updateComponents() {
        String groupField = field.getText().trim();
        ok.setEnabled(groupField.matches("\\w+"));
    }
}
