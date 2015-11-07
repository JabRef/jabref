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
package net.sf.jabref.gui.search;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.autocompleter.AutoCompleteSupport;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.SearchRules;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.logic.search.rules.util.SentenceAnalyzer;

import org.apache.commons.logging.LogFactory;
import org.gpl.JSplitButton.JSplitButton;
import org.gpl.JSplitButton.action.SplitButtonActionListener;

/**
 * The search bar at the top of the screen allowing the user to search his database.
 */
public class SearchBar extends JPanel {

    private final JabRefFrame frame;

    private JSearchTextField searchField;
    private JSplitButton searchButton;
    private JPopupMenu popupMenu;

    private JMenuItem clearSearch;
    private JRadioButtonMenuItem modeIncremental, modeFloat, modeFilter, modeLiveFilter, modeResultsInDialog,
            modeGlobal;

    private JMenu settings;
    private JCheckBoxMenuItem selectMatches, caseSensitive, regularExp, highlightWords, autoComplete;

    AutoCompleteSupport<String> autoCompleteSupport;

    SearchWorker worker;

    private final ArrayList<SearchTextListener> listeners = new ArrayList<SearchTextListener>();


    /**
     * Initializes the search bar.
     *
     * @param frame the main window
     */
    public SearchBar(JabRefFrame frame) {
        super();

        this.frame = frame;
        worker = new SearchWorker(frame);

        // Init controls
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;

        initSearchField();
        c.gridx = 0;
        c.gridy = 0;
        this.add(searchField, c);
        initSearchButton();
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        this.add(searchButton, c);
    }

    /**
     * Initializes the search button and its popup menu
     */
    private void initSearchButton() {
        // Create search button
        searchButton = new JSplitButton(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
        searchButton.setMinimumSize(new Dimension(50, 25));
        searchButton.setBackground(searchField.getBackground());
        searchButton.setContentAreaFilled(false);
        searchButton.setOpaque(true);
        searchButton.addSplitButtonActionListener(new SplitButtonActionListener() {

            @Override
            public void buttonClicked(ActionEvent e) {
                performSearch();
            }

            @Override
            public void splitButtonClicked(ActionEvent e) {
            }
        });

        // Populate popup menu and add it to search button
        popupMenu = new JPopupMenu("");

        clearSearch = new JMenuItem(Localization.lang("Clear search"));
        clearSearch.addActionListener(e -> clearSearch());
        popupMenu.add(clearSearch);
        popupMenu.addSeparator();

        initSearchModeMenu();
        for (SearchMode mode : SearchMode.values()) {
            popupMenu.add(getSearchModeMenuItem(mode));
        }
        popupMenu.addSeparator();

        initSearchSettingsMenu();
        popupMenu.add(settings);

        JMenuItem help = new JMenuItem(Localization.lang("Help"), IconTheme.JabRefIcon.HELP.getSmallIcon());
        help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.searchHelp, Localization.lang("Help")));
        popupMenu.add(help);

        searchButton.setPopupMenu(popupMenu);

        updateSearchButtonText();
    }

    /**
     * Initializes the popup menu items for controlling search settings
     */
    private void initSearchSettingsMenu() {
        // Create menu items
        settings = new JMenu(Localization.lang("Settings"));
        selectMatches = new JCheckBoxMenuItem(Localization.lang("Select matches"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_SELECT_MATCHES));
        selectMatches.addActionListener(ae -> updatePrefs());
        caseSensitive = new JCheckBoxMenuItem(Localization.lang("Case sensitive"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE));
        caseSensitive.addActionListener(ae -> updatePrefs());
        regularExp = new JCheckBoxMenuItem(Localization.lang("Use regular expressions"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
        regularExp.addActionListener(ae -> updatePrefs());
        highlightWords = new JCheckBoxMenuItem(Localization.lang("Highlight Words"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_HIGHLIGHT_WORDS));
        highlightWords.addActionListener(ae -> updatePrefs());
        autoComplete = new JCheckBoxMenuItem(Localization.lang("Autocomplete names"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_AUTO_COMPLETE));
        autoComplete.addActionListener(ae -> updatePrefs());

        // Add them to the menu
        settings.add(selectMatches);
        settings.addSeparator();
        settings.add(caseSensitive);
        settings.add(regularExp);
        settings.addSeparator();
        settings.add(highlightWords);
        settings.addSeparator();
        settings.add(autoComplete);
    }

    /**
     * Initializes the popup menu items controlling the search mode
     */
    private void initSearchModeMenu() {
        ButtonGroup searchMethod = new ButtonGroup();
        for (SearchMode mode : SearchMode.values()) {
            // Create menu items
            switch (mode) {
            case Incremental:
                modeIncremental = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_INCREMENTAL));
                break;
            case Float:
                modeFloat = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_FLOAT));
                break;
            case Filter:
                modeFilter = new JRadioButtonMenuItem(mode.getDisplayName(), true);
                break;
            case LiveFilter:
                modeLiveFilter = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_LIVE_FILTER));
                break;
            case ResultsInDialog:
                modeResultsInDialog = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_RESULTS_IN_DIALOG));
                break;
            case Global:
                modeGlobal = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_GLOBAL));
                break;
            }

            // Set tooltips on menu items
            getSearchModeMenuItem(mode).setToolTipText(mode.getToolTipText());

            // Add menu item to group
            searchMethod.add(getSearchModeMenuItem(mode));

            // Listen to selection changed events
            getSearchModeMenuItem(mode).addItemListener(e -> updateSearchButtonText());
        }
    }

    /**
     * Initializes the search text field
     */
    private void initSearchField() {
        searchField = new JSearchTextField();
        searchField.setTextWhenNotFocused(Localization.lang("Search..."));
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        // Add autocompleter
        autoCompleteSupport = new AutoCompleteSupport<String>(searchField);
        autoCompleteSupport.install();

        // Add the global focus listener, so a menu item can see if this field was focused when an action was called.
        searchField.addFocusListener(Globals.focusListener);

        // Search if user press enter
        searchField.addActionListener(e -> performSearch());

        // Restart incremental search if focus was lost
        searchField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getSearchMode() == SearchMode.Incremental) {
                    clearSearch();
                }
            }
        });

        // Register key binding to repeat the previous incremental search (i.e. jump to next match)
        searchField.getInputMap().put(Globals.prefs.getKey(KeyBinds.REPEAT_INCREMENTAL_SEARCH), "repeat");
        searchField.getActionMap().put("repeat", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (modeIncremental.isSelected()) {
                    performSearch();
                }
            }
        });

        // Subscribe to changes to the text in the search field in order to "live search"
        // TODO: With this implementation "onSearchTextChanged" gets called two times when setText() is invoked (once for removing the initial string and then again for inserting the new one). This happens for example when an autocompletion is accepted.
        searchField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                LogFactory.getLog(SearchBar.class).debug("Text insert: " + e.toString());
                onSearchTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                LogFactory.getLog(SearchBar.class).debug("Text remove: " + e.toString());
                onSearchTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                LogFactory.getLog(SearchBar.class).debug("Text updated: " + e.toString());
            }
        });
    }

    /**
     * Returns the item in the popup menu of the search button corresponding to the given search mode
     */
    private JRadioButtonMenuItem getSearchModeMenuItem(SearchMode mode) {
        switch (mode) {
        case Incremental:
            return modeIncremental;
        case Filter:
            return modeFilter;
        case Float:
            return modeFloat;
        case Global:
            return modeGlobal;
        case LiveFilter:
            return modeLiveFilter;
        case ResultsInDialog:
            return modeResultsInDialog;
        }
        return null;
    }

    /**
     * Switches to another search mode.
     *
     * @param mode the new search mode
     */
    private void setSearchMode(SearchMode mode) {
        getSearchModeMenuItem(mode).setSelected(true);
    }

    /**
     * Returns the currently activated search mode.
     *
     * @return current search mode
     */
    private SearchMode getSearchMode() {
        if (modeIncremental.isSelected()) {
            return SearchMode.Incremental;
        }
        if (modeFloat.isSelected()) {
            return SearchMode.Float;
        }
        if (modeFilter.isSelected()) {
            return SearchMode.Filter;
        }
        if (modeLiveFilter.isSelected()) {
            return SearchMode.LiveFilter;
        }
        if (modeResultsInDialog.isSelected()) {
            return SearchMode.ResultsInDialog;
        }
        if (modeGlobal.isSelected()) {
            return SearchMode.Global;
        }

        return SearchMode.Incremental;
    }

    /**
     * Adds a SearchTextListener to the search bar. The added listener is immediately informed about the current search.
     * Subscribers will be notified about searches.
     *
     * @param l SearchTextListener to be added
     */
    public void addSearchListener(SearchTextListener l) {
        if (listeners.contains(l)) {
            return;
        } else {
            listeners.add(l);
        }

        // fire event for the new subscriber
        l.searchText(getSearchwords(searchField.getText()));
    }

    /**
     * Remove a SearchTextListener
     *
     * @param l SearchTextListener to be removed
     */
    public void removeSearchListener(SearchTextListener l) {
        listeners.remove(l);
    }

    /**
     * Parses the search query for valid words and returns a list these words. For example, "The great Vikinger" will
     * give ["The","great","Vikinger"]
     *
     * @param searchText the search query
     * @return list of words found in the search query
     */
    private List<String> getSearchwords(String searchText) {
        return (new SentenceAnalyzer(searchText)).getWords();
    }

    /**
     * Fires an event if a search was started (or cleared)
     *
     * @param searchText the search query
     */
    private void fireSearchlistenerEvent(String searchText) {
        // Parse the search string to words
        List<String> words;
        if ((searchText == null) || (searchText.isEmpty())) {
            words = null;
        } else {
            words = getSearchwords(searchText);
        }

        // Fire an event for every listener
        for (SearchTextListener s : listeners) {
            s.searchText(words);
        }
    }

    /**
     * Save current settings.
     */
    public void updatePrefs() {
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_INCREMENTAL, modeIncremental.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_FLOAT, modeFloat.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_LIVE_FILTER, modeLiveFilter.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_RESULTS_IN_DIALOG, modeResultsInDialog.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_GLOBAL, modeGlobal.isSelected());

        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_SELECT_MATCHES, selectMatches.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE, caseSensitive.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_REG_EXP, regularExp.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_HIGHLIGHT_WORDS, highlightWords.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_AUTO_COMPLETE, autoComplete.isSelected());
    }

    /**
     * Switches search mode to incremental and sets the focus to the search field.
     */
    public void startIncrementalSearch() {
        setSearchMode(SearchMode.Incremental);
        searchField.requestFocus();
    }

    /**
     * Focuses the search field if it is not focused. Otherwise, cycles to the next search type.
     */
    public void focus() {
        if (searchField.hasFocus()) {

            switch (getSearchMode()) {
            case Incremental:
                setSearchMode(SearchMode.Float);
                break;
            case Float:
                setSearchMode(SearchMode.Filter);
                break;
            case Filter:
                setSearchMode(SearchMode.LiveFilter);
                break;
            case LiveFilter:
                setSearchMode(SearchMode.ResultsInDialog);
                break;
            case ResultsInDialog:
                setSearchMode(SearchMode.Global);
                break;
            case Global:
                setSearchMode(SearchMode.Incremental);
                break;

            }
        } else {
            searchField.requestFocus();
        }
    }

    /**
     * Reacts to the change of the search text. A change in the search query results in an immediate search in
     * incremental or live filter search mode.
     */
    private void onSearchTextChanged() {
        if ((getSearchMode() == SearchMode.Incremental) || (getSearchMode() == SearchMode.LiveFilter)) {
            // wait until the text is changed
            SwingUtilities.invokeLater(() -> performSearch());
        }
    }

    /**
     * Clears (asynchronously) the current search. This includes resetting the search text.
     */
    private void clearSearch() {
        SwingUtilities.invokeLater(() -> {
            worker.restart();

            searchField.setText("");

            fireSearchlistenerEvent(null);
        });
    }

    /**
     * Performs a new search based on the current search query.
     */
    private void performSearch() {
        String searchText = searchField.getText();

        // Notify others about the search
        fireSearchlistenerEvent(searchText);

        // An empty search field should cause the search to be cleared.
        if (searchText.isEmpty()) {
            clearSearch();
            return;
        }

        if (frame.basePanel() == null) {
            return;
        }

        // TODO: General observation: Should SearchRule not contain the search query? That is upon creation the search rule safes the searchText as a private field. Then also the other methods would act versus the saved query, i.e. validateSearchString() without argument. Or is there a way a search rule is created without a search text?
        // Search
        SearchRule searchRule = SearchRules.getSearchRuleByQuery(searchText, Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));

        if (!searchRule.validateSearchStrings(searchText)) {
            frame.basePanel().output(Localization.lang("Search failed: illegal search expression"));
            clearSearch();
            return;
        }

        worker.initSearch(searchRule, searchText, getSearchMode());
        // TODO: What is the purpose of implementing the AbstractWorker interface if we call the worker that stupidly?
        worker.getWorker().run();
        worker.getCallBack().update();
    }

    /**
     * Updates the text on the search button to reflect the type of search that will happen on click.
     */
    private void updateSearchButtonText() {
        if (GrammarBasedSearchRule.isValid(caseSensitive.isSelected(), regularExp.isSelected(), searchField.getText())) {
            searchButton.setToolTipText(Localization.lang("Search specified field(s)"));
        } else {
            searchButton.setToolTipText(Localization.lang("Search all fields"));
        }
    }

    /**
     * Sets the autocompleter used in the search field.
     * @param searchCompleter the autocompleter
     */
    public void setAutoCompleter(AutoCompleter<String> searchCompleter) {
        this.autoCompleteSupport.setAutoCompleter(searchCompleter);
    }
}
