/*
 Copyright (C) 2004 R. Nagel
 Copyright (C) 2016 JabRef Contributors


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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.wizard.auximport.AuxSubGenerator;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class FromAuxDialog extends JDialog {
    private final JPanel statusPanel = new JPanel();
    private final JPanel buttons = new JPanel();
    private final JButton selectInDBButton = new JButton();
    private final JButton generateButton = new JButton();
    private final JButton cancelButton = new JButton();
    private final JButton parseButton = new JButton();

    private final JComboBox<String> dbChooser = new JComboBox<>();
    private JTextField auxFileField;

    private JList<String> notFoundList;
    private JTextArea statusInfos;

    // all open databases from JabRefFrame
    private final JTabbedPane parentTabbedPane;

    private boolean generatePressed;

    private final AuxSubGenerator auxParser;

    private static final Log LOGGER = LogFactory.getLog(FromAuxDialog.class);

    private final JabRefFrame parent;


    public FromAuxDialog(JabRefFrame frame, String title, boolean modal,
            JTabbedPane viewedDBs) {
        super(frame, title, modal);

        parentTabbedPane = viewedDBs;
        parent = frame;

        auxParser = new AuxSubGenerator(null);

        try {
            jbInit();
            pack();
            setSize(600, 500);
        } catch (Exception ex) {
            LOGGER.warn("Problem creating dialog", ex);
        }
    }

    private void jbInit() {
        JPanel panel1 = new JPanel();

        panel1.setLayout(new BorderLayout());
        selectInDBButton.setText(Localization.lang("Select"));
        selectInDBButton.setEnabled(false);
        selectInDBButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                FromAuxDialog.this.select_actionPerformed();
            }
        });
        generateButton.setText(Localization.lang("Generate"));
        generateButton.setEnabled(false);
        generateButton.addActionListener(new FromAuxDialog_generate_actionAdapter(this));
        cancelButton.setText(Localization.lang("Cancel"));
        cancelButton.addActionListener(new FromAuxDialog_Cancel_actionAdapter(this));
        parseButton.setText(Localization.lang("Parse"));
        parseButton.addActionListener(new FromAuxDialog_parse_actionAdapter(this));

        initPanels();

        // insert the buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JPanel buttonPanel = bb.getPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bb.addGlue();
        bb.addButton(parseButton);
        bb.addRelatedGap();
        bb.addButton(selectInDBButton);
        bb.addButton(generateButton);
        bb.addButton(cancelButton);
        bb.addGlue();
        this.setModal(true);
        this.setResizable(true);
        this.setTitle(Localization.lang("AUX file import"));
        JLabel desc = new JLabel("<html><h3>" + Localization.lang("AUX file import") + "</h3><p>"
                + Localization.lang("This feature generates a new database based on which entries "
                + "are needed in an existing LaTeX document.") + "</p>"
                + "<p>" + Localization.lang("You need to select one of your open databases from which to choose "
                + "entries, as well as the AUX file produced by LaTeX when compiling your document.") + "</p></html>");
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
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private void initPanels() {
        // collect the names of all open databases
        int len = parentTabbedPane.getTabCount();
        int toSelect = -1;
        for (int i = 0; i < len; i++) {
            dbChooser.addItem(parentTabbedPane.getTitleAt(i));
            if (parent.getBasePanelAt(i) == parent.getCurrentBasePanel()) {
                toSelect = i;
            }
        }
        if (toSelect >= 0) {
            dbChooser.setSelectedIndex(toSelect);
        }

        auxFileField = new JTextField("", 25);
        JButton browseAuxFileButton = new JButton(Localization.lang("Browse"));
        browseAuxFileButton.addActionListener(new BrowseAction(auxFileField, parent));
        notFoundList = new JList<>();
        JScrollPane listScrollPane = new JScrollPane(notFoundList);
        //listScrollPane.setPreferredSize(new Dimension(250, 120));
        statusInfos = new JTextArea("", 5, 20);
        JScrollPane statusScrollPane = new JScrollPane(statusInfos);
        //statusScrollPane.setPreferredSize(new Dimension(250, 120));
        //statusInfos.setBorder(BorderFactory.createEtchedBorder());
        statusInfos.setEditable(false);

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout(
                "left:pref, 4dlu, fill:pref:grow, 4dlu, left:pref", ""), buttons);
        b.appendSeparator(Localization.lang("Options"));
        b.append(Localization.lang("Reference database") + ":");
        b.append(dbChooser, 3);
        b.nextLine();
        b.append(Localization.lang("LaTeX AUX file") + ":");
        b.append(auxFileField);
        b.append(browseAuxFileButton);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        b = new DefaultFormBuilder(new FormLayout(
                "fill:pref:grow, 4dlu, fill:pref:grow", "pref, pref, fill:pref:grow"), statusPanel);
        b.appendSeparator(Localization.lang("Result"));
        b.append(Localization.lang("Unknown bibtex entries") + ":");
        b.append(Localization.lang("Messages") + ":");
        b.nextLine();
        b.append(listScrollPane);
        b.append(statusScrollPane);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    void generate_actionPerformed(ActionEvent e) {
        generatePressed = true;
        dispose();
    }

    void cancel_actionPerformed(ActionEvent e) {
        dispose();
    }

    private void select_actionPerformed() {
        BibDatabase db = getGenerateDB();
        MainTable mainTable = parent.getCurrentBasePanel().mainTable;
        BibDatabase database = parent.getCurrentBasePanel().getDatabase();
        mainTable.clearSelection();
        for (BibEntry newEntry : db.getEntries()) {
            // the entries are not the same objects as in the original database
            // therefore, we have to search for the entries in the original database
            // to be able to find them in the maintable
            BibEntry origEntry = database.getEntryByKey(newEntry.getCiteKey());
            int row = mainTable.findEntry(origEntry);
            mainTable.addSelection(row);
        }
    }

    void parse_actionPerformed(ActionEvent e) {
        parseButton.setEnabled(false);
        BasePanel bp = (BasePanel) parentTabbedPane.getComponentAt(
                dbChooser.getSelectedIndex());
        notFoundList.removeAll();
        statusInfos.setText(null);
        BibDatabase refBase = bp.getDatabase();
        String auxName = auxFileField.getText();

        if ((auxName != null) && (refBase != null) && !auxName.isEmpty()) {
            auxParser.clear();
            List<String> list = auxParser.generate(auxName, refBase);
            notFoundList.setListData(list.toArray(new String[list.size()]));
            statusInfos.append(auxParser.getInformation(false));

            selectInDBButton.setEnabled(true);
            generateButton.setEnabled(true);
        }

        // the generated database contains no entries -> no active generate-button
        if (auxParser.emptyGeneratedDatabase()) {
            statusInfos.append("\n" + Localization.lang("empty database"));
            generateButton.setEnabled(false);
        }

        parseButton.setEnabled(true);
    }

    public boolean generatePressed() {
        return generatePressed;
    }

    public BibDatabase getGenerateDB() {
        return auxParser.getGeneratedDatabase();
    }

    /**
     * Action used to produce a "Browse" button for one of the text fields.
     */
    static class BrowseAction extends AbstractAction {
        private final JTextField comp;
        private final JabRefFrame _frame;


        public BrowseAction(JTextField tc, JabRefFrame frame) {
            super(Localization.lang("Browse"));
            _frame = frame;
            comp = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String chosen = FileDialogs.getNewFile(_frame,
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
class FromAuxDialog_generate_actionAdapter implements ActionListener {

    private final FromAuxDialog adaptee;


    FromAuxDialog_generate_actionAdapter(FromAuxDialog adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adaptee.generate_actionPerformed(e);
    }
}

class FromAuxDialog_Cancel_actionAdapter implements ActionListener {

    private final FromAuxDialog adaptee;


    FromAuxDialog_Cancel_actionAdapter(FromAuxDialog adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adaptee.cancel_actionPerformed(e);
    }
}

class FromAuxDialog_parse_actionAdapter implements ActionListener {

    private final FromAuxDialog adaptee;


    FromAuxDialog_parse_actionAdapter(FromAuxDialog adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adaptee.parse_actionPerformed(e);
    }
}
