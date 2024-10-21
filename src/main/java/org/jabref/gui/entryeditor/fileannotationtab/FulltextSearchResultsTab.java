package org.jabref.gui.entryeditor.fileannotationtab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.documentviewer.DocumentViewerView;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.maintable.OpenExternalFileAction;
import org.jabref.gui.maintable.OpenFolderAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.util.OptionalObjectProperty;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FulltextSearchResultsTab extends EntryEditorTab {

    public static final String NAME = "Search results";
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextSearchResultsTab.class);

    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final ActionFactory actionFactory;
    private final BibDatabaseContext databaseContext;
    private final TaskExecutor taskExecutor;
    private final TextFlow content;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    private BibEntry entry;
    private DocumentViewerView documentViewerView;

    public FulltextSearchResultsTab(StateManager stateManager,
                                    GuiPreferences preferences,
                                    DialogService dialogService,
                                    BibDatabaseContext databaseContext,
                                    TaskExecutor taskExecutor,
                                    OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.actionFactory = new ActionFactory();
        this.taskExecutor = taskExecutor;
        this.searchQueryProperty = searchQueryProperty;

        content = new TextFlow();
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        content.setPadding(new Insets(10));
        setContent(scrollPane);
        setText(Localization.lang("Search results"));
        searchQueryProperty.addListener((observable, oldValue, newValue) -> bindToEntry(entry));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return searchQueryProperty.get().map(query -> query.isValid() && query.getSearchFlags().contains(SearchFlags.FULLTEXT)).orElse(false);
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (entry == null || !shouldShow(entry)) {
            return;
        }
        if (documentViewerView == null) {
            documentViewerView = new DocumentViewerView();
        }
        this.entry = entry;
        content.getChildren().clear();

        stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get().ifPresent(searchQuery -> {
            SearchResults searchResults = searchQuery.getSearchResults();
            if (searchResults != null) {
                Map<String, List<SearchResult>> searchResultsForEntry = searchResults.getFileSearchResultsForEntry(entry);
                if (searchResultsForEntry.isEmpty()) {
                    content.getChildren().add(new Text(Localization.lang("No search matches.")));
                } else {
                    // Iterate through files with search hits
                    for (Map.Entry<String, List<SearchResult>> iterator : searchResultsForEntry.entrySet()) {
                        entry.getFiles().stream().filter(file -> file.getLink().equals(iterator.getKey())).findFirst().ifPresent(linkedFile -> {
                            content.getChildren().addAll(createFileLink(linkedFile), lineSeparator());
                            // Iterate through pages (within file) with search hits
                            for (SearchResult searchResult : iterator.getValue()) {
                                for (String resultTextHtml : searchResult.getContentResultStringsHtml()) {
                                    content.getChildren().addAll(TooltipTextUtil.createTextsFromHtml(resultTextHtml.replace("</b> <b>", " ")));
                                    content.getChildren().addAll(new Text(System.lineSeparator()), lineSeparator(0.8), createPageLink(linkedFile, searchResult.getPageNumber()));
                                }
                                if (!searchResult.getAnnotationsResultStringsHtml().isEmpty()) {
                                    Text annotationsText = new Text(System.lineSeparator() + Localization.lang("Found matches in annotations:") + System.lineSeparator() + System.lineSeparator());
                                    annotationsText.setStyle("-fx-font-style: italic;");
                                    content.getChildren().add(annotationsText);

                                    for (String resultTextHtml : searchResult.getAnnotationsResultStringsHtml()) {
                                        content.getChildren().addAll(TooltipTextUtil.createTextsFromHtml(resultTextHtml.replace("</b> <b>", " ")));
                                        content.getChildren().addAll(new Text(System.lineSeparator()), lineSeparator(0.8), createPageLink(linkedFile, searchResult.getPageNumber()));
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private Text createFileLink(LinkedFile linkedFile) {
        Text fileLinkText = new Text(Localization.lang("Found match in %0", linkedFile.getLink()) + System.lineSeparator() + System.lineSeparator());
        fileLinkText.setStyle("-fx-font-weight: bold;");

        ContextMenu fileContextMenu = getFileContextMenu(linkedFile);
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

    private Text createPageLink(LinkedFile linkedFile, int pageNumber) {
        Text pageLink = new Text(Localization.lang("On page %0", pageNumber) + System.lineSeparator() + System.lineSeparator());
        pageLink.setStyle("-fx-font-style: italic; -fx-font-weight: bold;");

        pageLink.setOnMouseClicked(event -> {
            if (MouseButton.PRIMARY == event.getButton()) {
                documentViewerView.switchToFile(linkedFile);
                documentViewerView.gotoPage(pageNumber);
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
                StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferences, entry, file, taskExecutor)));
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
