package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Clas used for
public class SaveCleanupActions {
    public static final List<CleanupJob> DEFAULT_SAVE_ACTIONS;
    public static final List<CleanupJob> RECOMMEND_BIBTEX_ACTIONS;
    public static final List<CleanupJob> RECOMMEND_BIBLATEX_ACTIONS;

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveCleanupActions.class);

    static {
        DEFAULT_SAVE_ACTIONS = FieldFormatterCleanupActions.DEFAULT_SAVE_ACTIONS
                .stream()
                .map(fieldFormatterCleanup -> (CleanupJob) fieldFormatterCleanup).collect(Collectors.toList());

        RECOMMEND_BIBTEX_ACTIONS = FieldFormatterCleanupActions.RECOMMEND_BIBTEX_ACTIONS
                .stream()
                .map(fieldFormatterCleanup -> (CleanupJob) fieldFormatterCleanup).collect(Collectors.toList());

        RECOMMEND_BIBLATEX_ACTIONS = FieldFormatterCleanupActions.RECOMMEND_BIBLATEX_ACTIONS
                .stream()
                .map(fieldFormatterCleanup -> (CleanupJob) fieldFormatterCleanup).collect(Collectors.toList());
    }

    private final boolean enabled;
    private final List<CleanupJob> actions;

    public SaveCleanupActions(boolean enabled, @NonNull List<CleanupJob> actions) {
        this.enabled = enabled;
        this.actions = actions;
    }

    /// Note: String parsing is done at [SaveCleanupActionsMapper#parseActions(String)]
    public static String getMetaDataString(List<CleanupJob> actionList, String newLineSeparator) {
        return SaveCleanupActionsMapper.serializeActions(actionList, newLineSeparator);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<CleanupJob> getConfiguredActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<FieldChange> applySaveActions(BibEntry entry) {
        if (enabled) {
            return applyAllActions(entry);
        } else {
            return List.of();
        }
    }

    private List<FieldChange> applyAllActions(BibEntry entry) {
        List<FieldChange> result = new ArrayList<>();

        for (CleanupJob action : actions) {
            result.addAll(action.cleanup(entry));
        }

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, enabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SaveCleanupActions other) {
            return Objects.equals(actions, other.actions) && (enabled == other.enabled);
        }
        return false;
    }

    @Override
    public String toString() {
        return "SaveCleanupActions{" +
                "enabled=" + enabled + "," +
                "actions=" + actions +
                "}";
    }
}
