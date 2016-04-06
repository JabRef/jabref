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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.WrapLayout;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.autocompleter.AutoCompleteSupport;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.gui.util.component.JTextFieldWithUnfocusedText;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.search.SearchQueryHighlightObservable;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The search bar at the top of the screen allowing the user to search his database.
 */
public class SearchBar extends JPanel {

    private static final Log LOGGER = LogFactory.getLog(SearchBar.class);

    private static final Color NO_RESULTS_COLOR = new Color(232, 202, 202);
    private static final Color RESULTS_FOUND_COLOR = new Color(217, 232, 202);
    private static final Color ADVANCED_SEARCH_COLOR = new Color(102, 255, 255);

    private final JButton openCurrentResultsInDialog;
    private final JButton globalSearch;
    private final JButton searchModeButton;

    private final BasePanel basePanel;

    private final SearchQueryHighlightObservable searchQueryHighlightObservable;
    private final JTextFieldWithUnfocusedText searchField;

    private SearchMode searchMode = getSearchModeFromSettings();

    private final JToggleButton caseSensitive;
    private final JToggleButton regularExp;

    private final JLabel currentResults = new JLabel("");

    private AutoCompleteSupport<String> autoCompleteSupport;
    private final JLabel searchIcon;

    /**
     * Initializes the search bar.
     *
     * @param basePanel the base panel
     */
    public SearchBar(BasePanel basePanel) {
        super();

        this.basePanel = Objects.requireNonNull(basePanel);
        this.searchQueryHighlightObservable = new SearchQueryHighlightObservable();

        currentResults.setFont(currentResults.getFont().deriveFont(Font.BOLD));

        caseSensitive = new JToggleButton(IconTheme.JabRefIcon.CASE_SENSITIVE.getSmallIcon(),Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE));
        caseSensitive.setToolTipText(Localization.lang("Case sensitive"));
        caseSensitive.addActionListener(e -> {
            performSearch();
            updatePreferences();
        });


        regularExp = new JToggleButton(IconTheme.JabRefIcon.REG_EX.getSmallIcon(), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
        regularExp.setToolTipText(Localization.lang("regular expression"));
        regularExp.addActionListener(e -> {
            performSearch();
            updatePreferences();
        });


        openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());
        openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        openCurrentResultsInDialog.addActionListener(ae -> {
            SearchResultsDialog searchDialog = new SearchResultsDialog(basePanel.frame(), Localization.lang("Search results in database %0 for %1",
                    basePanel.getBibDatabaseContext().getDatabaseFile().getName(), this.getSearchQuery().localize()));
            List<BibEntry> entries = basePanel.getDatabase().getEntries().stream().filter(BibEntry::isSearchHit).collect(Collectors.toList());
            searchDialog.addEntries(entries, basePanel);
            searchDialog.selectFirstEntry();
            searchDialog.setVisible(true);
        });
        openCurrentResultsInDialog.setEnabled(false);

        // Init controls
        setLayout(new WrapLayout(FlowLayout.LEFT));

        searchIcon = new JLabel(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
        this.add(searchIcon);
        this.searchField = initSearchField();
        if (OS.OS_X) {
            searchField.putClientProperty("JTextField.variant", "search");
        }
        this.add(searchField);

        JButton clearSearchButton = new JButton(IconTheme.JabRefIcon.CLOSE.getSmallIcon());
        clearSearchButton.setToolTipText(Localization.lang("Clear"));
        clearSearchButton.addActionListener(l -> endSearch());

        this.add(clearSearchButton);

        searchModeButton = new JButton();
        updateSearchModeButtonText();
        searchModeButton.addActionListener(l -> toggleSearchModeAndSearch());

        JToolBar toolBar = new OSXCompatibleToolbar();
        toolBar.setFloatable(false);
        toolBar.add(clearSearchButton);
        toolBar.addSeparator();
        toolBar.add(regularExp);
        toolBar.add(caseSensitive);
        toolBar.addSeparator();
        toolBar.add(searchModeButton);
        toolBar.addSeparator();
        toolBar.add(openCurrentResultsInDialog);
        globalSearch = new JButton(Localization.lang("Search globally"));
        globalSearch.setToolTipText(Localization.lang("Search in all open databases"));
        globalSearch.addActionListener(l -> {
            AbstractWorker worker = new GlobalSearchWorker(basePanel.frame(), getSearchQuery());
            worker.run();
            worker.update();
        });
        globalSearch.setEnabled(false);
        toolBar.add(globalSearch);
        toolBar.addSeparator();
        toolBar.add(new HelpAction(HelpFiles.searchHelp));

        this.add(toolBar);
        this.add(currentResults);

        paintBackgroundWhite(this);
    }

    private void paintBackgroundWhite(Container container) {
        container.setBackground(Color.WHITE);
        for (Component component : container.getComponents()) {
            component.setBackground(Color.WHITE);

            if (component instanceof Container) {
                paintBackgroundWhite((Container) component);
            }
        }
    }

    private static SearchMode getSearchModeFromSettings() {
        if (Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_FILTER)) {
            return SearchMode.FILTER;
        } else if (Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_FLOAT)) {
            return SearchMode.FLOAT;
        } else {
            return SearchMode.FILTER;
        }
    }

    private void toggleSearchModeAndSearch() {
        this.searchMode = searchMode == SearchMode.FILTER ? SearchMode.FLOAT : SearchMode.FILTER;
        updatePreferences();
        updateSearchModeButtonText();
        performSearch();
    }

    private void updateSearchModeButtonText() {
        searchModeButton.setText(searchMode.getDisplayName());
        searchModeButton.setToolTipText(searchMode.getToolTipText());
    }

    /**
     * Initializes the search text field
     */
    private JTextFieldWithUnfocusedText initSearchField() {
        JTextFieldWithUnfocusedText searchField = new JTextFieldWithUnfocusedText(Localization.lang("Search") + "...");
        searchField.setColumns(30);

        searchField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getExtendedKeyCode() == KeyEvent.VK_ESCAPE) {
                    endSearch();
                }
            }
        });

        // Add autocompleter
        autoCompleteSupport = new AutoCompleteSupport<>(searchField);
        autoCompleteSupport.install();

        // Add the global focus listener, so a menu item can see if this field was focused when an action was called.
        searchField.addFocusListener(Globals.focusListener);

        // Search if user press enter
        searchField.addActionListener(e -> performSearch());

        // Subscribe to changes to the text in the search field in order to "live search"
        JTextFieldChangeListenerUtil.addChangeListener(searchField, e -> performSearch());

        return searchField;
    }

    private void endSearch() {
        // first focus request is necessary so that the UI stays nice
        basePanel.mainTable.requestFocus();
        clearSearch();
        basePanel.mainTable.requestFocus();
    }

    /**
     * Save current settings.
     */
    private void updatePreferences() {
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_FLOAT, searchMode == SearchMode.FLOAT);
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_FILTER, searchMode == SearchMode.FILTER);

        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE, caseSensitive.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_REG_EXP, regularExp.isSelected());
    }

    /**
     * Focuses the search field if it is not focused.
     */
    public void focus() {
        if (!searchField.hasFocus()) {
            searchField.requestFocus();
        }
    }

    /**
     * Clears the current search. This includes resetting the search text.
     */
    private void clearSearch() {
        searchField.setText("");
        searchField.setBackground(Color.WHITE);

        searchQueryHighlightObservable.reset();

        this.currentResults.setText("");

        basePanel.mainTable.getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);

        globalSearch.setEnabled(false);
        openCurrentResultsInDialog.setEnabled(false);

        searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
    }

    private SearchWorker worker;

    /**
     * Performs a new search based on the current search query.
     */
    private void performSearch() {
        if(worker != null) {
            worker.cancel(true);
        }

        // An empty search field should cause the search to be cleared.
        if (searchField.getText().isEmpty()) {
            clearSearch();
            return;
        }

        SearchQuery searchQuery = getSearchQuery();
        LOGGER.debug("Searching " + searchQuery + " in " + basePanel.getTabTitle());

        if (!searchQuery.isValid()) {
            informUserAboutInvalidSearchQuery();

            return;
        }

        worker = new SearchWorker(basePanel, searchQuery, searchMode);
        worker.execute();
    }

    private void informUserAboutInvalidSearchQuery() {
        searchField.setBackground(NO_RESULTS_COLOR);

        searchQueryHighlightObservable.reset();

        globalSearch.setEnabled(false);
        openCurrentResultsInDialog.setEnabled(false);

        basePanel.mainTable.getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);

        searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon().createWithNewColor(NO_RESULTS_COLOR));
        searchIcon.setToolTipText(Localization.lang("Search failed: illegal search expression"));

        currentResults.setText(Localization.lang("Search failed: illegal search expression"));
    }

    /**
     * Sets the autocompleter used in the search field.
     *
     * @param searchCompleter the autocompleter
     */
    public void setAutoCompleter(AutoCompleter<String> searchCompleter) {
        this.autoCompleteSupport.setAutoCompleter(searchCompleter);
    }

    public SearchQueryHighlightObservable getSearchQueryHighlightObservable() {
        return searchQueryHighlightObservable;
    }

    boolean isStillValidQuery(SearchQuery query) {
        return query.getQuery().equals(this.searchField.getText())
                && (query.isRegularExpression() == regularExp.isSelected())
                && (query.isCaseSensitive() == caseSensitive.isSelected());
    }

    private SearchQuery getSearchQuery() {
        return new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
    }

    void updateResults(int matched, String description, boolean grammarBasedSearch) {
        if (matched == 0) {
            // nothing found
            this.currentResults.setText(Localization.lang("No results found."));
            this.searchField.setBackground(NO_RESULTS_COLOR);
        } else {
            // specific set found, could be all
            this.currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
            this.searchField.setBackground(RESULTS_FOUND_COLOR);
        }
        this.searchField.setToolTipText("<html>" + description + "</html>");


        if (grammarBasedSearch) {
            searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon().createWithNewColor(ADVANCED_SEARCH_COLOR));
            searchIcon.setToolTipText(Localization.lang("Advanced search active."));
        } else {
            searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
            searchIcon.setToolTipText(Localization.lang("Normal search active."));
        }

        globalSearch.setEnabled(true);
        openCurrentResultsInDialog.setEnabled(true);
    }
}
