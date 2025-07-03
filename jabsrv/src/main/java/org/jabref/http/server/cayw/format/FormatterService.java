package org.jabref.http.server.cayw.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import org.jvnet.hk2.annotations.Service;

@Service
public class FormatterService {

    private final Map<String, CAYWFormatter> formatters;

    public FormatterService() {
        this.formatters = new HashMap<>();
        registerFormatter(new SimpleJsonFormatter());
        registerFormatter(new BibLatexFormatter());
    }

    public void registerFormatter(CAYWFormatter formatter) {
        formatters.putIfAbsent(formatter.getFormatName(), formatter);
    }

    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) throws IllegalArgumentException {
        CAYWFormatter formatter = formatters.get(queryParams.getFormat().toLowerCase());
        return formatter.format(queryParams, caywEntries);
    }
}
