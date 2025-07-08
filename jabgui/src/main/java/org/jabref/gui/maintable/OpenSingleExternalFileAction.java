package org.jabref.gui.maintable;

import java.util.Objects;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class OpenSingleExternalFileAction extends SimpleCommand {

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final TaskExecutor taskExecutor;
    private final BibDatabaseContext databaseContext;

    public OpenSingleExternalFileAction(DialogService dialogService,
                                        GuiPreferences preferences,
                                        BibEntry entry,
                                        LinkedFile linkedFile,
                                        TaskExecutor taskExecutor,
                                        BibDatabaseContext databaseContext) {
        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferences);
        this.entry = Objects.requireNonNull(entry);
        this.linkedFile = Objects.requireNonNull(linkedFile);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.databaseContext = Objects.requireNonNull(databaseContext);

        this.setExecutable(true);
    }

    @Override
    public void execute() {
        LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                linkedFile,
                entry,
                databaseContext,
                taskExecutor,
                dialogService,
                preferences);
        linkedFileViewModel.open();
    }
}
