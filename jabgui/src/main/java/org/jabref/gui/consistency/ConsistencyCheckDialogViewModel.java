package org.jabref.gui.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialogViewModel.class);

    private final BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final BibDatabaseMode bibDatabaseMode;

    private final List<Field> allReportedFields;
    private final ObservableList<ConsistencyMessage> tableData = FXCollections.observableArrayList();
    private final StringProperty selectedEntryType = new SimpleStringProperty();

    public ConsistencyCheckDialogViewModel(DialogService dialogService,
                                           GuiPreferences preferences,
                                           BibEntryTypesManager entryTypesManager,
                                           BibDatabaseMode bibDatabaseMode,
                                           BibliographyConsistencyCheck.Result result) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.bibDatabaseMode = bibDatabaseMode;
        this.result = result;

        TableDataWriter tableDataWriter = new TableDataWriter();
        this.allReportedFields = tableDataWriter.getAllReportedFields();
        tableDataWriter.writeFindings();
    }

    public StringProperty selectedEntryTypeProperty() {
        return selectedEntryType;
    }

    public List<String> getEntryTypes() {
        List<String> entryTypes = new ArrayList<>();
        result.entryTypeToResultMap().forEach((entrySet, _) -> entryTypes.add(entrySet.getDisplayName()));
        return entryTypes;
    }

    public ObservableList<ConsistencyMessage> getTableData() {
        return tableData;
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

        TableDataWriter() {
            super(ConsistencyCheckDialogViewModel.this.result, Writer.nullWriter(), true,
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
                                                                               .map(cell -> cell.replaceAll("\\s+", " "))
                                                                               .toList();
            tableData.add(new ConsistencyMessage(message, bibEntry));
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

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(exportPath.get()));
             BibliographyConsistencyCheckResultTxtWriter bibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true, entryTypesManager, bibDatabaseMode)) {
            bibliographyConsistencyCheckResultTxtWriter.writeFindings();
        } catch (IOException e) {
            LOGGER.error(Localization.lang("Problem when exporting file"), e);
            dialogService.showErrorDialogAndWait(Localization.lang("Failed to export file."));
        }
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

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(exportPath.get()));
             BibliographyConsistencyCheckResultCsvWriter bibliographyConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true, entryTypesManager, bibDatabaseMode)) {
            bibliographyConsistencyCheckResultCsvWriter.writeFindings();
        } catch (IOException e) {
            LOGGER.error(Localization.lang("Problem when exporting file"), e);
            dialogService.showErrorDialogAndWait(Localization.lang("Failed to export file."));
        }
    }
}
