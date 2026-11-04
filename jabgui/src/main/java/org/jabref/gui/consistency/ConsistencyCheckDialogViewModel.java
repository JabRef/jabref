package org.jabref.gui.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.bst.BstVM;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.types.EntryType;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialogViewModel.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final BibDatabaseMode bibDatabaseMode;
    private final TaskExecutor taskExecutor;

    private List<Field> allReportedFields;
    private final List<BibEntry> sourceEntries;
    private final Map<BibEntry, BibEntry> originalEntries = new IdentityHashMap<>();
    private final ObservableList<String> allEntryTypes = FXCollections.observableArrayList();
    private final IntegerProperty reportRevision = new SimpleIntegerProperty();
    private final StringProperty auxFileName = new SimpleStringProperty("");
    private final ConsistencyReport fullReport;
    private long auxLoadGeneration;
    private final ObservableList<ConsistencyMessage> tableData = FXCollections.observableArrayList();
    private final Map<String, List<Field>> entryTypeToReportedFields = new HashMap<>();
    private final FilteredList<String> entryTypes;
    private final StringProperty selectedEntryType = new SimpleStringProperty();
    private final BooleanProperty restrictToBstFields = new SimpleBooleanProperty(false);
    private final StringProperty bstStyleName = new SimpleStringProperty("");
    private Predicate<Field> selectedFields = _ -> true;
    private long bstLoadGeneration;
    private final ObjectBinding<Predicate<Field>> shownFields = Bindings.createObjectBinding(() -> {
        if (!restrictToBstFields.get()) {
            return _ -> true;
        }
        return selectedFields;
    }, restrictToBstFields, bstStyleName);

    public ConsistencyCheckDialogViewModel(DialogService dialogService,
                                           GuiPreferences preferences,
                                           BibEntryTypesManager entryTypesManager,
                                           BibDatabaseMode bibDatabaseMode,
                                           BibliographyConsistencyCheck.Result result,
                                           TaskExecutor taskExecutor,
                                           List<BibEntry> entries) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = new BibEntryTypesManager();
        this.entryTypesManager.addCustomOrModifiedTypes(List.copyOf(entryTypesManager.getAllCustomizedTypes(bibDatabaseMode)), bibDatabaseMode);
        this.bibDatabaseMode = bibDatabaseMode;
        this.taskExecutor = taskExecutor;
        Map<BibEntry, BibEntry> snapshots = new IdentityHashMap<>();
        this.sourceEntries = entries.stream().map(entry -> {
            BibEntry snapshot = new BibEntry(entry);
            snapshots.put(entry, snapshot);
            originalEntries.put(snapshot, entry);
            return snapshot;
        }).toList();
        this.result = snapshotResult(result, snapshots);
        this.entryTypes = new FilteredList<>(allEntryTypes);
        this.fullReport = prepareReport(this.result);
        applyReport(fullReport);
        // An entry type without a visible column has no differences to show
        this.entryTypes.predicateProperty().bind(shownFields.map(shown -> entryType -> entryTypeToReportedFields.get(entryType).stream().anyMatch(shown)));
        this.entryTypes.addListener((ListChangeListener<String>) _ -> {
            if (!entryTypes.contains(selectedEntryType.get())) {
                selectedEntryType.set(entryTypes.isEmpty() ? null : entryTypes.getFirst());
            }
        });
    }

    public StringProperty selectedEntryTypeProperty() {
        return selectedEntryType;
    }

    // [impl->req~ux.consistency.export-snapshot~1]
    private BibliographyConsistencyCheck.Result snapshotResult(BibliographyConsistencyCheck.Result result, Map<BibEntry, BibEntry> snapshots) {
        Map<EntryType, BibliographyConsistencyCheck.EntryTypeResult> snapshotResults = new LinkedHashMap<>();
        result.entryTypeToResultMap().forEach((entryType, entryTypeResult) -> {
            List<BibEntry> snapshotEntries = entryTypeResult.sortedEntries().stream().map(snapshots::get).toList();
            snapshotResults.put(entryType, new BibliographyConsistencyCheck.EntryTypeResult(Set.copyOf(entryTypeResult.fields()), snapshotEntries));
        });
        return new BibliographyConsistencyCheck.Result(snapshotResults);
    }

    private ConsistencyReport prepareReport(BibliographyConsistencyCheck.Result findings) {
        try (TableDataWriter writer = new TableDataWriter(findings)) {
            writer.writeFindings();
            return new ConsistencyReport(findings, List.copyOf(writer.getAllReportedFields()), List.copyOf(writer.messages), Map.copyOf(writer.reportedFieldsByType));
        }
    }

    private void applyReport(ConsistencyReport report) {
        result = report.result();
        allReportedFields = report.fields();
        entryTypeToReportedFields.clear();
        entryTypeToReportedFields.putAll(report.fieldsByType());
        tableData.clear();
        allEntryTypes.setAll(result.entryTypeToResultMap().keySet().stream().map(EntryType::getDisplayName).sorted().toList());
        reportRevision.set(reportRevision.get() + 1);
        tableData.setAll(report.messages());
        if (!entryTypes.contains(selectedEntryType.get())) {
            selectedEntryType.set(entryTypes.isEmpty() ? null : entryTypes.getFirst());
        }
    }

    public ReadOnlyIntegerProperty reportRevisionProperty() {
        return reportRevision;
    }

    public ReadOnlyStringProperty auxFileNameProperty() {
        return auxFileName;
    }

    public void browseAuxFile() {
        FileDialogConfiguration configuration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .build();
        dialogService.showFileOpenDialog(configuration).ifPresent(this::loadAuxFile);
    }

    // [impl->req~ux.consistency.aux-entry-selection~1]
    @VisibleForTesting
    void loadAuxFile(Path auxFile) {
        long generation = ++auxLoadGeneration;
        BackgroundTask.wrap(() -> {
            Set<String> keys = new DefaultAuxParser(new BibDatabase()).parseStrict(auxFile).getUniqueKeys();
            List<BibEntry> selectedEntries = sourceEntries.stream()
                    .filter(entry -> keys.contains("*") || entry.getCitationKey().filter(keys::contains).isPresent())
                    .toList();
            BibliographyConsistencyCheck.Result findings = new BibliographyConsistencyCheck().checkEntries(selectedEntries, bibDatabaseMode, entryTypesManager, (_, _) -> {
            });
            return prepareReport(findings);
        }).onSuccess(report -> {
            if (generation == auxLoadGeneration) {
                applyReport(report);
                auxFileName.set(auxFile.getFileName().toString());
            }
        }).onFailure(exception -> {
            if (generation == auxLoadGeneration) {
                LOGGER.error("Could not read .aux file {}", auxFile, exception);
                dialogService.showErrorDialogAndWait(Localization.lang("Could not read the .aux file."), exception);
            }
        }).executeWith(taskExecutor);
    }

    public void showAllEntries() {
        ++auxLoadGeneration;
        applyReport(fullReport);
        auxFileName.set("");
    }

    @NullMarked
    private record ConsistencyReport(BibliographyConsistencyCheck.Result result, List<Field> fields,
                                     List<ConsistencyMessage> messages, Map<String, List<Field>> fieldsByType) {
    }

    /// The entry types having a visible column
    public ObservableList<String> getEntryTypes() {
        return entryTypes;
    }

    public ObservableList<ConsistencyMessage> getTableData() {
        return tableData;
    }

    public List<Field> getReportedFields() {
        return allReportedFields;
    }

    /// Which field columns are shown: all, or only the fields of the chosen `.bst` style
    public ObjectBinding<Predicate<Field>> shownFieldsProperty() {
        return shownFields;
    }

    public BooleanProperty restrictToBstFieldsProperty() {
        return restrictToBstFields;
    }

    /// File name of the chosen `.bst` style; empty if none was chosen yet
    public ReadOnlyStringProperty bstStyleNameProperty() {
        return bstStyleName;
    }

    // [impl->req~ux.consistency.bst-field-columns~1]
    public void browseBstFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.BST)
                .withDefaultExtension(StandardFileType.BST)
                .build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(this::loadBstFile);
    }

    /// Reads the fields of the given style and restricts the columns to them
    @VisibleForTesting
    void loadBstFile(Path bstFile) {
        long generation = ++bstLoadGeneration;
        BackgroundTask.wrap(() -> new BstVM(bstFile).getFieldSelection())
                      .onSuccess(selection -> {
                          if (generation != bstLoadGeneration) {
                              return;
                          }
                          if (selection.isEmpty()) {
                              LOGGER.error("No ENTRY fields in .bst file {}", bstFile);
                              dialogService.showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."));
                              return;
                          }
                          selectedFields = selection.orElseThrow();
                          bstStyleName.set(bstFile.getFileName().toString());
                          restrictToBstFields.set(true);
                          shownFields.invalidate();
                      })
                      .onFailure(exception -> {
                          if (generation == bstLoadGeneration) {
                              LOGGER.error("Could not read .bst file {}", bstFile, exception);
                              dialogService.showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."), exception);
                          }
                      })
                      .executeWith(taskExecutor);
    }

    public List<String> getColumnNames() {
        List<String> result = new ArrayList<>(allReportedFields.size() + 2); // there are two extra columns
        result.add("Entry Type");
        result.add("CitationKey");
        allReportedFields.forEach(field -> result.add(FieldTextMapper.getDisplayName(field).trim()));
        return result;
    }

    /// Fills [#tableData] using the same columns and cell symbols as the TXT and CSV exports
    private class TableDataWriter extends BibliographyConsistencyCheckResultWriter {

        private final List<ConsistencyMessage> messages = new ArrayList<>();
        private final Map<String, List<Field>> reportedFieldsByType = new HashMap<>();

        TableDataWriter(BibliographyConsistencyCheck.Result findings) {
            super(findings, Writer.nullWriter(), true,
                    ConsistencyCheckDialogViewModel.this.entryTypesManager, ConsistencyCheckDialogViewModel.this.bibDatabaseMode);
        }

        List<Field> getAllReportedFields() {
            return allReportedFields;
        }

        @Override
        public void writeFindings() {
            try {
                super.writeFindings();
            } catch (IOException e) {
                // writeBibEntry does not perform any I/O
                LOGGER.error("Could not collect consistency check findings", e);
            }
        }

        @Override
        protected void writeBibEntry(BibEntry bibEntry, EntryTypeReport entryTypeReport) {
            List<String> message = getFindingsAsList(bibEntry, entryTypeReport).stream()
                                                                               .map(cell -> WHITESPACE.matcher(cell).replaceAll(" "))
                                                                               .toList();
            // [impl->req~ux.consistency.entry-navigation~1]
            messages.add(new ConsistencyMessage(message, originalEntries.getOrDefault(bibEntry, bibEntry)));
            reportedFieldsByType.put(entryTypeReport.displayName(), entryTypeReport.reportedFields());
        }

        @Override
        public void close() {
        }
    }

    protected void startExportAsTxt() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.TXT)
                .withDefaultExtension(StandardFileType.TXT)
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        if (exportPath.isEmpty()) {
            return;
        }

        Path path = exportPath.orElseThrow();
        BibliographyConsistencyCheck.Result exportResult = result;
        // [impl->req~ux.consistency.export-background~1]
        BackgroundTask.wrap(() -> {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path));
                 BibliographyConsistencyCheckResultTxtWriter resultWriter = new BibliographyConsistencyCheckResultTxtWriter(exportResult, writer, true, entryTypesManager, bibDatabaseMode)) {
                resultWriter.writeFindings();
            }
            return path;
        }).onFailure(this::showExportError).executeWith(taskExecutor);
    }

    protected void startExportAsCsv() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.CSV)
                .withDefaultExtension(StandardFileType.CSV)
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        if (exportPath.isEmpty()) {
            return;
        }

        Path path = exportPath.orElseThrow();
        BibliographyConsistencyCheck.Result exportResult = result;
        // [impl->req~ux.consistency.export-background~1]
        BackgroundTask.wrap(() -> {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path));
                 BibliographyConsistencyCheckResultCsvWriter resultWriter = new BibliographyConsistencyCheckResultCsvWriter(exportResult, writer, true, entryTypesManager, bibDatabaseMode)) {
                resultWriter.writeFindings();
            }
            return path;
        }).onFailure(this::showExportError).executeWith(taskExecutor);
    }

    private void showExportError(Exception exception) {
        LOGGER.error("Problem when exporting file", exception);
        dialogService.showErrorDialogAndWait(Localization.lang("Failed to export file."));
    }
}
