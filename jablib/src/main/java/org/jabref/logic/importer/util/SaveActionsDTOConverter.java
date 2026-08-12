package org.jabref.logic.importer.util;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.jabref.model.metadata.SaveActionsDTO;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class SaveActionsDTOConverter {
    private static final Gson GSON = new Gson();
    private static final Type SAVE_ACTIONS_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    public static SaveActionsDTO fromJson(JsonObject saveActionsJson) {
        SaveActionsDTO saveActionsDTO = new SaveActionsDTO();
        saveActionsDTO.state = saveActionsJson.get("state").getAsBoolean();

        JsonObject actionsJson = saveActionsJson.deepCopy();
        actionsJson.remove("state");

        Map<String, List<String>> actions = GSON.fromJson(actionsJson, SAVE_ACTIONS_TYPE);
        if (actions != null) {
            saveActionsDTO.actions.putAll(actions);
        }
        return saveActionsDTO;
    }

    public static JsonObject toJson(SaveActionsDTO saveActionsDTO) {
        JsonObject saveActionsJson = new JsonObject();
        saveActionsJson.addProperty("state", saveActionsDTO.state);

        JsonObject actionsJson = GSON.toJsonTree(saveActionsDTO.actions, SAVE_ACTIONS_TYPE).getAsJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : actionsJson.entrySet()) {
            saveActionsJson.add(entry.getKey(), entry.getValue());
        }

        return saveActionsJson;
    }
}
