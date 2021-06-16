package org.jabref.logic.cleanup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISSN;

public class ISSNCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<String> issnString = entry.getField(StandardField.ISSN);
        if (!issnString.isPresent()) {
            return Collections.emptyList();
        }

        ISSN issn = new ISSN(issnString.get());
        if (issn.isCanBeCleaned()) {
            String newValue = issn.getCleanedISSN();
            FieldChange change = new FieldChange(entry, StandardField.ISSN, issnString.get(), newValue);
            entry.setField(StandardField.ISSN, newValue);
            return Collections.singletonList(change);
        }
        return Collections.emptyList();
    }
}
