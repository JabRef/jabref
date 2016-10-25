package net.sf.jabref.gui.importer.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.MergeDialog;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableInsertString;
import net.sf.jabref.logic.importer.OpenDatabase;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AppendDatabaseAction implements BaseAction {
    private static final Log LOGGER = LogFactory.getLog(AppendDatabaseAction.class);

    private final JabRefFrame frame;
    private final BasePanel panel;

    private final List<File> filesToOpen = new ArrayList<>();

    public AppendDatabaseAction(JabRefFrame frame, BasePanel panel) {
        this.frame = frame;
        this.panel = panel;
    }

    @Override
    public void action() {

        filesToOpen.clear();
        final MergeDialog md = new MergeDialog(frame, Localization.lang("Append database"), true);
        md.setLocationRelativeTo(panel);
        md.setVisible(true);
        if (md.isOkPressed()) {
            FileDialog dialog = new FileDialog(frame).withExtension(FileExtensions.BIBTEX_DB);
            dialog.setDefaultExtension(FileExtensions.BIBTEX_DB);
            List<String> chosen = dialog.showDialogAndGetMultipleFiles();
            if (chosen.isEmpty()) {
                return;
            }
            for (String aChosen : chosen) {
                filesToOpen.add(new File(aChosen));
            }

            // Run the actual open in a thread to prevent the program
            // locking until the file is loaded.
            JabRefExecutorService.INSTANCE.execute(
                    () -> openIt(md.importEntries(), md.importStrings(), md.importGroups()));
        }

    }

    private void openIt(boolean importEntries, boolean importStrings, boolean importGroups) {
        if (filesToOpen.isEmpty()) {
            return;
        }
        for (File file : filesToOpen) {
            try {
                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, file.getParent());
                // Should this be done _after_ we know it was successfully opened?
                ParserResult pr = OpenDatabase.loadDatabase(file,
                        Globals.prefs.getImportFormatPreferences());
                AppendDatabaseAction.mergeFromBibtex(frame, panel, pr, importEntries, importStrings, importGroups);
                panel.output(Localization.lang("Imported from database") + " '" + file.getPath() + "'");
            } catch (IOException | KeyCollisionException ex) {
                LOGGER.warn("Could not open database", ex);
                JOptionPane.showMessageDialog(panel, ex.getMessage(), Localization.lang("Open database"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void mergeFromBibtex(JabRefFrame frame, BasePanel panel, ParserResult pr, boolean importEntries,
            boolean importStrings, boolean importGroups) throws KeyCollisionException {

        BibDatabase fromDatabase = pr.getDatabase();
        List<BibEntry> appendedEntries = new ArrayList<>();
        List<BibEntry> originalEntries = new ArrayList<>();
        BibDatabase database = panel.getDatabase();

        NamedCompound ce = new NamedCompound(Localization.lang("Append database"));
        MetaData meta = pr.getMetaData();

        if (importEntries) { // Add entries
            boolean overwriteOwner = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER);
            boolean overwriteTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP);

            for (BibEntry originalEntry : fromDatabase.getEntries()) {
                BibEntry be = (BibEntry) originalEntry.clone();
                be.setId(IdGenerator.next());
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

        ce.end();
        panel.getUndoManager().addEdit(ce);
        panel.markBaseChanged();
    }
}
