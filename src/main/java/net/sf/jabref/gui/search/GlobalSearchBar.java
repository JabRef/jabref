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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.OSXCompatibleToolbar;
import net.sf.jabref.gui.autocompleter.AutoCompleteSupport;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.maintable.MainTable;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.gui.util.component.JTextFieldWithPlaceholder;
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

    private final JLabel searchIcon = new JLabel(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
    private final JTextFieldWithPlaceholder searchField = new JTextFieldWithPlaceholder(Localization.lang("Search") + "...");
    private JButton openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());

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

    /**
     * if this flag is set the searchbar won't be selected after the next search
     */
    private boolean dontSelectSearchBar;


    public GlobalSearchBar(JabRefFrame frame) {
        super();
        this.frame = Objects.requireNonNull(frame);
        SearchPreferences searchPreferences = new SearchPreferences(Globals.prefs);
        searchDisplayMode = searchPreferences.getSearchMode();

        // fits the standard "found x entries"-message thus hinders the searchbar to jump around while searching if the frame width is too small
        currentResults.setPreferredSize(new Dimension(150, 5));
        currentResults.setFont(currentResults.getFont().deriveFont(Font.BOLD));
        searchField.setColumns(30);

        JToggleButton globalSearch = new JToggleButton(IconTheme.JabRefIcon.GLOBAL_SEARCH.getSmallIcon(), searchPreferences.isGlobalSearch());
        globalSearch.setToolTipText(Localization.lang("Search in all open databases"));

        // default action to be performed for toggling globalSearch
        AbstractAction globalSearchStandardAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchPreferences.setGlobalSearch(globalSearch.isSelected());
                updateOpenCurrentResultsTooltip(globalSearch.isSelected());
            }
        };

        // additional action for global search shortcut
        AbstractAction globalSearchShortCutAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalSearch.setSelected(true);
                globalSearchStandardAction.actionPerformed(new ActionEvent(this, 0, "fire standard action"));
                focus();
            }
        };

        String searchGlobalByKey = "searchGlobalByKey";
        globalSearch.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Globals.getKeyPrefs().getKey(KeyBinding.GLOBAL_SEARCH), searchGlobalByKey);
        globalSearch.getActionMap().put(searchGlobalByKey, globalSearchShortCutAction);

        globalSearch.addActionListener(globalSearchStandardAction);

        openCurrentResultsInDialog.setDisabledIcon(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon().createDisabledIcon());
        openCurrentResultsInDialog.addActionListener(event -> {
            if (globalSearch.isSelected()) {
                performGlobalSearch();
            } else {
                openLocalFindingsInExternalPanel();
            }
        });
        openCurrentResultsInDialog.setEnabled(false);
        updateOpenCurrentResultsTooltip(globalSearch.isSelected());

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

        JButton clearSearchButton = new JButton(IconTheme.JabRefIcon.CLOSE.getSmallIcon());
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
                if (autoCompleteSupport.isVisible()) {
                    autoCompleteSupport.setVisible(false);
                } else {
                    endSearch();
                }
            }
        });

        autoCompleteSupport.install();

        String acceptSearch = "acceptSearch";
        searchField.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.ACCEPT), acceptSearch);
        searchField.getActionMap().put(acceptSearch, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoCompleteSupport.setVisible(false);
                BasePanel currentBasePanel = frame.getCurrentBasePanel();
                Globals.getFocusListener().setFocused(currentBasePanel.getMainTable());
                currentBasePanel.getMainTable().requestFocus();
            }
        });

        setLayout(new FlowLayout(FlowLayout.RIGHT));
        JToolBar toolBar = new OSXCompatibleToolbar();
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
        toolBar.addSeparator();
        toolBar.add(globalSearch);
        toolBar.add(regularExp);
        toolBar.add(caseSensitive);
        toolBar.add(searchModeButton);
        toolBar.addSeparator();
        toolBar.add(new HelpAction(HelpFile.SEARCH));
        toolBar.addSeparator();
        toolBar.add(currentResults);
        this.add(toolBar);
    }

    public void performGlobalSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel == null || validateSearchResultFrame(true)) {
            return;
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

    private void openLocalFindingsInExternalPanel() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel == null || validateSearchResultFrame(false)) {
            return;
        }

        if (searchField.getText().isEmpty()) {
            focus();
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
    }

    private boolean validateSearchResultFrame(boolean globalSearch) {
        if (searchResultFrame != null) {
            if (searchResultFrame.isGlobalSearch() == globalSearch && isStillValidQuery(searchResultFrame.getSearchQuery())) {
                searchResultFrame.focus();
                return true;
            } else {
                searchResultFrame.dispose();
                return false;
            }
        }

        return false;
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

    public void endSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel != null) {
            clearSearch(currentBasePanel);
            MainTable mainTable = frame.getCurrentBasePanel().getMainTable();
            Globals.getFocusListener().setFocused(mainTable);
            mainTable.requestFocus();
            SwingUtilities.invokeLater(() -> mainTable.ensureVisible(mainTable.getSelectedRow()));
        }
    }

    /**
     * Focuses the search field if it is not focused.
     */
    public void focus() {
        if (!searchField.hasFocus()) {
            searchField.requestFocus();
            searchField.selectAll();
        }
    }

    private void clearSearch(BasePanel currentBasePanel) {
        currentResults.setText("");
        searchField.setText("");
        searchField.setBackground(NEUTRAL_COLOR);
        searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getSmallIcon());
        searchQueryHighlightObservable.reset();
        openCurrentResultsInDialog.setEnabled(false);

        if (currentBasePanel != null) {
            currentBasePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);
        }

        if (dontSelectSearchBar){
            dontSelectSearchBar = false;
            return;
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
        openCurrentResultsInDialog.setEnabled(false);
    }

    public void setAutoCompleter(AutoCompleter<String> searchCompleter) {
        this.autoCompleteSupport.setAutoCompleter(searchCompleter);
    }

    public SearchQueryHighlightObservable getSearchQueryHighlightObservable() {
        return searchQueryHighlightObservable;
    }

    public boolean isStillValidQuery(SearchQuery query) {
        return query.getQuery().equals(this.searchField.getText())
                && (query.isRegularExpression() == regularExp.isSelected())
                && (query.isCaseSensitive() == caseSensitive.isSelected());
    }

    private SearchQuery getSearchQuery() {
        SearchQuery searchQuery = new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
        this.frame.getCurrentBasePanel().setCurrentSearchQuery(searchQuery);
        return searchQuery;
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

        openCurrentResultsInDialog.setEnabled(true);
    }

    public void setSearchResultFrame(SearchResultFrame searchResultFrame) {
        this.searchResultFrame = searchResultFrame;
    }

    public void setSearchTerm(String searchTerm, boolean dontSelectSearchBar) {
        if (searchTerm.equals(searchField.getText())){
            return;
        }

        setDontSelectSearchBar(dontSelectSearchBar);
        searchField.setText(searchTerm);
        // to hinder the autocomplete window to popup
        autoCompleteSupport.setVisible(false);
    }

    public void setDontSelectSearchBar(boolean dontSelectSearchBar) {
        this.dontSelectSearchBar = dontSelectSearchBar;
    }

    private void updateOpenCurrentResultsTooltip(boolean globalSearchEnabled) {
        if (globalSearchEnabled) {
            openCurrentResultsInDialog.setToolTipText(Localization.lang("Show global search results in a window"));
        } else {
            openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        }
    }

}
