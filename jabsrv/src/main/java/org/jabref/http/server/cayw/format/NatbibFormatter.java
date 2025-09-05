package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

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

        List<BibEntry> bibEntries = caywEntries.stream()
                                               .map(CAYWEntry::bibEntry)
                                               .toList();

        return "\\%s{%s}".formatted(command,
                bibEntries.stream()
                          .map(BibEntry::getCitationKey)
                          .flatMap(Optional::stream)
                          .collect(Collectors.joining(",")));
    }
}
