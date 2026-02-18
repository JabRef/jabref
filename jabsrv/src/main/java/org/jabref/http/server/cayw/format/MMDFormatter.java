package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.MediaType;

public class MMDFormatter implements CAYWFormatter {
    private static final String MMD_BRACKET_SEPARATOR = new String(new char[] {'\\', ']', '\\', '['});

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

            String prefix = props.getPrefix().orElse(null);
            String postnote = props.getPostnote().orElse(null);

            /// Both prefix and postnote
            if (prefix != null && postnote != null) {
                return "[" + prefix + MMD_BRACKET_SEPARATOR + postnote + "][#" + key + "]";
            }

            if (prefix != null) {
                return "[" + prefix + "][#" + key + "]";
            }

            if (postnote != null) {
                return "[" + MMD_BRACKET_SEPARATOR + postnote + "][#" + key + "]";
            }
            
            return "[#" + key + "]";
        });
    }
}
