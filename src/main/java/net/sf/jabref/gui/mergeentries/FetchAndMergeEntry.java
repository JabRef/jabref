package net.sf.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.DOItoBibTeX;
import net.sf.jabref.logic.importer.fetcher.IsbnFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

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

    /**
     * Default constructor
     *
     * @param entry - BibEntry to fetch information for
     * @param panel - current BasePanel
     * @param fields - List of fields to get information from, one at a time in given order
     */

    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, List<String> fields) {
        for (String field : fields) {
            Optional<String> fieldContent = entry.getFieldOptional(field);

            // Get better looking name for status messages
            String type = FieldName.getDisplayName(field);

            if (fieldContent.isPresent()) {
                Optional<BibEntry> fetchedEntry = Optional.empty();
                // Get entry based on field
                if (FieldName.DOI.equals(field)) {
                    fetchedEntry = new DOItoBibTeX().getEntryFromDOI(fieldContent.get(),
                            ImportFormatPreferences.fromPreferences(Globals.prefs));
                } else if (FieldName.ISBN.equals(field)) {

                    try {
                        fetchedEntry = new IsbnFetcher().performSearchById(fieldContent.get());
                    } catch (FetcherException fe) {
                        fe.printStackTrace();
                    }

                } else if (FieldName.EPRINT.equals(field)) {
                    try {
                        fetchedEntry = new ArXiv().performSearchById(fieldContent.get());
                    } catch (FetcherException e) {
                        panel.frame().setStatus(
                                Localization.lang("Cannot get info based on given %0:_%1", type, fieldContent.get()));
                    }
                }

                if (fetchedEntry.isPresent()) {
                    MergeFetchedEntryDialog dialog = new MergeFetchedEntryDialog(panel, entry, fetchedEntry.get(),
                            type);
                    dialog.setVisible(true);
                } else {
                    panel.frame()
                            .setStatus(Localization.lang("Cannot get info based on given %0:_%1", type,
                                    fieldContent.get()));
                }
            } else {
                panel.frame().setStatus(Localization.lang("No %0 found", type));
            }
        }
    }

    public static String getDisplayNameOfSupportedFields() {
        return FieldName.orFields(SUPPORTED_FIELDS.stream().map(fieldName -> FieldName.getDisplayName(fieldName))
                .collect(Collectors.toList()));
    }
}
