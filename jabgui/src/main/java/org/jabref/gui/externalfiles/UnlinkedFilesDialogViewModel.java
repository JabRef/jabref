package org.jabref.gui.externalfiles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlinkedFilesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkedFilesDialogViewModel.class);

    private final ImportHandler importHandler;

    private final StringProperty directoryPath = new SimpleStringProperty("");
    private final ObjectProperty<FileExtensionViewModel> selectedExtension = new SimpleObjectProperty<>();
    private final ObjectProperty<DateRange> selectedDate = new SimpleObjectProperty<>();
    private final ObjectProperty<ExternalFileSorter> selectedSort = new SimpleObjectProperty<>();

    private final ObjectProperty<Optional<FileNodeViewModel>> treeRootProperty = new SimpleObjectProperty<>();

    private final SimpleListProperty<TreeItem<FileNodeViewModel>> checkedFileListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty taskActiveProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty progressValueProperty = new SimpleDoubleProperty(0);
    private final StringProperty progressTextProperty = new SimpleStringProperty();

    private final ObservableList<ImportFilesResultItemViewModel> resultList = FXCollections.observableArrayList();

    private final ObservableList<FileExtensionViewModel> fileFilterList;
    private final ObservableList<DateRange> dateFilterList;
    private final ObservableList<ExternalFileSorter> fileSortList;

    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final BibDatabaseContext bibDatabase;
    private final TaskExecutor taskExecutor;
    private final StateManager stateManager;
    private final FunctionBasedValidator<String> scanDirectoryValidator;
    /**
     * NEW: stores candidate BibEntries per selected file
     */
    private final ObjectProperty<Map<Path, List<BibEntry>>> candidateEntriesMap = new SimpleObjectProperty<>(new HashMap<>());
    /**
     * Stores successfully linked entries (file path -> entry) for "jump to entry" functionality
     */
    private final Map<String, BibEntry> linkedEntries = new HashMap<>();
    private BackgroundTask<FileNodeViewModel> findUnlinkedFilesTask;

    public UnlinkedFilesDialogViewModel(DialogService dialogService, UndoManager undoManager, FileUpdateMonitor fileUpdateMonitor, GuiPreferences preferences, StateManager stateManager, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;

        this.bibDatabase = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));

        this.importHandler = new ImportHandler(bibDatabase, preferences, fileUpdateMonitor, undoManager, stateManager, dialogService, taskExecutor);

        this.fileFilterList = FXCollections.observableArrayList(new FileExtensionViewModel(StandardFileType.ANY_FILE, preferences.getExternalApplicationsPreferences()), new FileExtensionViewModel(StandardFileType.HTML, preferences.getExternalApplicationsPreferences()), new FileExtensionViewModel(StandardFileType.MARKDOWN, preferences.getExternalApplicationsPreferences()), new FileExtensionViewModel(StandardFileType.PDF, preferences.getExternalApplicationsPreferences()));

        this.dateFilterList = FXCollections.observableArrayList(DateRange.values());
        this.fileSortList = FXCollections.observableArrayList(ExternalFileSorter.values());

        Predicate<String> isDirectory = path -> Files.isDirectory(Path.of(path));
        this.scanDirectoryValidator = new FunctionBasedValidator<>(directoryPath, isDirectory, ValidationMessage.error(Localization.lang("Please enter a valid file path.")));

        treeRootProperty.set(Optional.empty());
    }

    /* ================= SEARCH ================= */

    public void startSearch() {
        Path directory = getSearchDirectory();

        Filter<Path> fileFilter = selectedExtension.getValue().dirFilter();
        DateRange dateFilter = selectedDate.getValue();
        ExternalFileSorter sorter = selectedSort.getValue();

        findUnlinkedFilesTask = new UnlinkedFilesCrawler(directory, fileFilter, dateFilter, sorter, bibDatabase, preferences.getFilePreferences()).onRunning(() -> {
            progressValueProperty.set(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressTextProperty.set(Localization.lang("Searching file system..."));
            taskActiveProperty.set(true);
            treeRootProperty.set(Optional.empty());
        }).onFinished(() -> {
            progressValueProperty.set(0);
            taskActiveProperty.set(false);
        }).onSuccess(root -> treeRootProperty.set(Optional.of(root)));

        findUnlinkedFilesTask.executeWith(taskExecutor);
    }

    /* ================= IMPORT (MANUAL LINKING PREP) ================= */

    public void startImport() {
        List<Path> selectedFiles = checkedFileListProperty.stream().map(TreeItem::getValue).map(FileNodeViewModel::getPath).filter(Files::isRegularFile).toList();

        if (selectedFiles.isEmpty()) {
            LOGGER.warn("There are no valid files checked for import");
            return;
        }

        resultList.clear();

        Map<Path, List<BibEntry>> map = new HashMap<>();

        for (Path file : selectedFiles) {
            List<BibEntry> candidates = findCandidateEntries(file);
            map.put(file, candidates);

            LOGGER.info("Found {} candidate entries for file {}", candidates.size(), file.getFileName());
        }

        candidateEntriesMap.set(map);

        // Check if any files have matching candidates
        boolean hasMatches = map.values().stream().anyMatch(list -> !list.isEmpty());

        if (hasMatches) {
            // Show manual linking dialog
            ManualLinkingDialog dialog = new ManualLinkingDialog(map);
            Optional<Map<Path, BibEntry>> result = dialogService.showCustomDialogAndWait(dialog);

            if (result.isPresent()) {
                Map<Path, BibEntry> selections = result.get();

                // Clear previous linked entries
                linkedEntries.clear();

                // Link each file to its selected entry
                for (Map.Entry<Path, BibEntry> entry : selections.entrySet()) {
                    Path file = entry.getKey();
                    BibEntry selectedEntry = entry.getValue();

                    try {
                        // Use the ImportHandler's file linker to attach the file
                        importHandler.getFileLinker().linkFilesToEntry(selectedEntry, List.of(file));

                        // Store the linked entry for "jump to" functionality
                        linkedEntries.put(file.toString(), selectedEntry);

                        resultList.add(new ImportFilesResultItemViewModel(file, true, Localization.lang("Successfully linked to entry: %0", selectedEntry.getCitationKey().orElse("unknown"))));
                        LOGGER.info("Linked file {} to entry {}", file.getFileName(), selectedEntry.getCitationKey().orElse("unknown"));
                    } catch (Exception e) {
                        resultList.add(new ImportFilesResultItemViewModel(file, false, Localization.lang("Failed to link file to entry: %0", e.getMessage())));
                        LOGGER.warn("Failed to link file {} to entry {}", file.getFileName(), selectedEntry.getCitationKey().orElse("unknown"), e);
                    }
                }
            } else {
                LOGGER.info("User cancelled manual linking");
            }
        } else {
            // No candidates found
            dialogService.notify(Localization.lang("No matching entries found for the selected files."));
        }
    }

    /* ================= HELPER ================= */

    private List<BibEntry> findCandidateEntries(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        // Remove extension
        String fileNameWithoutExt = fileName.replaceFirst("\\.[^.]+$", "");

        List<BibEntry> allEntries = bibDatabase.getDatabase().getEntries();

        // Strategy 1: Match by citation key in filename
        List<BibEntry> citationKeyMatches = allEntries.stream().filter(entry -> entry.getCitationKey().map(key -> fileNameWithoutExt.contains(key.toLowerCase())).orElse(false)).toList();

        if (!citationKeyMatches.isEmpty()) {
            LOGGER.debug("Found {} citation key matches for file {}", citationKeyMatches.size(), fileName);
            return citationKeyMatches;
        }

        // Strategy 2: Match by title similarity (if title exists in filename)
        List<BibEntry> titleMatches = allEntries.stream().filter(entry -> entry.getTitle().isPresent()).filter(entry -> {
            String title = entry.getTitle().get().toLowerCase();
            // Extract words from title (3+ chars) and check if they appear in filename
            String[] titleWords = title.split("\\W+");
            long matchingWords = 0;
            for (String word : titleWords) {
                if (word.length() >= 3 && fileNameWithoutExt.contains(word.toLowerCase())) {
                    matchingWords++;
                }
            }
            // Consider it a match if 30% or more of significant title words are in filename
            return titleWords.length > 0 && ((double) matchingWords / titleWords.length) >= 0.3;
        }).toList();

        if (!titleMatches.isEmpty()) {
            LOGGER.debug("Found {} title matches for file {}", titleMatches.size(), fileName);
            return titleMatches;
        }

        // Strategy 3: If no matches, return all entries so user can still choose
        LOGGER.debug("No specific matches found for file {}, showing all entries", fileName);
        return allEntries;
    }

    /**
     * Jump to (focus) a linked entry in the main table
     *
     * @param filePath the file path whose linked entry should be focused
     */
    public void jumpToLinkedEntry(String filePath) {
        BibEntry entry = linkedEntries.get(filePath);
        if (entry != null) {
            stateManager.setSelectedEntries(List.of(entry));
            LOGGER.info("Jumped to entry: {}", entry.getCitationKey().orElse("unknown"));
        } else {
            LOGGER.warn("No linked entry found for file: {}", filePath);
        }
    }

    /* ================= EXPORT ================= */

    public void startExport() {
        List<Path> fileList = checkedFileListProperty.stream().map(item -> item.getValue().getPath()).filter(Files::isRegularFile).toList();

        if (fileList.isEmpty()) {
            LOGGER.warn("There are no valid files checked for export");
            return;
        }

        FileDialogConfiguration config = new FileDialogConfiguration.Builder().withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory()).addExtensionFilter(StandardFileType.TXT).withDefaultExtension(StandardFileType.TXT).build();

        Optional<Path> exportPath = dialogService.showFileSaveDialog(config);
        if (exportPath.isEmpty()) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(exportPath.get(), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            for (Path file : fileList) {
                writer.write(file.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.error("Error exporting", e);
        }
    }

    /* ================= PROPERTIES ================= */

    public ObservableList<ImportFilesResultItemViewModel> resultTableItems() {
        return resultList;
    }

    public ObjectProperty<Map<Path, List<BibEntry>>> candidateEntriesMapProperty() {
        return candidateEntriesMap;
    }

    public ObjectProperty<Optional<FileNodeViewModel>> treeRootProperty() {
        return treeRootProperty;
    }

    public SimpleListProperty<TreeItem<FileNodeViewModel>> checkedFileListProperty() {
        return checkedFileListProperty;
    }

    public StringProperty directoryPathProperty() {
        return directoryPath;
    }

    public ValidationStatus directoryPathValidationStatus() {
        return scanDirectoryValidator.getValidationStatus();
    }

    public BooleanProperty taskActiveProperty() {
        return taskActiveProperty;
    }

    public DoubleProperty progressValueProperty() {
        return progressValueProperty;
    }

    public StringProperty progressTextProperty() {
        return progressTextProperty;
    }

    public ObservableList<FileExtensionViewModel> getFileFilters() {
        return fileFilterList;
    }

    public ObservableList<DateRange> getDateFilters() {
        return dateFilterList;
    }

    public ObservableList<ExternalFileSorter> getSorters() {
        return fileSortList;
    }

    public ObjectProperty<FileExtensionViewModel> selectedExtensionProperty() {
        return selectedExtension;
    }

    public ObjectProperty<DateRange> selectedDateProperty() {
        return selectedDate;
    }

    public ObjectProperty<ExternalFileSorter> selectedSortProperty() {
        return selectedSort;
    }

    public void browseFileDirectory() {
        DirectoryDialogConfiguration config = new DirectoryDialogConfiguration.Builder().withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory()).build();

        dialogService.showDirectorySelectionDialog(config).ifPresent(path -> directoryPath.set(path.toString()));
    }

    public void cancelTasks() {
        if (findUnlinkedFilesTask != null) {
            findUnlinkedFilesTask.cancel();
        }
    }

    private Path getSearchDirectory() {
        Path dir = Path.of(directoryPath.get());
        if (!Files.isDirectory(dir)) {
            dir = Path.of(System.getProperty("user.dir"));
            directoryPath.set(dir.toString());
        }
        return dir;
    }

    /* ================= INNER CLASSES FOR MANUAL LINKING ================= */

    /**
     * Inner class: Dialog for manual file-to-entry linking
     */
    private static class ManualLinkingDialog extends BaseDialog<Map<Path, BibEntry>> {
        private final TableView<FileLinkingItem> linkingTable;
        private final Button linkButton;
        private final Map<Path, BibEntry> selections = new HashMap<>();
        private final List<FileLinkingItem> items;

        public ManualLinkingDialog(Map<Path, List<BibEntry>> candidateEntriesMap) {
            this.setTitle(Localization.lang("Manual File Linking"));

            // Create items for files that have candidates
            this.items = candidateEntriesMap.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(entry -> new FileLinkingItem(entry.getKey(), entry.getValue())).toList();

            VBox content = createContent();
            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            linkButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            linkButton.setText(Localization.lang("Link Files"));
            linkButton.setDisable(true);

            linkingTable = createTable();
            ((VBox) content.getChildren().get(3)).getChildren().add(linkingTable);
            VBox.setVgrow(linkingTable, Priority.ALWAYS);

            setResultConverter(button -> {
                if (button == ButtonType.OK && selections.size() == items.size()) {
                    return selections;
                }
                return null;
            });
        }

        private VBox createContent() {
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setPrefWidth(800);
            vbox.setPrefHeight(600);

            Label headerLabel = new Label(Localization.lang("Manual linking required"));
            headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            Label instructionLabel = new Label(Localization.lang("Select the BibTeX entry to link to each file:"));
            instructionLabel.setWrapText(true);

            HBox infoBox = new HBox(5);
            Label infoIcon = new Label("ℹ");
            infoIcon.setStyle("-fx-font-size: 14px;");
            Label infoText = new Label(Localization.lang("Select an entry from the dropdown for each file before linking."));
            infoText.setWrapText(true);
            HBox.setHgrow(infoText, Priority.ALWAYS);
            infoBox.getChildren().addAll(infoIcon, infoText);

            VBox tableContainer = new VBox();
            VBox.setVgrow(tableContainer, Priority.ALWAYS);

            vbox.getChildren().addAll(headerLabel, instructionLabel, new Separator(), tableContainer, infoBox);

            return vbox;
        }

        private TableView<FileLinkingItem> createTable() {
            TableView<FileLinkingItem> table = new TableView<>();
            table.setItems(FXCollections.observableArrayList(items));

            // File column
            TableColumn<FileLinkingItem, String> fileColumn = new TableColumn<>(Localization.lang("File"));
            fileColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFile().getFileName().toString()));
            fileColumn.setPrefWidth(250);

            // Candidates column
            TableColumn<FileLinkingItem, String> candidatesColumn = new TableColumn<>(Localization.lang("Candidates"));
            candidatesColumn.setCellValueFactory(cellData -> {
                int count = cellData.getValue().getCandidates().size();
                return new SimpleStringProperty(count + " " + (count == 1 ? Localization.lang("entry") : Localization.lang("entries")));
            });
            candidatesColumn.setPrefWidth(150);

            // Selection column
            TableColumn<FileLinkingItem, BibEntry> selectionColumn = new TableColumn<>(Localization.lang("Select Entry"));
            selectionColumn.setCellFactory(column -> new SelectionCell());
            selectionColumn.setPrefWidth(400);

            table.getColumns().addAll(fileColumn, candidatesColumn, selectionColumn);
            return table;
        }

        private static class BibEntryCell extends javafx.scene.control.ListCell<BibEntry> {
            @Override
            protected void updateItem(BibEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                } else {
                    StringBuilder display = new StringBuilder();
                    entry.getCitationKey().ifPresent(key -> display.append("[").append(key).append("] "));
                    entry.getTitle().ifPresent(title -> {
                        String shortTitle = title.length() > 50 ? title.substring(0, 47) + "..." : title;
                        display.append(shortTitle);
                    });
                    if (display.length() == 0) {
                        display.append(Localization.lang("Untitled entry"));
                    }
                    setText(display.toString());
                }
            }
        }

        private class SelectionCell extends TableCell<FileLinkingItem, BibEntry> {
            private final ComboBox<BibEntry> comboBox = new ComboBox<>();

            public SelectionCell() {
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.setButtonCell(new BibEntryCell());
                comboBox.setCellFactory(listView -> new BibEntryCell());

                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && getTableRow() != null) {
                        FileLinkingItem item = getTableRow().getItem();
                        if (item != null) {
                            selections.put(item.getFile(), newVal);
                            linkButton.setDisable(selections.size() != items.size());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(BibEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    FileLinkingItem item = getTableRow().getItem();
                    comboBox.setItems(FXCollections.observableArrayList(item.getCandidates()));
                    comboBox.setPromptText(Localization.lang("Select entry..."));
                    setGraphic(comboBox);
                }
            }
        }
    }

    /**
     * Inner class: Represents a file with its candidate entries
     */
    private static class FileLinkingItem {
        private final Path file;
        private final List<BibEntry> candidates;

        public FileLinkingItem(Path file, List<BibEntry> candidates) {
            this.file = file;
            this.candidates = candidates;
        }

        public Path getFile() {
            return file;
        }

        public List<BibEntry> getCandidates() {
            return candidates;
        }
    }
}
