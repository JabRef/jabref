package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.MediaType;

public class MMDFormatter implements CAYWFormatter {

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        return caywEntries.stream()
                          .map(this::formatEntry)
                          .flatMap(Optional::stream)
                          .collect(Collectors.joining(""));
    }

    private Optional<String> formatEntry(CAYWEntry entry) {
        return entry.bibEntry().getCitationKey().map(key -> {
            CitationProperties props = entry.citationProperties();

            String sep = "\\]\\[";

            if (props.getPrefix().isPresent() && props.getPostnote().isEmpty()) {
                return "[" + props.getPrefix().get() + "][#" + key + "]";
            }

            if (props.getPrefix().isPresent() || props.getPostnote().isPresent()) {
                return "[" + props.getPrefix().orElse("") + sep + props.getPostnote().orElse("") + "][#" + key + "]";
            }

            return "[#" + key + "]";
        });
    }
}
