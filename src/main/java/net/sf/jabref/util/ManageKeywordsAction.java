/*  Copyright (C) 2003-2012 JabRef contributors.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.jabref.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.TreeSet;

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

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.gui.AutoCompleteListener;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import net.sf.jabref.gui.undo.NamedCompound;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * An Action for launching mass field.
 *
 * Functionality:
 * * Defaults to selected entries, or all entries if none are selected.
 * * Input field name
 * * Either set field, or clear field.
 */
public class ManageKeywordsAction extends MnemonicAwareAction {

    private final JabRefFrame frame;

    private JDialog diag;

    // keyword to add
    private JTextField keyword;

    private DefaultListModel keywordListModel;
    private JList keywordList;

    private JRadioButton intersectKeywords;
    private JRadioButton mergeKeywords;

    private boolean cancelled;

    private final TreeSet<String> sortedKeywordsOfAllEntriesBeforeUpdateByUser = new TreeSet<String>();


    public ManageKeywordsAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Manage keywords"));
        this.frame = frame;
    }

    private void createDialog() {
        keyword = new JTextField();

        keywordListModel = new DefaultListModel();
        keywordList = new JList(keywordListModel);
        keywordList.setVisibleRowCount(8);
        JScrollPane kPane = new JScrollPane(keywordList);

        diag = new JDialog(frame, Localization.lang("Manage keywords"), true);

        JButton ok = new JButton(Localization.lang("Ok"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        JButton add = new JButton(Localization.lang("Add"));
        JButton remove = new JButton(Localization.lang("Remove"));

        keywordList.setVisibleRowCount(10);

        intersectKeywords = new JRadioButton("Display keywords appearing in ALL entries");
        mergeKeywords = new JRadioButton("Display keywords appearing in ANY entry");
        ButtonGroup group = new ButtonGroup();
        group.add(intersectKeywords);
        group.add(mergeKeywords);
        ActionListener stateChanged = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                fillKeyWordList();
            }
        };
        intersectKeywords.addActionListener(stateChanged);
        mergeKeywords.addActionListener(stateChanged);
        intersectKeywords.setSelected(true);

        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("fill:200dlu, 4dlu, left:pref, 4dlu, left:pref", ""));
        builder.appendSeparator(Localization.lang("Keywords of selected entries"));
        builder.append(intersectKeywords, 5);
        builder.nextLine();
        builder.append(mergeKeywords, 5);
        builder.nextLine();
        builder.append(kPane, 3);
        builder.add(remove);
        builder.nextLine();
        builder.append(keyword, 3);
        builder.append(add);
        builder.nextLine();

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = false;
                diag.dispose();
            }
        });

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        final ActionListener addActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                String text = keyword.getText().trim();
                if (text.isEmpty()) {
                    // no text to add, do nothing
                    return;
                }
                if (keywordListModel.isEmpty()) {
                    keywordListModel.addElement(text);
                } else {
                    int idx = 0;
                    String element = (String) keywordListModel.getElementAt(idx);
                    while (idx < keywordListModel.size() &&
                            element.compareTo(text) < 0) {
                        idx++;
                    }
                    if (idx == keywordListModel.size()) {
                        // list is empty or word is greater than last word in list
                        keywordListModel.addElement(text);
                    } else if (element.compareTo(text) == 0) {
                        // nothing to do, word already in table
                    } else {
                        keywordListModel.add(idx, text);
                    }
                }
                keyword.setText(null);
                keyword.requestFocusInWindow();
            }
        };
        add.addActionListener(addActionListener);

        final ActionListener removeActionListenter = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                // keywordList.getSelectedIndices(); does not work, therefore we operate on the values
                String[] values = (String[]) keywordList.getSelectedValues();

                for (String val : values) {
                    keywordListModel.removeElement(val);
                }
            }
        };
        remove.addActionListener(removeActionListenter);
        keywordList.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent arg0) {
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeActionListenter.actionPerformed(null);
                }
            }
        });

        AutoCompleter autoComp = JabRef.jrf.basePanel().getAutoCompleters().get("keywords");
        AutoCompleteListener acl = new AutoCompleteListener(autoComp);
        keyword.addKeyListener(acl);
        keyword.addFocusListener(acl);
        keyword.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

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
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        //diag.pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BasePanel bp = frame.basePanel();
        if (bp == null) {
            return;
        }
        if (bp.getSelectedEntries().length == 0) {
            // no entries selected, silently ignore action
            return;
        }

        // Lazy creation of the dialog:
        if (diag == null) {
            createDialog();
        }

        cancelled = true;

        fillKeyWordList();

        diag.pack();
        Util.placeDialog(diag, frame);
        diag.setVisible(true);
        if (cancelled) {
            return;
        }

        HashSet<String> keywordsToAdd = new HashSet<String>();
        HashSet<String> userSelectedKeywords = new HashSet<String>();
        // build keywordsToAdd and userSelectedKeywords in parallel
        for (Enumeration keywords = keywordListModel.elements(); keywords.hasMoreElements();) {
            String keyword = (String) keywords.nextElement();
            userSelectedKeywords.add(keyword);
            if (!sortedKeywordsOfAllEntriesBeforeUpdateByUser.contains(keyword)) {
                keywordsToAdd.add(keyword);
            }
        }

        HashSet<String> keywordsToRemove = new HashSet<String>();
        for (String keyword : sortedKeywordsOfAllEntriesBeforeUpdateByUser) {
            if (!userSelectedKeywords.contains(keyword)) {
                keywordsToRemove.add(keyword);
            }
        }

        if (keywordsToAdd.isEmpty() && keywordsToRemove.isEmpty()) {
            // nothing to be done if nothing is new and nothing is obsolete
            return;
        }

        if (SpecialFieldsUtils.keywordSyncEnabled()) {
            if (!keywordsToAdd.isEmpty()) {
                // we need to check whether a special field is added
                // for each field:
                //   check if something is added
                //   if yes, add all keywords of that special fields to the keywords to be removed

                HashSet<String> clone;

                // Priority
                clone = createClone(keywordsToAdd);
                clone.retainAll(Priority.getInstance().getKeyWords());
                if (!clone.isEmpty()) {
                    keywordsToRemove.addAll(Priority.getInstance().getKeyWords());
                }

                // Quality
                clone = createClone(keywordsToAdd);
                clone.retainAll(Quality.getInstance().getKeyWords());
                if (!clone.isEmpty()) {
                    keywordsToRemove.addAll(Quality.getInstance().getKeyWords());
                }

                // Rank
                clone = createClone(keywordsToAdd);
                clone.retainAll(Rank.getInstance().getKeyWords());
                if (!clone.isEmpty()) {
                    keywordsToRemove.addAll(Rank.getInstance().getKeyWords());
                }

                // Relevance
                clone = createClone(keywordsToAdd);
                clone.retainAll(Relevance.getInstance().getKeyWords());
                if (!clone.isEmpty()) {
                    keywordsToRemove.addAll(Relevance.getInstance().getKeyWords());
                }

                // Read status
                clone = createClone(keywordsToAdd);
                clone.retainAll(ReadStatus.getInstance().getKeyWords());
                if (!clone.isEmpty()) {
                    keywordsToRemove.addAll(ReadStatus.getInstance().getKeyWords());
                }

                // Printed
                clone = createClone(keywordsToAdd);
                clone.retainAll(Printed.getInstance().getKeyWords());
                if (!clone.isEmpty()) {
                    keywordsToRemove.addAll(Printed.getInstance().getKeyWords());
                }
            }
        }

        BibtexEntry[] entries = bp.getSelectedEntries();
        NamedCompound ce = new NamedCompound(Localization.lang("Update keywords"));
        for (BibtexEntry entry : entries) {
            ArrayList<String> separatedKeywords = Util.getSeparatedKeywords(entry);

            // we "intercept" with a treeset
            // pro: no duplicates
            // possible con: alphabetical sorting of the keywords
            TreeSet<String> keywords = new TreeSet<String>();
            keywords.addAll(separatedKeywords);

            // update keywords
            keywords.removeAll(keywordsToRemove);
            keywords.addAll(keywordsToAdd);

            // put keywords back
            separatedKeywords.clear();
            separatedKeywords.addAll(keywords);
            Util.putKeywords(entry, separatedKeywords, ce);

            if (SpecialFieldsUtils.keywordSyncEnabled()) {
                SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ce);
            }
        }
        ce.end();
        bp.undoManager.addEdit(ce);
        bp.markBaseChanged();
    }

    @SuppressWarnings("unchecked")
    private HashSet<String> createClone(HashSet<String> keywordsToAdd) {
        return (HashSet<String>) keywordsToAdd.clone();
    }

    private void fillKeyWordList() {
        BasePanel bp = frame.basePanel();
        BibtexEntry[] entries = bp.getSelectedEntries();

        // fill dialog with values
        keywordListModel.clear();
        sortedKeywordsOfAllEntriesBeforeUpdateByUser.clear();

        if (mergeKeywords.isSelected()) {
            for (BibtexEntry entry : entries) {
                ArrayList<String> separatedKeywords = Util.getSeparatedKeywords(entry);
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);
            }
        } else {
            assert intersectKeywords.isSelected();

            // all keywords from first entry have to be added
            BibtexEntry firstEntry = entries[0];
            ArrayList<String> separatedKeywords = Util.getSeparatedKeywords(firstEntry);
            sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);

            // for the remaining entries, intersection has to be used
            // this approach ensures that one empty keyword list leads to an empty set of common keywords
            for (int i = 1; i < entries.length; i++) {
                BibtexEntry entry = entries[i];
                separatedKeywords = Util.getSeparatedKeywords(entry);
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.retainAll(separatedKeywords);
            }
        }
        for (String s : sortedKeywordsOfAllEntriesBeforeUpdateByUser) {
            keywordListModel.addElement(s);
        }
    }

}
