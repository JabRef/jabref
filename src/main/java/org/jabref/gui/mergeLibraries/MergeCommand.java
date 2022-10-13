package org.jabref.gui.mergeLibraries;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import java.nio.file.Path;

public class MergeCommand extends SimpleCommand {
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    public MergeCommand(JabRefFrame frame,PreferencesService preferences, StateManager stateManager) {

        this.preferences = preferences;
        this.frame = frame;
        this.dialogService = frame.getDialogService();
    }

    @Override
    public void execute() {
        //TODO: write the execute method
        //open dialog box using the dialog service which will return a path to a directory
        //find all the .lib files by crawling in doMerge and merge them
        //then add the database to the StateManager's activeDatabase


    }

    public BibDatabase doMerge(Path filepath, BibDatabase database){
        return null;
    }
}
