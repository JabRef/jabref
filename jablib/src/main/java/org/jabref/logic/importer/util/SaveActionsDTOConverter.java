package org.jabref.logic.importer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.model.metadata.SaveActionsDTO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SaveActionsDTOConverter {
    public static SaveActionsDTO fromJson(JsonObject saveActionsJson) {
        SaveActionsDTO saveActionsDTO = new SaveActionsDTO();
        saveActionsDTO.state = saveActionsJson.get("state").getAsBoolean();
        for (Map.Entry<String, JsonElement> entry : saveActionsJson.entrySet()) {
            // Already parsed before
            if ("state".equals(entry.getKey())) {
                continue;
            }
            List<String> actions = new ArrayList<>();
            for (JsonElement action : entry.getValue().getAsJsonArray()) {
                actions.add(action.getAsString());
            }
            saveActionsDTO.actions.put(entry.getKey(), actions);
        }
        return saveActionsDTO;
    }

    public static JsonObject toJson(SaveActionsDTO saveActionsDTO) {
        JsonObject saveActionsJson = new JsonObject();
        saveActionsJson.addProperty("state", saveActionsDTO.state);
        for (Map.Entry<String, List<String>> entry : saveActionsDTO.actions.entrySet()) {
            JsonArray formatters = new JsonArray();
            for (String formatter : entry.getValue()) {
                formatters.add(formatter);
            }
            saveActionsJson.add(entry.getKey(), formatters);
        }
        return saveActionsJson;
    }
}
