package org.jabref.languageserver;

import com.google.gson.JsonObject;
import org.hisp.dhis.jsontree.JsonMixed;
import org.slf4j.Logger;

public class ExtensionSettings {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExtensionSettings.class);

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
    }

    public static ExtensionSettings getDefaultSettings() {
        return new ExtensionSettings();
    }

    public void copyFromJsonObject(JsonObject object) {
        org.hisp.dhis.jsontree.JsonObject json = JsonMixed.of(object.toString());
        this.consistencyCheck = json.getBoolean("jabref.consistencyCheck.enabled").booleanValue(this.consistencyCheck);
        this.consistencyCheckRequired = json.getBoolean("jabref.consistencyCheck.required").booleanValue(this.consistencyCheckRequired);
        this.consistencyCheckOptional = json.getBoolean("jabref.consistencyCheck.optional").booleanValue(this.consistencyCheckOptional);
        this.consistencyCheckUnknown = json.getBoolean("jabref.consistencyCheck.unknown").booleanValue(this.consistencyCheckUnknown);
        this.integrityCheck = json.getBoolean("jabref.integrityCheck.enabled").booleanValue(this.integrityCheck);
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
