// ArxivCleanup.java
package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class ArxivCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<String> note = entry.getField(StandardField.NOTE);
        Optional<String> version = entry.getField(StandardField.VERSION);
        Optional<String> institution = entry.getField(StandardField.INSTITUTION);
        Optional<String> eid = entry.getField(StandardField.EID);

        if (note.isPresent() && note.get().contains("arXiv")) {
            String cleanedNote = note.get().replaceAll("\\s+", "");
            entry.setField(StandardField.EPRINT, cleanedNote);
            changes.add(new FieldChange(entry, StandardField.NOTE, note.get(), ""));
            entry.clearField(StandardField.NOTE);
        }

        if (institution.isPresent() && institution.get().equalsIgnoreCase("arxiv")) {
            entry.setField(StandardField.EPRINTTYPE, "arxiv");
            changes.add(new FieldChange(entry, StandardField.INSTITUTION, institution.get(), ""));
            entry.clearField(StandardField.INSTITUTION);
        }

        if (version.isPresent()) {
            entry.setField(StandardField.EPRINTCLASS, version.get());
            changes.add(new FieldChange(entry, StandardField.VERSION, version.get(), ""));
            entry.clearField(StandardField.VERSION);
        }

        if (eid.isPresent() && eid.get().contains("arXiv")) {
            String cleanedEid = eid.get().replaceAll("\\s+", "");
            entry.setField(StandardField.EPRINT, cleanedEid);
            changes.add(new FieldChange(entry, StandardField.EID, eid.get(), ""));
            entry.clearField(StandardField.EID);
        }

        return changes;

    }
}
