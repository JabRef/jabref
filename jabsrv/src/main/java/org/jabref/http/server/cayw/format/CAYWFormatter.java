package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;

public interface CAYWFormatter {

    String getFormatName();

    String format(CAYWQueryParams caywQueryParams, List<CAYWEntry> caywEntries);
}
