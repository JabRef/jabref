package org.jabref.logic.cleanup;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.DOI;

/// Formats the DOI (e.g. removes http part) and also infers DOIs from the note, url, eprint, ee, article-doi, location-id or source fields.
public class DoiCleanup implements CleanupJob {

    /// Fields to check for DOIs.
    private static final List<Field> FIELDS = List.of(
            StandardField.NOTE,
            StandardField.URL,
            StandardField.EPRINT,
            new UnknownField("ee"),
            new UnknownField("article-doi")
    );

    /// Fields that may contain a DOI alongside bibliographic metadata.
    private static final List<Field> EMBEDDED_DOI_FIELDS = List.of(
            new UnknownField("location-id"),
            new UnknownField("source")
    );

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        AtomicBoolean validDoiExistsInDoiField = new AtomicBoolean(false);

        entry.getField(StandardField.DOI)
             .ifPresent(currentlyStoredDoi -> {
                 String decodedDoiFieldValue;
                 try {
                     decodedDoiFieldValue = URLDecoder.decode(currentlyStoredDoi, StandardCharsets.UTF_8);
                 } catch (IllegalArgumentException _) {
                     // If decoding fails, we keep the original value
                     decodedDoiFieldValue = currentlyStoredDoi;
                 }

                 String cleanCurrentlyStoredDoi = decodedDoiFieldValue;

                 DOI.parse(cleanCurrentlyStoredDoi)
                    .map(DOI::asString)
                    .ifPresent(parsedDoi -> {
                        validDoiExistsInDoiField.set(true);
                        if (!parsedDoi.equals(cleanCurrentlyStoredDoi)) {
                            entry.setField(StandardField.DOI, parsedDoi);

                            FieldChange change = new FieldChange(entry, StandardField.DOI, currentlyStoredDoi, parsedDoi);
                            changes.add(change);
                        }

                        // Doi field seems to contain Doi -> cleanup note, url, eprint, ee, article-doi field
                        for (Field field : FIELDS) {
                            entry.getField(field)
                                 .flatMap(DOI::parse) // only returns something if **complete** field is a DOI
                                 .ifPresent(_ -> removeFieldValue(entry, field, changes));
                        }
                    });
             });

        for (Field field : FIELDS) {
            entry.getField(field)
                 .flatMap(DOI::parse) // covers a full DOI only
                 .ifPresent(doi -> {
                     if (!validDoiExistsInDoiField.get()) {
                         Optional<FieldChange> change = entry.setField(StandardField.DOI, doi.asString());
                         change.ifPresent(changes::add);
                     }
                     removeFieldValue(entry, field, changes);
                 });
        }

        if (entry.getField(StandardField.DOI).isEmpty()) {
            EMBEDDED_DOI_FIELDS.stream()
                               .map(entry::getField)
                               .flatMap(Optional::stream)
                               .flatMap(value -> DOI.findInText(value).stream())
                               .findFirst()
                               .ifPresent(doi -> entry.setField(StandardField.DOI, doi.asString()).ifPresent(changes::add));
        }

        return changes;
    }

    private void removeFieldValue(BibEntry entry, Field field, List<FieldChange> changes) {
        CleanupJob eraser = new FieldFormatterCleanup(field, new ClearFormatter());
        changes.addAll(eraser.cleanup(entry));
    }
}
