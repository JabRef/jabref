package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class BibLatexFormatter implements CAYWFormatter {

    private static final Set<String> STAR_COMMANDS = Set.of("cite", "autocite", "parencite");

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

        /// Single entry if suppress author + supported command
        if (caywEntries.size() == 1) {
            return formatSingleEntry(caywEntries.getFirst(), command);
        }

        /// Multiple entries with s-affixed, never starred
        return "\\%ss%s".formatted(command,
                caywEntries.stream()
                           .map(this::formatMulticiteEntry)
                           .flatMap(Optional::stream)
                           .collect(Collectors.joining("")));
    }

    private String formatSingleEntry(CAYWEntry entry, String command) {
        return entry.bibEntry().getCitationKey().map(key -> {
            CitationProperties props = entry.citationProperties();
            String prenote = props.getPrefix().orElse("");
            String postnote = props.getPostnote().orElse("");

            String star = (props.isOmitAuthor() && STAR_COMMANDS.contains(command)) ? "*" : "";

            if (prenote.isEmpty() && postnote.isEmpty()) {
                return "\\%s%s{%s}".formatted(command, star, key);
            }
            return "\\%s%s[%s][%s]{%s}".formatted(command, star, prenote, postnote, key);
        }).orElse("");
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
