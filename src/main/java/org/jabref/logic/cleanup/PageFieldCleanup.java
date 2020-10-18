package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * adds the article ID of a journal as the page count, but only if the page field is empty
 */
public class PageFieldCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();
        Optional<String> doiAsString = entry.getField(StandardField.DOI);

        if (doiAsString.isPresent() && !entry.hasField(StandardField.PAGES)) {
            String articleId = new String();
            int index = doiAsString.get().length() - 1;
            while (Character.isDigit(doiAsString.get().charAt(index))) {
                articleId = doiAsString.get().charAt(index--) + articleId;
            }
            entry.setField(StandardField.PAGES, articleId);
            FieldChange change = new FieldChange(entry, StandardField.PAGES, "", articleId);
            changes.add(change);
        }

        return changes;
    }
}
