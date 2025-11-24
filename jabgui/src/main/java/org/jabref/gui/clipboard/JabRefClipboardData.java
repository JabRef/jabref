package org.jabref.gui.clipboard;

import java.util.Optional;

import org.jabref.gui.StateManager;
import org.jabref.model.TransferMode;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public record JabRefClipboardData(
        String bibDatabaseContextId,
        TransferMode transferMode
) {
    public static final JabRefClipboardData NULL_OBJECT = new JabRefClipboardData("-1", TransferMode.COPY);

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefClipboardData.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String asJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not create JSON", e);
            return "{}";
        }
    }

    /// In case of a conversion error, the NULL_OBJECT is returned
    public static JabRefClipboardData fromJson(String json) {
        try {
            return MAPPER.readValue(json, JabRefClipboardData.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not parse JSON", e);
            return NULL_OBJECT;
        }
    }

    public Optional<org.jabref.model.TransferInformation> toTransferInformation(StateManager stateManager) {
        return stateManager.getOpenDatabases().stream()
                           .filter(context -> context.getUid().equals(bibDatabaseContextId())).findFirst()
                           .map(context -> new org.jabref.model.TransferInformation(context, transferMode()));
    }
}
