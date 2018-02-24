package org.jabref.gui.sharelatex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import org.jabref.JabRefGUI;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.gui.shared.MergeSharedEntryDialog;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.sharelatex.ShareLatexManager;
import org.jabref.logic.sharelatex.ShareLatexParser;
import org.jabref.logic.sharelatex.events.ShareLatexEntryMessageEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.sharelatex.ShareLatexProject;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShareLatexProjectDialogViewModel extends AbstractViewModel {

    private static final Log LOGGER = LogFactory.getLog(ShareLatexProjectDialogViewModel.class);

    private final StateManager stateManager;
    private final ShareLatexManager manager;
    private final SimpleListProperty<ShareLatexProjectViewModel> projects = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final ImportFormatPreferences prefs;

    private final FileUpdateMonitor fileMonitor;

    public ShareLatexProjectDialogViewModel(StateManager stateManager, ShareLatexManager manager, ImportFormatPreferences prefs, FileUpdateMonitor fileMonitor) {
        this.stateManager = stateManager;
        this.prefs = prefs;
        this.fileMonitor = fileMonitor;
        manager.registerListener(this);
        this.manager = manager;

    }

    public void addProjects(List<ShareLatexProject> projectsToAdd) {
        this.projects.clear();
        this.projects.addAll(projectsToAdd.stream().map(ShareLatexProjectViewModel::new).collect(Collectors.toList()));
    }

    public SimpleListProperty<ShareLatexProjectViewModel> projectsProperty() {
        return this.projects;
    }

    @Subscribe
    public void listenToSharelatexEntryMessage(ShareLatexEntryMessageEvent event) {

        Path actualDbPath = stateManager.getActiveDatabase().get().getDatabasePath().get();
        List<BibEntry> entries = event.getEntries();

        try {
            ParserResult result = new BibtexImporter(prefs, fileMonitor).importDatabase(event.getNewDatabaseContent());

            ShareLatexParser parser = new ShareLatexParser();
            if (event.getPosition() != -1) {
                //Was the change on the sharelatex server side  actually an entry?
                Optional<BibEntry> entryFromPosition = parser.getEntryFromPosition(result, event.getPosition());

                if (entryFromPosition.isPresent()) {

                    BibEntry identifedEntry = entryFromPosition.get();
                    Optional<BibEntry> entryFromSharelatex = entries.stream().filter(searchEntry -> searchEntry.equals(identifedEntry)).findFirst();

                    //we search the local datase for an etry with the cite key
                    Optional<BibEntry> entryFromLocalDatabase = stateManager.getActiveDatabase().get().getDatabase().getEntryByKey(identifedEntry.getCiteKey());

                    if (entryFromSharelatex.isPresent() && entryFromLocalDatabase.isPresent()) {
                        MergeSharedEntryDialog dlg = new MergeSharedEntryDialog(JabRefGUI.getMainFrame(),
                                entryFromLocalDatabase.get(),
                                entryFromSharelatex.get(),
                                stateManager.getActiveDatabase().get().getMode());
                        dlg.setMetaData(stateManager.getActiveDatabase().get().getMetaData());

                        dlg.showMergeDialog();

                        //TODO: After merge we probably need to send the new content to the server

                    }

                } else {

                    try (BufferedWriter writer = Files.newBufferedWriter(actualDbPath, StandardCharsets.UTF_8)) {
                        writer.write(event.getNewDatabaseContent());
                        writer.close();

                    } catch (IOException e) {
                        LOGGER.error("Problem writing new database content", e);
                    }
                }
                System.out.println("Changed chars: " + event.getChars());
            }
        } catch (IOException e1) {
            LOGGER.error("Problem parsing position new database content", e1);

        }

    }

}
