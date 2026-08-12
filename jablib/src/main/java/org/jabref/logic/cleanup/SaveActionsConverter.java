package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.SaveActionsDTO;

public class SaveActionsConverter {
    public static FieldFormatterCleanupActions fromDTO(SaveActionsDTO saveActionsDTO) {
        List<FieldFormatterCleanup> actions = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : saveActionsDTO.actions.entrySet()) {
            Field field = FieldFactory.parseField(entry.getKey());
            for (String formatterName : entry.getValue()) {
                actions.add(new FieldFormatterCleanup(field, FieldFormatterCleanupActions.getFormatterFromString(formatterName)));
            }
        }
        return new FieldFormatterCleanupActions(saveActionsDTO.state, actions);
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
