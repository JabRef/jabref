package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.MergeDialog;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileExtensions;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AppendDatabaseAction implements BaseAction {

    private static final Log LOGGER = LogFactory.getLog(AppendDatabaseAction.class);

    private final JabRefFrame frame;
    private final BasePanel panel;

    private final List<Path> filesToOpen = new ArrayList<>();

    public AppendDatabaseAction(JabRefFrame frame, BasePanel panel) {
        this.frame = frame;
        this.panel = panel;
    }

    @Override
    public void action() {

        filesToOpen.clear();
        final MergeDialog md = new MergeDialog(frame, Localization.lang("Append library"), true);
        md.setLocationRelativeTo(panel);
        md.setVisible(true);
        if (md.isOkPressed()) {

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .withDefaultExtension(FileExtensions.BIBTEX_DB)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                    .build();
            DialogService ds = new FXDialogService();

            List<Path> chosen = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration));
            if (chosen.isEmpty()) {
                return;
            }
            filesToOpen.addAll(chosen);

            // Run the actual open in a thread to prevent the program
            // locking until the file is loaded.
            JabRefExecutorService.INSTANCE.execute(
                    () -> openIt(md.importEntries(), md.importStrings(), md.importGroups(), md.importSelectorWords()));

        }

    }

    private void openIt(boolean importEntries, boolean importStrings, boolean importGroups,
            boolean importSelectorWords) {
        if (filesToOpen.isEmpty()) {
            return;
        }
        for (Path file : filesToOpen) {
            try {
                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, file.getParent().toString());
                // Should this be done _after_ we know it was successfully opened?
                ParserResult pr = OpenDatabase.loadDatabase(file.toFile(),
                        Globals.prefs.getImportFormatPreferences());
                AppendDatabaseAction.mergeFromBibtex(frame, panel, pr, importEntries, importStrings, importGroups,
                        importSelectorWords);
                panel.output(Localization.lang("Imported from library") + " '" + file + "'");
            } catch (IOException | KeyCollisionException ex) {
                LOGGER.warn("Could not open database", ex);
                JOptionPane.showMessageDialog(panel, ex.getMessage(), Localization.lang("Open library"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void mergeFromBibtex(JabRefFrame frame, BasePanel panel, ParserResult pr, boolean importEntries,
            boolean importStrings, boolean importGroups, boolean importSelectorWords) throws KeyCollisionException {

        BibDatabase fromDatabase = pr.getDatabase();
        List<BibEntry> appendedEntries = new ArrayList<>();
        List<BibEntry> originalEntries = new ArrayList<>();
        BibDatabase database = panel.getDatabase();

        NamedCompound ce = new NamedCompound(Localization.lang("Append library"));
        MetaData meta = pr.getMetaData();

        if (importEntries) { // Add entries
            boolean overwriteOwner = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER);
            boolean overwriteTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP);

            for (BibEntry originalEntry : fromDatabase.getEntries()) {
                BibEntry be = (BibEntry) originalEntry.clone();
                UpdateField.setAutomaticFields(be, overwriteOwner, overwriteTimeStamp,
                        Globals.prefs.getUpdateFieldPreferences());
                database.insertEntry(be);
                appendedEntries.add(be);
                originalEntries.add(originalEntry);
                ce.addEdit(new UndoableInsertEntry(database, be, panel));
            }
        }

        if (importStrings) {
            for (BibtexString bs : fromDatabase.getStringValues()) {
                if (!database.hasStringLabel(bs.getName())) {
                    database.addString(bs);
                    ce.addEdit(new UndoableInsertString(panel, database, bs));
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
                        group.add(appendedEntries);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error(e);
                    }
                }

                // groupsSelector is always created, even when no groups
                // have been defined. therefore, no check for null is
                // required here
                frame.getGroupSelector().addGroups(newGroups, ce);
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
}
