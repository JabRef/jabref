package org.jabref.logic.importer.util;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.metadata.SaveActionsDTO;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class SaveActionsDTOConverter {
    private static final Gson GSON = new Gson();
    private static final String FIELD_FORMATTER_CLEANUPS = "fieldFormatterCleanups";
    private static final Type SAVE_ACTIONS_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    public static SaveActionsDTO fromJson(JsonObject saveActionsJson) throws ParseException {
        JsonElement stateElement = saveActionsJson.get("state");
        if ((stateElement == null) || !stateElement.isJsonPrimitive() || !stateElement.getAsJsonPrimitive().isBoolean()) {
            throw new ParseException("Missing or invalid 'state' in save actions JSON metadata");
        }

        JsonElement fieldFormatterCleanupsElement = saveActionsJson.get(FIELD_FORMATTER_CLEANUPS);
        if ((fieldFormatterCleanupsElement == null) || !fieldFormatterCleanupsElement.isJsonObject()) {
            throw new ParseException("Missing or invalid 'fieldFormatterCleanups' in save actions JSON metadata");
        }

        Map<String, List<String>> fieldFormatterCleanups;
        try {
            fieldFormatterCleanups = GSON.fromJson(fieldFormatterCleanupsElement, SAVE_ACTIONS_TYPE);
        } catch (JsonParseException | IllegalStateException exception) {
            throw new ParseException("Could not parse 'fieldFormatterCleanups' in save actions JSON metadata", exception);
        }

        SaveActionsDTO saveActionsDTO = new SaveActionsDTO();
        saveActionsDTO.state = stateElement.getAsBoolean();
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
