package org.jabref.model.cleanup;

public enum BooktitleCleanupField {
    MONTH("Month", BooktitleCleanupAction.REPLACE_IF_EMPTY),
    YEAR("Year", BooktitleCleanupAction.REPLACE_IF_EMPTY),
    PAGE_RANGE("Page Range", BooktitleCleanupAction.REPLACE_IF_EMPTY),
    LOCATION("Location", BooktitleCleanupAction.REPLACE_IF_EMPTY);

    private final String displayName;
    private final BooktitleCleanupAction defaultAction;

    BooktitleCleanupField(String displayName, BooktitleCleanupAction defaultAction) {
        this.displayName = displayName;
        this.defaultAction = defaultAction;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BooktitleCleanupAction getDefaultAction() {
        return defaultAction;
    }
}
