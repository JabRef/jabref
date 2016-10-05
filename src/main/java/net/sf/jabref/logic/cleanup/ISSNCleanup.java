package net.sf.jabref.logic.cleanup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.util.ISSN;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;


public class ISSNCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<String> issnString = entry.getField(FieldName.ISSN);
        if (!issnString.isPresent()) {
            return Collections.emptyList();
        }

        ISSN issn = new ISSN(issnString.get());
        if (issn.isCanBeCleaned()) {
            String newValue = issn.getCleanedISSN();
            FieldChange change = new FieldChange(entry, FieldName.ISSN, issnString.get(), newValue);
            entry.setField(FieldName.ISSN, newValue);
            return Collections.singletonList(change);
        }
        return Collections.emptyList();
    }

}
