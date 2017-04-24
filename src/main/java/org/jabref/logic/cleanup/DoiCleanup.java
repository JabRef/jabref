package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

/**
 * Formats the DOI (e.g. removes http part) and also moves DOIs from note, url or ee field to the doi field.
 */
public class DoiCleanup implements CleanupJob {

    /**
     * Fields to check for DOIs.
     */
    private static final List<String> FIELDS = Arrays.asList(FieldName.NOTE, FieldName.URL, "ee");

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {

        List<FieldChange> changes = new ArrayList<>();

        // First check if the Doi Field is empty
        if (entry.hasField(FieldName.DOI)) {
            String doiFieldValue = entry.getField(FieldName.DOI).orElse(null);

            Optional<DOI> doi = DOI.parse(doiFieldValue);

            if (doi.isPresent()) {
                String newValue = doi.get().getDOI();
                if (!doiFieldValue.equals(newValue)) {
                    entry.setField(FieldName.DOI, newValue);

                    FieldChange change = new FieldChange(entry, FieldName.DOI, doiFieldValue, newValue);
                    changes.add(change);
                }

                // Doi field seems to contain Doi -> cleanup note, url, ee field
                for (String field : FIELDS) {
                    entry.getField(field).flatMap(DOI::parse)
                            .ifPresent(unused -> removeFieldValue(entry, field, changes));
                }
            }
        } else {
            // As the Doi field is empty we now check if note, url, or ee field contains a Doi
            for (String field : FIELDS) {
                Optional<DOI> doi = entry.getField(field).flatMap(DOI::parse);

                if (doi.isPresent()) {
                    // update Doi
                    String oldValue = entry.getField(FieldName.DOI).orElse(null);
                    String newValue = doi.get().getDOI();

                    entry.setField(FieldName.DOI, newValue);

                    FieldChange change = new FieldChange(entry, FieldName.DOI, oldValue, newValue);
                    changes.add(change);

                    removeFieldValue(entry, field, changes);
                }
            }
        }

        return changes;
    }

    private void removeFieldValue(BibEntry entry, String field, List<FieldChange> changes) {
        CleanupJob eraser = new FieldFormatterCleanup(field, new ClearFormatter());
        changes.addAll(eraser.cleanup(entry));
    }
}
