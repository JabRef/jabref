package org.jabref.gui.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.SpecialField;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialogViewModel.class);

    private BibliographyConsistencyCheck.Report report;
    private final BibliographyConsistencyCheck.Snapshot snapshot;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;

    private List<Field> allReportedFields;
    private final ObservableList<String> allEntryTypes = FXCollections.observableArrayList();
    private final IntegerProperty reportRevision = new SimpleIntegerProperty();
    private final StringProperty auxFileName = new SimpleStringProperty("");
    private final BooleanProperty restrictToAuxEntries = new SimpleBooleanProperty(false);
    private BibliographyConsistencyCheck.@Nullable Report auxReport;
    private final BibliographyConsistencyCheck.Report fullReport;
    private long auxLoadGeneration;
    private AtomicBoolean auxCancelled = new AtomicBoolean();
    private @Nullable Future<?> auxFuture;
    private AtomicBoolean bstCancelled = new AtomicBoolean();
    private @Nullable Future<?> bstFuture;
    private final ObservableList<ConsistencyMessage> tableData = FXCollections.observableArrayList();
    private final Map<String, List<Field>> entryTypeToReportedFields = new HashMap<>();
    private final FilteredList<String> entryTypes;
    private final StringProperty selectedEntryType = new SimpleStringProperty();
    private final BooleanProperty restrictToBstFields = new SimpleBooleanProperty(false);
    private final StringProperty bstStyleName = new SimpleStringProperty("");
    private Predicate<Field> selectedFields = _ -> true;
    private long bstLoadGeneration;
    private long fieldDisplayGeneration;
    private final ObjectBinding<Predicate<Field>> shownFields = Bindings.createObjectBinding(() -> {
        if (!restrictToBstFields.get()) {
            return _ -> true;
        }
        return selectedFields;
    }, restrictToBstFields, bstStyleName);

    public ConsistencyCheckDialogViewModel(DialogService dialogService,
                                           GuiPreferences preferences,
                                           BibliographyConsistencyCheck.Snapshot snapshot,
                                           TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.snapshot = snapshot;
        this.taskExecutor = taskExecutor;
        this.entryTypes = new FilteredList<>(allEntryTypes);
        this.fullReport = snapshot.check(Set.of("*"), Thread.currentThread()::isInterrupted);
        this.restrictToAuxEntries.addListener((_, _, restrict) -> switchEntrySelection(restrict));
        this.restrictToBstFields.addListener((_, _, _) -> fieldDisplayGeneration++);
        applyReport(fullReport);
        // An entry type without a visible column has no differences to show
        this.entryTypes.predicateProperty().bind(shownFields.map(shown -> entryType -> entryTypeToReportedFields.getOrDefault(entryType, List.of()).stream()
                                                                                                                .filter(field -> !(field instanceof SpecialField)).anyMatch(shown)));
        this.entryTypes.addListener((ListChangeListener<String>) _ -> selectVisibleEntryType());
        selectVisibleEntryType();
    }

    public StringProperty selectedEntryTypeProperty() {
        return selectedEntryType;
    }

    public boolean hasFindings() {
        return !fullReport.messages().isEmpty();
    }

    private void selectVisibleEntryType() {
        if (!entryTypes.contains(selectedEntryType.get())) {
            selectedEntryType.set(firstVisibleEntryType());
        }
    }

    private @Nullable String firstVisibleEntryType() {
        return entryTypes.isEmpty() ? null : entryTypes.getFirst();
    }

    private void applyReport(BibliographyConsistencyCheck.Report report) {
        this.report = report;
        allReportedFields = report.fields();
        entryTypeToReportedFields.clear();
        entryTypeToReportedFields.putAll(report.fieldsByType());
        tableData.clear();
        allEntryTypes.setAll(report.fieldsByType().keySet().stream().sorted().toList());
        reportRevision.set(reportRevision.get() + 1);
        tableData.setAll(report.messages());
        selectVisibleEntryType();
    }

    public ReadOnlyIntegerProperty reportRevisionProperty() {
        return reportRevision;
    }

    public ReadOnlyStringProperty auxFileNameProperty() {
        return auxFileName;
    }

    public BooleanProperty restrictToAuxEntriesProperty() {
        return restrictToAuxEntries;
    }

    private void switchEntrySelection(boolean restrict) {
        if (restrict) {
            if (auxReport != null) {
                applyReport(auxReport);
            }
        } else {
            // A pending .aux load must not re-enable the restriction
            cancelAuxLoad();
            ++auxLoadGeneration;
            if (report != fullReport) {
                applyReport(fullReport);
            }
        }
    }

    /// Uses the `.aux` file next to the library as default selection, if there is exactly one
    public void useAuxFileNextTo(Path bibFile) {
        loadAuxFile(() -> findAuxFileNextTo(bibFile), exception -> LOGGER.warn("Could not load a default .aux file next to {}", bibFile, exception));
    }

    private Optional<Path> findAuxFileNextTo(Path bibFile) throws IOException {
        Path directory = bibFile.toAbsolutePath().getParent();
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> auxFiles = files.filter(Files::isRegularFile)
                                       .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".aux"))
                                       .limit(2).toList();
            return auxFiles.size() == 1 ? Optional.of(auxFiles.getFirst()) : Optional.empty();
        }
    }

    public void browseAuxFile() {
        FileDialogConfiguration configuration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .build();
        dialogService.showFileOpenDialog(configuration).ifPresent(this::loadAuxFile);
    }

    // [impl->req~ux.consistency.aux-entry-selection~2]
    @VisibleForTesting
    void loadAuxFile(Path auxFile) {
        loadAuxFile(() -> Optional.of(auxFile), exception -> {
            LOGGER.error("Could not read .aux file {}", auxFile, exception);
            dialogService.showErrorDialogAndWait(Localization.lang("Could not read the .aux file."), exception);
        });
    }

    private void loadAuxFile(Callable<Optional<Path>> fileSelection, Consumer<Exception> onFailure) {
        cancelAuxLoad();
        long generation = ++auxLoadGeneration;
        AtomicBoolean cancelled = new AtomicBoolean();
        auxCancelled = cancelled;
        auxFuture = BackgroundTask.wrap(() -> {
            checkCancelled(cancelled);
            Optional<Path> selectedFile = fileSelection.call();
            if (selectedFile.isEmpty()) {
                return Optional.<AuxSelection>empty();
            }
            Path auxFile = selectedFile.orElseThrow();
            Set<String> keys = new DefaultAuxParser(new BibDatabase()).parseStrict(auxFile, cancelled::get).getUniqueKeys();
            BibliographyConsistencyCheck.Report findings = snapshot.check(keys, () -> cancelled.get() || Thread.currentThread().isInterrupted());
            return Optional.of(new AuxSelection(auxFile.getFileName().toString(), findings));
        }).onSuccess(result -> {
            if (!cancelled.get() && generation == auxLoadGeneration) {
                result.ifPresent(selection -> {
                    auxReport = selection.report();
                    auxFileName.set(selection.fileName());
                    if (restrictToAuxEntries.get()) {
                        applyReport(selection.report());
                    } else {
                        restrictToAuxEntries.set(true);
                    }
                });
            }
        }).onFailure(exception -> {
            if (!cancelled.get() && generation == auxLoadGeneration) {
                onFailure.accept(exception);
            }
        }).executeWith(taskExecutor);
    }

    @NullMarked
    private record AuxSelection(String fileName, BibliographyConsistencyCheck.Report report) {
    }

    private void cancelAuxLoad() {
        auxCancelled.set(true);
        if (auxFuture != null) {
            auxFuture.cancel(true);
            auxFuture = null;
        }
    }

    private void cancelBstLoad() {
        bstCancelled.set(true);
        if (bstFuture != null) {
            bstFuture.cancel(true);
            bstFuture = null;
        }
    }

    public void cancelPendingLoads() {
        cancelAuxLoad();
        cancelBstLoad();
    }

    private static void checkCancelled(AtomicBoolean cancelled) {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
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

    // [impl->req~ux.consistency.bst-field-columns~2]
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
        cancelBstLoad();
        long generation = ++bstLoadGeneration;
        long displayGeneration = fieldDisplayGeneration;
        AtomicBoolean cancelled = new AtomicBoolean();
        bstCancelled = cancelled;
        bstFuture = BackgroundTask.wrap(() -> {
                                      checkCancelled(cancelled);
                                      return new BstVM(bstFile).getFieldSelection();
                                  })
                                  .onSuccess(selection -> {
                                      if (cancelled.get() || generation != bstLoadGeneration) {
                                          return;
                                      }
                                      if (selection.isEmpty()) {
                                          LOGGER.error("No ENTRY fields in .bst file {}", bstFile);
                                          dialogService.showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."));
                                          return;
                                      }
                                      selectedFields = selection.orElseThrow();
                                      bstStyleName.set(bstFile.getFileName().toString());
                                      if (displayGeneration == fieldDisplayGeneration) {
                                          restrictToBstFields.set(true);
                                      }
                                      shownFields.invalidate();
                                  })
                                  .onFailure(exception -> {
                                      if (!cancelled.get() && generation == bstLoadGeneration) {
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
        BibliographyConsistencyCheck.Report exportReport = report;
        // [impl->req~ux.consistency.export-background~1]
        BackgroundTask.wrap(() -> {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path))) {
                exportReport.writeTxt(writer);
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
        BibliographyConsistencyCheck.Report exportReport = report;
        // [impl->req~ux.consistency.export-background~1]
        BackgroundTask.wrap(() -> {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path))) {
                exportReport.writeCsv(writer);
            }
            return path;
        }).onFailure(this::showExportError).executeWith(taskExecutor);
    }

    private void showExportError(Exception exception) {
        LOGGER.error("Problem when exporting file", exception);
        dialogService.showErrorDialogAndWait(Localization.lang("Failed to export file."));
    }
}
