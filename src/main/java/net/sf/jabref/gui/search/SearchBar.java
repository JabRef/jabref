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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
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
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.SearchRules;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.logic.search.rules.util.SentenceAnalyzer;

import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.LogFactory;
import org.gpl.JSplitButton.JSplitButton;
import org.gpl.JSplitButton.action.SplitButtonActionListener;

/**
 * The search bar at the top of the screen allowing the user to search his database.
 */
public class SearchBar extends JPanel {

    private SearchQuery getSearchQuery() {
        return new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
    }

    public void updateResults(int matched, String description) {
        if(matched == 0) {
            this.currentResults.setText(Localization.lang("No results found."));
        } else {
            this.currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
        }
        this.searchField.setToolTipText("<html>" + description + "</html>");
    }

    private final JabRefFrame frame;

    private JSearchTextField searchField;
    private JSplitButton searchButton;

    private JRadioButtonMenuItem modeFloat, modeLiveFilter, modeGlobal;

    private JMenu settings;
    private JCheckBoxMenuItem highlightWords, autoComplete;

    private final JToggleButton caseSensitive;
    private final JToggleButton regularExp;

    private final JButton openCurrentResultsInDialog;

    private final JLabel currentResults = new JLabel("");

    AutoCompleteSupport<String> autoCompleteSupport;

    SearchWorker worker;

    private final ArrayList<SearchTextListener> listeners = new ArrayList<>();


    /**
     * Initializes the search bar.
     *
     * @param frame the main window
     */
    public SearchBar(JabRefFrame frame) {
        super();

        this.frame = frame;
        worker = new SearchWorker(frame);

        currentResults.setFont(currentResults.getFont().deriveFont(Font.BOLD));

        caseSensitive = new JToggleButton(IconTheme.JabRefIcon.CASE_SENSITIVE.getSmallIcon(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE));
        caseSensitive.setToolTipText(Localization.lang("Case sensitive"));
        caseSensitive.addActionListener(ae -> updatePrefs());
        caseSensitive.addActionListener(ae -> performSearch());
        caseSensitive.addChangeListener(c -> performSearch());
        regularExp = new JToggleButton(IconTheme.JabRefIcon.REGEX.getSmallIcon(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
        regularExp.setToolTipText(Localization.lang("Use regular expressions"));
        regularExp.addActionListener(ae -> updatePrefs());
        regularExp.addActionListener(ae -> performSearch());
        regularExp.addChangeListener(c -> performSearch());

        openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());
        openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        openCurrentResultsInDialog.addActionListener(ae -> {
            SearchResultsDialog searchDialog = new SearchResultsDialog(frame, Localization.lang("Search results in database %0 for %1",
                    frame.basePanel().getFile().getName(), this.getSearchQuery().toString()));
            for (BibtexEntry entry : frame.basePanel().getDatabase().getEntries()) {
                if (entry.isSearchHit()) {
                    searchDialog.addEntry(entry, frame.basePanel());
                }
            }
            searchDialog.selectFirstEntry();
            searchDialog.setVisible(true);
        });

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

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(regularExp);
        toolBar.add(caseSensitive);
        toolBar.addSeparator();
        toolBar.add(openCurrentResultsInDialog);
        toolBar.addSeparator();
        toolBar.add(new HelpAction(frame.helpDiag, GUIGlobals.searchHelp, Localization.lang("Help")));
        this.add(toolBar);
        this.add(currentResults);
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



        searchButton.setPopupMenu(createPopupMenu());

        updateSearchButtonText();
    }

    private JPopupMenu createPopupMenu() {
        // Populate popup menu and add it to search button
        JPopupMenu menu = new JPopupMenu("");

        initSearchModeMenu();
        menu.add(getSearchModeMenuItem(SearchMode.FILTER));
        menu.add(getSearchModeMenuItem(SearchMode.FLOAT));
        menu.addSeparator();
        menu.add(getSearchModeMenuItem(SearchMode.GLOBAL));
        menu.addSeparator();
        highlightWords = new JCheckBoxMenuItem(Localization.lang("Highlight Words"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_HIGHLIGHT_WORDS));
        highlightWords.addActionListener(ae -> updatePrefs());
        autoComplete = new JCheckBoxMenuItem(Localization.lang("Autocomplete names"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_AUTO_COMPLETE));
        autoComplete.addActionListener(ae -> updatePrefs());
        menu.add(highlightWords);
        menu.add(autoComplete);
        return menu;
    }

    /**
     * Initializes the popup menu items controlling the search mode
     */
    private void initSearchModeMenu() {
        ButtonGroup searchMethod = new ButtonGroup();
        for (SearchMode mode : SearchMode.values()) {
            // Create menu items
            switch (mode) {
            case FLOAT:
                modeFloat = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_FLOAT));
                break;
            case FILTER:
                modeLiveFilter = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_LIVE_FILTER));
                break;
            case GLOBAL:
                modeGlobal = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_GLOBAL));
                break;
            }

            // Set tooltips on menu items
            getSearchModeMenuItem(mode).setToolTipText(mode.getToolTipText());

            // Add menu item to group
            searchMethod.add(getSearchModeMenuItem(mode));

            // Listen to selection changed events
            getSearchModeMenuItem(mode).addChangeListener(e -> updateSearchButtonText());
            getSearchModeMenuItem(mode).addChangeListener(e -> performSearch());
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
        autoCompleteSupport = new AutoCompleteSupport<>(searchField);
        autoCompleteSupport.install();

        // Add the global focus listener, so a menu item can see if this field was focused when an action was called.
        searchField.addFocusListener(Globals.focusListener);

        // Search if user press enter
        searchField.addActionListener(e -> performSearch());

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
        case FLOAT:
            return modeFloat;
        case GLOBAL:
            return modeGlobal;
        case FILTER:
            return modeLiveFilter;
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
        if (modeFloat.isSelected()) {
            return SearchMode.FLOAT;
        }
        if (modeLiveFilter.isSelected()) {
            return SearchMode.FILTER;
        }
        if (modeGlobal.isSelected()) {
            return SearchMode.GLOBAL;
        }

        return SearchMode.FILTER;
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
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_FLOAT, modeFloat.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_LIVE_FILTER, modeLiveFilter.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_GLOBAL, modeGlobal.isSelected());

        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE, caseSensitive.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_REG_EXP, regularExp.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_HIGHLIGHT_WORDS, highlightWords.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_AUTO_COMPLETE, autoComplete.isSelected());
    }

    /**
     * Focuses the search field if it is not focused. Otherwise, cycles to the next search type.
     */
    public void focus() {
        if (searchField.hasFocus()) {

            switch (getSearchMode()) {
            case FLOAT:
                setSearchMode(SearchMode.FILTER);
                break;
            case FILTER:
                setSearchMode(SearchMode.GLOBAL);
                break;
            case GLOBAL:
                setSearchMode(SearchMode.FLOAT);
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
        if ((getSearchMode() == SearchMode.FILTER) || (getSearchMode() == SearchMode.FLOAT)) {
            // wait until the text is changed
            SwingUtilities.invokeLater(this::performSearch);
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

            this.currentResults.setText("");
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



        worker.initSearch(getSearchQuery(), getSearchMode());
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
