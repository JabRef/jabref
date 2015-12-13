package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * Formats the DOI (e.g. removes http part) and also moves DOIs from note, url or ee field to the doi field.
 */
public class DoiCleanup implements Cleaner {

    /**
     * Fields to check for DOIs.
     */
    String[] fields = {"note", "url", "ee"};


    public DoiCleanup() {

    }

    @Override
    public List<FieldChange> cleanup(BibtexEntry entry) {

        ArrayList<FieldChange> changes = new ArrayList<>();

        // First check if the Doi Field is empty
        if (entry.getField("doi") != null) {
            String doiFieldValue = entry.getField("doi");

            Optional<DOI> doi = DOI.build(doiFieldValue);

            if (doi.isPresent()) {
                String newValue = doi.get().getDOI();
                if (!doiFieldValue.equals(newValue)) {
                    entry.setField("doi", newValue);

                    FieldChange change = new FieldChange(entry, "doi", doiFieldValue, newValue);
                    changes.add(change);
                }

                // Doi field seems to contain Doi -> cleanup note, url, ee field
                for (String field : fields) {
                    DOI.build(entry.getField((field))).ifPresent(unused -> removeFieldValue(entry, field, changes));
                }
            }
        } else {
            // As the Doi field is empty we now check if note, url, or ee field contains a Doi
            for (String field : fields) {
                Optional<DOI> doi = DOI.build(entry.getField(field));

                if (doi.isPresent()) {
                    // update Doi
                    String oldValue = entry.getField("doi");
                    String newValue = doi.get().getDOI();

                    entry.setField("doi", newValue);

                    FieldChange change = new FieldChange(entry, "doi", oldValue, newValue);
                    changes.add(change);

                    removeFieldValue(entry, field, changes);
                }
            }
        }

        return changes;
    }

    private void removeFieldValue(BibtexEntry entry, String field, ArrayList<FieldChange> changes) {
        RemoveFieldCleanup cleaner = new RemoveFieldCleanup(field);
        changes.addAll(cleaner.cleanup(entry));
    }
}
