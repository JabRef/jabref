/*
 Copyright (C) 2004 R. Nagel

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

// A wizard dialog for generating a new sub database from existing TeX aux file
//
// created by : r.nagel 23.08.2004
//
// modified : 18.04.2006 r.nagel
//            insert a "short info" section


package net.sf.jabref.wizard.auximport.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.wizard.auximport.AuxSubGenerator;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class FromAuxDialog
        extends JDialog {
    private JPanel statusPanel = new JPanel();
    private JPanel buttons = new JPanel();
    private JButton okButton = new JButton();
    private JButton cancelButton = new JButton();
    private JButton generateButton = new JButton();

    private JComboBox dbChooser = new JComboBox();
    private JTextField auxFileField;
    private JButton browseAuxFileButton;

    private JList notFoundList;
    private JTextArea statusInfos;

    // all open databases from JabRefFrame
    private JTabbedPane parentTabbedPane;

    private boolean okPressed = false;

    private AuxSubGenerator auxParser;

    public FromAuxDialog(JabRefFrame frame, String title, boolean modal,
                         JTabbedPane viewedDBs) {
        super(frame, title, modal);

        parentTabbedPane = viewedDBs;

        auxParser = new AuxSubGenerator(null);

        try {
            jbInit(frame);
            pack();
            setSize(600, 500);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit(JabRefFrame parent) {
        JPanel panel1 = new JPanel();

        panel1.setLayout(new BorderLayout());
        okButton.setText(Globals.lang("Ok"));
        okButton.setEnabled(false);
        okButton.addActionListener(new FromAuxDialog_ok_actionAdapter(this));
        cancelButton.setText(Globals.lang("Cancel"));
        cancelButton.addActionListener(new FromAuxDialog_Cancel_actionAdapter(this));
        generateButton.setText(Globals.lang("Generate"));
        generateButton.addActionListener(new FromAuxDialog_generate_actionAdapter(this));

        initPanels(parent);

        // insert the buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JPanel buttonPanel = bb.getPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bb.addGlue();
        bb.addButton(generateButton);
        bb.addRelatedGap();
        bb.addButton(okButton);
        bb.addButton(cancelButton);
        bb.addGlue();
        this.setModal(true);
        this.setResizable(true);
        this.setTitle(Globals.lang("AUX file import"));
        JLabel desc = new JLabel("<html><h3>"+Globals.lang("AUX file import")+"</h3><p>"
                    +Globals.lang("This feature generates a new database based on which entries "
                    +"are needed in an existing LaTeX document.")+"</p>"
                    +"<p>"+Globals.lang("You need to select one of your open databases from which to choose "
                    +"entries, as well as the AUX file produced by LaTeX when compiling your document.")+"</p></html>");
        desc.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel1.add(desc, BorderLayout.NORTH);

        JPanel centerPane = new JPanel(new BorderLayout());
        centerPane.add(buttons, BorderLayout.NORTH);
        centerPane.add(statusPanel, BorderLayout.CENTER);

        getContentPane().add(panel1, BorderLayout.NORTH);
        getContentPane().add(centerPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Key bindings:
        ActionMap am = statusPanel.getActionMap();
        InputMap im = statusPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(parent.prefs().getKey("Close dialog"), "close");
        am.put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private void initPanels(JabRefFrame parent) {
        // collect the names of all open databases
        int len = parentTabbedPane.getTabCount();
        int toSelect = -1;
        for (int i = 0; i<len; i++) {
            dbChooser.addItem(parentTabbedPane.getTitleAt(i));
            if (parent.baseAt(i) == parent.basePanel())
                toSelect = i;
        }
        if (toSelect >= 0)
            dbChooser.setSelectedIndex(toSelect);

        auxFileField = new JTextField("", 25);
        browseAuxFileButton = new JButton(Globals.lang("Browse"));
        browseAuxFileButton.addActionListener(new BrowseAction(auxFileField, parent));
        notFoundList = new JList();
        JScrollPane listScrollPane = new JScrollPane(notFoundList);
        //listScrollPane.setPreferredSize(new Dimension(250, 120));
        statusInfos = new JTextArea("", 5, 20);
        JScrollPane statusScrollPane = new JScrollPane(statusInfos);
        //statusScrollPane.setPreferredSize(new Dimension(250, 120));
        //statusInfos.setBorder(BorderFactory.createEtchedBorder());
        statusInfos.setEditable(false);

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout(
                "left:pref, 4dlu, fill:pref:grow, 4dlu, left:pref", ""), buttons);
        b.appendSeparator(Globals.lang("Options"));
        b.append(Globals.lang("Reference database") + ":");
        b.append(dbChooser, 3);
        b.nextLine();
        b.append(Globals.lang("LaTeX AUX file") + ":");
        b.append(auxFileField);
        b.append(browseAuxFileButton);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        b = new DefaultFormBuilder(new FormLayout(
                "fill:pref:grow, 4dlu, fill:pref:grow", "pref, pref, fill:pref:grow"), statusPanel);
        b.appendSeparator(Globals.lang("Unknown bibtex entries")+":");
        b.append(Globals.lang("Unknown bibtex entries")+":");
        b.append(Globals.lang("Messages")+":");
        b.nextLine();
        b.append(listScrollPane);
        b.append(statusScrollPane);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    }

    void ok_actionPerformed(ActionEvent e) {
        okPressed = true;
        dispose();
    }

    void Cancel_actionPerformed(ActionEvent e) {
        dispose();
    }

    void generate_actionPerformed(ActionEvent e) {
        generateButton.setEnabled(false);
        BasePanel bp = (BasePanel) parentTabbedPane.getComponentAt(
                dbChooser.getSelectedIndex());
        notFoundList.removeAll();
        statusInfos.setText(null);
        BibtexDatabase refBase = bp.getDatabase();
        String auxName = auxFileField.getText();

        if (auxName != null) {
            if ((refBase != null) && (auxName.length() > 0)) {
                auxParser.clear();
                notFoundList.setListData(auxParser.generate(auxName, refBase));

                statusInfos.append(Globals.lang("keys in database") + " " +
                        refBase.getEntryCount());
                statusInfos.append("\n" + Globals.lang("found in aux file") + " " +
                        auxParser.getFoundKeysInAux());
                statusInfos.append("\n" + Globals.lang("resolved") + " " +
                        auxParser.getResolvedKeysCount());
                statusInfos.append("\n" + Globals.lang("not found") + " " +
                        auxParser.getNotResolvedKeysCount());
                statusInfos.append("\n" + Globals.lang("crossreferenced entries included") + " " +
                        auxParser.getCrossreferencedEntriesCount());


                int nested = auxParser.getNestedAuxCounter();
                if (nested > 0) {
                    statusInfos.append("\n" + Globals.lang("nested_aux_files") + " " +
                            nested);
                }

                okButton.setEnabled(true);
            }
        }

        // the generated database contains no entries -> no active ok-button
        if (auxParser.getGeneratedDatabase().getEntryCount() < 1) {
            statusInfos.append("\n" + Globals.lang("empty database"));
            okButton.setEnabled(false);
        }

        generateButton.setEnabled(true);
    }

    public boolean okPressed() {
        return okPressed;
    }

    public BibtexDatabase getGenerateDB() {
        return auxParser.getGeneratedDatabase();
    }

// ---------------------------------------------------------------------------

    /**
     * Action used to produce a "Browse" button for one of the text fields.
     */
    class BrowseAction
            extends AbstractAction {
        private JTextField comp;
        private JabRefFrame _frame;

        public BrowseAction(JTextField tc, JabRefFrame frame) {
            super(Globals.lang("Browse"));
            _frame = frame;
            comp = tc;
        }

        public void actionPerformed(ActionEvent e) {
            String chosen = null;
            chosen = FileDialogs.getNewFile(_frame,
                    new File(comp.getText()),
                    ".aux",
                    JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                comp.setText(newFile.getPath());
            }
        }
    }

}

// ----------- helper class -------------------
class FromAuxDialog_ok_actionAdapter
        implements java.awt.event.ActionListener {
    FromAuxDialog adaptee;

    FromAuxDialog_ok_actionAdapter(FromAuxDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.ok_actionPerformed(e);
    }
}

class FromAuxDialog_Cancel_actionAdapter
        implements java.awt.event.ActionListener {
    FromAuxDialog adaptee;

    FromAuxDialog_Cancel_actionAdapter(FromAuxDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.Cancel_actionPerformed(e);
    }
}

class FromAuxDialog_generate_actionAdapter
        implements java.awt.event.ActionListener {
    FromAuxDialog adaptee;

    FromAuxDialog_generate_actionAdapter(FromAuxDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.generate_actionPerformed(e);
    }
}
