package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.UpdateFieldPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

public class ImportHandler {

    private final BibDatabaseContext database;
    private final UpdateFieldPreferences updateFieldPreferences;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;

    public ImportHandler(DialogService dialogService,
                         BibDatabaseContext database,
                         ExternalFileTypes externalFileTypes,
                         FilePreferences filePreferences,
                         ImportFormatPreferences importFormatPreferences,
                         UpdateFieldPreferences updateFieldPreferences,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager) {

        this.dialogService = dialogService;
        this.database = database;
        this.updateFieldPreferences = updateFieldPreferences;
        this.fileUpdateMonitor = fileupdateMonitor;

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
            } else {
                entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
            }

            insertEntries(entriesToAdd);
            entriesToAdd.forEach(entry -> ce.addEdit(new UndoableInsertEntry(database.getDatabase(), entry)));
        }
        ce.end();
        undoManager.addEdit(ce);
    }

    private BibEntry createEmptyEntryWithLink(Path file) {
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.TITLE, file.getFileName().toString());
        linker.addFilesToEntry(entry, Collections.singletonList(file));
        return entry;
    }

    public void importEntriesFromBibFiles(Path bibFile) {
        List<BibEntry> entriesToImport = contentImporter.importFromBibFile(bibFile, fileUpdateMonitor);
        insertEntries(entriesToImport);
    }

    private void insertEntries(List<BibEntry> entries) {
        database.getDatabase().insertEntries(entries);

        if (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)) {
            // Set owner field to default value
            UpdateField.setAutomaticFields(entries, true, true, updateFieldPreferences);
        }
    }
}
