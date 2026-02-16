package org.jabref.http.server.cayw.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.http.server.cayw.gui.CitationProperties;

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

            String keyRef = key.contains("/")
                            ? "label(\"%s\")".formatted(key)
                            : "<%s>".formatted(key);

            List<String> args = new ArrayList<>();
            args.add(keyRef);
            props.getFormattedLocator().ifPresent(l -> args.add("supplement: [%s]".formatted(l)));
            if (props.isOmitAuthor()) {
                args.add("form: \"year\"");
            }

            return "#cite(%s)".formatted(String.join(", ", args));
        });
    }
}
