package org.jabref.logic.cleanup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Collects file links from the ps and pdf fields, and add them to the list contained in the file field.
 */
public class UpgradePdfPsToFileCleanup implements CleanupJob {

    // Field name and file type name (from ExternalFileTypes)
    private final Map<Field, String> fields = new HashMap<>();

    public UpgradePdfPsToFileCleanup() {
        fields.put(StandardField.PDF, "PDF");
        fields.put(StandardField.PS, "PostScript");
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // If there are already links in the file field, keep those on top:
        String oldFileContent = entry.getField(StandardField.FILE).orElse(null);

        List<LinkedFile> fileList = new ArrayList<>(entry.getFiles());
        int oldItemCount = fileList.size();
        for (Map.Entry<Field, String> field : fields.entrySet()) {
            entry.getField(field.getKey()).ifPresent(fieldContent -> {
                if (fieldContent.trim().isEmpty()) {
                    return;
                }
                Path path = Path.of(fieldContent);
                LinkedFile flEntry = new LinkedFile(path.getFileName().toString(), path, field.getValue());
                fileList.add(flEntry);

                entry.clearField(field.getKey());
                changes.add(new FieldChange(entry, field.getKey(), fieldContent, null));
            });
        }

        if (fileList.size() != oldItemCount) {
            String newValue = FileFieldWriter.getStringRepresentation(fileList);
            entry.setField(StandardField.FILE, newValue);
            changes.add(new FieldChange(entry, StandardField.FILE, oldFileContent, newValue));
        }

        return changes;
    }
}
