package org.jabref.gui.entryeditor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultLatexParser;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.texparser.LatexParserResults;
import org.jabref.model.util.DirectoryMonitorManager;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatexCitationsTabViewModel extends AbstractViewModel {

    public enum Status {
        IN_PROGRESS,
        CITATIONS_FOUND,
        NO_RESULTS,
        ERROR
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LatexCitationsTabViewModel.class);
    private static final String TEX_EXT = ".tex";
    private static final IOFileFilter FILE_FILTER = FileFilterUtils.or(FileFilterUtils.suffixFileFilter(TEX_EXT), FileFilterUtils.directoryFileFilter());
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final ObjectProperty<Path> directory;
    private final ObservableList<Citation> citationList;
    private final ObjectProperty<Status> status;
    private final StringProperty searchError;
    private final BooleanProperty updateStatusOnCreate;
    private final DefaultLatexParser latexParser;
    private final LatexParserResults latexFiles;
    private final DirectoryMonitorManager directoryMonitorManager;
    private final FileAlterationListener listener;
    private FileAlterationObserver observer;
    private BibEntry currentEntry;

    public LatexCitationsTabViewModel(BibDatabaseContext databaseContext,
                                      PreferencesService preferencesService,
                                      DialogService dialogService,
                                      DirectoryMonitorManager directoryMonitorManager) {

        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.directory = new SimpleObjectProperty<>(databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                                                                   .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory())));

        this.citationList = FXCollections.observableArrayList();
        this.status = new SimpleObjectProperty<>(Status.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
        this.directoryMonitorManager = directoryMonitorManager;
        this.updateStatusOnCreate = new SimpleBooleanProperty(false);
        this.listener = getListener();

        this.latexParser = new DefaultLatexParser();
        this.latexFiles = new LatexParserResults();
    }

    private FileAlterationListener getListener() {
        return new FileAlterationListener() {
            @Override
            public void onStart(FileAlterationObserver observer) {
                if (!updateStatusOnCreate.get()) {
                    DefaultTaskExecutor.runInJavaFXThread(() -> status.set(Status.IN_PROGRESS));
                }
            }

            @Override
            public void onStop(FileAlterationObserver observer) {
                if (!updateStatusOnCreate.get()) {
                    updateStatusOnCreate.set(true);
                    updateStatus();
                }
            }

            @Override
            public void onFileCreate(File file) {
                Path path = file.toPath();
                LatexParserResult result = latexParser.parse(path).get();
                latexFiles.add(path, result);

                Optional<String> citationKey = currentEntry.getCitationKey();
                if (citationKey.isPresent()) {
                    Collection<Citation> citations = result.getCitationsByKey(citationKey.get());
                    DefaultTaskExecutor.runInJavaFXThread(() -> citationList.addAll(citations));
                }

                if (updateStatusOnCreate.get()) {
                    updateStatus();
                }
            }

            @Override
            public void onFileDelete(File file) {
                LatexParserResult result = latexFiles.remove(file.toPath());

                Optional<String> citationKey = currentEntry.getCitationKey();
                if (citationKey.isPresent()) {
                    Collection<Citation> citations = result.getCitationsByKey(citationKey.get());
                    DefaultTaskExecutor.runInJavaFXThread(() -> citationList.removeAll(citations));
                    updateStatus();
                }
            }

            @Override
            public void onFileChange(File file) {
                onFileDelete(file);
                onFileCreate(file);
                updateStatus();
            }

            @Override
            public void onDirectoryChange(File directory) {
            }

            @Override
            public void onDirectoryCreate(File directory) {
            }

            @Override
            public void onDirectoryDelete(File directory) {
            }
        };
    }

    public void bindToEntry(BibEntry entry) {
        checkAndUpdateDirectory();

        currentEntry = entry;
        Optional<String> citationKey = entry.getCitationKey();

        if (observer == null) {
            observer = new FileAlterationObserver(directory.get().toFile(), FILE_FILTER);
            directoryMonitorManager.addObserver(observer, listener);
        }

        if (citationKey.isPresent()) {
            citationList.setAll(latexFiles.getCitationsByKey(citationKey.get()));
            if (!status.get().equals(Status.IN_PROGRESS)) {
                updateStatus();
            }
        } else {
            searchError.set(Localization.lang("Selected entry does not have an associated citation key."));
            status.set(Status.ERROR);
        }
    }

    public void setLatexDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(directory.get()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory ->
                databaseContext.getMetaData().setLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost(), selectedDirectory.toAbsolutePath()));

        checkAndUpdateDirectory();
    }

    private void checkAndUpdateDirectory() {
        Path newDirectory = databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                                           .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory()));

        if (!newDirectory.equals(directory.get())) {
            status.set(Status.IN_PROGRESS);
            updateStatusOnCreate.set(false);
            citationList.clear();
            latexFiles.clear();

            directoryMonitorManager.removeObserver(observer);
            directory.set(newDirectory);
            observer = new FileAlterationObserver(directory.get().toFile(), FILE_FILTER);
            directoryMonitorManager.addObserver(observer, listener);
        }
    }

    private void updateStatus() {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            if (!Files.exists(directory.get())) {
                searchError.set(Localization.lang("Current search directory does not exist: %0", directory.get()));
                status.set(Status.ERROR);
            } else if (citationList.isEmpty()) {
                status.set(Status.NO_RESULTS);
            } else {
                status.set(Status.CITATIONS_FOUND);
            }
        });
    }

    public ObjectProperty<Path> directoryProperty() {
        return directory;
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexCitationsTab();
    }
}
