package org.jabref.http.server.cayw.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.MediaType;

public class TypstFormatter implements CAYWFormatter {

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        return caywEntries.stream()
                          .map(this::formatEntry)
                          .flatMap(Optional::stream)
                          .collect(Collectors.joining(" "));
    }

    private Optional<String> formatEntry(CAYWEntry entry) {
        return entry.bibEntry().getCitationKey().map(key -> {
            CitationProperties props = entry.citationProperties();

            /// <key> or label("key") for keys with /
            String keyRef = key.contains("/")
                            ? "label(\"%s\")".formatted(key)
                            : "<%s>".formatted(key);

            String locator = props.getFormattedLocator().orElse("");
            boolean omitAuthor = props.isOmitAuthor();

            /// #cite(<key>, supplement: [...], form: "year")
            List<String> args = new ArrayList<>();
            args.add(keyRef);

            if (!locator.isEmpty()) {
                args.add("supplement: [%s]".formatted(locator));
            }

            if (omitAuthor) {
                args.add("form: \"year\"");
            }

            return "#cite(%s)".formatted(String.join(", ", args));
        });
    }
}
