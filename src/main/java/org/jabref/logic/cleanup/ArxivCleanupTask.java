package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class ArxivCleanupTask implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<String> noteField = entry.getField(StandardField.NOTE);
        Optional<String> versionField = entry.getField(StandardField.VERSION);
        Optional<String> institutionField = entry.getField(StandardField.INSTITUTION);
        Optional<String> eidField = entry.getField(StandardField.EID);

        if (noteField.isPresent() && noteField.get().contains("arXiv")) {
            String eprint = extractEprint(noteField.get());
            Optional<String> oldEprint = entry.getField(StandardField.EPRINT);
            changes.add(new FieldChange(entry, StandardField.EPRINT, oldEprint.orElse(null), eprint));
            entry.setField(StandardField.EPRINT, eprint);
        }

        if (institutionField.isPresent() && institutionField.get().equalsIgnoreCase("arxiv")) {
            Optional<String> oldEprintType = entry.getField(StandardField.EPRINTTYPE);
            changes.add(new FieldChange(entry, StandardField.EPRINTTYPE, oldEprintType.orElse(null), "arxiv"));
            entry.setField(StandardField.EPRINTTYPE, "arxiv");
        }

        if (versionField.isPresent()) {
            Optional<String> oldEprintClass = entry.getField(StandardField.EPRINTCLASS);
            changes.add(new FieldChange(entry, StandardField.EPRINTCLASS, oldEprintClass.orElse(null), versionField.get()));
            entry.setField(StandardField.EPRINTCLASS, versionField.get());
        }

        if (eidField.isPresent() && eidField.get().contains("arXiv")) {
            String eprint = extractEprint(eidField.get());
            Optional<String> oldEprint = entry.getField(StandardField.EPRINT);
            changes.add(new FieldChange(entry, StandardField.EPRINT, oldEprint.orElse(null), eprint));
            entry.setField(StandardField.EPRINT, eprint);
        }

        return changes;
    }

    private String extractEprint(String field) {
        return field.substring(field.indexOf("arXiv") + 6).trim();
    }
}
