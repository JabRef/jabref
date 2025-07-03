package org.jabref.http.server.cayw.format;

import java.util.HashMap;
import java.util.Map;

import org.jvnet.hk2.annotations.Service;

@Service
public class FormatterService {

    private final Map<String, CAYWFormatter> formatters;

    public FormatterService() {
        this.formatters = new HashMap<>();
        registerFormatter(new SimpleJsonFormatter());
    }

    public void registerFormatter(CAYWFormatter formatter) {
        String formatName = formatter.getFormatName();
        if (formatters.containsKey(formatName)) {
            return;
        }
        formatters.put(formatName, formatter);
    }

    public CAYWFormatter getFormatter(String formatName) throws IllegalArgumentException {
        CAYWFormatter formatter = formatters.get(formatName);
        if (formatter == null) {
            throw new IllegalArgumentException("Unknown format: " + formatName);
        }
        return formatter;
    }
}
