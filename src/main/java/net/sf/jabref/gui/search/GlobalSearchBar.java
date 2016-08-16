/*  Copyright (C) 2003-2016 JabRef contributors.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.OSXCompatibleToolbar;
import net.sf.jabref.gui.WrapLayout;
import net.sf.jabref.gui.autocompleter.AutoCompleteSupport;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.gui.util.component.JTextFieldWithUnfocusedText;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.search.SearchQueryHighlightObservable;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.SearchPreferences;

public class GlobalSearchBar extends JPanel {

    private static final Color NEUTRAL_COLOR = Color.WHITE;
    private static final Color NO_RESULTS_COLOR = new Color(232, 202, 202);
    private static final Color RESULTS_FOUND_COLOR = new Color(217, 232, 202);
    private static final Color ADVANCED_SEARCH_COLOR = new Color(102, 255, 255);

    private final JabRefFrame frame;

    private final JToolBar toolBar = new OSXCompatibleToolbar();
    private final JLabel searchIcon = new JLabel(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
    private final JTextFieldWithUnfocusedText searchField = new JTextFieldWithUnfocusedText(Localization.lang("Search") + "...");
    private final JButton clearSearchButton = new JButton(IconTheme.JabRefIcon.CLOSE.getSmallIcon());
    private final JButton openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());
    private final JButton globalSearch = new JButton(Localization.lang("Search globally"));
    private final JToggleButton caseSensitive;
    private final JToggleButton regularExp;
    private final JButton searchModeButton = new JButton();
    private final JLabel currentResults = new JLabel("");

    private AutoCompleteSupport<String> autoCompleteSupport = new AutoCompleteSupport<>(searchField);
    private final SearchQueryHighlightObservable searchQueryHighlightObservable = new SearchQueryHighlightObservable();

    private SearchWorker searchWorker;
    private GlobalSearchWorker globalSearchWorker;

    private SearchResultFrame searchResultFrame;

    private SearchDisplayMode searchDisplayMode;


    public GlobalSearchBar(JabRefFrame frame) {
        super();
        this.frame = Objects.requireNonNull(frame);
        SearchPreferences searchPreferences = new SearchPreferences(Globals.prefs);
        searchDisplayMode = searchPreferences.getSearchMode();

        // fits the standard "found x entries"-message thus hinders the searchbar to jump around while searching
        currentResults.setPreferredSize(new Dimension(150, 5));
        currentResults.setFont(currentResults.getFont().deriveFont(Font.BOLD));
        searchField.setColumns(30);

        openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        openCurrentResultsInDialog.addActionListener(event -> {
            BasePanel currentBasePanel = frame.getCurrentBasePanel();
            if (currentBasePanel == null) {
                return;
            }
            SearchResultFrame searchDialog = new SearchResultFrame(currentBasePanel.frame(),
                    Localization.lang("Search results in database %0 for %1", currentBasePanel.getBibDatabaseContext()
                                    .getDatabaseFile().map(File::getName).orElse(GUIGlobals.UNTITLED_TITLE),
                    this.getSearchQuery().localize()),
                    getSearchQuery(), false);
            List<BibEntry> entries = currentBasePanel.getDatabase().getEntries().stream()
                    .filter(BibEntry::isSearchHit)
                    .collect(Collectors.toList());
            searchDialog.addEntries(entries, currentBasePanel);
            searchDialog.selectFirstEntry();
            searchDialog.setVisible(true);
        });
        openCurrentResultsInDialog.setEnabled(false);

        globalSearch.setToolTipText(Localization.lang("Search in all open databases"));
        globalSearch.addActionListener(event -> performGlobalSearch());
        globalSearch.setEnabled(false);

        regularExp = new JToggleButton(IconTheme.JabRefIcon.REG_EX.getSmallIcon(),
                searchPreferences.isRegularExpression());
        regularExp.setToolTipText(Localization.lang("regular expression"));
        regularExp.addActionListener(event -> {
            searchPreferences.setRegularExpression(regularExp.isSelected());
            performSearch();
        });

        caseSensitive = new JToggleButton(IconTheme.JabRefIcon.CASE_SENSITIVE.getSmallIcon(),
                searchPreferences.isCaseSensitive());
        caseSensitive.setToolTipText(Localization.lang("Case sensitive"));
        caseSensitive.addActionListener(event -> {
            searchPreferences.setCaseSensitive(caseSensitive.isSelected());
            performSearch();
        });

        updateSearchModeButtonText();
        searchModeButton.addActionListener(event -> toggleSearchModeAndSearch());

        clearSearchButton.setToolTipText(Localization.lang("Clear"));
        clearSearchButton.addActionListener(event -> endSearch());

        searchField.addFocusListener(Globals.getFocusListener());
        searchField.addActionListener(event -> performSearch());
        JTextFieldChangeListenerUtil.addChangeListener(searchField, e -> performSearch());

        String endSearch = "endSearch";
        searchField.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.CLEAR_SEARCH), endSearch);
        searchField.getActionMap().put(endSearch, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                endSearch();
            }
        });

        autoCompleteSupport.install();

        setLayout(new WrapLayout(FlowLayout.RIGHT));
        toolBar.setFloatable(false);
        if (OS.OS_X) {
            searchField.putClientProperty("JTextField.variant", "search");
            toolBar.add(searchField);
        } else {
            toolBar.add(searchIcon);
            toolBar.add(searchField);
            toolBar.add(clearSearchButton);
        }
        toolBar.addSeparator();
        toolBar.add(openCurrentResultsInDialog);
        toolBar.add(globalSearch);
        toolBar.addSeparator();
        toolBar.add(regularExp);
        toolBar.add(caseSensitive);
        toolBar.addSeparator();
        toolBar.add(searchModeButton);
        toolBar.addSeparator();
        toolBar.add(new HelpAction(HelpFile.SEARCH));
        toolBar.addSeparator();
        toolBar.add(currentResults);
        this.add(toolBar);
    }

    public void performGlobalSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel == null) {
            return;
        }

        if (searchResultFrame != null) {
            if (searchResultFrame.isGlobalSearch() && isStillValidQuery(searchResultFrame.getSearchQuery())) {
                searchResultFrame.focus();
                return;
            } else {
                searchResultFrame.dispose();
            }
        }

        if (globalSearchWorker != null) {
            globalSearchWorker.cancel(true);
        }

        if (searchField.getText().isEmpty()) {
            focus();
            return;
        }

        globalSearchWorker = new GlobalSearchWorker(currentBasePanel.frame(), getSearchQuery());
        globalSearchWorker.execute();
    }

    private void toggleSearchModeAndSearch() {
        int nextSearchMode = (searchDisplayMode.ordinal() + 1) % SearchDisplayMode.values().length;
        searchDisplayMode = SearchDisplayMode.values()[nextSearchMode];
        new SearchPreferences(Globals.prefs).setSearchMode(searchDisplayMode);
        updateSearchModeButtonText();
        performSearch();
    }

    private void updateSearchModeButtonText() {
        searchModeButton.setText(searchDisplayMode.getDisplayName());
        searchModeButton.setToolTipText(searchDisplayMode.getToolTipText());
    }

    private void endSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel != null) {
            clearSearch(currentBasePanel);
            currentBasePanel.getMainTable().requestFocus();
        }
    }

    /**
     * Focuses the search field if it is not focused.
     */
    public void focus() {
        if (!searchField.hasFocus()) {
            searchField.requestFocus();
        }
    }

    private void clearSearch(BasePanel currentBasePanel) {
        SearchQuery searchQuery = getSearchQuery();
        updateResults(0, searchQuery.getDescription(), searchQuery.isGrammarBasedSearch());

        currentResults.setText("");
        searchField.setText("");
        searchField.setBackground(NEUTRAL_COLOR);
        searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
        searchQueryHighlightObservable.reset();
        globalSearch.setEnabled(false);
        openCurrentResultsInDialog.setEnabled(false);

        if (currentBasePanel != null) {
            currentBasePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);
        }

        focus();
    }

    public void performSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel == null) {
            return;
        }

        if (searchWorker != null) {
            searchWorker.cancel(true);
        }

        // An empty search field should cause the search to be cleared.
        if (searchField.getText().isEmpty()) {
            clearSearch(currentBasePanel);
            return;
        }

        SearchQuery searchQuery = getSearchQuery();
        if (!searchQuery.isValid()) {
            informUserAboutInvalidSearchQuery();
            return;
        }

        searchWorker = new SearchWorker(currentBasePanel, searchQuery, searchDisplayMode);
        searchWorker.execute();
    }

    private void informUserAboutInvalidSearchQuery() {
        searchField.setBackground(NO_RESULTS_COLOR);

        searchQueryHighlightObservable.reset();

        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        currentBasePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);

        searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon().createWithNewColor(NO_RESULTS_COLOR));
        String illegalSearch = Localization.lang("Search failed: illegal search expression");
        searchIcon.setToolTipText(illegalSearch);
        currentResults.setText(illegalSearch);
        globalSearch.setEnabled(false);
        openCurrentResultsInDialog.setEnabled(false);
    }

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

    public void updateResults(int matched, String description, boolean grammarBasedSearch) {
        if (matched == 0) {
            currentResults.setText(Localization.lang("No results found."));
            this.searchField.setBackground(NO_RESULTS_COLOR);
        } else {
            currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
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

    public void setSearchResultFrame(SearchResultFrame searchResultFrame) {
        this.searchResultFrame = searchResultFrame;
    }

    public SearchResultFrame getSearchResultFrame() {
        return searchResultFrame;
    }

}
