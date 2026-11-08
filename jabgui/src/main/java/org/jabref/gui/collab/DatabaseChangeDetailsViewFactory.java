package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.entrychange.EntryWithPreviewAndSourceDetailsView;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.preamblechange.PreambleChangeDetailsView;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringadd.BibTexStringAddDetailsView;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringchange.BibTexStringChangeDetailsView;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringdelete.BibTexStringDeleteDetailsView;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.gui.collab.stringrename.BibTexStringRenameDetailsView;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class DatabaseChangeDetailsViewFactory {
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final PreviewViewer previewViewer;
    private final TaskExecutor taskExecutor;
    private final Optional<Path> backupPath;

    public DatabaseChangeDetailsViewFactory(BibDatabaseContext databaseContext,
                                            DialogService dialogService,
                                            GuiPreferences preferences,
                                            BibEntryTypesManager entryTypesManager,
                                            PreviewViewer previewViewer,
                                            TaskExecutor taskExecutor,
                                            Optional<Path> backupPath) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.previewViewer = previewViewer;
        this.taskExecutor = taskExecutor;
        this.backupPath = backupPath;
    }

    private Labeled createOnDiskHeader() {
        String text = Localization.lang("On disk");
        if (backupPath.isEmpty()) {
            return new Label(text);
        }

        Path path = backupPath.orElseThrow();
        Hyperlink link = new Hyperlink(text);
        link.setTooltip(new Tooltip(path.toString()));
        link.setOnAction(_ -> {
            try {
                NativeDesktop.openFolderAndSelectFile(path, preferences.getExternalApplicationsPreferences(), dialogService);
            } catch (IOException e) {
                dialogService.showErrorDialogAndWait(Localization.lang("Open folder"), e);
            }
        });
        return link;
    }

    public DatabaseChangeDetailsView create(DatabaseChange databaseChange) {
        return switch (databaseChange) {
            case EntryChange entryChange ->
                    new EntryChangeDetailsView(
                            entryChange.getOldEntry(),
                            entryChange.getNewEntry(),
                            databaseContext,
                            dialogService,
                            preferences,
                            entryTypesManager,
                            previewViewer,
                            taskExecutor,
                            createOnDiskHeader()
                    );
            case EntryAdd entryAdd ->
                    new EntryWithPreviewAndSourceDetailsView(
                            entryAdd.getAddedEntry(),
                            databaseContext,
                            preferences,
                            entryTypesManager,
                            previewViewer
                    );
            case EntryDelete entryDelete ->
                    new EntryWithPreviewAndSourceDetailsView(
                            entryDelete.getDeletedEntry(),
                            databaseContext,
                            preferences,
                            entryTypesManager,
                            previewViewer
                    );
            case BibTexStringAdd stringAdd ->
                    new BibTexStringAddDetailsView(stringAdd);
            case BibTexStringDelete stringDelete ->
                    new BibTexStringDeleteDetailsView(stringDelete);
            case BibTexStringChange stringChange ->
                    new BibTexStringChangeDetailsView(stringChange);
            case BibTexStringRename stringRename ->
                    new BibTexStringRenameDetailsView(stringRename);
            case MetadataChange metadataChange ->
                    new MetadataChangeDetailsView(
                            metadataChange,
                            preferences.getCitationKeyPatternPreferences().getKeyPatterns(),
                            createOnDiskHeader()
                    );
            case GroupChange groupChange ->
                    new GroupChangeDetailsView(groupChange, createOnDiskHeader());
            case PreambleChange preambleChange ->
                    new PreambleChangeDetailsView(preambleChange);
        };
    }
}
