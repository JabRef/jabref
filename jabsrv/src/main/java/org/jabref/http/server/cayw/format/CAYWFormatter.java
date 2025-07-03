package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface CAYWFormatter {

    String getFormatName();

    String format(List<BibEntry> bibEntries);
}
