package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.UpdateFieldPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.util.FileUpdateMonitor;

public class ImportHandler {

    private final BibDatabaseContext database;
    private final UpdateFieldPreferences updateFieldPreferences;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;
    private final StateManager stateManager;

    public ImportHandler(DialogService dialogService,
                         BibDatabaseContext database,
                         ExternalFileTypes externalFileTypes,
                         FilePreferences filePreferences,
                         ImportFormatPreferences importFormatPreferences,
                         UpdateFieldPreferences updateFieldPreferences,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager,
                         StateManager stateManager) {

        this.dialogService = dialogService;
        this.database = database;
        this.updateFieldPreferences = updateFieldPreferences;
        this.fileUpdateMonitor = fileupdateMonitor;
        this.stateManager = stateManager;

        this.linker = new ExternalFilesEntryLinker(externalFileTypes, filePreferences, database);
        this.contentImporter = new ExternalFilesContentImporter(importFormatPreferences);
        this.undoManager = undoManager;
    }

    public ExternalFilesEntryLinker getLinker() {
        return linker;
    }

    public void importAsNewEntries(List<Path> files) {
        CompoundEdit ce = new CompoundEdit();
        for (Path file : files) {
            List<BibEntry> entriesToAdd;
            if (FileUtil.getFileExtension(file).filter("pdf"::equals).isPresent()) {
                List<BibEntry> pdfResult = contentImporter.importPDFContent(file);
                List<BibEntry> xmpEntriesInFile = contentImporter.importXMPContent(file);

                // First try xmp import, if empty try pdf import, otherwise create empty entry
                if (!xmpEntriesInFile.isEmpty()) {
                    if (!pdfResult.isEmpty()) {
                        //FIXME: Show merge dialog?
                        entriesToAdd = xmpEntriesInFile;
                    } else {
                        entriesToAdd = xmpEntriesInFile;
                    }
                } else {
                    if (!pdfResult.isEmpty()) {
                        entriesToAdd = pdfResult;
                    } else {
                        entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
                    }
                }
            } else if (FileUtil.isBibFile(file)) {
                entriesToAdd = contentImporter.importFromBibFile(file, fileUpdateMonitor);
            } else {
                entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
            }

            importEntries(entriesToAdd);
            ce.addEdit(new UndoableInsertEntries(database.getDatabase(), entriesToAdd));
        }
        ce.end();
        undoManager.addEdit(ce);
    }

    private BibEntry createEmptyEntryWithLink(Path file) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, file.getFileName().toString());
        linker.addFilesToEntry(entry, Collections.singletonList(file));
        return entry;
    }

    public void importEntries(List<BibEntry> entries) {
        //TODO: Add undo/redo
        //undoManager.addEdit(new UndoableInsertEntries(panel.getDatabase(), entries));

        database.getDatabase().insertEntries(entries);

        // Set owner/timestamp
        UpdateField.setAutomaticFields(entries, updateFieldPreferences);

        // Generate bibtex keys
        generateKeys(entries);

        // Add to group
        addToGroups(entries, stateManager.getSelectedGroup(database));
    }

    private void addToGroups(List<BibEntry> entries, Collection<GroupTreeNode> groups) {
        for (GroupTreeNode node : groups) {
            if (node.getGroup() instanceof GroupEntryChanger) {
                GroupEntryChanger entryChanger = (GroupEntryChanger) node.getGroup();
                List<FieldChange> undo = entryChanger.add(entries);
                // TODO: Add undo
                //if (!undo.isEmpty()) {
                //    ce.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(new GroupTreeNodeViewModel(node),
                //            undo));
                //}
            }
        }
    }

    /**
     * Generate keys for given entries.
     *
     * @param entries entries to generate keys for
     */
    private void generateKeys(List<BibEntry> entries) {
        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(
                database.getMetaData().getCiteKeyPattern(Globals.prefs.getBibtexKeyPatternPreferences().getKeyPattern()),
                database.getDatabase(),
                Globals.prefs.getBibtexKeyPatternPreferences());

        for (BibEntry entry : entries) {
            keyGenerator.generateAndSetKey(entry);
        }
    }
}
