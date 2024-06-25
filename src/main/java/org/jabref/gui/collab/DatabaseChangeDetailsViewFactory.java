package org.jabref.gui.collab;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
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
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

public class DatabaseChangeDetailsViewFactory {
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;
    private final PreferencesService preferencesService;
    private final BibEntryTypesManager entryTypesManager;
    private final PreviewViewer previewViewer;
    private final TaskExecutor taskExecutor;

    public DatabaseChangeDetailsViewFactory(BibDatabaseContext databaseContext,
                                            DialogService dialogService,
                                            StateManager stateManager,
                                            ThemeManager themeManager,
                                            PreferencesService preferencesService,
                                            BibEntryTypesManager entryTypesManager,
                                            PreviewViewer previewViewer,
                                            TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
        this.preferencesService = preferencesService;
        this.entryTypesManager = entryTypesManager;
        this.previewViewer = previewViewer;
        this.taskExecutor = taskExecutor;
    }

    public DatabaseChangeDetailsView create(DatabaseChange databaseChange) {
        return switch (databaseChange) {
            case EntryChange entryChange -> new EntryChangeDetailsView(
                entryChange.getOldEntry(),
                entryChange.getNewEntry(),
                databaseContext,
                dialogService,
                stateManager,
                themeManager,
                preferencesService,
                entryTypesManager,
                previewViewer,
                taskExecutor
            );
            case EntryAdd entryAdd -> new EntryWithPreviewAndSourceDetailsView(
                entryAdd.getAddedEntry(),
                databaseContext,
                preferencesService,
                entryTypesManager,
                previewViewer
            );
            case EntryDelete entryDelete -> new EntryWithPreviewAndSourceDetailsView(
                entryDelete.getDeletedEntry(),
                databaseContext,
                preferencesService,
                entryTypesManager,
                previewViewer
            );
            case BibTexStringAdd stringAdd -> new BibTexStringAddDetailsView(stringAdd);
            case BibTexStringDelete stringDelete -> new BibTexStringDeleteDetailsView(stringDelete);
            case BibTexStringChange stringChange -> new BibTexStringChangeDetailsView(stringChange);
            case BibTexStringRename stringRename -> new BibTexStringRenameDetailsView(stringRename);
            case MetadataChange metadataChange -> new MetadataChangeDetailsView(
                metadataChange,
                preferencesService.getCitationKeyPatternPreferences().getKeyPatterns()
            );
            case GroupChange groupChange -> new GroupChangeDetailsView(groupChange);
            case PreambleChange preambleChange -> new PreambleChangeDetailsView(preambleChange);
        };
    }
}
