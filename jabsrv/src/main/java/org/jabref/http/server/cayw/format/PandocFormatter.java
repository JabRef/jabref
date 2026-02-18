package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.MediaType;

public class PandocFormatter implements CAYWFormatter {

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        return "[%s]".formatted(caywEntries.stream()
                                           .map(this::formatEntry)
                                           .flatMap(Optional::stream)
                                           .collect(Collectors.joining("; ")));
    }

    private Optional<String> formatEntry(CAYWEntry entry) {
        return entry.bibEntry().getCitationKey().map(key -> {
            CitationProperties props = entry.citationProperties();

            String prefix = props.getPrefix().orElse("");
            String citationKey = (props.isOmitAuthor() ? "-@" : "@") + key;
            String locator = props.getFormattedLocator().orElse("");
            String suffix = props.getSuffix().orElse("");

            StringBuilder result = new StringBuilder();

            if (!prefix.isEmpty()) {
                result.append(prefix).append(" ");
            }

            result.append(citationKey);

            if (!locator.isEmpty()) {
                result.append(", ").append(locator);
            }

            if (!suffix.isEmpty()) {
                result.append(", ").append(suffix);
            }

            return result.toString();
        });
    }
}
