package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class nabbingFormatter implements CAYWFormatter {

    @Override
    public String getFormatName() {
        return "nabbing";
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> cawEntries) {
        String command = queryParams.getCommand();
        if (command == null || command.isBlank()) {
            command = "citep"; // default for natbib
        }

        List<BibEntry> bibEntries = cawEntries.stream()
                                              .map(CAYWEntry::bibEntry)
                                              .toList();

        return "\\%s{%s}".formatted(command,
                bibEntries.stream()
                          .map(entry -> entry.getCitationKey().orElse(""))
                          .collect(Collectors.joining(",")));
    }
}
