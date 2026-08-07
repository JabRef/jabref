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

            StringBuilder result = new StringBuilder();

            props.getPrefix().ifPresent(prefix -> result.append(prefix).append(" "));

            String citationKey = (props.isOmitAuthor() ? "-@" : "@") + key;
            result.append(citationKey);

            if (props.getFormattedLocator().isPresent() || props.getSuffix().isPresent()) {
                result.append(", ");
                props.getFormattedLocator().ifPresent(result::append);
                if (props.getFormattedLocator().isPresent()) {
                    props.getSuffix().ifPresent(suffix -> result.append(" ").append(suffix));
                } else {
                    props.getSuffix().ifPresent(result::append);
                }
            }

            return result.toString();
        });
    }
}
