package org.jabref.http.server.cayw.format;

import java.util.List;

import org.jabref.http.dto.cayw.SimpleJson;
import org.jabref.model.entry.BibEntry;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jvnet.hk2.annotations.Service;

@Service
public class SimpleJsonFormatter implements CAYWFormatter {

    public SimpleJsonFormatter() {
    }

    @Override
    public String getFormatName() {
        return "simple-json";
    }

    @Override
    public String format(List<BibEntry> bibEntries) {
        JsonArray jsonArray = new JsonArray();
        for (BibEntry bibEntry : bibEntries) {
            SimpleJson simpleJson = SimpleJson.fromBibEntry(bibEntry);
            jsonArray.add(toJson(simpleJson));
        }

        List<SimpleJson> simpleJsons = bibEntries.stream()
                .map(SimpleJson::fromBibEntry)
                .toList();
        return new GsonBuilder().setPrettyPrinting().create().toJson(simpleJsons);
    }

    private JsonObject toJson(SimpleJson simpleJson) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", simpleJson.id());
        jsonObject.addProperty("citationKey", simpleJson.citationKey());
        return jsonObject;
    }
}
