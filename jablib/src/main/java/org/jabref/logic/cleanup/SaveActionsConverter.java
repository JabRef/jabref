package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.jabref.model.metadata.SaveActionsDTO;

public class SaveActionsConverter {
    public static FieldFormatterCleanupActions fromDTO(SaveActionsDTO saveActionsDTO) {
        boolean enabled = saveActionsDTO.state;
        StringBuilder actionsStringBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : saveActionsDTO.actions.entrySet()) {
            StringJoiner joiner = new StringJoiner(",");
            for (String formatter : entry.getValue()) {
                joiner.add(formatter);
            }
            actionsStringBuilder.append(entry.getKey())
                                .append("[")
                                .append(joiner)
                                .append(']');
        }
        List<FieldFormatterCleanup> actions = FieldFormatterCleanupMapper.parseActions(actionsStringBuilder.toString());
        return new FieldFormatterCleanupActions(enabled, actions);
    }

    public static SaveActionsDTO toDTO(FieldFormatterCleanupActions saveActions) {
        SaveActionsDTO saveActionsDTO = new SaveActionsDTO();
        saveActionsDTO.state = saveActions.isEnabled();
        for (FieldFormatterCleanup action : saveActions.getConfiguredActions()) {
            String field = action.getField().getName();
            String formatter = action.getFormatter().getKey();
            saveActionsDTO.actions
                    .computeIfAbsent(field, _ -> new ArrayList<>())
                    .add(formatter);
        }
        return saveActionsDTO;
    }
}
