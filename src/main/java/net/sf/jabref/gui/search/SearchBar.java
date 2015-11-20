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
import net.sf.jabref.logic.search.SearchObservable;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * The search bar at the top of the screen allowing the user to search his database.
 */
public class SearchBar extends JPanel {

    private SearchQuery getSearchQuery() {
        return new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
    }

    public void updateResults(int matched, String description) {
        if (matched == 0) {
            this.currentResults.setText(Localization.lang("No results found."));
            this.searchField.setBackground(Color.RED);
        } else {
            this.currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
            this.searchField.setBackground(Color.GREEN);
        }
        this.searchField.setToolTipText("<html>" + description + "</html>");
    }

    private final BasePanel basePanel;
    private final SearchObservable searchObservable;

    private final JSearchTextField searchField;

    private JRadioButtonMenuItem modeFloat;
    private JRadioButtonMenuItem modeLiveFilter;

    private final JCheckBox caseSensitive;
    private final JCheckBox regularExp;

    private final JLabel currentResults = new JLabel("");

    AutoCompleteSupport<String> autoCompleteSupport;

    private final SearchWorker worker;

    /**
     * Initializes the search bar.
     *
     * @param frame the main window
     */
    public SearchBar(BasePanel basePanel) {
        super();

        this.basePanel = basePanel;
        this.searchObservable = new SearchObservable();

        worker = new SearchWorker(basePanel);

        currentResults.setFont(currentResults.getFont().deriveFont(Font.BOLD));

        caseSensitive = new JCheckBox(Localization.lang("Match case"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE));
        caseSensitive.addItemListener(ae -> performSearch());
        caseSensitive.addItemListener(ae -> updatePrefs());
        regularExp = new JCheckBox(Localization.lang("Regex"), Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
        regularExp.addItemListener(ae -> performSearch());
        regularExp.addItemListener(ae -> updatePrefs());

        JButton openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());
        openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        openCurrentResultsInDialog.addActionListener(ae -> {
            SearchResultsDialog searchDialog = new SearchResultsDialog(basePanel.frame(), Localization.lang("Search results in database %0 for %1",
                    basePanel.getDatabaseFile().getName(), this.getSearchQuery().toString()));
            basePanel.getDatabase().getEntries().stream().filter(BibtexEntry::isSearchHit).forEach(entry -> searchDialog.addEntry(entry, basePanel));
            searchDialog.selectFirstEntry();
            searchDialog.setVisible(true);
        });

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
        JButton globalSearch = new JButton(Localization.lang("Search globally"));
        globalSearch.setToolTipText(Localization.lang("Search in all open databases"));
        globalSearch.addActionListener(l -> {
            AbstractWorker worker = new GlobalSearchWorker(basePanel.frame(), getSearchQuery());
            worker.run();
            worker.update();
        });
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

            if(component instanceof Container) {
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
            getSearchModeMenuItem(mode).addChangeListener(e -> performSearch());
        }
    }

    /**
     * Initializes the search text field
     */
    private JSearchTextField initSearchField() {
        JSearchTextField searchField = new JSearchTextField();
        searchField.setTextWhenNotFocused(Localization.lang("Search..."));
        searchField.setColumns(30);

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
        if (searchField.hasFocus()) {

            switch (getSearchMode()) {
            case FLOAT:
                setSearchMode(SearchMode.FILTER);
                break;
            case FILTER:
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
            searchField.setBackground(Color.WHITE);

            searchObservable.fireSearchlistenerEvent(null);

            this.currentResults.setText("");
        });
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

        if (basePanel == null) {
            return;
        }

        SearchQuery searchQuery = getSearchQuery();

        // Notify others about the search
        // TODO SIMON should be done in update method
        searchObservable.fireSearchlistenerEvent(getSearchQuery());

        if (!searchQuery.isValidQuery()) {
            basePanel.output(Localization.lang("Search failed: illegal search expression"));
            clearSearch();
            return;
        }

        worker.initSearch(searchQuery, getSearchMode());
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

    public SearchObservable getSearchObservable() {
        return searchObservable;
    }
}
