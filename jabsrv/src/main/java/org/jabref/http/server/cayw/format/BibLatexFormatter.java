package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.HttpHeaders;
import org.jvnet.hk2.annotations.Service;

@Service
public class BibLatexFormatter implements CAYWFormatter {

    @Override
    public String getFormatName() {
        return "biblatex";
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        String command = queryParams.getCommand().orElse("autocite");

        List<BibEntry> bibEntries = caywEntries.stream()
                .map(CAYWEntry::getValue)
                .toList();

        return String.format("\\%s{%s}", command,
                bibEntries.stream()
                          .map(entry -> entry.getCitationKey().orElse(""))
                          .collect(Collectors.joining(",")));
    }
}
