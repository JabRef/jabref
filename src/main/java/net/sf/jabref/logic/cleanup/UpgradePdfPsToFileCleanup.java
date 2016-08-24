package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

/**
 * Collects file links from the given set of fields, and add them to the list contained in the file field.
 */
public class UpgradePdfPsToFileCleanup implements CleanupJob {

    private final List<String> fields;

    public UpgradePdfPsToFileCleanup(List<String> fields) {
        this.fields = Objects.requireNonNull(fields);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // If there are already links in the file field, keep those on top:
        String oldFileContent = entry.getFieldOptional(FieldName.FILE).orElse(null);

        List<ParsedFileField> fileList = new ArrayList<>(FileField.parse(oldFileContent));
        int oldItemCount = fileList.size();
        for (String field : fields) {
            entry.getFieldOptional(field).ifPresent(o -> {
                if (o.trim().isEmpty()) {
                    return;
                }
                File f = new File(o);
                ParsedFileField flEntry = new ParsedFileField(f.getName(), o,
                        ExternalFileTypes.getInstance().getExternalFileTypeNameByExt(field));
                fileList.add(flEntry);

                entry.clearField(field);
                changes.add(new FieldChange(entry, field, o, null));
            });
        }

        if (fileList.size() != oldItemCount) {
            String newValue = FileField.getStringRepresentation(fileList);
            entry.setField(FieldName.FILE, newValue);
            changes.add(new FieldChange(entry, FieldName.FILE, oldFileContent, newValue));
        }

        return changes;
    }
}
