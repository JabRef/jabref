package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;

public class TypstFormatter implements CAYWFormatter {

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
                         .map(entry -> entry.getCitationKey().map(key -> {
                             if (key.contains("/")) {
                                 return "#cite(label(\"%s\"))".formatted(key);
                             } else {
                                 return "#cite(<%s>)".formatted(key);
                             }
                         }))
                         .flatMap(Optional::stream)
                         .collect(Collectors.joining(" "));
    }
}
