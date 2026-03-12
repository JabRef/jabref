package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class NatbibFormatter implements CAYWFormatter {

    private final String defaultCommand;

    public NatbibFormatter(String defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        String command = queryParams.getCommand().orElse(defaultCommand);

        boolean hasAnyProperties = caywEntries.stream()
                                              .anyMatch(e -> e.citationProperties().hasProperties());

        if (!hasAnyProperties) {
            String keys = caywEntries.stream()
                                     .map(e -> e.bibEntry().getCitationKey())
                                     .flatMap(Optional::stream)
                                     .collect(Collectors.joining(","));
            return "\\%s{%s}".formatted(command, keys);
        }

        return caywEntries.stream()
                          .map(e -> formatEntry(e, command))
                          .flatMap(Optional::stream)
                          .collect(Collectors.joining(""));
    }

    private Optional<String> formatEntry(CAYWEntry entry, String command) {
        return entry.bibEntry().getCitationKey().map(key -> {
            CitationProperties props = entry.citationProperties();

            /// No prenote/postnote: \command{key}
            if (props.getPrefix().isEmpty() && props.getPostnote().isEmpty()) {
                return "\\%s{%s}".formatted(command, key);
            }

            /// With prenote/postnote: \command[prenote][postnote]{key}
            return "\\%s[%s][%s]{%s}".formatted(command, props.getPrefix().orElse(""), props.getPostnote().orElse(""), key);
        });
    }
}
