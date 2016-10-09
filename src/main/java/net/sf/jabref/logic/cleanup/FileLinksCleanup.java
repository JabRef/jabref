package net.sf.jabref.logic.cleanup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

/**
 * Fixes the format of the file field. For example, if the file link is empty but the description wrongly contains the path.
 */
public class FileLinksCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<String> oldValue = entry.getField(FieldName.FILE);
        if (!oldValue.isPresent()) {
            return Collections.emptyList();
        }

        List<ParsedFileField> fileList = FileField.parse(oldValue.get());

        // Parsing automatically moves a single description to link, so we just need to write the fileList back again
        String newValue = FileField.getStringRepresentation(fileList);
        if (!oldValue.get().equals(newValue)) {
            entry.setField(FieldName.FILE, newValue);
            FieldChange change = new FieldChange(entry, FieldName.FILE, oldValue.get(), newValue);
            return Collections.singletonList(change);
        }
        return Collections.emptyList();
    }
}
