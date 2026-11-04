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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialogViewModel.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<Field> DATE_FIELDS = Set.of(StandardField.DATE, StandardField.YEAR, StandardField.MONTH, StandardField.DAY);

    private final BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final BibDatabaseMode bibDatabaseMode;
    private final TaskExecutor taskExecutor;

    private final List<Field> allReportedFields;
    private final ObservableList<ConsistencyMessage> tableData = FXCollections.observableArrayList();
    private final Map<String, List<Field>> entryTypeToReportedFields = new HashMap<>();
    private final FilteredList<String> entryTypes;
    private final StringProperty selectedEntryType = new SimpleStringProperty();
    private final BooleanProperty restrictToBstFields = new SimpleBooleanProperty(false);
    private final StringProperty bstStyleName = new SimpleStringProperty("");
    private Set<String> bstEntryFields = Set.of();
    private long bstLoadGeneration;
    private final ObjectBinding<Predicate<Field>> shownFields = Bindings.createObjectBinding(() -> {
        if (!restrictToBstFields.get()) {
            return _ -> true;
        }
        Set<String> entryFields = bstEntryFields;
        return field -> isReadByStyle(entryFields, field);
    }, restrictToBstFields, bstStyleName);

    public ConsistencyCheckDialogViewModel(DialogService dialogService,
                                           GuiPreferences preferences,
                                           BibEntryTypesManager entryTypesManager,
                                           BibDatabaseMode bibDatabaseMode,
                                           BibliographyConsistencyCheck.Result result,
                                           TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = new BibEntryTypesManager();
        this.entryTypesManager.addCustomOrModifiedTypes(List.copyOf(entryTypesManager.getAllCustomizedTypes(bibDatabaseMode)), bibDatabaseMode);
        this.bibDatabaseMode = bibDatabaseMode;
        this.taskExecutor = taskExecutor;
        Map<BibEntry, BibEntry> originalEntries = new IdentityHashMap<>();
        this.result = snapshotResult(result, originalEntries);

        try (TableDataWriter tableDataWriter = new TableDataWriter(originalEntries)) {
            this.allReportedFields = tableDataWriter.getAllReportedFields();
            tableDataWriter.writeFindings();
        }

        List<String> allEntryTypes = this.result.entryTypeToResultMap().keySet().stream().map(EntryType::getDisplayName).sorted().toList();
        this.entryTypes = new FilteredList<>(FXCollections.observableList(allEntryTypes));
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
    private BibliographyConsistencyCheck.Result snapshotResult(BibliographyConsistencyCheck.Result result, Map<BibEntry, BibEntry> originalEntries) {
        Map<EntryType, BibliographyConsistencyCheck.EntryTypeResult> snapshotResults = new LinkedHashMap<>();
        result.entryTypeToResultMap().forEach((entryType, entryTypeResult) -> {
            List<BibEntry> snapshots = entryTypeResult.sortedEntries().stream().map(entry -> {
                BibEntry snapshot = new BibEntry(entry);
                originalEntries.put(snapshot, entry);
                return snapshot;
            }).toList();
            snapshotResults.put(entryType, new BibliographyConsistencyCheck.EntryTypeResult(Set.copyOf(entryTypeResult.fields()), snapshots));
        });
        return new BibliographyConsistencyCheck.Result(snapshotResults);
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
        BackgroundTask.wrap(() -> new BstVM(bstFile).getEntryFields())
                      .onSuccess(entryFields -> {
                          if (generation != bstLoadGeneration) {
                              return;
                          }
                          if (entryFields.isEmpty()) {
                              LOGGER.error("No ENTRY fields in .bst file {}", bstFile);
                              dialogService.showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."));
                              return;
                          }
                          bstEntryFields = entryFields;
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

    /// A style reads a field also through its alias, mirroring [BibEntry#getFieldOrAlias]: `journal` is read from `journaltitle`,
    /// `date` from `year`, `month` and `day`, and the other way round.
    private static boolean isReadByStyle(Set<String> entryFields, Field field) {
        Stream<Field> fieldsSeenByStyle = DATE_FIELDS.contains(field)
                                          ? DATE_FIELDS.stream()
                                          : Stream.concat(Stream.of(field), Optional.ofNullable(EntryConverter.FIELD_ALIASES.get(field)).stream());
        return fieldsSeenByStyle.map(seen -> seen.getName().toLowerCase(Locale.ROOT)).anyMatch(entryFields::contains);
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

        private final Map<BibEntry, BibEntry> originalEntries;

        TableDataWriter(Map<BibEntry, BibEntry> originalEntries) {
            super(ConsistencyCheckDialogViewModel.this.result, Writer.nullWriter(), true,
                    ConsistencyCheckDialogViewModel.this.entryTypesManager, ConsistencyCheckDialogViewModel.this.bibDatabaseMode);
            this.originalEntries = originalEntries;
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
            tableData.add(new ConsistencyMessage(message, originalEntries.getOrDefault(bibEntry, bibEntry)));
            entryTypeToReportedFields.put(entryTypeReport.displayName(), entryTypeReport.reportedFields());
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
        // [impl->req~ux.consistency.export-background~1]
        BackgroundTask.wrap(() -> {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path));
                 BibliographyConsistencyCheckResultTxtWriter resultWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true, entryTypesManager, bibDatabaseMode)) {
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
        // [impl->req~ux.consistency.export-background~1]
        BackgroundTask.wrap(() -> {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path));
                 BibliographyConsistencyCheckResultCsvWriter resultWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true, entryTypesManager, bibDatabaseMode)) {
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
