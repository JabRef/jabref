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
///  1. eprint holds an arXiv ID and there is no valid DOI (absent or unparseable) -> create the DOI from the eprint.
///  2. the DOI field holds an arXiv DOI matching the eprint -> drop the redundant eprint fields.
///  3. otherwise (e.g. a real publisher DOI next to an arXiv eprint, or no arXiv data) -> keep both.
///
/// This job is self-contained and independent of cleanup job ordering. The consolidation reads the
/// already-normalized `eprint` and `doi` fields. When no consolidation is possible from the
/// fields as they are, the arXiv identifier might still live in a field such as url/journal/note, from
/// where [EprintCleanup] would move it into the eprint field. In that case this job applies
/// [EprintCleanup] itself, but only after probing (on a copy) that doing so actually enables a
/// consolidation - an entry without arXiv data is left untouched, regardless of whether
/// [EprintCleanup] runs separately or at all.
public class ArXivDoiCleanup implements CleanupJob {

    /// arXiv DOIs use the prefix "10.48550/arXiv." (the DOI registrant assigned to arXiv).
    private static final String ARXIV_DOI_PREFIX = "10.48550/arxiv.";

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        // First, try to consolidate using the fields as they currently are.
        List<FieldChange> changes = consolidate(entry);
        if (!changes.isEmpty()) {
            return changes;
        }

        // No consolidation was possible. The arXiv identifier might still sit in url/journal/note/etc.,
        // from where EprintCleanup would move it into the eprint field. Probe on a copy (which does not
        // fire entry listeners) whether running EprintCleanup first would enable a consolidation. Only
        // when it would do we touch the real entry; otherwise the entry is left untouched.
        BibEntry probe = new BibEntry(entry);
        new EprintCleanup().cleanup(probe);
        if (consolidate(probe).isEmpty()) {
            return List.of();
        }

        // Consolidation becomes possible once the eprint is populated: apply the eprint move and the
        // consolidation to the real entry.
        List<FieldChange> appliedChanges = new ArrayList<>(new EprintCleanup().cleanup(entry));
        appliedChanges.addAll(consolidate(entry));
        return appliedChanges;
    }

    /// Consolidates the arXiv identifier onto the DOI field using the already-populated `eprint`
    /// and `doi` fields. Returns the changes made, or an empty list when no rule applies.
    private List<FieldChange> consolidate(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<ArXivIdentifier> eprint = entry.getField(StandardField.EPRINT)
                                                .flatMap(ArXivIdentifier::parse);

        // Rule 1: create the DOI from the arXiv eprint if there is no valid DOI yet. A present-but-invalid
        // doi value (garbage that DOI.parse rejects) is treated as missing and gets overwritten; a valid
        // non-arXiv publisher DOI parses successfully and is preserved (rule 3).
        boolean noValidDoi = entry.getField(StandardField.DOI).flatMap(DOI::parse).isEmpty();
        if (eprint.isPresent() && noValidDoi) {
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
