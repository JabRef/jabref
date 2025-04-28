package org.jabref.http.dto;

import java.io.IOException;
import java.io.StringWriter;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.SharedBibEntryData;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The data transfer object (DTO) for an BibEntry
 *
 * @param sharingMetadata the data used for sharing
 * @param userComments the comments before the BibTeX entry
 * @param citationKey the citation key (duplicated from BibEntry to ease processing by the client)
 * @param bibtex the BibEntry as BibTeX string (see ADR-0027 for more information, why we don't use a HashMap / JSON)
 */
public record BibEntryDTO(SharedBibEntryData sharingMetadata, String userComments, String citationKey, String bibtex) implements Comparable<BibEntryDTO> {

    public static final Logger LOGGER = LoggerFactory.getLogger(BibEntryDTO.class);

    public BibEntryDTO(BibEntry bibEntry, BibDatabaseMode bibDatabaseMode, FieldPreferences fieldWriterPreferences, BibEntryTypesManager bibEntryTypesManager) {
        this(bibEntry.getSharedBibEntryData(),
                bibEntry.getUserComments(),
                bibEntry.getCitationKey().orElse(""),
                convertToString(bibEntry, bibDatabaseMode, fieldWriterPreferences, bibEntryTypesManager)
        );
    }

    private static String convertToString(BibEntry entry, BibDatabaseMode bibDatabaseMode, FieldPreferences fieldWriterPreferences, BibEntryTypesManager bibEntryTypesManager) {
        StringWriter rawEntry = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntry, "\n");
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(fieldWriterPreferences), bibEntryTypesManager);
        try {
            bibtexEntryWriter.write(entry, bibWriter, bibDatabaseMode);
        } catch (IOException e) {
            LOGGER.warn("Problem creating BibTeX entry.", e);
            return "error";
        }
        return rawEntry.toString();
    }

    @Override
    public int compareTo(BibEntryDTO o) {
        int sharingComparison = sharingMetadata.compareTo(o.sharingMetadata);
        if (sharingComparison != 0) {
            return sharingComparison;
        }
        LOGGER.error("Comparing equal DTOs");
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("sharingMetadata", sharingMetadata)
                          .add("userComments", userComments)
                          .add("citationkey", citationKey)
                          .add("bibtex", bibtex)
                          .toString();
    }
}
