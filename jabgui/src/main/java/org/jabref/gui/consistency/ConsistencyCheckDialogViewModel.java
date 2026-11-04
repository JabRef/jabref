package org.jabref.gui.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialogViewModel.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final BibDatabaseMode bibDatabaseMode;
    private final TaskExecutor taskExecutor;

    private final List<Field> allReportedFields;
    private final ObservableList<ConsistencyMessage> tableData = FXCollections.observableArrayList();
    private final StringProperty selectedEntryType = new SimpleStringProperty();
    private final ObjectProperty<Predicate<Field>> shownFields = new SimpleObjectProperty<>(_ -> true);
    private final StringProperty bstStyleName = new SimpleStringProperty("");

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

    public List<String> getEntryTypes() {
        List<String> entryTypes = new ArrayList<>();
        result.entryTypeToResultMap().forEach((entrySet, _) -> entryTypes.add(entrySet.getDisplayName()));
        return entryTypes;
    }

    public ObservableList<ConsistencyMessage> getTableData() {
        return tableData;
    }

    public List<Field> getReportedFields() {
        return allReportedFields;
    }

    /// Which field columns are shown; all unless restricted by [#restrictToBstFields()]
    public ReadOnlyObjectProperty<Predicate<Field>> shownFieldsProperty() {
        return shownFields;
    }

    /// File name of the `.bst` style the columns are restricted to; empty if all columns are shown
    public ReadOnlyStringProperty bstStyleNameProperty() {
        return bstStyleName;
    }

    // [impl->req~ux.consistency.bst-field-columns~1]
    public void restrictToBstFields() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.BST)
                .withDefaultExtension(StandardFileType.BST)
                .build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(this::restrictToBstFields);
    }

    @VisibleForTesting
    void restrictToBstFields(Path bstFile) {
        try {
            Set<String> entryFields = new BstVM(bstFile).getEntryFields();
            shownFields.set(field -> isReadByStyle(entryFields, field));
            bstStyleName.set(bstFile.getFileName().toString());
        } catch (IOException | ParseCancellationException e) {
            LOGGER.error("Could not read .bst file {}", bstFile, e);
            dialogService.showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."), e);
        }
    }

    /// A style reads a field also through its alias or date parts, mirroring [BibEntry#getFieldOrAlias]
    private static boolean isReadByStyle(Set<String> entryFields, Field field) {
        Stream<Field> fieldsSeenByStyle = field == StandardField.DATE
                                          ? Stream.of(StandardField.DATE, StandardField.YEAR, StandardField.MONTH, StandardField.DAY)
                                          : Stream.concat(Stream.of(field), Optional.ofNullable(EntryConverter.FIELD_ALIASES.get(field)).stream());
        return fieldsSeenByStyle.map(seen -> seen.getName().toLowerCase(Locale.ROOT)).anyMatch(entryFields::contains);
    }

    public void showAllFields() {
        shownFields.set(_ -> true);
        bstStyleName.set("");
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
