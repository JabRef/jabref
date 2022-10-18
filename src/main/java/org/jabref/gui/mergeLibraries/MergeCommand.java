package org.jabref.gui.mergeLibraries;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import java.nio.file.Path;
import java.util.Optional;

public class MergeCommand extends SimpleCommand {
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StateManager stateManager;

    public MergeCommand(JabRefFrame frame,PreferencesService preferences, StateManager stateManager) {

        this.preferences = preferences;
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {
        //TODO: write the execute method

        //construct a configuration with the current directory
        //(this really could have just been a constructor so think of it like that)
        DirectoryDialogConfiguration.Builder config = new DirectoryDialogConfiguration.Builder().withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory());
        //open dialog box using the dialog service which will return a path to a directory
        Optional<Path> path = dialogService.showDirectorySelectionDialog(config.build());


        Optional<BibDatabaseContext> database = stateManager.getActiveDatabase();
        if(path.isPresent() && database.isPresent())
            doMerge(path.get(), database.get().getDatabase());
    }

    public BibDatabase doMerge(Path filepath, BibDatabase database){
        //find all the .lib files by crawling in doMerge and merge them
        //then add the database to the StateManager's activeDatabase

        return null;
    }
}
