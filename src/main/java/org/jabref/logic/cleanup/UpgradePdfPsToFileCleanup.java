package org.jabref.logic.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;

/**
 * Collects file links from the ps and pdf fields, and add them to the list contained in the file field.
 */
public class UpgradePdfPsToFileCleanup implements CleanupJob {

    // Field name and file type name (from ExternalFileTypes)
    private final Map<String, String> fields = new HashMap<>();


    public UpgradePdfPsToFileCleanup() {
        fields.put(FieldName.PDF, "PDF");
        fields.put(FieldName.PS, "PostScript");
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // If there are already links in the file field, keep those on top:
        String oldFileContent = entry.getField(FieldName.FILE).orElse(null);

        List<LinkedFile> fileList = new ArrayList<>(entry.getFiles());
        int oldItemCount = fileList.size();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            entry.getField(field.getKey()).ifPresent(o -> {
                if (o.trim().isEmpty()) {
                    return;
                }
                File f = new File(o);
                LinkedFile flEntry = new LinkedFile(f.getName(), o, field.getValue());
                fileList.add(flEntry);

                entry.clearField(field.getKey());
                changes.add(new FieldChange(entry, field.getKey(), o, null));
            });
        }

        if (fileList.size() != oldItemCount) {
            String newValue = FileFieldWriter.getStringRepresentation(fileList);
            entry.setField(FieldName.FILE, newValue);
            changes.add(new FieldChange(entry, FieldName.FILE, oldFileContent, newValue));
        }

        return changes;
    }
}
