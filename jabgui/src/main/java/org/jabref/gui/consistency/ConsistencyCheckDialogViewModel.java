package org.jabref.gui.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.types.EntryType;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialogViewModel.class);

    private final BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;

    private final List<Field> allReportedFields;
    private final ObservableList<ConsistencyMessage> tableData = FXCollections.observableArrayList();
    private final StringProperty selectedEntryType = new SimpleStringProperty();

    public ConsistencyCheckDialogViewModel(DialogService dialogService,
                                           GuiPreferences preferences,
                                           BibEntryTypesManager entryTypesManager,
                                           BibliographyConsistencyCheck.Result result) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.result = result;

        this.allReportedFields = result.entryTypeToResultMap().values().stream()
                                       .flatMap(entryTypeResult -> entryTypeResult.fields().stream())
                                       .sorted(Comparator.comparing(Field::getName))
                                       .distinct()
                                       .toList();

        result.entryTypeToResultMap().entrySet().stream()
              .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
              .forEach(Unchecked.consumer(this::writeMapEntry));
    }

    public StringProperty selectedEntryTypeProperty() {
        return selectedEntryType;
    }

    public List<String> getEntryTypes() {
        List<String> entryTypes = new ArrayList<>();
        result.entryTypeToResultMap().forEach((entrySet, _) -> entryTypes.add(entrySet.toString()));
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

    private void writeMapEntry(Map.Entry<EntryType, BibliographyConsistencyCheck.EntryTypeResult> mapEntry) {
        BibDatabaseMode bibDatabaseMode = BibDatabaseMode.BIBTEX;
        String entryType = mapEntry.getKey().getDisplayName();

        Optional<BibEntryType> bibEntryType = this.entryTypesManager.enrich(mapEntry.getKey(), bibDatabaseMode);
        Set<Field> requiredFields = bibEntryType
                .map(BibEntryType::getRequiredFields)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(orFields -> orFields.getFields().stream())
                .collect(Collectors.toSet());
        Set<Field> optionalFields = bibEntryType
                .map(BibEntryType::getOptionalFields)
                .stream()
                .flatMap(Collection::stream)
                .map(BibField::field)
                .collect(Collectors.toSet());

        BibliographyConsistencyCheck.EntryTypeResult entries = mapEntry.getValue();
        SequencedCollection<BibEntry> bibEntries = entries.sortedEntries();

        bibEntries.forEach(Unchecked.consumer(bibEntry ->
                writeBibEntry(bibEntry, entryType, requiredFields, optionalFields)
        ));
    }

    private void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) {
        List<String> theRecord = getFindingsAsList(bibEntry, entryType, requiredFields, optionalFields);
        List<String> message = new ArrayList<>();
        for (String s : theRecord) {
            String modifiedString = s.replaceAll("\\s+", " ");
            message.add(modifiedString);
        }
        tableData.add(new ConsistencyMessage(message, bibEntry));
    }

    private List<String> getFindingsAsList(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) {
        List<String> result = new ArrayList<>(allReportedFields.size() + 2);
        result.add(entryType);
        result.add(bibEntry.getCitationKey().orElse(""));
        allReportedFields.forEach(field ->
                result.add(bibEntry.getField(field).map(_ -> {
                    if (requiredFields.contains(field)) {
                        return ConsistencySymbol.REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY.getText();
                    } else if (optionalFields.contains(field)) {
                        return ConsistencySymbol.OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY.getText();
                    } else {
                        return ConsistencySymbol.UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY.getText();
                    }
                }).orElse(ConsistencySymbol.UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY.getText()))
        );
        return result;
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
             BibliographyConsistencyCheckResultTxtWriter bibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true)) {
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
             BibliographyConsistencyCheckResultCsvWriter bibliographyConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            bibliographyConsistencyCheckResultCsvWriter.writeFindings();
        } catch (IOException e) {
            LOGGER.error(Localization.lang("Problem when exporting file"), e);
            dialogService.showErrorDialogAndWait(Localization.lang("Failed to export file."));
        }
    }
}
