package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class FormattedBibliographyFormatter implements CAYWFormatter {

    private final CliPreferences preferences;
    private final BibDatabaseContext databaseContext;

    public FormattedBibliographyFormatter(CliPreferences preferences, BibDatabaseContext databaseContext) {
        this.preferences = preferences;
        this.databaseContext = databaseContext;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        List<BibEntry> bibEntries = caywEntries.stream()
                                               .map(CAYWEntry::bibEntry)
                                               .toList();

        return bibEntries.stream()
                         .map(entry -> preferences.getPreviewPreferences().getSelectedPreviewLayout().generatePreview(entry, databaseContext))
                         .collect(Collectors.joining("\n\n"));
    }
}
