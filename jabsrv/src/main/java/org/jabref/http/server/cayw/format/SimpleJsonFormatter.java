package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.dto.cayw.SimpleJson;
import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jvnet.hk2.annotations.Service;

@Service
public class SimpleJsonFormatter implements CAYWFormatter {

    @Override
    public String getFormatName() {
        return "simple-json";
    }

    @Override
    public String format(CAYWQueryParams queryParams, List<CAYWEntry> caywEntries) {
        JsonArray jsonArray = new JsonArray();
        List<BibEntry> bibEntries = caywEntries.stream()
                                              .map(CAYWEntry::getValue)
                                              .toList();
        for (BibEntry bibEntry : bibEntries) {
            SimpleJson simpleJson = SimpleJson.fromBibEntry(bibEntry);
            jsonArray.add(toJson(simpleJson));
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray);
    }

    private JsonObject toJson(SimpleJson simpleJson) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", simpleJson.id());
        jsonObject.addProperty("citationKey", simpleJson.citationKey());
        return jsonObject;
    }
}
