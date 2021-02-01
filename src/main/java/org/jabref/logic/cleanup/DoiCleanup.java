package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.DOI;

/**
 * Formats the DOI (e.g. removes http part) and also moves DOIs from note, url or ee field to the doi field.
 */
public class DoiCleanup implements CleanupJob {

    /**
     * Fields to check for DOIs.
     */
    private static final List<Field> FIELDS = Arrays.asList(StandardField.NOTE, StandardField.URL, new UnknownField("ee"));

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {

        List<FieldChange> changes = new ArrayList<>();

        // First check if the Doi Field is empty
        if (entry.hasField(StandardField.DOI)) {
            String doiFieldValue = entry.getField(StandardField.DOI).orElse(null);

            Optional<DOI> doi = DOI.parse(doiFieldValue);

            if (doi.isPresent()) {
                String newValue = doi.get().getDOI();
                if (!doiFieldValue.equals(newValue)) {
                    entry.setField(StandardField.DOI, newValue);

                    FieldChange change = new FieldChange(entry, StandardField.DOI, doiFieldValue, newValue);
                    changes.add(change);
                }

                // Doi field seems to contain Doi -> cleanup note, url, ee field
                for (Field field : FIELDS) {
                    entry.getField(field).flatMap(DOI::parse)
                        .ifPresent(unused -> removeFieldValue(entry, field, changes));
                }
            }
        } else {
            // As the Doi field is empty we now check if note, url, or ee field contains a Doi
            for (Field field : FIELDS) {
                Optional<DOI> doi = entry.getField(field).flatMap(DOI::parse);

                if (doi.isPresent()) {
                    // Update Doi
                    Optional<FieldChange> change = entry.setField(StandardField.DOI, doi.get().getDOI());
                    change.ifPresent(changes::add);
                    removeFieldValue(entry, field, changes);
                }
            }
        }
        return changes;
    }

    private void removeFieldValue(BibEntry entry, Field field, List<FieldChange> changes) {
        CleanupJob eraser = new FieldFormatterCleanup(field, new ClearFormatter());
        changes.addAll(eraser.cleanup(entry));
    }
}
