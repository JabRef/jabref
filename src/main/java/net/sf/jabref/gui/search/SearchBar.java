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
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.autocompleter.AutoCompleteSupport;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.search.SearchQueryLocalizer;
import net.sf.jabref.logic.search.SearchTextObservable;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * The search bar at the top of the screen allowing the user to search his database.
 */
public class SearchBar extends JPanel {

    private static final Log LOGGER = LogFactory.getLog(SearchBar.class);

    public static final Color NO_RESULTS_COLOR = new Color(232, 202, 202);
    public static final Color RESULTS_FOUND_COLOR = new Color(217, 232, 202);
    private final JButton openCurrentResultsInDialog;
    private final JButton globalSearch;

    private SearchQuery getSearchQuery() {
        return new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
    }

    public void updateResults(int matched, String description) {
        if (matched == 0) {
            this.currentResults.setText(Localization.lang("No results found."));
            this.searchField.setBackground(NO_RESULTS_COLOR);
        } else {
            this.currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
            this.searchField.setBackground(RESULTS_FOUND_COLOR);
        }
        this.searchField.setToolTipText("<html>" + description + "</html>");

        globalSearch.setEnabled(true);
        openCurrentResultsInDialog.setEnabled(true);
    }

    private final BasePanel basePanel;
    private final SearchTextObservable searchTextObservable;

    private final JSearchTextField searchField;

    private JRadioButtonMenuItem modeFloat;
    private JRadioButtonMenuItem modeLiveFilter;

    private final JCheckBox caseSensitive;
    private final JCheckBox regularExp;

    private final JLabel currentResults = new JLabel("");

    AutoCompleteSupport<String> autoCompleteSupport;

    /**
     * Initializes the search bar.
     *
     * @param frame the main window
     */
    public SearchBar(BasePanel basePanel) {
        super();

        this.basePanel = Objects.requireNonNull(basePanel);
        this.searchTextObservable = new SearchTextObservable();

        currentResults.setFont(currentResults.getFont().deriveFont(Font.BOLD));

        caseSensitive = new JCheckBox(Localization.lang("Match case"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE));
        caseSensitive.addItemListener(ae -> performSearch());
        caseSensitive.addItemListener(ae -> updatePrefs());
        regularExp = new JCheckBox(Localization.lang("Regex"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
        regularExp.addItemListener(ae -> performSearch());
        regularExp.addItemListener(ae -> updatePrefs());

        openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());
        openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        openCurrentResultsInDialog.addActionListener(ae -> {
            SearchResultsDialog searchDialog = new SearchResultsDialog(basePanel.frame(), Localization.lang("Search results in database %0 for %1",
                    basePanel.getDatabaseFile().getName(), SearchQueryLocalizer.localize(this.getSearchQuery())));
            basePanel.getDatabase().getEntries().stream().filter(BibtexEntry::isSearchHit).forEach(entry -> searchDialog.addEntry(entry, basePanel));
            searchDialog.selectFirstEntry();
            searchDialog.setVisible(true);
        });
        openCurrentResultsInDialog.setEnabled(false);

        // Init controls
        setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel searchIcon = new JLabel(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
        this.add(searchIcon);
        this.searchField = initSearchField();
        this.add(searchField);
        JButton button = new JButton(Localization.lang("View"));
        JPopupMenu settingsMenu = createSettingsMenu();
        button.addActionListener(l -> settingsMenu.show(button, 0, button.getHeight()));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(regularExp);
        toolBar.add(caseSensitive);
        toolBar.addSeparator();
        toolBar.add(button);
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
        toolBar.add(new HelpAction(basePanel.frame().helpDiag, GUIGlobals.searchHelp, Localization.lang("Help")));

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

    private JPopupMenu createSettingsMenu() {
        // Populate popup menu and add it to search button
        JPopupMenu menu = new JPopupMenu("Settings");
        initSearchModeMenu();
        menu.add(getSearchModeMenuItem(SearchMode.FILTER));
        menu.add(getSearchModeMenuItem(SearchMode.FLOAT));
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
                modeFloat = new JRadioButtonMenuItem(String.format("%s - %s", mode.getDisplayName(), mode.getToolTipText()),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_FLOAT));
                break;
            case FILTER:
                modeLiveFilter = new JRadioButtonMenuItem(String.format("%s - %s", mode.getDisplayName(), mode.getToolTipText()),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_MODE_LIVE_FILTER));
                break;
            }

            // Add menu item to group
            searchMethod.add(getSearchModeMenuItem(mode));

            // Listen to selection changed events
            getSearchModeMenuItem(mode).addItemListener(e -> performSearch());
        }
    }

    /**
     * Initializes the search text field
     */
    private JSearchTextField initSearchField() {
        JSearchTextField searchField = new JSearchTextField();
        searchField.setTextWhenNotFocused(Localization.lang("Search..."));
        searchField.setColumns(30);

        searchField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getExtendedKeyCode() == KeyEvent.VK_ESCAPE) {
                    basePanel.mainTable.requestFocus();
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

    /**
     * Returns the item in the popup menu of the search button corresponding to the given search mode
     */
    private JRadioButtonMenuItem getSearchModeMenuItem(SearchMode mode) {
        switch (mode) {
        case FLOAT:
            return modeFloat;
        case FILTER:
        default:
            return modeLiveFilter;
        }
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

        return SearchMode.FILTER;
    }

    /**
     * Save current settings.
     */
    public void updatePrefs() {
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_FLOAT, modeFloat.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_MODE_LIVE_FILTER, modeLiveFilter.isSelected());

        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE, caseSensitive.isSelected());
        Globals.prefs.putBoolean(JabRefPreferences.SEARCH_REG_EXP, regularExp.isSelected());
    }

    /**
     * Focuses the search field if it is not focused. Otherwise, cycles to the next search type.
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

        searchTextObservable.fireSearchlistenerEvent(null);

        this.currentResults.setText("");

        if (basePanel.isShowingFloatSearch()) {
            basePanel.mainTable.stopShowingFloatSearch();
        }
        basePanel.stopShowingFilterSearch();

        globalSearch.setEnabled(false);
        openCurrentResultsInDialog.setEnabled(false);
    }

    /**
     * Performs a new search based on the current search query.
     */
    private void performSearch() {
        // An empty search field should cause the search to be cleared.
        if (searchField.getText().isEmpty()) {
            clearSearch();
            return;
        }

        SearchQuery searchQuery = getSearchQuery();
        LOGGER.debug("Searching " + searchQuery.toString() + " in " + basePanel.getTabTitle());

        if (!searchQuery.isValidQuery()) {
            basePanel.output(Localization.lang("Search failed: illegal search expression"));
            clearSearch();
            return;
        }

        SearchWorker worker = new SearchWorker(basePanel, searchQuery, getSearchMode());
        worker.getWorker().run();
        worker.getCallBack().update();
    }

    /**
     * Sets the autocompleter used in the search field.
     *
     * @param searchCompleter the autocompleter
     */
    public void setAutoCompleter(AutoCompleter<String> searchCompleter) {
        this.autoCompleteSupport.setAutoCompleter(searchCompleter);
    }

    public SearchTextObservable getSearchTextObservable() {
        return searchTextObservable;
    }

    public boolean isStillValidQuery(SearchQuery query) {
        return query.query.equals(this.searchField.getText())
                && query.regularExpression == regularExp.isSelected()
                && query.caseSensitive == caseSensitive.isSelected();
    }
}
