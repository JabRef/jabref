package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.http.server.cayw.gui.CitationProperties;

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

            StringBuilder keyPart = new StringBuilder("#").append(key);
            props.getFormattedLocator().ifPresent(l -> keyPart.append(", ").append(l));
            props.getSuffix().ifPresent(s -> keyPart.append(", ").append(s));

            if (props.getPrefix().isPresent()) {
                return "[%s][%s]".formatted(props.getPrefix().get(), keyPart);
            }
            return "[%s][]".formatted(keyPart);
        });
    }
}
