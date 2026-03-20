package org.jabref.logic.preferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jabref.model.metadata.MetaData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class ScopedPreferenceExporter {

    public static void exportToJson(Path filePath, CliPreferences globalPrefs, Map<String, MetaData> libraries) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject root = new JsonObject();

        // Global
        JsonObject globalObj = new JsonObject();
        globalObj.addProperty("addcreationdate", globalPrefs.getTimestampPreferences().shouldAddCreationDate());
        root.add("global", globalObj);

        // Libraries
        JsonObject librariesObj = new JsonObject();
        for (Map.Entry<String, MetaData> entry : libraries.entrySet()) {
            JsonObject libObj = new JsonObject();
            MetaData metaData = entry.getValue();

            boolean addCreationDate = metaData.getAddCreationDate()
                                              .orElse(globalPrefs.getTimestampPreferences().shouldAddCreationDate());

            libObj.addProperty("addcreationdate", addCreationDate);
            librariesObj.add(entry.getKey(), libObj);
        }
        root.add("libraries", librariesObj);

        Files.writeString(Path.of(filePath + ".json"), gson.toJson(root));
    }
}
