package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.jspecify.annotations.NonNull;

record MultiSelectionMenuBuilder(
        DialogService dialogService,
        BibDatabaseContext databaseContext,
        ObservableOptionalValue<BibEntry> bibEntry,
        GuiPreferences preferences,
        LinkedFilesEditorViewModel viewModel
) implements ContextMenuBuilder {

    MultiSelectionMenuBuilder(@NonNull DialogService dialogService,
                              @NonNull BibDatabaseContext databaseContext,
                              @NonNull ObservableOptionalValue<BibEntry> bibEntry,
                              @NonNull GuiPreferences preferences,
                              @NonNull LinkedFilesEditorViewModel viewModel) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.bibEntry = bibEntry;
        this.preferences = preferences;
        this.viewModel = viewModel;
    }

    @Override
    public boolean supports(ObservableList<LinkedFileViewModel> selection) {
        return selection != null && selection.size() > 1;
    }

    @Override
    public List<MenuItem> buildMenu(ObservableList<LinkedFileViewModel> selection) {
        ActionFactory actionFactory = new ActionFactory();
        List<MenuItem> menuItems = new ArrayList<>();

        SelectionChecks checks = new SelectionChecks(databaseContext, preferences);

        menuItems.add(batchCommandItem(actionFactory, StandardActions.OPEN_FILE, selection, _ -> true));
        menuItems.add(customBatchItem(actionFactory, StandardActions.OPEN_FOLDER, selection, checks::isLocalAndExists, checks::openContainingFolders));
        menuItems.add(batchCommandItem(actionFactory, StandardActions.DOWNLOAD_FILE, selection, checks::isOnline));
        menuItems.add(batchCommandItem(actionFactory, StandardActions.REDOWNLOAD_FILE, selection, checks::hasSourceUrl));
        menuItems.add(batchCommandItem(actionFactory, StandardActions.MOVE_FILE_TO_FOLDER, selection, checks::isMovableToDefaultDir));
        menuItems.add(buildCopyToFolderItem(actionFactory, selection, checks));
        menuItems.add(customBatchItem(actionFactory, StandardActions.REMOVE_LINKS, selection, _ -> true,
                linkedFileViewModels -> linkedFileViewModels.forEach(linkedFileViewModel ->
                        new ContextAction(StandardActions.REMOVE_LINKS, linkedFileViewModel, databaseContext, bibEntry, preferences, viewModel).execute())));
        menuItems.add(batchCommandItem(actionFactory, StandardActions.DELETE_FILE, selection, checks::isLocalAndExists));

        return menuItems;
    }

    private MenuItem buildCopyToFolderItem(ActionFactory actionFactory,
                                           ObservableList<LinkedFileViewModel> selection,
                                           SelectionChecks checks) {
        SimpleCommand copyCommand = new SimpleCommand() {
            {
                executable.bind(Bindings.createBooleanBinding(
                        () -> selection.stream().anyMatch(checks::isLocalAndExists),
                        selection));
            }

            @Override
            public void execute() {
                var localLinkedFiles = selection.stream()
                                                .filter(checks::isLocalAndExists)
                                                .toList();

                if (localLinkedFiles.isEmpty()) {
                    return;
                }

                DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                        .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                        .build();

                var exportDirectory = dialogService.showDirectorySelectionDialog(directoryDialogConfiguration);
                if (exportDirectory.isEmpty()) {
                    return;
                }
                Path exportPath = exportDirectory.get();

                int copiedCount = 0;
                int skippedCount = 0;

                for (LinkedFileViewModel linkedFileViewModel : localLinkedFiles) {
                    var sourcePath = linkedFileViewModel.getFile().findIn(databaseContext, preferences.getFilePreferences());
                    if (sourcePath.isEmpty()) {
                        skippedCount++;
                        continue;
                    }
                    Path source = sourcePath.get();
                    Path target = exportPath.resolve(source.getFileName());

                    boolean replaceExisting = false;
                    boolean copySucceeded = FileUtil.copyFile(source, target, replaceExisting);

                    if (copySucceeded) {
                        copiedCount++;
                    } else {
                        skippedCount++;
                    }
                }

                if (copiedCount > 0) {
                    dialogService.notify(Localization.lang("Copied %0 file(s) to %1", copiedCount, exportPath));
                }
                if (skippedCount > 0) {
                    dialogService.showInformationDialogAndWait(
                            Localization.lang("Copy linked file(s)"),
                            Localization.lang("%0 file(s) were skipped (already exist or not accessible).", skippedCount));
                }
            }
        };

        return actionFactory.createMenuItem(StandardActions.COPY_FILE_TO_FOLDER, copyCommand);
    }

    private MenuItem batchCommandItem(ActionFactory actionFactory,
                                      StandardActions action,
                                      ObservableList<LinkedFileViewModel> selection,
                                      Predicate<LinkedFileViewModel> enablePredicate) {

        SimpleCommand command = new SimpleCommand() {
            {
                executable.bind(Bindings.createBooleanBinding(
                        () -> selection.stream().anyMatch(enablePredicate),
                        selection));
            }

            @Override
            public void execute() {
                List<LinkedFileViewModel> snapshot = new ArrayList<>(selection);
                snapshot.stream()
                        .filter(enablePredicate)
                        .forEach(linkedFileViewModel ->
                                new ContextAction(action, linkedFileViewModel, databaseContext, bibEntry, preferences, viewModel).execute());
            }
        };

        return actionFactory.createMenuItem(action, command);
    }

    private MenuItem customBatchItem(ActionFactory actionFactory,
                                     StandardActions action,
                                     ObservableList<LinkedFileViewModel> selection,
                                     Predicate<LinkedFileViewModel> enablePredicate,
                                     Consumer<List<LinkedFileViewModel>> consumer) {

        SimpleCommand command = new SimpleCommand() {
            {
                executable.bind(Bindings.createBooleanBinding(
                        () -> selection.stream().anyMatch(enablePredicate),
                        selection));
            }

            @Override
            public void execute() {
                List<LinkedFileViewModel> snapshot = new ArrayList<>(selection);
                consumer.accept(snapshot);
            }
        };

        return actionFactory.createMenuItem(action, command);
    }
}
