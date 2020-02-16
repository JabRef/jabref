package org.jabref.gui.referencemetadata;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * This action triggers fetching current citation metadata (e.g. citation counts) from the web for the currently selected entries in a library.
 */
public class FetchReferenceMetadata extends SimpleCommand {

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final StateManager stateManager;
    private UndoManager undoManager;
    private TaskExecutor taskExecutor;

    public FetchReferenceMetadata(JabRefFrame frame, JabRefPreferences preferences, StateManager stateManager, UndoManager undoManager, TaskExecutor taskExecutor) {
        this.dialogService = frame.getDialogService();
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, Localization.lang("This operation fetches the citation counts for the currently selected entries online."), Localization.lang("This operation requires one or more entries to be selected.")));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        ObservableList<BibEntry> entries = stateManager.getSelectedEntries();

        final NamedCompound nc = new NamedCompound(Localization.lang("Fetch citation counts"));

        Task<List<BibEntry>> fetchReferenceMetadataTask = new Task<List<BibEntry>>() {

            @Override
            protected List<BibEntry> call() {

                // prepare requestObject

                JsonArray entriesArray = new JsonArray();

                for (BibEntry entry : entries) {
                    JsonArray creatorsArray = new JsonArray();

                    String creatorsString = "";
                    String creatorsType = "";

                    creatorsString = entry.getField(StandardField.AUTHOR).orElse("").trim();

                    if (creatorsString.length() != 0) {
                        creatorsType = "author";
                    }
                    else {
                        creatorsString = entry.getField(StandardField.EDITOR).orElse("").trim();

                        if (creatorsString.length() != 0) {
                            creatorsType = "editor";
                        }
                        else
                        {
                            creatorsType = "";
                        }
                    }

                    AuthorList authorList = AuthorList.parse(creatorsString);

                    for (Author author : authorList.getAuthors()) {
                        JsonObject creator = new JsonObject();
                        creator.addProperty("firstName", author.getFirst().orElse("").trim());
                        creator.addProperty("lastName", author.getLast().orElse("").trim());
                        creator.addProperty("creatorType", creatorsType);
                        creatorsArray.add(creator);
                    }

                    JsonObject entryObject = new JsonObject();
                    entryObject.addProperty("entryId", entry.getId());
                    entryObject.addProperty("title", entry.getTitle().orElse("").trim());
                    entryObject.addProperty("year", entry.getField(StandardField.YEAR).orElse("").trim());
                    entryObject.addProperty("date", entry.getField(StandardField.DATE).orElse("").trim());
                    entryObject.addProperty("dio", entry.getField(StandardField.DOI).orElse("").trim());
                    entryObject.add("creators", creatorsArray);

                    entriesArray.add(entryObject);
                }

                JsonObject requestObject = new JsonObject();

                requestObject.addProperty("databaseId", database.getDatabase().getSharedDatabaseID().orElse(""));
                requestObject.add("entries", entriesArray);

                // submit requestObject

                // receive and process results (updating or adding citation counts)

                for (BibEntry entry : entries) {
                    // fetch existing citation count (for testing purposes)
                    String existingCitationCount = entry.getField(SpecialField.CITATION_COUNT).orElse("").trim();

                    // set new or updated citation count (for testing purposes)
                    entry.setField(SpecialField.CITATION_COUNT, "0004321");

                    // fetch updated citation count (for testing purposes)
                    String updatedCitationCount = entry.getField(SpecialField.CITATION_COUNT).orElse("").trim();
                }

                return new ArrayList<BibEntry>();
            }

            @Override
            protected void succeeded() {
                if (!getValue().isEmpty()) {
                    if (nc.hasEdits()) {
                        nc.end();
                        undoManager.addEdit(nc);
                    }
                    dialogService.notify(Localization.lang("Finished fetching citation counts."));
                } else {
                    dialogService.notify(Localization.lang("Finished fetching citation counts.") + " " + Localization.lang("There was nothing to do."));
                }
            }
        };

        dialogService.showProgressDialogAndWait(
                Localization.lang("Fetching citation counts online"),
                Localization.lang("Querying citations counts..."),
                fetchReferenceMetadataTask);
        taskExecutor.execute(fetchReferenceMetadataTask);
    }
}
