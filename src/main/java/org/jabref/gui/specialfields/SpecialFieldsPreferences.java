package org.jabref.gui.specialfields;

public class SpecialFieldsPreferences {

    public static final int COLUMN_RANKING_WIDTH = 5 * 16; // Width of Ranking Icon Column

    private final boolean specialFieldsEnabled;
    private final boolean autoSyncSpecialFieldsToKeyWords;
    private final boolean serializeSpecialFields;

    public SpecialFieldsPreferences(boolean specialFieldsEnabled, boolean autoSyncSpecialFieldsToKeyWords, boolean serializeSpecialFields) {
        this.specialFieldsEnabled = specialFieldsEnabled;
        this.autoSyncSpecialFieldsToKeyWords = autoSyncSpecialFieldsToKeyWords;
        this.serializeSpecialFields = serializeSpecialFields;
    }

    public boolean isSpecialFieldsEnabled() {
        return specialFieldsEnabled;
    }

    public boolean shouldAutoSyncSpecialFieldsToKeyWords() {
        return autoSyncSpecialFieldsToKeyWords;
    }

    public boolean shouldSerializeSpecialFields() {
        return serializeSpecialFields;
    }

    public boolean isKeywordSyncEnabled() {
        return specialFieldsEnabled && autoSyncSpecialFieldsToKeyWords;
    }
}
