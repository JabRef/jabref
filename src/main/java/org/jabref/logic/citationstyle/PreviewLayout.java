package org.jabref.logic.citationstyle;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public interface PreviewLayout {

    String generatePreview(BibEntry entry, BibDatabase database);

    String getName();
}
