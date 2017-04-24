package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

/**
 * Class for fetching and merging information based on a specific field
 *
 */
public class FetchAndMergeEntry {

    // A list of all field which are supported
    public static List<String> SUPPORTED_FIELDS = Arrays.asList(FieldName.DOI, FieldName.EPRINT, FieldName.ISBN);

    /**
     * Convenience constructor for a single field
     *
     * @param entry - BibEntry to fetch information for
     * @param panel - current BasePanel
     * @param field - field to get information from
     */
    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, String field) {
        this(entry, panel, Arrays.asList(field));
    }

    public FetchAndMergeEntry(BibEntry entry, String field) {
        this(entry, JabRefGUI.getMainFrame().getCurrentBasePanel(), field);
    }

    /**
     * Default constructor
     *
     * @param entry - BibEntry to fetch information for
     * @param panel - current BasePanel
     * @param fields - List of fields to get information from, one at a time in given order
     */
    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, List<String> fields) {
        for (String field : fields) {
            if (entry.hasField(field)) {
                new FetchAndMergeWorker(panel, entry, field).execute();
            } else {
                panel.frame().setStatus(Localization.lang("No %0 found", FieldName.getDisplayName(field)));
            }
        }
    }

    public static String getDisplayNameOfSupportedFields() {
        return FieldName.orFields(SUPPORTED_FIELDS.stream()
                .map(FieldName::getDisplayName)
                .collect(Collectors.toList()));
    }
}
