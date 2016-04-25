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

package net.sf.jabref.gui.auximport;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;

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

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.auxparser.AuxParser;
import net.sf.jabref.logic.auxparser.AuxParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class FromAuxDialog extends JDialog {
    private final JPanel statusPanel = new JPanel();
    private final JPanel buttons = new JPanel();
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

    private AuxParser auxParser;

    private final JabRefFrame parentFrame;


    public FromAuxDialog(JabRefFrame frame, String title, boolean modal,
                         JTabbedPane viewedDBs) {
        super(frame, title, modal);

        parentTabbedPane = viewedDBs;
        parentFrame = frame;

        jbInit();
        pack();
        setSize(600, 500);
    }

    private void jbInit() {
        JPanel panel1 = new JPanel();

        panel1.setLayout(new BorderLayout());
        generateButton.setText(Localization.lang("Generate"));
        generateButton.setEnabled(false);
        generateButton.addActionListener(e -> {
            generatePressed = true;
            dispose();
        });
        cancelButton.setText(Localization.lang("Cancel"));
        cancelButton.addActionListener(e -> dispose());

        parseButton.setText(Localization.lang("Parse"));
        parseButton.addActionListener(e -> parseActionPerformed());

        initPanels();

        // insert the buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JPanel buttonPanel = bb.getPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bb.addGlue();
        bb.addButton(parseButton);
        bb.addRelatedGap();
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
            if (parentFrame.getBasePanelAt(i) == parentFrame.getCurrentBasePanel()) {
                toSelect = i;
            }
        }
        if (toSelect >= 0) {
            dbChooser.setSelectedIndex(toSelect);
        }

        auxFileField = new JTextField("", 25);
        JButton browseAuxFileButton = new JButton(Localization.lang("Browse"));
        browseAuxFileButton.addActionListener(new BrowseAction(auxFileField, parentFrame));
        notFoundList = new JList<>();
        JScrollPane listScrollPane = new JScrollPane(notFoundList);
        statusInfos = new JTextArea("", 5, 20);
        JScrollPane statusScrollPane = new JScrollPane(statusInfos);
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
        b.append(Localization.lang("Unknown BibTeX entries") + ":");
        b.append(Localization.lang("Messages") + ":");
        b.nextLine();
        b.append(listScrollPane);
        b.append(statusScrollPane);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    private void parseActionPerformed() {
        parseButton.setEnabled(false);
        BasePanel bp = (BasePanel) parentTabbedPane.getComponentAt(dbChooser.getSelectedIndex());
        notFoundList.removeAll();
        statusInfos.setText(null);
        BibDatabase refBase = bp.getDatabase();
        String auxName = auxFileField.getText();

        if ((auxName != null) && (refBase != null) && !auxName.isEmpty()) {
            auxParser = new AuxParser(auxName, refBase);
            AuxParserResult result = auxParser.parse();
            notFoundList.setListData(result.getUnresolvedKeys().toArray(new String[result.getUnresolvedKeys().size()]));
            statusInfos.append(result.getInformation(false));

            generateButton.setEnabled(true);

            // the generated database contains no entries -> no active generate-button
            if (!result.getGeneratedBibDatabase().hasEntries()) {
                statusInfos.append("\n" + Localization.lang("empty database"));
                generateButton.setEnabled(false);
            }
        } else {
            generateButton.setEnabled(false);
        }

        parseButton.setEnabled(true);
    }

    public boolean generatePressed() {
        return generatePressed;
    }

    public BibDatabase getGenerateDB() {
        return auxParser.parse().getGeneratedBibDatabase();
    }

    /**
     * Action used to produce a "Browse" button for one of the text fields.
     */
    static class BrowseAction extends AbstractAction {
        private final JTextField comp;
        private final JabRefFrame frame;


        public BrowseAction(JTextField tc, JabRefFrame frame) {
            super(Localization.lang("Browse"));
            this.frame = frame;
            comp = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String chosen = FileDialogs.getNewFile(frame,
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
