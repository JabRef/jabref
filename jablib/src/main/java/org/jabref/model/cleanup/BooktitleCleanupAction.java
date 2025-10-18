package org.jabref.model.cleanup;

public enum BooktitleCleanupAction {
    REMOVE_ONLY("Remove only"),
    REPLACE("Move and replace"),
    REPLACE_IF_EMPTY("Move if empty"),
    SKIP("Ignore");

    private final String displayName;

    BooktitleCleanupAction(String name) {
        this.displayName = name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
