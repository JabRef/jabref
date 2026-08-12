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
    private static final String FIELD_FORMATTER_CLEANUPS = "fieldFormatterCleanups";
    private static final Type SAVE_ACTIONS_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    public static SaveActionsDTO fromJson(JsonObject saveActionsJson) {
        SaveActionsDTO saveActionsDTO = new SaveActionsDTO();
        saveActionsDTO.state = saveActionsJson.get("state").getAsBoolean();

        JsonObject fieldFormatterCleanupsJson = saveActionsJson.getAsJsonObject(FIELD_FORMATTER_CLEANUPS);

        Map<String, List<String>> fieldFormatterCleanups = GSON.fromJson(fieldFormatterCleanupsJson, SAVE_ACTIONS_TYPE);
        if (fieldFormatterCleanups != null) {
            saveActionsDTO.fieldFormatterCleanups.putAll(fieldFormatterCleanups);
        }
        return saveActionsDTO;
    }

    public static JsonObject toJson(SaveActionsDTO saveActionsDTO) {
        JsonObject saveActionsJson = new JsonObject();
        saveActionsJson.addProperty("state", saveActionsDTO.state);
        saveActionsJson.add(
                FIELD_FORMATTER_CLEANUPS,
                GSON.toJsonTree(saveActionsDTO.fieldFormatterCleanups, SAVE_ACTIONS_TYPE).getAsJsonObject());
        return saveActionsJson;
    }
}
