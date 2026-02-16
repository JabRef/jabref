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
public class BibLatexFormatter implements CAYWFormatter {

    private final String defaultCommand;

    public BibLatexFormatter(String defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        String command = queryParams.getCommand().orElse(defaultCommand);

        boolean anyHasProperties = caywEntries.stream()
                                              .anyMatch(e -> e.citationProperties().hasProperties());

        if (!anyHasProperties) {
            return "\\%s{%s}".formatted(command,
                    caywEntries.stream()
                               .map(e -> e.bibEntry().getCitationKey())
                               .flatMap(Optional::stream)
                               .collect(Collectors.joining(",")));
        }

        boolean anySuppressAuthor = caywEntries.stream()
                                               .anyMatch(e -> e.citationProperties().isOmitAuthor());
        String star = anySuppressAuthor ? "*" : "";

        return "\\%ss%s%s".formatted(command, star,
                caywEntries.stream()
                           .map(this::formatMulticiteEntry)
                           .flatMap(Optional::stream)
                           .collect(Collectors.joining("")));
    }

    private Optional<String> formatMulticiteEntry(CAYWEntry entry) {
        return entry.bibEntry().getCitationKey().map(key -> {
            CitationProperties props = entry.citationProperties();
            String prenote = props.getPrefix().orElse("");
            String postnote = props.getPostnote().orElse("");

            if (prenote.isEmpty() && postnote.isEmpty()) {
                return "{%s}".formatted(key);
            }
            return "[%s][%s]{%s}".formatted(prenote, postnote, key);
        });
    }
}
