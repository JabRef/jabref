package org.jabref.languageserver;

import com.google.gson.JsonObject;

public class ExtensionSettings {
    private boolean consistencyCheck;
    private boolean integrityCheck;

    private ExtensionSettings() {
        this.consistencyCheck = true;
        this.integrityCheck = true;
    }

    public static ExtensionSettings getDefaultSettings() {
        return new ExtensionSettings();
    }

    public void setConsistencyCheck(boolean consistencyCheck) {
        this.consistencyCheck = consistencyCheck;
    }

    public boolean isConsistencyCheck() {
        return consistencyCheck;
    }

    public void setIntegrityCheck(boolean integrityCheck) {
        this.integrityCheck = integrityCheck;
    }

    public boolean isIntegrityCheck() {
        return integrityCheck;
    }

    public void copyFromJsonObject(JsonObject json) {
        if (json.has("jabref") && json.get("jabref").isJsonObject()) {
            json = json.getAsJsonObject("jabref");
        }
        if (json.has("consistencyCheck") && json.get("consistencyCheck").isJsonPrimitive()) {
            this.consistencyCheck = json.get("consistencyCheck").getAsBoolean();
        }
        if (json.has("integrityCheck") && json.get("integrityCheck").isJsonPrimitive()) {
            this.integrityCheck = json.get("integrityCheck").getAsBoolean();
        }
    }

    @Override
    public String toString() {
        return "ExtensionSettings{" +
                "consistencyCheck=" + consistencyCheck +
                ", integrityCheck=" + integrityCheck +
                '}';
    }
}
