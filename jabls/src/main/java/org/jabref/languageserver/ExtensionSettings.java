package org.jabref.languageserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
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
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(object.toString());
            this.consistencyCheck = node.at("/jabref/consistencyCheck/enabled").asBoolean(this.consistencyCheck);
            this.consistencyCheckRequired = node.at("/jabref/consistencyCheck/required").asBoolean(this.consistencyCheckRequired);
            this.consistencyCheckOptional = node.at("/jabref/consistencyCheck/optional").asBoolean(this.consistencyCheckOptional);
            this.consistencyCheckUnknown = node.at("/jabref/consistencyCheck/unknown").asBoolean(this.consistencyCheckUnknown);
            this.integrityCheck = node.at("/jabref/integrityCheck/enabled").asBoolean(this.integrityCheck);
        } catch (JsonProcessingException processingException) {
            LOGGER.error("Error parsing settings from JSON", processingException);
        }
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
