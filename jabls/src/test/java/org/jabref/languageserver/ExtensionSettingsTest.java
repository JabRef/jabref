package org.jabref.languageserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionSettingsTest {

    @Test
    void copyFromJsonObject() {
        ExtensionSettings settings = ExtensionSettings.getDefaultSettings();
        settings.copyFromJsonObject(new com.google.gson.JsonParser().parse("""
                {
                  "jabref": {
                    "consistencyCheck": {
                      "enabled": false,
                      "required": false,
                      "optional": false,
                      "unknown": false
                    },
                    "integrityCheck": {
                      "enabled": false
                    }
                  }
                }
                """).getAsJsonObject());

        assertFalse(settings.isConsistencyCheck());
        assertFalse(settings.isConsistencyCheckRequired());
        assertFalse(settings.isConsistencyCheckOptional());
        assertFalse(settings.isConsistencyCheckUnknown());
        assertFalse(settings.isIntegrityCheck());
    }

    @Test
    void copyFromJsonObjectPartial() {
        ExtensionSettings settings = ExtensionSettings.getDefaultSettings();
        settings.copyFromJsonObject(new com.google.gson.JsonParser().parse("""
                {
                  "jabref": {
                    "consistencyCheck": {
                      "enabled": false
                    }
                  }
                }
                """).getAsJsonObject());

        assertFalse(settings.isConsistencyCheck());
        assertTrue(settings.isConsistencyCheckRequired());
        assertTrue(settings.isConsistencyCheckOptional());
        assertTrue(settings.isConsistencyCheckUnknown());
        assertTrue(settings.isIntegrityCheck());
    }
}
