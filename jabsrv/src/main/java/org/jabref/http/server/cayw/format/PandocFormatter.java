package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;

public class PandocFormatter implements CAYWFormatter {

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        List<BibEntry> bibEntries = caywEntries.stream()
                                               .map(CAYWEntry::bibEntry)
                                               .toList();

        return "[%s]".formatted(bibEntries.stream()
                                          .map(entry -> entry.getCitationKey().map("@%s"::formatted))
                                          .flatMap(Optional::stream)
                                          .collect(Collectors.joining("; ")));
    }
}
