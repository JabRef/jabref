package org.jabref.logic.preferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jabref.model.metadata.MetaData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ScopedPreferenceImporter {

    public static void importFromJson(Path filePath, CliPreferences globalPrefs, Map<String, MetaData> libraries) throws IOException {
        Gson gson = new Gson();
        String json = Files.readString(filePath);
        JsonObject root = gson.fromJson(json, JsonObject.class);

        if (root.has("global")) {
            JsonObject globalObj = root.getAsJsonObject("global");
            if (globalObj.has("addcreationdate")) {
                boolean addCreationDate = globalObj.get("addcreationdate").getAsBoolean();
                globalPrefs.getTimestampPreferences().setAddCreationDate(addCreationDate);
            }
        }

        if (root.has("libraries")) {
            JsonObject librariesObj = root.getAsJsonObject("libraries");
            for (Map.Entry<String, MetaData> entry : libraries.entrySet()) {
                String libName = entry.getKey();
                if (librariesObj.has(libName)) {
                    JsonObject libObj = librariesObj.getAsJsonObject(libName);
                    if (libObj.has("addcreationdate")) {
                        boolean addCreationDate = libObj.get("addcreationdate").getAsBoolean();
                        entry.getValue().setAddCreationDate(addCreationDate);
                    }
                }
            }
        }
    }
}
