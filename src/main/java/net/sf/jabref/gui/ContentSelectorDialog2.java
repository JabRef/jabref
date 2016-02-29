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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jgoodies.forms.builder.ButtonBarBuilder;

import net.sf.jabref.util.Util;

class ContentSelectorDialog2 extends JDialog {

    private final GridBagLayout gbl = new GridBagLayout();
    private final GridBagConstraints con = new GridBagConstraints();
    private final JPanel fieldPan = new JPanel();
    private final JPanel wordPan = new JPanel();
    private final JPanel buttonPan = new JPanel();
    private final JPanel fieldNamePan = new JPanel();
    private final JPanel wordEditPan = new JPanel();

    private final String WORD_FIRSTLINE_TEXT = Localization.lang("<select word>");
    private final String FIELD_FIRST_LINE = Localization.lang("<field name>");
    private final MetaData metaData;
    private String currentField;
    private final JabRefFrame frame;
    private final BasePanel panel;
    private final JButton newField = new JButton(Localization.lang("New"));
    private final JButton removeField = new JButton(Localization.lang("Remove"));
    private final JButton newWord = new JButton(Localization.lang("New"));
    private final JButton removeWord = new JButton(Localization.lang("Remove"));
    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JButton cancel = new JButton();
    private final JButton apply = new JButton(Localization.lang("Apply"));
    private final DefaultListModel<String> fieldListModel = new DefaultListModel<>();
    private DefaultListModel<String> wordListModel = new DefaultListModel<>();
    private final JList<String> fieldList = new JList<>(fieldListModel);
    private final JList<String> wordList = new JList<>(wordListModel);
    private final JTextField fieldNameField = new JTextField("", 20);
    private final JTextField wordEditField = new JTextField("", 20);
    private final JScrollPane fPane = new JScrollPane(fieldList);
    private final JScrollPane wPane = new JScrollPane(wordList);

    private final Map<String, DefaultListModel<String>> wordListModels = new HashMap<>();
    private final List<String> removedFields = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(ContentSelectorDialog2.class);

    /**
     *
     * @param owner the parent Window (Dialog or Frame)
     * @param frame the JabRef Frame
     * @param panel the currently selected BasePanel
     * @param modal should this dialog be modal?
     * @param metaData The metadata of the current database
     * @param fieldName the field this selector is initialized for. May be null.
     */
    public ContentSelectorDialog2(Window owner, JabRefFrame frame, BasePanel panel, boolean modal, MetaData metaData,
            String fieldName) {
        super(owner, Localization.lang("Setup selectors"));
        this.setModal(modal);
        this.metaData = metaData;
        this.frame = frame;
        this.panel = panel;
        this.currentField = fieldName;

        initLayout();

        setupFieldSelector();
        setupWordSelector();
        setupActions();
        Util.bindCloseDialogKeyToCancelAction(this.rootPane, cancel.getAction());
        int fieldInd = fieldListModel.indexOf(currentField);
        if (fieldInd >= 0) {
            fieldList.setSelectedIndex(fieldInd);
        }

        pack();
    }

    private void setupActions() {

        wordList.addListSelectionListener(e -> {
            wordEditField.setText(wordList.getSelectedValue());
            wordEditField.selectAll();
            new FocusRequester(wordEditField);
        });

        newWord.addActionListener(e -> newWordAction());

        ActionListener wordEditFieldListener = e -> {
            String old = wordList.getSelectedValue();
            String newVal = wordEditField.getText();
            if ("".equals(newVal) || newVal.equals(old)) {
                return; // Empty string or no change.
            }
            int index = wordList.getSelectedIndex();
            if (wordListModel.contains(newVal)) {
                // ensure that word already in list is visible
                index = wordListModel.indexOf(newVal);
                wordList.ensureIndexIsVisible(index);
                return;
            }

            int newIndex = findPos(wordListModel, newVal);
            if (index >= 0) {
                // initiate replacement of selected word
                wordListModel.remove(index);
                if (newIndex > index) {
                    // newIndex has to be adjusted after removal of previous entry
                    newIndex--;
                }
            }
            wordListModel.add(newIndex, newVal);
            wordList.ensureIndexIsVisible(newIndex);
            wordEditField.selectAll();
        };
        wordEditField.addActionListener(wordEditFieldListener);

        removeWord.addActionListener(e -> {
            int index = wordList.getSelectedIndex();
            if (index == -1) {
                return;
            }
            wordListModel.remove(index);
            wordEditField.setText("");
            if (!wordListModel.isEmpty()) {
                wordList.setSelectedIndex(Math.min(index, wordListModel.size() - 1));
            }
        });

        fieldList.addListSelectionListener(e -> {
            currentField = fieldList.getSelectedValue();
            fieldNameField.setText("");
            setupWordSelector();
        });

        newField.addActionListener(e -> {
            if (!fieldListModel.get(0).equals(FIELD_FIRST_LINE)) {
                // only add <field name> once
                fieldListModel.add(0, FIELD_FIRST_LINE);
            }
            fieldList.setSelectedIndex(0);
            fPane.getVerticalScrollBar().setValue(0);
            fieldNameField.setEnabled(true);
            fieldNameField.setText(currentField);
            fieldNameField.selectAll();

            new FocusRequester(fieldNameField);
        });

        fieldNameField.addActionListener(e -> fieldNameField.transferFocus());

        fieldNameField.addFocusListener(new FocusAdapter() {

            /**
             * Adds the text value to the list
             */
            @Override
            public void focusLost(FocusEvent e) {
                String s = fieldNameField.getText();
                fieldNameField.setText("");
                fieldNameField.setEnabled(false);
                if (!FIELD_FIRST_LINE.equals(s) && !"".equals(s)) {
                    // user has typed something

                    // remove "<first name>" from list
                    fieldListModel.remove(0);

                    int pos;
                    if (fieldListModel.contains(s)) {
                        // field already exists, scroll to that field (below)
                        pos = fieldListModel.indexOf(s);
                    } else {
                        // Add new field.
                        pos = findPos(fieldListModel, s);
                        fieldListModel.add(Math.max(0, pos), s);
                    }
                    fieldList.setSelectedIndex(pos);
                    fieldList.ensureIndexIsVisible(pos);
                    currentField = s;
                    setupWordSelector();
                    newWordAction();
                    //new FocusRequester(wordEditField);
                }
            }
        });

        removeField.addActionListener(e -> {
            int index = fieldList.getSelectedIndex();
            if (index == -1) {
                return;
            }
            String fieldName = fieldListModel.get(index);
            removedFields.add(fieldName);
            fieldListModel.remove(index);
            wordListModels.remove(fieldName);
            fieldNameField.setText("");
            if (!fieldListModel.isEmpty()) {
                fieldList.setSelectedIndex(Math.min(index, wordListModel.size() - 1));
            }
        });

        ok.addActionListener(e -> {
            try {
                applyChanges();
                dispose();
            } catch (Exception ex) {
                LOGGER.info("Could not apply changes in \"Setup selectors\"", ex);
                JOptionPane.showMessageDialog(frame, Localization.lang("Could not apply changes."));
            }
        });

        apply.addActionListener(e -> {
            // Store if an entry is currently being edited:
            if (!"".equals(wordEditField.getText())) {
                wordEditFieldListener.actionPerformed(null);
            }
            try {
                applyChanges();
            } catch (Exception ex) {
                LOGGER.info("Could not apply changes in \"Setup selectors\"", ex);
                JOptionPane.showMessageDialog(frame, Localization.lang("Could not apply changes."));
            }
        });

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        cancelAction.putValue(Action.NAME, Localization.lang("Cancel"));
        cancel.setAction(cancelAction);
    }

    private void newWordAction() {
        if (wordListModel.isEmpty() ||
                !wordListModel.get(0).equals(WORD_FIRSTLINE_TEXT)) {
            wordListModel.add(0, WORD_FIRSTLINE_TEXT);
        }
        wordList.setSelectedIndex(0);
        wPane.getVerticalScrollBar().setValue(0);
    }

    private void applyChanges() {
        boolean changedFieldSet = false; // Watch if we need to rebuild entry editors

        // First remove the mappings for fields that have been deleted.
        // If these were re-added, they will be added below, so it doesn't
        // cause any harm to remove them here.
        for (String fieldName : removedFields) {
            metaData.remove(Globals.SELECTOR_META_PREFIX + fieldName);
            changedFieldSet = true;
        }

        // Cycle through all fields that we have created listmodels for:
        for (String fieldName : wordListModels.keySet()) {
            // For each field name, store the values:
            if ((fieldName == null) || FIELD_FIRST_LINE.equals(fieldName)) {
                continue;
            }
            DefaultListModel<String> lm = wordListModels.get(fieldName);
            int start = 0;
            // Avoid storing the <new word> marker if it is there:
            if (!lm.isEmpty()) {
                while ((start < lm.size()) && lm.get(start).equals(WORD_FIRSTLINE_TEXT)) {
                    start++;
                }
            }
            List<String> data = metaData.getData(Globals.SELECTOR_META_PREFIX + fieldName);
            boolean bNewField = false;
            if (data == null) {
                bNewField = true;
                data = new ArrayList<>();
                changedFieldSet = true;

            } else {
                data.clear();
            }
            for (int wrd = start; wrd < lm.size(); wrd++) {
                String word = lm.get(wrd);
                data.add(word);
            }
            if (bNewField) {
                metaData.putData(Globals.SELECTOR_META_PREFIX + fieldName, data);
            }
        }

        // System.out.println("TODO: remove metadata for removed selector field.");
        panel.markNonUndoableBaseChanged();

        // Update all selectors in the current BasePanel.
        if (changedFieldSet) {
            panel.rebuildAllEntryEditors();
        } else {
            panel.updateAllContentSelectors();
        }
        panel.getAutoCompleters().addContentSelectorValuesToAutoCompleters(panel.getBibDatabaseContext().getMetaData());

    }

    /**
     * Set the contents of the field selector list.
     *
     */
    private void setupFieldSelector() {
        fieldListModel.clear();
        SortedSet<String> contents = new TreeSet<>();
        for (String s : metaData) {
            if (s.startsWith(Globals.SELECTOR_META_PREFIX)) {
                contents.add(s.substring(Globals.SELECTOR_META_PREFIX.length()));
            }
        }
        if (contents.isEmpty()) {
            // if nothing was added, put the default fields (as described in the help)
            fieldListModel.addElement("author");
            fieldListModel.addElement("journal");
            fieldListModel.addElement("keywords");
            fieldListModel.addElement("publisher");
        } else {
            for (String s : contents) {
                fieldListModel.addElement(s);
            }
        }

        if (currentField == null) {
            // if dialog is created for the whole database,
            // select the first field to avoid confusions in GUI usage
            fieldList.setSelectedIndex(0);
        } else {
            // a specific field has been chosen at the constructor
            // select this field
            int i = fieldListModel.indexOf(currentField);
            if (i != -1) {
                // field has been found in list, select it
                fieldList.setSelectedIndex(i);
            }
        }
    }

    private void setupWordSelector() {

        // Have we already created a listmodel for this field?
        wordListModel = wordListModels.get(currentField);
        if (wordListModel == null) {
            wordListModel = new DefaultListModel<>();
            wordList.setModel(wordListModel);
            wordListModels.put(currentField, wordListModel);
            List<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX + currentField);
            if (items != null) {
                TreeSet<String> wordSet = new TreeSet<>(items);
                int index = 0;
                for (String s : wordSet) {
                    wordListModel.add(index, s);
                    index++;
                }
            }
        } else {
            wordList.setModel(wordListModel);
        }
    }

    private static int findPos(DefaultListModel<String> lm, String item) {
        for (int i = 0; i < lm.size(); i++) {
            String s = lm.get(i);
            if (item.compareToIgnoreCase(s) < 0) { // item precedes s
                return i;
            }
        }
        return lm.size();
    }

    private void initLayout() {
        fieldNameField.setEnabled(false);
        fieldList.setVisibleRowCount(4);
        wordList.setVisibleRowCount(10);
        final String VAL = "Uren luren himmelturen, ja Besseggen.";
        fieldList.setPrototypeCellValue(VAL);
        wordList.setPrototypeCellValue(VAL);

        fieldPan.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createEtchedBorder(),
                        Localization.lang("Field name")));
        wordPan.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createEtchedBorder(),
                        Localization.lang("Keyword")));
        fieldPan.setLayout(gbl);
        wordPan.setLayout(gbl);
        con.insets = new Insets(2, 2, 2, 2);
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = 2;
        con.weightx = 1;
        con.weighty = 1;
        con.gridx = 0;
        con.gridy = 0;
        gbl.setConstraints(fPane, con);
        fieldPan.add(fPane);
        gbl.setConstraints(wPane, con);
        wordPan.add(wPane);
        con.gridwidth = 1;
        con.gridx = 2;
        //con.weightx = 0.7;
        con.gridheight = 2;
        gbl.setConstraints(fieldNamePan, con);
        fieldPan.add(fieldNamePan);
        gbl.setConstraints(wordEditPan, con);
        wordPan.add(wordEditPan);
        con.gridx = 0;
        con.gridy = 1;
        con.weightx = 0;
        con.weighty = 0;
        con.gridwidth = 1;
        con.gridheight = 1;
        con.fill = GridBagConstraints.NONE;
        con.anchor = GridBagConstraints.WEST;
        gbl.setConstraints(newField, con);
        fieldPan.add(newField);
        gbl.setConstraints(newWord, con);
        wordPan.add(newWord);
        con.gridx = 1;
        //con.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(removeField, con);
        fieldPan.add(removeField);
        gbl.setConstraints(removeWord, con);
        wordPan.add(removeWord);
        con.anchor = GridBagConstraints.WEST;
        con.gridx = 0;
        con.gridy = 0;
        gbl.setConstraints(fieldNameField, con);
        fieldNamePan.add(fieldNameField);
        gbl.setConstraints(wordEditField, con);
        wordEditPan.add(wordEditField);

        // Add buttons:
        ButtonBarBuilder bsb = new ButtonBarBuilder(buttonPan);
        bsb.addGlue();
        bsb.addButton(ok);
        bsb.addButton(apply);
        bsb.addButton(cancel);
        bsb.addRelatedGap();
        bsb.addButton(new HelpAction(HelpFiles.contentSelectorHelp).getHelpButton());
        bsb.addGlue();

        // Add panels to dialog:
        con.fill = GridBagConstraints.BOTH;
        getContentPane().setLayout(gbl);
        con.weightx = 1;
        con.weighty = 0.5;
        con.gridwidth = 1;
        con.gridheight = 1;
        con.gridx = 0;
        con.gridy = 0;
        gbl.setConstraints(fieldPan, con);
        getContentPane().add(fieldPan);
        con.gridy = 1;
        gbl.setConstraints(wordPan, con);
        getContentPane().add(wordPan);
        con.weighty = 0;
        con.gridy = 2;
        con.insets = new Insets(12, 2, 2, 2);
        gbl.setConstraints(buttonPan, con);
        getContentPane().add(buttonPan);

    }

}
