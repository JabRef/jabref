package org.jabref.gui.entryeditor.fileannotationtab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.documentviewer.DocumentViewerView;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.maintable.OpenFolderAction;
import org.jabref.gui.maintable.OpenSingleExternalFileAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FulltextSearchResultsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextSearchResultsTab.class);

    @NullMarked
    private record SearchDisplay(BibEntry entry, SearchQuery query, SearchResults results) {
    }

    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final ActionFactory actionFactory;
    private final TaskExecutor taskExecutor;
    private final TextFlow content;

    private BibEntry entry;
    private DocumentViewerView documentViewerView;

    /// Content available only while an active, valid fulltext search query exists.
    private final ObservableValue<Boolean> contentVisibility;
    private final ChangeListener<Optional<SearchResults>> searchResultsListener = (_, _, _) -> updateSearch();
    private final ChangeListener<Optional<SearchQuery>> searchQueryListener;
    private final ChangeListener<Optional<LibraryTab>> activeTabListener = (_, previous, current) -> {
        previous.ifPresent(tab -> tab.searchResultsProperty().removeListener(searchResultsListener));
        current.ifPresent(tab -> tab.searchResultsProperty().addListener(searchResultsListener));
        updateSearch();
    };

    public FulltextSearchResultsTab(StateManager stateManager,
                                    GuiPreferences preferences,
                                    DialogService dialogService,
                                    TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.actionFactory = new ActionFactory();
        this.taskExecutor = taskExecutor;

        this.contentVisibility = Bindings.createBooleanBinding(
                () -> stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get()
                                  .map(query -> query.isValid() && query.getSearchFlags().contains(SearchFlags.FULLTEXT))
                                  .orElse(false),
                stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH));
        setContentDrivenVisibility(contentVisibility);

        content = new TextFlow();
        searchQueryListener = (_, _, _) -> content.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        content.setPadding(new Insets(10));
        setContent(scrollPane);
        setText(EntryEditorTabModel.BuiltIn.FULLTEXT_SEARCH_RESULTS.displayName());

        stateManager.activeTabProperty().get()
                    .ifPresent(tab -> tab.searchResultsProperty().addListener(searchResultsListener));
        stateManager.activeTabProperty().addListener(activeTabListener);
        stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).addListener(searchQueryListener);
    }

    @Override
    protected void dispose() {
        stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).removeListener(searchQueryListener);
        stateManager.activeTabProperty().removeListener(activeTabListener);
        stateManager.activeTabProperty().get()
                    .ifPresent(tab -> tab.searchResultsProperty().removeListener(searchResultsListener));
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (entry == null || !contentVisibility.getValue()) {
            return;
        }
        this.entry = entry;
        updateSearch();
    }

    // [impl->req~jabgui.search.fulltext.entry-editor-results~1]
    private void updateSearch() {
        content.getChildren().clear();
        Optional.ofNullable(entry)
                .flatMap(selectedEntry -> stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get()
                                                      .flatMap(searchQuery -> stateManager.activeTabProperty().get()
                                                                                          .flatMap(tab -> tab.searchResultsProperty().get())
                                                                                          .map(results -> new SearchDisplay(selectedEntry, searchQuery, results))))
                .ifPresent(display -> renderResults(display.entry(), display.query(), display.results()));
    }

    private void renderResults(BibEntry selectedEntry, SearchQuery searchQuery, SearchResults searchResults) {
        Map<String, List<SearchResult>> searchResultsForEntry = searchResults.getFileSearchResultsForEntry(selectedEntry);
        if (searchResultsForEntry.isEmpty()) {
            content.getChildren().add(new Text(Localization.lang("No search matches.")));
        } else {
            // Iterate through files with search hits
            for (Map.Entry<String, List<SearchResult>> iterator : searchResultsForEntry.entrySet()) {
                selectedEntry.getFiles().stream().filter(file -> file.getLink().equals(iterator.getKey())).findFirst().ifPresent(linkedFile -> {
                    content.getChildren().addAll(createFileLink(linkedFile), lineSeparator());
                    // Iterate through pages (within file) with search hits
                    for (SearchResult searchResult : iterator.getValue()) {
                        for (String resultTextHtml : searchResult.getContentResultStringsHtml()) {
                            content.getChildren().addAll(TooltipTextUtil.createTextsFromHtml(resultTextHtml.replace("</b> <b>", " ")));
                            content.getChildren().addAll(new Text(System.lineSeparator()), lineSeparator(0.8), createPageLink(linkedFile, searchResult.getPageNumber(), searchQuery.getSearchExpression()));
                        }
                        if (!searchResult.getAnnotationsResultStringsHtml().isEmpty()) {
                            Text annotationsText = new Text(System.lineSeparator() + Localization.lang("Found matches in annotations:") + System.lineSeparator() + System.lineSeparator());
                            annotationsText.getStyleClass().add("italic");
                            content.getChildren().add(annotationsText);

                            for (String resultTextHtml : searchResult.getAnnotationsResultStringsHtml()) {
                                content.getChildren().addAll(TooltipTextUtil.createTextsFromHtml(resultTextHtml.replace("</b> <b>", " ")));
                                content.getChildren().addAll(new Text(System.lineSeparator()), lineSeparator(0.8), createPageLink(linkedFile, searchResult.getPageNumber(), searchQuery.getSearchExpression()));
                            }
                        }
                    }
                });
            }
        }
    }

    private Text createFileLink(LinkedFile linkedFile) {
        Text fileLinkText = new Text(Localization.lang("Found match in %0", linkedFile.getLink()) + System.lineSeparator() + System.lineSeparator());
        fileLinkText.getStyleClass().add("bold");

        ContextMenu fileContextMenu = getFileContextMenu(linkedFile);
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().orElse(new BibDatabaseContext());
        Path resolvedPath = linkedFile.findIn(databaseContext, preferences.getFilePreferences()).orElse(Path.of(linkedFile.getLink()));
        Tooltip fileLinkTooltip = new Tooltip(resolvedPath.toAbsolutePath().toString());
        Tooltip.install(fileLinkText, fileLinkTooltip);
        fileLinkText.setOnMouseClicked(event -> {
            if (MouseButton.PRIMARY == event.getButton()) {
                try {
                    NativeDesktop.openBrowser(resolvedPath.toUri(), preferences.getExternalApplicationsPreferences());
                } catch (IOException e) {
                    LOGGER.error("Cannot open {}.", resolvedPath, e);
                }
            } else {
                fileContextMenu.show(fileLinkText, event.getScreenX(), event.getScreenY());
            }
        });
        return fileLinkText;
    }

    private Text createPageLink(LinkedFile linkedFile, int pageNumber, String searchExpression) {
        Text pageLink = new Text(Localization.lang("On page %0", pageNumber) + System.lineSeparator() + System.lineSeparator());
        pageLink.getStyleClass().addAll("italic", "bold");

        pageLink.setOnMouseClicked(event -> {
            if (MouseButton.PRIMARY == event.getButton()) {
                if (documentViewerView == null) {
                    documentViewerView = new DocumentViewerView();
                }
                documentViewerView.switchToFile(linkedFile);
                documentViewerView.gotoPage(pageNumber);
                documentViewerView.highlightText(searchExpression);
                documentViewerView.disableLiveMode();
                dialogService.showCustomDialog(documentViewerView);
            }
        });
        return pageLink;
    }

    private ContextMenu getFileContextMenu(LinkedFile file) {
        ContextMenu fileContextMenu = new ContextMenu();
        fileContextMenu.getItems().add(actionFactory.createMenuItem(
                StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferences, entry, file, taskExecutor)));
        fileContextMenu.getItems().add(actionFactory.createMenuItem(
                StandardActions.OPEN_EXTERNAL_FILE, new OpenSingleExternalFileAction(dialogService, preferences, entry, file, taskExecutor, stateManager)));
        return fileContextMenu;
    }

    private Separator lineSeparator() {
        return lineSeparator(1.0);
    }

    private Separator lineSeparator(double widthMultiplier) {
        Separator lineSeparator = new Separator(Orientation.HORIZONTAL);
        lineSeparator.prefWidthProperty().bind(content.widthProperty().multiply(widthMultiplier));
        lineSeparator.setPrefHeight(15);
        return lineSeparator;
    }
}
