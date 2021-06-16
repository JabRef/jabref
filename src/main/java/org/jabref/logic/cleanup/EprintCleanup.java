package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;

/**
 * Formats the DOI (e.g. removes http part) and also moves DOIs from note, url or ee field to the doi field.
 */
public class EprintCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {

        List<FieldChange> changes = new ArrayList<>();

        for (Field field : Arrays.asList(StandardField.URL, StandardField.JOURNAL, StandardField.JOURNALTITLE, StandardField.NOTE)) {
            Optional<ArXivIdentifier> arXivIdentifier = entry.getField(field).flatMap(ArXivIdentifier::parse);

            if (arXivIdentifier.isPresent()) {
                entry.setField(StandardField.EPRINT, arXivIdentifier.get().getNormalized())
                     .ifPresent(changes::add);

                entry.setField(StandardField.EPRINTTYPE, "arxiv")
                     .ifPresent(changes::add);

                arXivIdentifier.get().getClassification().ifPresent(classification ->
                        entry.setField(StandardField.EPRINTCLASS, classification)
                             .ifPresent(changes::add)
                );

                entry.clearField(field)
                     .ifPresent(changes::add);

                if (field.equals(StandardField.URL)) {
                    // If we clear the URL field, we should also clear the URL-date field
                    entry.clearField(StandardField.URLDATE)
                         .ifPresent(changes::add);
                }
            }
        }

        return changes;
    }
}
