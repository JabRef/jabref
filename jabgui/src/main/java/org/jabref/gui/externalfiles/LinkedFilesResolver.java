package org.jabref.gui.externalfiles;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

public class LinkedFilesResolver {

    private LinkedFilesResolver() {
    }

    public static List<LinkedFile> getLinkedFilesToOpen(BibEntry entry, BibDatabaseContext databaseContext) {
        if (!entry.getFiles().isEmpty()) {
            return entry.getFiles();
        }

        return entry.getField(StandardField.CROSSREF)
                    .filter(crossref -> !crossref.isBlank())
                    .flatMap(databaseContext.getDatabase()::getEntryByCitationKey)
                    .map(BibEntry::getFiles)
                    .filter(parentFiles -> !parentFiles.isEmpty())
                    .orElse(List.of());
    }
}
