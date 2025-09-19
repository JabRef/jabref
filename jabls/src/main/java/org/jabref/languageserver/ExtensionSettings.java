package org.jabref.languageserver;

import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ExtensionSettings {
    private final Gson gson;
    private boolean consistencyCheck;
    private boolean consistencyCheckRequired;
    private boolean consistencyCheckOptional;
    private boolean consistencyCheckUnknown;
    private boolean integrityCheck;

    private ExtensionSettings() {
        this.consistencyCheck = true;
        this.consistencyCheckRequired = true;
        this.consistencyCheckOptional = true;
        this.consistencyCheckUnknown = true;
        this.integrityCheck = true;
        this.gson = new GsonBuilder().create();
    }

    public static ExtensionSettings getDefaultSettings() {
        return new ExtensionSettings();
    }

    public void copyFromJsonObject(JsonObject json) {
        this.consistencyCheck = assignIfPresent(json, this.consistencyCheck, "jabref", "consistencyCheck", "enabled");
        this.consistencyCheckRequired = assignIfPresent(json, this.consistencyCheckRequired, "jabref", "consistencyCheck", "required");
        this.consistencyCheckOptional = assignIfPresent(json, this.consistencyCheckOptional, "jabref", "consistencyCheck", "optional");
        this.consistencyCheckUnknown = assignIfPresent(json, this.consistencyCheckUnknown, "jabref", "consistencyCheck", "unknown");
        this.integrityCheck = assignIfPresent(json, this.integrityCheck, "jabref", "integrityCheck", "enabled");
    }

    private boolean assignIfPresent(JsonObject obj, boolean current, String... path) {
        return assignIfPresent(obj, current, Boolean.class, path);
    }

    private <T> T assignIfPresent(JsonObject obj, T current, Class<T> type, String... path) {
        JsonObject currentObject = obj;
        for (String key : path) {
            Optional<JsonElement> element = Optional.ofNullable(currentObject.get(key));
            if (element.isEmpty()) {
                return current;
            }
            if (element.get().isJsonObject()) {
                currentObject = element.get().getAsJsonObject();
                continue;
            }
            try {
                T v = gson.fromJson(element.get(), type);
                if (v != null) {
                    return v;
                }
            } catch (JsonParseException ignore) {
                return current;
            }
        }
        return current;
    }

    public boolean isConsistencyCheck() {
        return consistencyCheck;
    }

    public boolean isIntegrityCheck() {
        return integrityCheck;
    }

    public boolean isConsistencyCheckRequired() {
        return consistencyCheckRequired;
    }

    public boolean isConsistencyCheckOptional() {
        return consistencyCheckOptional;
    }

    public boolean isConsistencyCheckUnknown() {
        return consistencyCheckUnknown;
    }

    @Override
    public String toString() {
        return "ExtensionSettings{" +
                "consistencyCheckRequired=" + consistencyCheckRequired +
                ", consistencyCheckOptional=" + consistencyCheckOptional +
                ", consistencyCheckUnknown=" + consistencyCheckUnknown +
                ", integrityCheck=" + integrityCheck +
                ", consistencyCheck=" + consistencyCheck +
                '}';
    }
}
