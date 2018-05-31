package org.jabref.gui.actions;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.specialfields.SpecialFieldsUtils;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.strings.StringUtil;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * An Action for launching keyword managing dialog
 *
 */
public class ManageKeywordsAction extends MnemonicAwareAction {

    private final JabRefFrame frame;
    private final KeywordList sortedKeywordsOfAllEntriesBeforeUpdateByUser = new KeywordList();
    private JDialog diag;
    private DefaultListModel<Keyword> keywordListModel;
    private JRadioButton intersectKeywords;
    private JRadioButton mergeKeywords;
    private boolean canceled;


    public ManageKeywordsAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Manage keywords"));
        this.frame = frame;
    }

    private void createDialog() {
        if (diag != null) {
            return;
        }
        // keyword to add
        JTextField keyword = new JTextField();

        keywordListModel = new DefaultListModel<>();
        JList<Keyword> keywordList = new JList<>(keywordListModel);
        keywordList.setVisibleRowCount(8);
        JScrollPane kPane = new JScrollPane(keywordList);

        diag = new JDialog(frame, Localization.lang("Manage keywords"), true);

        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        JButton add = new JButton(Localization.lang("Add"));
        JButton remove = new JButton(Localization.lang("Remove"));
        JButton replace = new JButton(Localization.lang("Replace"));
        JButton join = new JButton(Localization.lang("Join"));

        join.setToolTipText(Localization.lang("Joins selected keywords and deletes selected keywords."));

        keywordList.setVisibleRowCount(10);

        intersectKeywords = new JRadioButton(Localization.lang("Display keywords appearing in ALL entries"));
        mergeKeywords = new JRadioButton(Localization.lang("Display keywords appearing in ANY entry"));
        ButtonGroup group = new ButtonGroup();
        group.add(intersectKeywords);
        group.add(mergeKeywords);
        ActionListener stateChanged = e -> fillKeyWordList();
        intersectKeywords.addActionListener(stateChanged);
        mergeKeywords.addActionListener(stateChanged);
        intersectKeywords.setSelected(true);

        FormBuilder builder = FormBuilder.create().layout(new FormLayout("fill:200dlu:grow, pref, fill:pref",
                "pref, 2dlu, pref, 1dlu, pref, 2dlu, fill:100dlu:grow, 4dlu, pref, 4dlu, pref, "));

        builder.addSeparator(Localization.lang("Keywords of selected entries")).xyw(1, 1, 3);
        builder.add(intersectKeywords).xyw(1, 3, 3);
        builder.add(mergeKeywords).xyw(1, 5, 3);
        builder.add(kPane).xywh(1, 7, 1, 3);
        builder.add(join).xy(2,9);
        builder.add(replace).xy(3, 9);
        builder.add(keyword).xy(1, 11);
        builder.add(add).xy(2, 11);
        builder.add(remove).xy(3, 11);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ok.addActionListener(e -> {
            canceled = false;
            diag.dispose();
        });

        Action cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        final ActionListener addActionListener = arg0 -> addButtonActionListener(keyword);

        add.addActionListener(addActionListener);

        final ActionListener removeActionListener = arg0 -> {
            // keywordList.getSelectedIndices(); does not work, therefore we operate on the values
            List<Keyword> values = keywordList.getSelectedValuesList();

            for (Keyword val : values) {
                keywordListModel.removeElement(val);
            }
        };

        remove.addActionListener(removeActionListener);

        final ActionListener joinActionListener = arg0 -> {
            List<Keyword> values = keywordList.getSelectedValuesList();
            String joinedKeyword = values.stream().map(currentKeyword -> currentKeyword.get()).collect(Collectors.joining(" "));
            this.addKeywordToKeywordListModel(joinedKeyword);

            for (Keyword val : values) {
                this.keywordListModel.removeElement(val);
            }
        };

        join.addActionListener(joinActionListener);

        final ActionListener replaceActionListener = arg0 -> {
            List<Keyword> values = keywordList.getSelectedValuesList();

            for (Keyword val : values) {
                keywordListModel.removeElement(val);
            }
            addButtonActionListener(keyword);
        };

        replace.addActionListener(replaceActionListener);

        //enable a user to press Delete to delete a keyword
        keywordList.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeActionListener.actionPerformed(null);
                }
            }
        });

        //enable a user to press Enter to add a keyword
        keyword.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addActionListener.actionPerformed(null);
                }
            }
        });

        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
    }

    private void addButtonActionListener(JTextField keywordTextField) {
        if (StringUtil.isBlank(keywordTextField.getText())) {
            return; // nothing to add
        }
        addKeywordToKeywordListModel(keywordTextField.getText());
        keywordTextField.setText(null);
        keywordTextField.requestFocusInWindow();

    }

    /**
     * Adds given keyword to the keyword list model
     */
    private void addKeywordToKeywordListModel(String keyword) {
        String keywordToAdd = Objects.requireNonNull(keyword).trim();
        Keyword newKeyword = new Keyword(keywordToAdd);
        if (keywordListModel.isEmpty()) {
            keywordListModel.addElement(newKeyword);
        } else {
            int idx = 0;
            Keyword element = keywordListModel.getElementAt(idx);
            while ((idx < keywordListModel.size()) && (element.compareTo(newKeyword) < 0)) {
                idx++;
            }
            if (idx == keywordListModel.size()) {
                // list is empty or word is greater than last word in list
                keywordListModel.addElement(newKeyword);
            } else if (element.compareTo(newKeyword) == 0) {
                // nothing to do, word already in table
            } else {
                keywordListModel.add(idx, newKeyword);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BasePanel bp = frame.getCurrentBasePanel();
        if (bp == null) {
            return;
        }
        if (bp.getSelectedEntries().isEmpty()) {
            bp.output(Localization.lang("Select at least one entry to manage keywords."));
            return;
        }

        // Lazy creation of the dialog:
        createDialog();

        canceled = true;

        fillKeyWordList();

        diag.pack();
        diag.setLocationRelativeTo(frame);
        diag.setVisible(true);
        if (canceled) {
            return;
        }

        KeywordList keywordsToAdd = new KeywordList();
        KeywordList userSelectedKeywords = new KeywordList();
        // build keywordsToAdd and userSelectedKeywords in parallel
        for (Enumeration<Keyword> keywords = keywordListModel.elements(); keywords.hasMoreElements();) {
            Keyword keyword = keywords.nextElement();
            userSelectedKeywords.add(keyword);
            if (!sortedKeywordsOfAllEntriesBeforeUpdateByUser.contains(keyword)) {
                keywordsToAdd.add(keyword);
            }
        }

        KeywordList keywordsToRemove = new KeywordList();
        for (Keyword kword : sortedKeywordsOfAllEntriesBeforeUpdateByUser) {
            if (!userSelectedKeywords.contains(kword)) {
                keywordsToRemove.add(kword);
            }
        }

        if (keywordsToAdd.isEmpty() && keywordsToRemove.isEmpty()) {
            // nothing to be done if nothing is new and nothing is obsolete
            return;
        }

        if (Globals.prefs.isKeywordSyncEnabled() && !keywordsToAdd.isEmpty()) {
            SpecialFieldsUtils.synchronizeSpecialFields(keywordsToAdd, keywordsToRemove);
        }

        NamedCompound ce = updateKeywords(bp.getSelectedEntries(), keywordsToAdd, keywordsToRemove);
        bp.getUndoManager().addEdit(ce);
        bp.markBaseChanged();
    }

    private NamedCompound updateKeywords(List<BibEntry> entries, KeywordList keywordsToAdd,
                                         KeywordList keywordsToRemove) {
        NamedCompound ce = new NamedCompound(Localization.lang("Update keywords"));
        for (BibEntry entry : entries) {
            KeywordList keywords = entry.getKeywords(Globals.prefs.getKeywordDelimiter());

            // update keywords
            keywords.removeAll(keywordsToRemove);
            keywords.addAll(keywordsToAdd);

            // put keywords back
            Optional<FieldChange> change = entry.putKeywords(keywords, Globals.prefs.getKeywordDelimiter());
            if (change.isPresent()) {
                ce.addEdit(new UndoableFieldChange(change.get()));
            }

            if (Globals.prefs.isKeywordSyncEnabled()) {
                SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, Globals.prefs.getKeywordDelimiter());
            }
        }
        ce.end();
        return ce;
    }

    private void fillKeyWordList() {
        BasePanel bp = frame.getCurrentBasePanel();
        List<BibEntry> entries = bp.getSelectedEntries();

        // fill dialog with values
        keywordListModel.clear();
        sortedKeywordsOfAllEntriesBeforeUpdateByUser.clear();

        if (mergeKeywords.isSelected()) {
            for (BibEntry entry : entries) {
                KeywordList separatedKeywords = entry.getKeywords(Globals.prefs.getKeywordDelimiter());
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);
            }
        } else {
            assert intersectKeywords.isSelected();

            // all keywords from first entry have to be added
            BibEntry firstEntry = entries.get(0);
            KeywordList separatedKeywords = firstEntry.getKeywords(Globals.prefs.getKeywordDelimiter());
            sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);

            // for the remaining entries, intersection has to be used
            // this approach ensures that one empty keyword list leads to an empty set of common keywords
            for (int i = 1; i < entries.size(); i++) {
                BibEntry entry = entries.get(i);
                separatedKeywords = entry.getKeywords(Globals.prefs.getKeywordDelimiter());
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.retainAll(separatedKeywords);
            }
        }
        for (Keyword keyword : sortedKeywordsOfAllEntriesBeforeUpdateByUser) {
            keywordListModel.addElement(keyword);
        }
    }

}
