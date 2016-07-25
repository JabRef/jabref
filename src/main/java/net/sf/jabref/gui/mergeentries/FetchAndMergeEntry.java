package net.sf.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.importer.fetcher.DOItoBibTeXFetcher;
import net.sf.jabref.importer.fetcher.ISBNtoBibTeXFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class FetchAndMergeEntry {


    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, String field) {
        this(entry, panel, Arrays.asList(field));
    }


    public FetchAndMergeEntry(BibEntry entry, BasePanel panel, List<String> fields) {
        for (String field : fields) {
            Optional<String> fieldContent = entry.getFieldOptional(field);

            // Get better looking name for status messages
            String type;
            switch(field) {
            case FieldName.DOI:
                type = "DOI";
                break;
            case FieldName.ISBN:
                type = "ISBN";
                break;
            default:
                type = field;
                break;
            }

            if (fieldContent.isPresent()) {
                Optional<BibEntry> fetchedEntry = Optional.empty();
                // Get entry based on field
                if (FieldName.DOI.equals(field)) {
                    fetchedEntry = new DOItoBibTeXFetcher().getEntryFromDOI(fieldContent.get());
                } else if (FieldName.ISBN.equals(field)) {
                    fetchedEntry = new ISBNtoBibTeXFetcher().getEntryFromISBN(fieldContent.get(), null);
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
}
