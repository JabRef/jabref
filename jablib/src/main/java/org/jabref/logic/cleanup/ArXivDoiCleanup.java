package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;

/// Consolidates the arXiv identifier of an entry onto the DOI field.
///
/// arXiv articles are automatically assigned a stable DOI with the prefix `10.48550/arXiv.`,
/// followed by the arXiv ID. Replacing an article with a new version does **not** generate a new
/// DOI; the metadata of the existing DOI is updated at DataCite instead. The DOI is therefore the
/// canonical, version-stable identifier and is preferred over the (redundant) eprint field.
///
/// See the [arXiv DOI documentation](https://info.arxiv.org/help/doi.html).
///
/// Rules:
///  1. eprint holds an arXiv ID and the DOI field is empty -> create the DOI from the eprint.
///  2. the DOI field holds an arXiv DOI matching the eprint -> drop the redundant eprint fields.
///  3. otherwise (e.g. a real publisher DOI next to an arXiv eprint, or no arXiv data) -> keep both.
///
/// This job is self-contained and order-independent with respect to {@link DoiCleanup} and
/// {@link EprintCleanup}: it re-parses the raw field values via {@link DOI#parse} and
/// {@link ArXivIdentifier#parse}, both of which tolerate URL/prefix forms.
public class ArXivDoiCleanup implements CleanupJob {

    /// arXiv DOIs use the prefix "10.48550/arXiv." (the DOI registrant assigned to arXiv).
    private static final String ARXIV_DOI_PREFIX = "10.48550/arxiv.";

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<ArXivIdentifier> eprint = entry.getField(StandardField.EPRINT)
                                                .flatMap(ArXivIdentifier::parse);

        // Rule 1: create the DOI from the arXiv eprint if no DOI is present yet.
        if (eprint.isPresent() && entry.getField(StandardField.DOI).isEmpty()) {
            eprint.flatMap(ArXivIdentifier::inferDOI)
                  .ifPresent(inferredDoi -> entry.setField(StandardField.DOI, inferredDoi.asString())
                                                 .ifPresent(changes::add));
        }

        // Rule 2: if the DOI is the arXiv DOI of the eprint, the eprint fields are redundant.
        Optional<String> arXivIdInDoi = entry.getField(StandardField.DOI)
                                             .flatMap(DOI::parse)
                                             .filter(ArXivDoiCleanup::isArXivDoi)
                                             .flatMap(ArXivDoiCleanup::arXivIdFromDoi);
        Optional<String> arXivIdInEprint = eprint.map(ArXivIdentifier::asStringWithoutVersion);
        if (arXivIdInDoi.isPresent() && arXivIdInDoi.equals(arXivIdInEprint)) {
            removeFieldValue(entry, StandardField.EPRINT, changes);
            removeFieldValue(entry, StandardField.EPRINTTYPE, changes);
            removeFieldValue(entry, StandardField.EPRINTCLASS, changes);
        }

        return changes;
    }

    private static boolean isArXivDoi(DOI doi) {
        return doi.asString().toLowerCase(Locale.ROOT).startsWith(ARXIV_DOI_PREFIX);
    }

    /// Extracts the normalized arXiv ID from an arXiv DOI (the part after the "10.48550/arXiv." prefix).
    private static Optional<String> arXivIdFromDoi(DOI doi) {
        String id = doi.asString().substring(ARXIV_DOI_PREFIX.length());
        return ArXivIdentifier.parse(id).map(ArXivIdentifier::asStringWithoutVersion);
    }

    private void removeFieldValue(BibEntry entry, Field field, List<FieldChange> changes) {
        CleanupJob eraser = new FieldFormatterCleanup(field, new ClearFormatter());
        changes.addAll(eraser.cleanup(entry));
    }
}
