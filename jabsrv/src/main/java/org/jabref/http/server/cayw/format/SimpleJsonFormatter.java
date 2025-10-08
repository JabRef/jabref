package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.dto.GsonFactory;
import org.jabref.http.dto.cayw.SimpleJson;
import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;

import com.google.gson.Gson;
import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

@Service
public class SimpleJsonFormatter implements CAYWFormatter {

    private final Gson gson;

    public SimpleJsonFormatter() {
        this.gson = new GsonFactory().provide();
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.APPLICATION_JSON_TYPE;
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        List<SimpleJson> simpleJsons = caywEntries.stream()
                                                  .map(caywEntry -> SimpleJson.fromBibEntry(caywEntry.bibEntry()))
                                                  .toList();
        return gson.toJson(simpleJsons);
    }
}
