package org.jabref.gui.entryeditor.fileannotationtab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.maintable.OpenExternalFileAction;
import org.jabref.gui.maintable.OpenFolderAction;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FulltextSearchResultsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextSearchResultsTab.class);

    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final ActionFactory actionFactory;

    private final ScrollPane scrollPane;
    private final TextFlow content;

    private BibEntry entry;

    public FulltextSearchResultsTab(StateManager stateManager, PreferencesService preferencesService, DialogService dialogService) {
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.actionFactory = new ActionFactory(preferencesService.getKeyBindingRepository());

        content = new TextFlow();
        scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        setContent(scrollPane);
        setText(Localization.lang("Search results"));
        this.stateManager.activeSearchQueryProperty().addListener((observable, oldValue, newValue) -> bindToEntry(entry));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return this.stateManager.activeSearchQueryProperty().isPresent().get() &&
                this.stateManager.activeSearchQueryProperty().get().isPresent() &&
                this.stateManager.activeSearchQueryProperty().get().get().getSearchFlags().contains(SearchRules.SearchFlags.FULLTEXT) &&
                this.stateManager.activeSearchQueryProperty().get().get().getQuery().length() > 0;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        this.entry = entry;
        if (!shouldShow(entry) || entry == null) {
            return;
        }
        PdfSearchResults searchResults = stateManager.activeSearchQueryProperty().get().get().getRule().getFulltextResults(stateManager.activeSearchQueryProperty().get().get().getQuery(), entry);

        content.getChildren().clear();

        if (searchResults.numSearchResults() == 0) {
            content.getChildren().add(new Text(Localization.lang("No search matches.")));
        }

        // Iterate through files with search hits
        for (Map.Entry<String, HashMap<Integer, List<SearchResult>>> resultsPath : searchResults.getSearchResultsByPathAndPage().entrySet()) {
            content.getChildren().addAll(createFileLink(resultsPath.getKey()), lineSeparator());

            // Iterate through pages (within file) with search hits
            for (Map.Entry<Integer, List<SearchResult>> resultsPage : resultsPath.getValue().entrySet()) {
                Text pageLinkText = new Text(Localization.lang("Page %0", resultsPage.getKey()) + System.lineSeparator()); // tooltip with absolute path?
                content.getChildren().addAll(pageLinkText, lineSeparator());

                // Iterate through search hits (within file within page)
                for (SearchResult searchResult : resultsPage.getValue()) {
                    for (String resultTextHtml : searchResult.getResultStringsHtml()) {
                        content.getChildren().addAll(highlightResultString(resultTextHtml));
                        content.getChildren().add(lineSeparator());
                    }
                }
            }
        }
    }

    private Text createFileLink(String pathToFile) {
        LinkedFile linkedFile = new LinkedFile("", Path.of(pathToFile), "pdf");
        Text fileLinkText = new Text(Localization.lang("Found match in %0", pathToFile) + System.lineSeparator());
        ContextMenu fileContextMenu = getFileContextMenu(linkedFile);
        Path resolvedPath = linkedFile.findIn(stateManager.getActiveDatabase().get(), preferencesService.getFilePreferences()).orElse(Path.of(pathToFile));
        Tooltip fileLinkTooltip = new Tooltip(resolvedPath.toAbsolutePath().toString());
        Tooltip.install(fileLinkText, fileLinkTooltip);
        fileLinkText.setOnMouseClicked(event -> {
            if (MouseButton.PRIMARY.equals(event.getButton())) {
                try {
                    JabRefDesktop.openBrowser(resolvedPath.toUri());
                } catch (IOException e) {
                    LOGGER.error("Cannot open {}.", resolvedPath.toString(), e);
                }
            } else {
                fileContextMenu.show(fileLinkText, event.getScreenX(), event.getScreenY());
            }
        });
        return fileLinkText;
    }

    private ContextMenu getFileContextMenu(LinkedFile file) {
        ContextMenu fileContextMenu = new ContextMenu();
        fileContextMenu.getItems().add(actionFactory.createMenuItem(StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferencesService, entry, file)));
        fileContextMenu.getItems().add(actionFactory.createMenuItem(StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferencesService)));
        return fileContextMenu;
    }

    private Separator lineSeparator() {
        Separator lineSeparator = new Separator(Orientation.HORIZONTAL);
        lineSeparator.prefWidthProperty().bind(content.widthProperty());
        lineSeparator.setPrefHeight(15);
        return lineSeparator;
    }

    private List<Text> highlightResultString(String htmlHighlightedResult) {
        List<Text> highlightedResultsStrings = new ArrayList<>();

        for (String fragment : htmlHighlightedResult.split(SearchResult.HIGHLIGHTING_PRE_TAG)) {
            String[] splitPost = fragment.split(SearchResult.HIGHLIGHTING_POST_TAG);
            if (splitPost.length > 2) {
                throw new IllegalArgumentException("More than one POST_TAG for a PRE_TAG");
            }
            if (splitPost.length == 1) {
                highlightedResultsStrings.add(new Text(splitPost[0]));
            } else {
                Text highlightedText = new Text(splitPost[0]);
                highlightedText.setStyle("-fx-fill: -jr-green;");
                highlightedResultsStrings.add(highlightedText);
                highlightedResultsStrings.add(new Text(splitPost[1]));
            }
        }

        return highlightedResultsStrings;
    }
}
