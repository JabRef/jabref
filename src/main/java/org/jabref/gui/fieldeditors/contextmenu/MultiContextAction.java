package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class MultiContextAction extends SimpleCommand {

    private final StandardActions command;
    private final ObservableList<LinkedFileViewModel> selectedFiles;
    private final BibDatabaseContext databaseContext;
    private final ObservableOptionalValue<BibEntry> bibEntry;
    private final GuiPreferences preferences;
    private final LinkedFilesEditorViewModel viewModel;

    public MultiContextAction(StandardActions command,
                              ObservableList<LinkedFileViewModel> selectedFiles,
                              BibDatabaseContext databaseContext,
                              ObservableOptionalValue<BibEntry> bibEntry,
                              GuiPreferences preferences,
                              LinkedFilesEditorViewModel viewModel) {
        this.command = command;
        this.selectedFiles = selectedFiles;
        this.databaseContext = databaseContext;
        this.bibEntry = bibEntry;
        this.preferences = preferences;
        this.viewModel = viewModel;

        this.executable.bind(Bindings.createBooleanBinding(
                () -> !selectedFiles.isEmpty(),
                selectedFiles
        ));
    }

    @Override
    public void execute() {
        List<LinkedFileViewModel> selectedFilesCopy = new ArrayList<>(selectedFiles);
        for (LinkedFileViewModel linkedFile : selectedFilesCopy) {
            new ContextAction(command, linkedFile, databaseContext, bibEntry, preferences, viewModel).execute();
        }
    }
}
