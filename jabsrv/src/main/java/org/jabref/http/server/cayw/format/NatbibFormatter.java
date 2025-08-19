package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class NatbibFormatter implements CAYWFormatter {

    @Override
    public List<String> getFormatNames() {
        return List.of("natbib", "nat");
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        String command = queryParams.getCommand();
        if (command == null) {
            command = "citep";
        }

        command = mapToNatbibCommand(command);

        List<BibEntry> bibEntries = caywEntries.stream()
                                               .map(CAYWEntry::bibEntry)
                                               .toList();

        return "\\%s{%s}".formatted(command,
                bibEntries.stream()
                          .map(entry -> entry.getCitationKey().orElse(""))
                          .collect(Collectors.joining(",")));
    }

    private String mapToNatbibCommand(String command) {
        return switch (command) {
            case "author" -> "citeauthor";
            case "textcite" -> "citet";
            case "year" -> "citeyear";
            default -> command;
        };
    }
}
