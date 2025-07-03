package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.dto.GsonFactory;
import org.jabref.http.dto.cayw.SimpleJson;
import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import org.jvnet.hk2.annotations.Service;

@Service
public class SimpleJsonFormatter implements CAYWFormatter {

    @Override
    public String getFormatName() {
        return "simple-json";
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        List<SimpleJson> simpleJsons = caywEntries.stream()
                                              .map(caywEntry -> SimpleJson.fromBibEntry(caywEntry.getBibEntry()))
                                              .toList();
        return new GsonFactory().provide().toJson(simpleJsons);
    }
}
