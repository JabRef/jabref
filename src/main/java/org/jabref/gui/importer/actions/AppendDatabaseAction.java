package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.CompoundEdit;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.importer.AppendDatabaseDialog;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppendDatabaseAction implements BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppendDatabaseAction.class);

    private final BasePanel panel;

    private final List<Path> filesToOpen = new ArrayList<>();
    private final DialogService dialogService;

    public AppendDatabaseAction(JabRefFrame frame, BasePanel panel) {
        this.panel = panel;
        dialogService = frame.getDialogService();
    }

    private static void mergeFromBibtex(BasePanel panel, ParserResult parserResult, boolean importEntries,
                                        boolean importStrings, boolean importGroups, boolean importSelectorWords) throws KeyCollisionException {

        BibDatabase fromDatabase = parserResult.getDatabase();
        List<BibEntry> entriesToAppend = new ArrayList<>();
        BibDatabase database = panel.getDatabase();

        NamedCompound ce = new NamedCompound(Localization.lang("Append library"));
        MetaData meta = parserResult.getMetaData();

        if (importEntries) { // Add entries
            boolean overwriteOwner = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER);
            boolean overwriteTimeStamp = Globals.prefs.getTimestampPreferences().overwriteTimestamp();

            for (BibEntry originalEntry : fromDatabase.getEntries()) {
                BibEntry entry = (BibEntry) originalEntry.clone();
                UpdateField.setAutomaticFields(entry, overwriteOwner, overwriteTimeStamp,
                        Globals.prefs.getUpdateFieldPreferences());
                entriesToAppend.add(entry);
            }
            database.insertEntries(entriesToAppend);
            ce.addEdit(new UndoableInsertEntries(database, entriesToAppend));
        }

        if (importStrings) {
            for (BibtexString bs : fromDatabase.getStringValues()) {
                if (!database.hasStringByName(bs.getName())) {
                    database.addString(bs);
                    ce.addEdit(new UndoableInsertString(database, bs));
                }
            }
        }

        if (importGroups) {
            meta.getGroups().ifPresent(newGroups -> {
                // ensure that there is always only one AllEntriesGroup
                if (newGroups.getGroup() instanceof AllEntriesGroup) {
                    // create a dummy group
                    try {
                        ExplicitGroup group = new ExplicitGroup("Imported", GroupHierarchyType.INDEPENDENT,
                                Globals.prefs.getKeywordDelimiter());
                        newGroups.setGroup(group);
                        group.add(entriesToAppend);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Problem appending entries to group", e);
                    }
                }

                addGroups(newGroups, ce);
            });
        }

        if (importSelectorWords) {

            for (ContentSelector selector : meta.getContentSelectorList()) {
                panel.getBibDatabaseContext().getMetaData().addContentSelector(selector);
            }
        }
        ce.end();
        panel.getUndoManager().addEdit(ce);
        panel.markBaseChanged();
    }

    /**
     * Adds the specified node as a child of the current root. The group contained in <b>newGroups </b> must not be of
     * type AllEntriesGroup, since every tree has exactly one AllEntriesGroup (its root). The <b>newGroups </b> are
     * inserted directly, i.e. they are not deepCopy()'d.
     */
    private static void addGroups(GroupTreeNode newGroups, CompoundEdit ce) {

        // paranoia: ensure that there are never two instances of AllEntriesGroup
        if (newGroups.getGroup() instanceof AllEntriesGroup) {
            return; // this should be impossible anyway
        }

        Globals.stateManager.getActiveDatabase()
                .map(BibDatabaseContext::getMetaData)
                .flatMap(MetaData::getGroups)
                .ifPresent(newGroups::moveTo);

        //UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot,
        //        new GroupTreeNodeViewModel(newGroups), UndoableAddOrRemoveGroup.ADD_NODE);
        //ce.addEdit(undo);
    }

    @Override
    public void action() {
        filesToOpen.clear();
        final AppendDatabaseDialog dialog = new AppendDatabaseDialog();
        Optional<Boolean> response = dialog.showAndWait();
        if (OptionalUtil.isPresentAndTrue(response)) {
            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .withDefaultExtension(StandardFileType.BIBTEX_DB)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                    .build();

            List<Path> chosen = dialogService.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration);
            if (chosen.isEmpty()) {
                return;
            }
            filesToOpen.addAll(chosen);

            if (filesToOpen.isEmpty()) {
                return;
            }

            for (Path file : filesToOpen) {
                // Run the actual open in a thread to prevent the program locking until the file is loaded.
                BackgroundTask.wrap(() -> openIt(file, dialog.importEntries(), dialog.importStrings(), dialog.importGroups(), dialog.importSelectorWords()))
                              .onSuccess(fileName -> dialogService.notify(Localization.lang("Imported from library") + " '" + fileName + "'"))
                              .onFailure(exception -> {
                                  LOGGER.warn("Could not open database", exception);
                                  dialogService.showErrorDialogAndWait(Localization.lang("Open library"), exception);})
                              .executeWith(Globals.TASK_EXECUTOR);
            }
        }
    }

    private String openIt(Path file, boolean importEntries, boolean importStrings, boolean importGroups,
                        boolean importSelectorWords) throws IOException, KeyCollisionException {
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, file.getParent().toString());
            // Should this be done _after_ we know it was successfully opened?
        ParserResult parserResult = OpenDatabase.loadDatabase(file,
                    Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
            AppendDatabaseAction.mergeFromBibtex(panel, parserResult, importEntries, importStrings, importGroups,
                    importSelectorWords);
            return file.toString();
    }

}
