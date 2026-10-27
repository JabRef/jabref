package org.jabref.logic.preferences;

import org.jabref.logic.importer.util.GrobidPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GrobidUrlMigration {
    private static final String LEGACY_GROBID_URL = "http://grobid.jabref.org:8070";

    private GrobidUrlMigration() {
    }

    public static void migrate(JabRefCliPreferences preferences) {
        if (LEGACY_GROBID_URL.equals(preferences.get(JabRefCliPreferences.GROBID_URL, ""))) {
            preferences.put(JabRefCliPreferences.GROBID_URL, GrobidPreferences.getDefault().getGrobidURL());
        }
    }
}
