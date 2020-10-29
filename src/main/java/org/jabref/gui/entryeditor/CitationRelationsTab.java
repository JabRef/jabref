package org.jabref.gui.entryeditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.CitationRelationFetcher;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * GUI for tab displaying an articles citation relations based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(org.jabref.gui.entryeditor.CitationRelationsTab.class);
    private final EntryEditorPreferences preferences;
    private final EntryEditor entryEditor;
    private final DialogService dialogService;

    public CitationRelationsTab(EntryEditor entryEditor, EntryEditorPreferences preferences, DialogService dialogService) {
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));
        this.entryEditor = entryEditor;
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    private SplitPane getPane(BibEntry entry) {
        VBox citedVBox = new VBox();
        VBox citedByVBox = new VBox();
        Label citedLabel = new Label("Cited");
        citedLabel.setStyle("-fx-padding: 5px");
        Label citedByLabel = new Label("Cited By");
        citedByLabel.setStyle("-fx-padding: 5px");
        ListView<String> citedListView = new ListView<>();
        ListView<String> citedByListView = new ListView<>();
        StackPane citedStackPane = new StackPane();
        StackPane citedByStackPane = new StackPane();
        Button startCitedButton = new Button("Search");
        Button startCitedByButton = new Button("Search");
        citedByStackPane.getChildren().add(citedByListView);
        citedStackPane.getChildren().add(citedListView);
        citedByStackPane.getChildren().add(startCitedByButton);
        citedStackPane.getChildren().add(startCitedButton);

        citedVBox.setFillWidth(true);
        citedByVBox.setFillWidth(true);
        citedVBox.getChildren().add(citedLabel);
        citedVBox.getChildren().add(citedStackPane);
        citedByVBox.getChildren().add(citedByLabel);
        citedByVBox.getChildren().add(citedByStackPane);
        citedVBox.setAlignment(Pos.TOP_CENTER);
        citedByVBox.setAlignment(Pos.TOP_CENTER);

        SplitPane container = new SplitPane(citedVBox, citedByVBox);
        setContent(container);

        ObservableList<String> citedList = FXCollections.observableArrayList();

        startCitedButton.setOnMouseClicked(event -> {
            startCitedButton.setVisible(false);
            CitationRelationFetcher fetcher = new CitationRelationFetcher();
            try {
                List<BibEntry> list = fetcher.performSearch(entry);
                for(BibEntry e : list) {
                    e.getField(StandardField.DOI).ifPresent(citedList::add);
                }
                citedListView.setItems(citedList);
            } catch (FetcherException e) {
                e.printStackTrace();
            }
        });

        return container;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.shouldShowCitationRelationsTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        // Ask for consent to send data
        setContent(getPane(entry));
        /*if (preferences.isMrdlibAccepted()) {
            setContent(getRelatedArticlesPane(entry));
        } else {
            setContent(getPrivacyDialog(entry));
        }*/
    }
}
