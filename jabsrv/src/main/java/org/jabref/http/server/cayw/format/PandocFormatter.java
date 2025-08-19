package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class PandocFormatter implements CAYWFormatter {
    @Override
    public List<String> getFormatNames() {
        return List.of("pandoc", "markdown");
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        String command = queryParams.getCommand();
        List<BibEntry> bibEntries = caywEntries.stream()
                                               .map(CAYWEntry::bibEntry)
                                               .toList();
        // Handle "parencite" command with square brackets
        if ("parencite".equalsIgnoreCase(command)) {
            return bibEntries.stream()
                             .map(bibEntry -> "[@" + bibEntry.getCitationKey().orElse("") + "]")
                             .collect(Collectors.joining(""));
        } else {
            // Default: bare @key format with semicolon separator
            return bibEntries.stream()
                             .map(bibEntry -> "@" + bibEntry.getCitationKey().orElse(""))
                             .collect(Collectors.joining("; "));
        }
    }
}
