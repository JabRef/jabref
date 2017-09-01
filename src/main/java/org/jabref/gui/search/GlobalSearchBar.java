package org.jabref.gui.search;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import javafx.css.PseudoClass;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.OSXCompatibleToolbar;
import org.jabref.gui.autocompleter.AppendPersonNamesStrategy;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.PersonNameStringConverter;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.search.SearchQueryHighlightObservable;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.SearchPreferences;

import org.fxmisc.easybind.EasyBind;

public class GlobalSearchBar extends JPanel {

    private static final Color NEUTRAL_COLOR = Color.WHITE;
    private static final Color NO_RESULTS_COLOR = new Color(232, 202, 202);
    private static final Color RESULTS_FOUND_COLOR = new Color(217, 232, 202);
    private static final Color ADVANCED_SEARCH_COLOR = new Color(102, 255, 255);

    private static final PseudoClass CLASS_NO_RESULTS = PseudoClass.getPseudoClass("emptyResult");
    private static final PseudoClass CLASS_RESULTS_FOUND = PseudoClass.getPseudoClass("emptyResult");

    private final JabRefFrame frame;

    private final TextField searchField = SearchTextField.create();
    private final JToggleButton caseSensitive;
    private final JToggleButton regularExp;
    private final JButton searchModeButton = new JButton();
    private final JLabel currentResults = new JLabel("");
    private final SearchQueryHighlightObservable searchQueryHighlightObservable = new SearchQueryHighlightObservable();
    private final JButton openCurrentResultsInDialog = new JButton(IconTheme.JabRefIcon.OPEN_IN_NEW_WINDOW.getSmallIcon());
    private final JFXPanel container;
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

        JToggleButton globalSearch = new JToggleButton(IconTheme.JabRefIcon.GLOBAL_SEARCH.getSmallIcon(), searchPreferences.isGlobalSearch());
        globalSearch.setToolTipText(Localization.lang("Search in all open libraries"));

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
        //TODO: These have to be somehow converted
        /*
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
        */

        /*
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
        */

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

        EasyBind.subscribe(searchField.textProperty(), searchText -> performSearch());

        container = OS.LINUX ? new CustomJFXPanel() : new JFXPanel();
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            container.setScene(new Scene(searchField));
            container.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    //We need to consume this event here to prevent the propgation of keybinding events back to the JFrame
                    Optional<KeyBinding> keyBinding = Globals.getKeyPrefs().mapToKeyBinding(e);
                    if (keyBinding.isPresent()) {
                        switch (keyBinding.get()) {
                            case CUT:
                            case COPY:
                            case PASTE:
                            case DELETE_ENTRY:
                            case SELECT_ALL:
                                e.consume();
                                break;
                            default:
                                //do nothing
                        }
                    }
                }
            });

        });

        setLayout(new FlowLayout(FlowLayout.RIGHT));
        JToolBar toolBar = new OSXCompatibleToolbar();
        toolBar.setFloatable(false);
        toolBar.add(container);
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
        if ((currentBasePanel == null) || validateSearchResultFrame(true)) {
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
        if ((currentBasePanel == null) || validateSearchResultFrame(false)) {
            return;
        }

        if (searchField.getText().isEmpty()) {
            focus();
            return;
        }

        SearchResultFrame searchDialog = new SearchResultFrame(currentBasePanel.frame(),
                Localization.lang("Search results in library %0 for %1", currentBasePanel.getBibDatabaseContext()
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
            if ((searchResultFrame.isGlobalSearch() == globalSearch) && isStillValidQuery(searchResultFrame.getSearchQuery())) {
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
        if (!searchField.isFocused()) {
            container.requestFocus();
            searchField.requestFocus();
        }
        searchField.selectAll();
    }

    private void clearSearch(BasePanel currentBasePanel) {
        currentResults.setText("");
        searchField.setText("");
        searchQueryHighlightObservable.reset();
        openCurrentResultsInDialog.setEnabled(false);

        if (currentBasePanel != null) {
            currentBasePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);
            currentBasePanel.setCurrentSearchQuery(null);
        }

        if (dontSelectSearchBar) {
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
        searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);

        searchQueryHighlightObservable.reset();

        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        currentBasePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);

        String illegalSearch = Localization.lang("Search failed: illegal search expression");
        currentResults.setText(illegalSearch);
        openCurrentResultsInDialog.setEnabled(false);
    }

    public void setAutoCompleter(AutoCompleteSuggestionProvider<Author> searchCompleter) {
        AutoCompletionTextInputBinding.autoComplete(searchField,
                searchCompleter,
                new PersonNameStringConverter(true, true, AutoCompleteFirstNameMode.BOTH),
                new AppendPersonNamesStrategy());

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
            searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);
        } else {
            currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
            searchField.pseudoClassStateChanged(CLASS_RESULTS_FOUND, true);
        }
        DefaultTaskExecutor.runInJavaFXThread(() -> searchField.setTooltip(new Tooltip(description)));
        openCurrentResultsInDialog.setEnabled(true);
    }

    public void setSearchResultFrame(SearchResultFrame searchResultFrame) {
        this.searchResultFrame = searchResultFrame;
    }

    public void setSearchTerm(String searchTerm) {
        if (searchTerm.equals(searchField.getText())) {
            return;
        }

        setDontSelectSearchBar();
        searchField.setText(searchTerm);
    }

    public void setDontSelectSearchBar() {
        this.dontSelectSearchBar = true;
    }

    private void updateOpenCurrentResultsTooltip(boolean globalSearchEnabled) {
        if (globalSearchEnabled) {
            openCurrentResultsInDialog.setToolTipText(Localization.lang("Show global search results in a window"));
        } else {
            openCurrentResultsInDialog.setToolTipText(Localization.lang("Show search results in a window"));
        }
    }

}
