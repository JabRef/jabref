package org.jabref.gui.entryeditor.citationcontexttab;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citation.contextextractor.CitationContextIntegrationService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationContextComponent extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationContextComponent.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final TaskExecutor taskExecutor;

    private final VBox contentBox;
    private final TableView<ExtractedContextRow> resultsTable;
    private final ProgressIndicator progressIndicator;
    private final Label statusLabel;
    private final List<ExtractedContextRow> extractedResults = new ArrayList<>();

    public CitationContextComponent(BibDatabaseContext bibDatabaseContext,
                                    BibEntry entry,
                                    DialogService dialogService,
                                    GuiPreferences preferences,
                                    BibEntryTypesManager entryTypesManager,
                                    TaskExecutor taskExecutor) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.taskExecutor = taskExecutor;

        this.contentBox = new VBox(10);
        this.contentBox.setPadding(new Insets(10));
        this.resultsTable = new TableView<>();
        this.progressIndicator = new ProgressIndicator();
        this.statusLabel = new Label();

        initializeComponent();
    }

    private void initializeComponent() {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            showError(
                    Localization.lang("Unable to extract citation contexts"),
                    Localization.lang("The library must be saved before extracting citation contexts.")
            );
            return;
        }

        if (entry.getCitationKey().isEmpty()) {
            showError(
                    Localization.lang("Unable to extract citation contexts"),
                    Localization.lang("Please provide a citation key for this entry.")
            );
            return;
        }

        List<LinkedFile> pdfFiles = entry.getFiles().stream()
                .filter(file -> FileUtil.isPDFFile(Path.of(file.getLink())))
                .collect(Collectors.toList());

        if (pdfFiles.isEmpty()) {
            showNoPdfFilesMessage();
            return;
        }

        showMainContent(pdfFiles);
    }

    private void showError(String title, String message) {
        VBox errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);

        errorBox.getChildren().addAll(titleLabel, messageLabel);
        setCenter(errorBox);
    }

    private void showNoPdfFilesMessage() {
        VBox messageBox = new VBox(10);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(20));

        Label titleLabel = new Label(Localization.lang("No PDF files attached"));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label messageLabel = new Label(Localization.lang("Attach a PDF file to this entry to extract citation contexts from it."));
        messageLabel.setWrapText(true);

        messageBox.getChildren().addAll(titleLabel, messageLabel);
        setCenter(messageBox);
    }

    private void showMainContent(List<LinkedFile> pdfFiles) {
        VBox headerBox = new VBox(5);
        headerBox.setPadding(new Insets(10));

        Label headerLabel = new Label(Localization.lang("Extract citation contexts"));
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label descriptionLabel = new Label(
                Localization.lang("Extract descriptions of cited papers from this PDF's Related Work section and add them to the cited entries' comment fields.")
        );
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: gray;");

        headerBox.getChildren().addAll(headerLabel, descriptionLabel);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(5, 10, 5, 10));

        Button extractButton = new Button(Localization.lang("Extract from this PDF"));
        extractButton.setGraphic(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        extractButton.setOnAction(e -> startExtraction(pdfFiles));

        Button applyButton = new Button(Localization.lang("Apply selected"));
        applyButton.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
        applyButton.setOnAction(e -> applySelectedContexts());
        applyButton.setDisable(true);

        resultsTable.getItems().addListener((javafx.collections.ListChangeListener<ExtractedContextRow>) change -> {
            applyButton.setDisable(resultsTable.getItems().isEmpty());
        });

        buttonBox.getChildren().addAll(extractButton, applyButton);

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPadding(new Insets(5, 10, 5, 10));
        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);
        statusLabel.setWrapText(true);
        statusBox.getChildren().addAll(progressIndicator, statusLabel);

        VBox resultsBox = createResultsTable();

        contentBox.getChildren().addAll(
                headerBox,
                new Separator(),
                buttonBox,
                statusBox,
                new Separator(),
                resultsBox
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        setCenter(scrollPane);
    }

    private VBox createResultsTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        Label sectionLabel = new Label(Localization.lang("Extracted citation contexts"));
        sectionLabel.setStyle("-fx-font-weight: bold;");

        TableColumn<ExtractedContextRow, Boolean> selectCol = new TableColumn<>("");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setPrefWidth(30);

        TableColumn<ExtractedContextRow, String> markerCol = new TableColumn<>(Localization.lang("Citation"));
        markerCol.setCellValueFactory(cellData -> cellData.getValue().citationMarkerProperty());
        markerCol.setPrefWidth(100);

        TableColumn<ExtractedContextRow, String> targetCol = new TableColumn<>(Localization.lang("Cited entry"));
        targetCol.setCellValueFactory(cellData -> cellData.getValue().targetEntryProperty());
        targetCol.setPrefWidth(150);

        TableColumn<ExtractedContextRow, String> contextCol = new TableColumn<>(Localization.lang("Context"));
        contextCol.setCellValueFactory(cellData -> cellData.getValue().contextTextProperty());
        contextCol.setPrefWidth(400);

        TableColumn<ExtractedContextRow, String> statusCol = new TableColumn<>(Localization.lang("Status"));
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(100);

        resultsTable.getColumns().addAll(selectCol, markerCol, targetCol, contextCol, statusCol);
        resultsTable.setEditable(true);
        resultsTable.setPlaceholder(new Label(Localization.lang("Click 'Extract from this PDF' to find citation contexts.")));
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        resultsTable.setMinHeight(250);

        CheckBox selectAllBox = new CheckBox(Localization.lang("Select all"));
        selectAllBox.setOnAction(e -> {
            boolean selected = selectAllBox.isSelected();
            resultsTable.getItems().forEach(row -> row.setSelected(selected));
        });

        box.getChildren().addAll(sectionLabel, selectAllBox, resultsTable);
        return box;
    }

    private void startExtraction(List<LinkedFile> pdfFiles) {
        String sourceCitationKey = entry.getCitationKey().orElse("");
        if (sourceCitationKey.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Error"),
                    Localization.lang("Please provide a citation key for this entry.")
            );
            return;
        }

        progressIndicator.setVisible(true);
        statusLabel.setText(Localization.lang("Extracting citation contexts from PDF..."));
        resultsTable.getItems().clear();
        extractedResults.clear();

        String username = preferences.getOwnerPreferences().getDefaultOwner();
        CitationContextIntegrationService service = new CitationContextIntegrationService(
                bibDatabaseContext.getDatabase(),
                bibDatabaseContext.getMode(),
                entryTypesManager,
                username
        );

        BackgroundTask.wrap(() -> {
            List<CitationContextIntegrationService.MatchedContext> allMatched = new ArrayList<>();

            for (LinkedFile linkedFile : pdfFiles) {
                if (!FileUtil.isPDFFile(Path.of(linkedFile.getLink()))) {
                    continue;
                }

                try {
                    Optional<Path> resolvedPath = linkedFile.findIn(bibDatabaseContext, preferences.getFilePreferences());
                    if (resolvedPath.isPresent()) {
                        LOGGER.info("Processing PDF: {}", resolvedPath.get());

                        List<CitationContextIntegrationService.MatchedContext> matched =
                                service.previewDocument(resolvedPath.get(), sourceCitationKey);
                        allMatched.addAll(matched);

                        int currentSize = allMatched.size();
                        Platform.runLater(() -> {
                            statusLabel.setText(Localization.lang("Found %0 citation context(s)...", String.valueOf(currentSize)));
                        });
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to process PDF {}: {}", linkedFile.getLink(), e.getMessage());
                }
            }

            return allMatched;
        })
        .onSuccess(matchedContexts -> {
            progressIndicator.setVisible(false);

            if (matchedContexts.isEmpty()) {
                statusLabel.setText(Localization.lang("No citation contexts found in this PDF."));
                return;
            }

            long matchedCount = matchedContexts.stream().filter(m -> m.isMatched()).count();
            long unmatchedCount = matchedContexts.size() - matchedCount;

            if (matchedCount > 0) {
                statusLabel.setText(Localization.lang("Found %0 citation context(s): %1 matched, %2 unmatched. Select which to apply.",
                        String.valueOf(matchedContexts.size()),
                        String.valueOf(matchedCount),
                        String.valueOf(unmatchedCount)));
            } else {
                statusLabel.setText(Localization.lang("Found %0 citation context(s), but none could be matched to library entries. " +
                        "Ensure the cited papers are in your library with matching author names and years.",
                        String.valueOf(matchedContexts.size())));
            }

            for (CitationContextIntegrationService.MatchedContext matched : matchedContexts) {
                ExtractedContextRow row = new ExtractedContextRow(
                        matched.context().citationMarker(),
                        matched.isMatched() ? getEntryDisplayName(matched.libraryEntry()) : Localization.lang("Not found"),
                        matched.context().contextText(),
                        matched.isMatched()
                                ? (matched.isNewEntry() ? Localization.lang("New entry") : Localization.lang("Existing"))
                                : Localization.lang("Unmatched"),
                        matched.isMatched(),
                        matched
                );
                extractedResults.add(row);
            }

            resultsTable.getItems().setAll(extractedResults);
        })
        .onFailure(ex -> {
            progressIndicator.setVisible(false);
            statusLabel.setText(Localization.lang("Error during extraction: %0", ex.getMessage()));
            LOGGER.error("Citation context extraction failed", ex);
        })
        .executeWith(taskExecutor);
    }

    private String getEntryDisplayName(BibEntry entry) {
        if (entry == null) {
            return Localization.lang("Unknown");
        }
        return entry.getCitationKey()
                .orElse(entry.getField(org.jabref.model.entry.field.StandardField.TITLE)
                        .map(t -> t.length() > 30 ? t.substring(0, 30) + "..." : t)
                        .orElse(Localization.lang("Untitled")));
    }

    private void applySelectedContexts() {
        List<ExtractedContextRow> selectedRows = extractedResults.stream()
                .filter(ExtractedContextRow::isSelected)
                .filter(row -> row.getMatchedContext() != null && row.getMatchedContext().isMatched())
                .collect(Collectors.toList());

        if (selectedRows.isEmpty()) {
            dialogService.showInformationDialogAndWait(
                    Localization.lang("No selection"),
                    Localization.lang("Please select at least one matched citation context to apply.")
            );
            return;
        }

        String username = preferences.getOwnerPreferences().getDefaultOwner();
        CitationContextIntegrationService service = new CitationContextIntegrationService(
                bibDatabaseContext.getDatabase(),
                bibDatabaseContext.getMode(),
                entryTypesManager,
                username
        );

        int applied = 0;
        int newEntriesAdded = 0;
        for (ExtractedContextRow row : selectedRows) {
            CitationContextIntegrationService.MatchedContext matched = row.getMatchedContext();
            if (matched != null && matched.libraryEntry() != null) {
                try {
                    BibEntry targetEntry = matched.libraryEntry();

                    if (matched.isNewEntry()) {
                        Optional<String> citationKey = targetEntry.getCitationKey();
                        boolean alreadyExists = citationKey.isPresent() &&
                                bibDatabaseContext.getDatabase().getEntryByCitationKey(citationKey.get()).isPresent();

                        if (!alreadyExists) {
                            bibDatabaseContext.getDatabase().insertEntry(targetEntry);
                            newEntriesAdded++;
                            LOGGER.info("Added new entry to library: {}", citationKey.orElse("(no key)"));
                        } else {
                            targetEntry = bibDatabaseContext.getDatabase().getEntryByCitationKey(citationKey.get()).get();
                        }
                    }

                    org.jabref.logic.citation.contextextractor.CitationCommentWriter writer =
                            new org.jabref.logic.citation.contextextractor.CitationCommentWriter(username);

                    boolean added = writer.addContextToEntry(targetEntry, matched.context());
                    if (added) {
                        applied++;
                        row.setStatus(Localization.lang("Applied"));
                    } else {
                        row.setStatus(Localization.lang("Duplicate"));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to apply context: {}", e.getMessage());
                    row.setStatus(Localization.lang("Failed"));
                }
            }
        }

        resultsTable.refresh();

        if (newEntriesAdded > 0) {
            statusLabel.setText(Localization.lang("Applied %0 context(s). Added %1 new entry(ies) to library.",
                    String.valueOf(applied), String.valueOf(newEntriesAdded)));
        } else {
            statusLabel.setText(Localization.lang("Applied %0 context(s) to cited entries.", String.valueOf(applied)));
        }

        if (applied > 0) {
            String message = newEntriesAdded > 0
                    ? Localization.lang("Successfully added %0 citation context(s) to the cited entries' comment fields. %1 new entry(ies) were added to your library.",
                            String.valueOf(applied), String.valueOf(newEntriesAdded))
                    : Localization.lang("Successfully added %0 citation context(s) to the cited entries' comment fields.", String.valueOf(applied));

            dialogService.showInformationDialogAndWait(
                    Localization.lang("Contexts applied"),
                    message
            );
        }
    }

    public static class ExtractedContextRow {
        private final SimpleBooleanProperty selected;
        private final SimpleStringProperty citationMarker;
        private final SimpleStringProperty targetEntry;
        private final SimpleStringProperty contextText;
        private final SimpleStringProperty status;
        private final CitationContextIntegrationService.MatchedContext matchedContext;

        public ExtractedContextRow(String citationMarker, String targetEntry, String contextText,
                                   String status, boolean selected,
                                   CitationContextIntegrationService.MatchedContext matchedContext) {
            this.selected = new SimpleBooleanProperty(selected);
            this.citationMarker = new SimpleStringProperty(citationMarker);
            this.targetEntry = new SimpleStringProperty(targetEntry);
            this.contextText = new SimpleStringProperty(contextText);
            this.status = new SimpleStringProperty(status);
            this.matchedContext = matchedContext;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public SimpleStringProperty citationMarkerProperty() {
            return citationMarker;
        }

        public SimpleStringProperty targetEntryProperty() {
            return targetEntry;
        }

        public SimpleStringProperty contextTextProperty() {
            return contextText;
        }

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String status) {
            this.status.set(status);
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public CitationContextIntegrationService.MatchedContext getMatchedContext() {
            return matchedContext;
        }
    }
}
