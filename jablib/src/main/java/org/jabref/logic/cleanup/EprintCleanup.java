package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;

/**
 * Formats the DOI (e.g. removes http part) and also moves DOIs from note, url or ee field to the doi field.
 * <p>
 * Background information on <a href="https://tex.stackexchange.com/questions/49757/what-should-an-entry-for-arxiv-entries-look-like-for-biblatex">tex.stackexchange</a>.
 */
public class EprintCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<String> version = entry.getField(StandardField.VERSION);
        Optional<String> institution = entry.getField(StandardField.INSTITUTION);

        for (Field field : List.of(
                StandardField.URL,
                StandardField.JOURNAL,
                StandardField.JOURNALTITLE,
                StandardField.VOLUME, // Sometimes, the LLM puts the arXiv ID in the volume field
                StandardField.NOTE,
                StandardField.EID,
                FieldFactory.parseField("arxiv"))) {
            entry.getField(field) // "getField" instead of "getFieldOrAlias", because the field is cleared later
                 .flatMap(ArXivIdentifier::parse)
                 .ifPresent(arXivIdentifier -> {
                     String normalizedEprint = arXivIdentifier.asString();

                     if (version.isPresent() && !normalizedEprint.contains("v" + version.get())) {
                         normalizedEprint += "v" + version.get();
                     }

                     if (institution.isPresent() && "arxiv".equalsIgnoreCase(institution.get())) {
                         entry.clearField(StandardField.INSTITUTION)
                              .ifPresent(changes::add);
                     }

                     entry.setField(StandardField.EPRINT, normalizedEprint)
                          .ifPresent(changes::add);

                     entry.setField(StandardField.EPRINTTYPE, "arxiv")
                          .ifPresent(changes::add);

                     arXivIdentifier.getClassification()
                                    .flatMap(classification -> entry.setField(StandardField.EPRINTCLASS, classification))
                                    .ifPresent(changes::add);

                     entry.clearField(field)
                          .ifPresent(changes::add);

                     if (StandardField.URL.equals(field)) {
                         // If we clear the URL field, we should also clear the URL-date field
                         entry.clearField(StandardField.URLDATE)
                              .ifPresent(changes::add);
                     }
                 });
        }

        entry.clearField(StandardField.VERSION).ifPresent(changes::add);

        // Remove `journal = {arXiv}` if present
        entry.getField(StandardField.JOURNAL)
             .filter(journal -> journal.toLowerCase().equals("arxiv"))
             .ifPresent(_ -> entry.clearField(StandardField.JOURNAL).ifPresent(changes::add));

        return changes;
    }
}
