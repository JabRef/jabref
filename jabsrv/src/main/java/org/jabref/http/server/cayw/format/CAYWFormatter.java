package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.server.cayw.gui.CAYWEntry;

import jakarta.ws.rs.core.HttpHeaders;

public interface CAYWFormatter {

    String getFormatName();

    String format(HttpHeaders httpHeaders, List<CAYWEntry> caywEntries);
}
