package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.logic.preview.PreviewPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.jsoup.Jsoup;
import org.jvnet.hk2.annotations.Service;

@Service
public class FormattedBibliographyFormatter implements CAYWFormatter {

    private final PreviewPreferences previewPreferences;
    private final BibDatabaseContext databaseContext;

    public FormattedBibliographyFormatter(PreviewPreferences previewPreferences, BibDatabaseContext databaseContext) {
        this.previewPreferences = previewPreferences;
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

        return Jsoup.parse(previewPreferences.getSelectedPreviewLayout().generatePreview(bibEntries, databaseContext)).text();
    }
}
