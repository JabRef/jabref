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

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.util.GUIGlobals;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.SearchRules;
import net.sf.jabref.logic.search.matchers.SearchMatcher;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

public class SearchManager extends SidePaneComponent
        implements ActionListener, KeyListener, ItemListener, CaretListener {

    private final JabRefFrame frame;

    private final IncrementalSearcher incSearcher;
    private SearchResultsDialog searchDialog;

    private AutoCompleteListener autoCompleteListener;

    /**
     * subscribed Objects
     */
    private final Vector<SearchTextListener> listeners = new Vector<>();

    //private JabRefFrame frame;
    private final JTextField searchField = new JTextField("", 12);
    private final JPopupMenu settings = new JPopupMenu();
    private final JButton openset = new JButton(Localization.lang("Settings"));
    private final JButton escape = new JButton(Localization.lang("Clear"));
    /**
     * This button's text will be set later.
     */
    private final JButton search = new JButton();
    private final JCheckBoxMenuItem searchReq;
    private final JCheckBoxMenuItem searchOpt;
    private final JCheckBoxMenuItem searchGen;
    private final JCheckBoxMenuItem searchAll;
    private final JCheckBoxMenuItem caseSensitive;
    private final JCheckBoxMenuItem regExpSearch;
    private final JCheckBoxMenuItem highLightWords;
    private final JCheckBoxMenuItem searchAutoComplete;

    private final JRadioButton increment;
    private final JRadioButton floatSearch;
    private final JRadioButton hideSearch;
    private final JRadioButton showResultsInDialog;
    private final JRadioButton searchAllBases;
    private final JCheckBoxMenuItem select;
    private boolean incSearch;
    private boolean startedFloatSearch;
    private boolean startedFilterSearch;

    private int incSearchPos = -1; // To keep track of where we are in

    // an incremental search. -1 means
    // that the search is inactive.

    public SearchManager(JabRefFrame frame, SidePaneManager manager) {
        super(manager, IconTheme.JabRefIcon.SEARCH.getIcon(), Localization.lang("Search"));

        this.frame = frame;
        incSearcher = new IncrementalSearcher(Globals.prefs);

        //setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.magenta));

        searchReq = new JCheckBoxMenuItem
                (Localization.lang("Search required fields"),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REQ));
        searchOpt = new JCheckBoxMenuItem
                (Localization.lang("Search optional fields"),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_OPT));
        searchGen = new JCheckBoxMenuItem
                (Localization.lang("Search general fields"),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_GEN));
        searchAll = new JCheckBoxMenuItem
                (Localization.lang("Search all fields"),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_ALL));
        regExpSearch = new JCheckBoxMenuItem
                (Localization.lang("Use regular expressions"),
                        Globals.prefs.getBoolean(JabRefPreferences.REG_EXP_SEARCH));

        increment = new JRadioButton(Localization.lang("Incremental"), false);
        floatSearch = new JRadioButton(Localization.lang("Float"), true);
        hideSearch = new JRadioButton(Localization.lang("Filter"), true);
        showResultsInDialog = new JRadioButton(Localization.lang("Show results in dialog"), true);
        searchAllBases = new JRadioButton(Localization.lang("Global search"),
                Globals.prefs.getBoolean(JabRefPreferences.SEARCH_ALL_BASES));
        ButtonGroup types = new ButtonGroup();
        types.add(increment);
        types.add(floatSearch);
        types.add(hideSearch);
        types.add(showResultsInDialog);
        types.add(searchAllBases);

        select = new JCheckBoxMenuItem(Localization.lang("Select matches"), false);
        increment.setToolTipText(Localization.lang("Incremental search"));
        floatSearch.setToolTipText(Localization.lang("Gray out non-matching entries"));
        hideSearch.setToolTipText(Localization.lang("Hide non-matching entries"));
        showResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));

        // Add an item listener that makes sure we only listen for key events
        // when incremental search is turned on.
        increment.addItemListener(this);
        floatSearch.addItemListener(this);
        hideSearch.addItemListener(this);
        showResultsInDialog.addItemListener(this);
        // Add the global focus listener, so a menu item can see if this field was focused when
        // an action was called.
        searchField.addFocusListener(Globals.focusListener);

        if (searchAll.isSelected()) {
            searchReq.setEnabled(false);
            searchOpt.setEnabled(false);
            searchGen.setEnabled(false);
        }
        searchAll.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                boolean state = !searchAll.isSelected();
                searchReq.setEnabled(state);
                searchOpt.setEnabled(state);
                searchGen.setEnabled(state);
            }
        });

        caseSensitive = new JCheckBoxMenuItem(Localization.lang("Case sensitive"),
                Globals.prefs.getBoolean(JabRefPreferences.CASE_SENSITIVE_SEARCH));

        highLightWords = new JCheckBoxMenuItem(Localization.lang("Highlight Words"),
                Globals.prefs.getBoolean(JabRefPreferences.HIGH_LIGHT_WORDS));

        searchAutoComplete = new JCheckBoxMenuItem(Localization.lang("Autocomplete names"),
                Globals.prefs.getBoolean(JabRefPreferences.SEARCH_AUTO_COMPLETE));
        settings.add(select);

        // 2005.03.29, trying to remove field category searches, to simplify
        // search usability.
        //settings.addSeparator();
        //settings.add(searchReq);
        //settings.add(searchOpt);
        //settings.add(searchGen);
        //settings.addSeparator();
        //settings.add(searchAll);
        // ---------------------------------------------------------------
        settings.addSeparator();
        settings.add(caseSensitive);
        settings.add(regExpSearch);
        settings.addSeparator();
        settings.add(highLightWords);
        settings.addSeparator();
        settings.add(searchAutoComplete);

        searchField.addActionListener(this);
        searchField.addCaretListener(this);
        search.addActionListener(this);
        searchField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                if (increment.isSelected()) {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                incSearch = false;
                incSearchPos = -1; // Reset incremental
                // search. This makes the
                // incremental search reset
                // once the user moves focus to
                // somewhere else.
                if (increment.isSelected()) {
                    //searchField.setText("");
                    //System.out.println("focuslistener");
                }
            }
        });
        escape.addActionListener(this);
        escape.setEnabled(false); // enabled after searching

        openset.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (settings.isVisible()) {
                    //System.out.println("oee");
                    //settings.setVisible(false);
                } else {
                    JButton src = (JButton) e.getSource();
                    settings.show(src, 0, openset.getHeight());
                }
            }
        });

        searchAutoComplete.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean(JabRefPreferences.SEARCH_AUTO_COMPLETE, searchAutoComplete.isSelected());
                if (SearchManager.this.frame.basePanel() != null) {
                    SearchManager.this.frame.basePanel().updateSearchManager();
                }

            }
        });
        Insets margin = new Insets(0, 2, 0, 2);
        //search.setMargin(margin);
        escape.setMargin(margin);
        openset.setMargin(margin);
        JButton help = new HelpAction(GUIGlobals.helpDiag, GUIGlobals.searchHelp).getIconButton();
        help.setMargin(margin);

        // Select the last used mode of search:
        if (Globals.prefs.getBoolean(JabRefPreferences.INCREMENT_S)) {
            increment.setSelected(true);
        } else if (Globals.prefs.getBoolean(JabRefPreferences.FLOAT_SEARCH)) {
            floatSearch.setSelected(true);
        } else if (Globals.prefs.getBoolean(JabRefPreferences.SHOW_SEARCH_IN_DIALOG)) {
            showResultsInDialog.setSelected(true);
        } else if (Globals.prefs.getBoolean(JabRefPreferences.SEARCH_ALL_BASES)) {
            searchAllBases.setSelected(true);
        } else {
            hideSearch.setSelected(true);
        }

        JPanel main = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        main.setLayout(gbl);
        GridBagConstraints con = new GridBagConstraints();
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;

        gbl.setConstraints(searchField, con);
        main.add(searchField);
        //con.gridwidth = 1;
        gbl.setConstraints(search, con);
        main.add(search);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(escape, con);
        main.add(escape);
        con.insets = new Insets(0, 2, 0, 0);
        gbl.setConstraints(increment, con);
        main.add(increment);
        gbl.setConstraints(floatSearch, con);
        main.add(floatSearch);
        gbl.setConstraints(hideSearch, con);
        main.add(hideSearch);
        gbl.setConstraints(showResultsInDialog, con);
        main.add(showResultsInDialog);
        gbl.setConstraints(searchAllBases, con);
        main.add(searchAllBases);
        con.insets = new Insets(0, 0, 0, 0);
        JPanel pan = new JPanel();
        GridBagLayout gb = new GridBagLayout();
        gbl.setConstraints(pan, con);
        pan.setLayout(gb);
        con.weightx = 1;
        con.gridwidth = 1;
        gb.setConstraints(openset, con);
        pan.add(openset);
        con.weightx = 0;
        gb.setConstraints(help, con);
        pan.add(help);
        main.add(pan);
        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        setContentContainer(main);

        searchField.getInputMap().put(Globals.prefs.getKey(KeyBinds.REPEAT_INCREMENTAL_SEARCH),
                "repeat");

        searchField.getActionMap().put("repeat", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (increment.isSelected()) {
                    repeatIncremental();
                }
            }
        });
        searchField.getInputMap().put(Globals.prefs.getKey(KeyBinds.CLEAR_SEARCH), "escape");
        searchField.getActionMap().put("escape", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hideAway();
                //SearchManager.this.actionPerformed(new ActionEvent(escape, 0, ""));
            }
        });
        setSearchButtonSizes();
        updateSearchButtonText();
    }

    public void setAutoCompleteListener(AutoCompleteListener listener) {
        this.autoCompleteListener = listener;
        updateKeyListeners();
    }

    /**
     * Add the correct key listeners to the search text field, depending on whether
     * and autocomplete listener has been set and whether incremental search
     * is selected.
     */
    private void updateKeyListeners() {
        KeyListener[] tmpListeners = searchField.getKeyListeners();
        for (KeyListener listener : tmpListeners) {
            searchField.removeKeyListener(listener);
        }
        if (increment.isSelected()) {
            searchField.addKeyListener(this);
        } else {
            if (searchAutoComplete.isSelected() && (autoCompleteListener != null)) {
                searchField.addKeyListener(autoCompleteListener);
            }
        }
    }

    /**
     * Subscribe to the SearchListener and receive events, if the user searches for some thing. You
     * will receive a list of words
     *
     * @param l
     */
    public void addSearchListener(SearchTextListener l) {
        if (listeners.contains(l)) {
            return;
        } else {
            listeners.add(l);
        }
        //fire event for the new subscriber
        l.searchText(getSearchwords(searchField.getText()));
    }

    /**
     * Remove object from the SearchListener
     *
     * @param l
     */
    public void removeSearchListener(SearchTextListener l) {
        listeners.remove(l);
    }

    /**
     * parse the search string for valid words and return a list of words
     * Like "The great Vikinger" will be ["The","great","Vikinger"]
     *
     * @param t
     * @return
     */
    private static ArrayList<String> getSearchwords(String t) {
        // for now ... just seperate words by whitespace
        String[] strings = t.split(" ");
        ArrayList<String> words = new ArrayList<>(strings.length);
        Collections.addAll(words, strings);
        return words;
    }

    /**
     * Fires an event if a search was started / canceled
     *
     * @param t
     */
    private void fireSearchlistenerEvent(String t) {
        // parse the Search string to words
        ArrayList<String> words;
        if ((t == null) || t.isEmpty()) {
            words = null;
        } else {
            words = getSearchwords(t);
        }

        //fire an event for every listener
        for (SearchTextListener s : listeners) {
            s.searchText(words);
        }
    }

    /**
     * force the search button to be large enough for
     * the longer of the two texts
     */
    private void setSearchButtonSizes() {
        search.setText(Localization.lang("Search specified field(s)"));
        Dimension size1 = search.getPreferredSize();
        search.setText(Localization.lang("Search all fields"));
        Dimension size2 = search.getPreferredSize();
        size2.width = Math.max(size1.width, size2.width);
        search.setMinimumSize(size2);
        search.setPreferredSize(size2);
    }

    /**
     * Instantiate the search dialog, unless it has already been instantiated:
     */
    private void instantiateSearchDialog() {
        if (searchDialog == null) {
            searchDialog = new SearchResultsDialog(frame, Localization.lang("Search results"));
        }
    }

    public void updatePrefs() {
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_REQ, searchReq.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_OPT, searchOpt.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_GEN, searchGen.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_ALL, searchAll.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.INCREMENT_S, increment.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SELECT_S, select.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.FLOAT_SEARCH, floatSearch.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.CASE_SENSITIVE_SEARCH,
                caseSensitive.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.REG_EXP_SEARCH, regExpSearch.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.HIGH_LIGHT_WORDS, highLightWords.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SHOW_SEARCH_IN_DIALOG, showResultsInDialog.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_ALL_BASES, searchAllBases.isSelected());
    }

    public void startIncrementalSearch() {
        increment.setSelected(true);
        searchField.setText("");
        //System.out.println("startIncrementalSearch");
        searchField.requestFocus();
    }

    /**
     * Clears and focuses the search field if it is not
     * focused. Otherwise, cycles to the next search type.
     */
    public void startSearch() {
        if (increment.isSelected() && incSearch) {
            repeatIncremental();
            return;
        }
        if (!searchField.hasFocus()) {
            //searchField.setText("");
            searchField.selectAll();
            searchField.requestFocus();
        } else {
            if (increment.isSelected()) {
                floatSearch.setSelected(true);
            } else if (floatSearch.isSelected()) {
                hideSearch.setSelected(true);
            } else if (hideSearch.isSelected()) {
                showResultsInDialog.setSelected(true);
            } else if (showResultsInDialog.isSelected()) {
                searchAllBases.setSelected(true);
            } else {
                increment.setSelected(true);
            }
            increment.revalidate();
            increment.repaint();

            searchField.requestFocus();

        }
    }

    private void clearSearchLater() {
        if (panel == null) {
            return;
        }

        Runnable t = new Runnable() {

            @Override
            public void run() {
                clearSearch();
            }
        };
        // do this after the button action is over
        SwingUtilities.invokeLater(t);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == escape) {
            incSearch = false;
            clearSearchLater();
        } else if (((e.getSource() == searchField) || (e.getSource() == search))
                && !increment.isSelected()
                && (panel != null)) {

            updatePrefs(); // Make sure the user's choices are recorded.
            if (searchField.getText().isEmpty()) {
                // An empty search field should cause the search to be cleared.
                clearSearchLater();
                return;
            }

            fireSearchlistenerEvent(searchField.getText());

            // Setup search parameters common to both normal and float.
            SearchRule searchRule = SearchRules.getSearchRuleByQuery(searchField.getText(),
                    Globals.prefs.getBoolean(JabRefPreferences.CASE_SENSITIVE_SEARCH),
                    Globals.prefs.getBoolean(JabRefPreferences.REG_EXP_SEARCH));

            if (!searchRule.validateSearchStrings(searchField.getText())) {
                panel.output(Localization.lang("Search failed: illegal search expression"));
                panel.stopShowingSearchResults();
                return;
            }
            SearchWorker worker = new SearchWorker(searchRule, searchField.getText());
            worker.getWorker().run();
            worker.getCallBack().update();
            escape.setEnabled(true);

            frame.basePanel().mainTable.setSelected(0);
        }
    }

    class SearchWorker extends AbstractWorker {

        private final SearchRule rule;
        private final String searchTerm;
        int hits;

        public SearchWorker(SearchRule rule, String searchTerm) {
            this.rule = rule;
            this.searchTerm = searchTerm;
        }

        @Override
        public void run() {
            if (!searchAllBases.isSelected()) {
                // Search only the current database:
                for (BibtexEntry entry : panel.getDatabase().getEntries()) {

                    boolean hit = rule.applyRule(searchTerm, entry);
                    entry.setSearchHit(hit);
                    if (hit) {
                        hits++;
                    }
                }
            } else {
                // Search all databases:
                for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
                    BasePanel p = frame.baseAt(i);
                    for (BibtexEntry entry : p.getDatabase().getEntries()) {

                        boolean hit = rule.applyRule(searchTerm, entry);
                        entry.setSearchHit(hit);
                        if (hit) {
                            hits++;
                        }
                    }
                }
            }
        }

        @Override
        public void update() {
            panel.output(Localization.lang("Searched database. Number of hits")
                    + ": " + hits);

            // Show the result in the chosen way:
            if (searchAllBases.isSelected()) {
                // Search all databases. This means we need to use the search results dialog.
                // Turn off other search mode, if activated:
                if (startedFloatSearch) {
                    panel.mainTable.stopShowingFloatSearch();
                    startedFloatSearch = false;
                }
                if (startedFilterSearch) {
                    panel.stopShowingSearchResults();
                    startedFilterSearch = false;
                }
                // Make sure the search dialog is instantiated and cleared:
                instantiateSearchDialog();
                searchDialog.clear();
                for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
                    BasePanel p = frame.baseAt(i);
                    for (BibtexEntry entry : p.getDatabase().getEntries()) {
                        if (entry.isSearchHit()) {
                            searchDialog.addEntry(entry, p);
                        }
                    }
                }
                searchDialog.selectFirstEntry();
                searchDialog.setVisible(true);
            } else if (showResultsInDialog.isSelected()) {
                // Turn off other search mode, if activated:
                if (startedFloatSearch) {
                    panel.mainTable.stopShowingFloatSearch();
                    startedFloatSearch = false;
                }
                if (startedFilterSearch) {
                    panel.stopShowingSearchResults();
                    startedFilterSearch = false;
                }
                // Make sure the search dialog is instantiated and cleared:
                instantiateSearchDialog();
                searchDialog.clear();
                for (BibtexEntry entry : panel.getDatabase().getEntries()) {
                    if (entry.isSearchHit()) {
                        searchDialog.addEntry(entry, panel);
                    }
                }
                searchDialog.selectFirstEntry();
                searchDialog.setVisible(true);
            } else if (hideSearch.isSelected()) {
                // Filtering search - removes non-hits from the table:
                if (startedFloatSearch) {
                    panel.mainTable.stopShowingFloatSearch();
                    startedFloatSearch = false;
                }
                startedFilterSearch = true;
                panel.setSearchMatcher(new SearchMatcher());

            } else {
                // Float search - floats hits to the top of the table:
                if (startedFilterSearch) {
                    panel.stopShowingSearchResults();
                    startedFilterSearch = false;
                }
                startedFloatSearch = true;
                panel.mainTable.showFloatSearch(new SearchMatcher());

            }

            // Afterwards, select all text in the search field.
            searchField.select(0, searchField.getText().length());

        }
    }

    private void clearSearch() {

        if (panel.isShowingFloatSearch()) {
            startedFloatSearch = false;
            panel.mainTable.stopShowingFloatSearch();
        } else if (panel.isShowingFilterSearch()) {
            startedFilterSearch = false;
            panel.stopShowingSearchResults();
        }

        // clear search means that nothing is searched for
        // even if a word is written in the text field,
        // nothing should be highlighted
        fireSearchlistenerEvent(null);

        // disable "Cancel" button to signal this to the user
        escape.setEnabled(false);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == increment) {
            if (startedFilterSearch || startedFloatSearch) {
                clearSearch();
            }
            updateSearchButtonText();

            // Make sure the correct key listener is activated:
            updateKeyListeners();

        } else /*if (e.getSource() == normal)*/ {
            updateSearchButtonText();

            // If this search type is disabled, remove reordering from
            // all databases.
            /*if ((panel != null) && increment.isSelected()) {
                clearSearch();
            } */
        }
    }

    private void repeatIncremental() {
        incSearchPos++;
        if (panel != null) {
            goIncremental();
        }
    }

    /**
     * Used for incremental search. Only activated when incremental
     * is selected.
     * <p/>
     * The variable incSearchPos keeps track of which entry was last
     * checked.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        if (e.isControlDown()) {
            return;
        }
        if (panel != null) {
            goIncremental();
        }
    }

    private void goIncremental() {
        incSearch = true;
        escape.setEnabled(true);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String text = searchField.getText();

                if (incSearchPos >= panel.getDatabase().getEntryCount()) {
                    panel.output('\'' + text + "' : " +
                            Localization.lang("Incremental search failed. Repeat to search from top.") + '.');
                    incSearchPos = -1;
                    return;
                }

                if (searchField.getText().isEmpty()) {
                    return;
                }
                if (incSearchPos < 0) {
                    incSearchPos = 0;
                }
                BibtexEntry be = panel.mainTable.getEntryAt(incSearchPos);
                while (!incSearcher.search(text, be)) {
                    incSearchPos++;
                    if (incSearchPos < panel.getDatabase().getEntryCount()) {
                        be = panel.mainTable.getEntryAt(incSearchPos);
                    } else {
                        panel.output('\'' + text + "' : " +
                                Localization.lang("Incremental search failed. Repeat to search from top."));
                        incSearchPos = -1;
                        return;
                    }
                }
                if (incSearchPos >= 0) {

                    panel.selectSingleEntry(incSearchPos);
                    panel.output('\'' + text + "' " +
                            Localization.lang("found") + '.');

                }
            }
        });
    }

    @Override
    public void componentClosing() {
        frame.searchToggle.setSelected(false);
        if (panel != null) {
            if (startedFilterSearch || startedFloatSearch) {
                clearSearch();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Nothing
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Nothing
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        if (e.getSource() == searchField) {
            updateSearchButtonText();
        }
    }

    /**
     * Updates the text on the search button to reflect
     * the type of search that will happen on click.
     */
    private void updateSearchButtonText() {
        // @formatter:off
        search.setText(isSpecificSearch() ? Localization.lang("Search specified field(s)") :
            Localization.lang("Search all fields"));
        // @formatter:on
    }

    private boolean isSpecificSearch() {
        return !increment.isSelected() && GrammarBasedSearchRule.isValid(caseSensitive.isSelected(), regExpSearch.isSelected(), searchField.getText());
    }

    @Override
    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
        if (panel != null) {
            escape.setEnabled(panel.isShowingFloatSearch()
                    || panel.isShowingFilterSearch());
        } else {
            escape.setEnabled(false);
        }
    }
}
