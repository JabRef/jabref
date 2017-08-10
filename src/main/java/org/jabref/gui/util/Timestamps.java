package org.jabref.gui.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

public class Timestamps {
    public static boolean includeTimestamps() {
        return Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP) && Globals.prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP);
    }

    public static boolean includeCreatedTimestamp() {
        return Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP);
    }

    public static String getFieldName() {
        return Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
    }

    public static String now() {
        String timeStampFormat = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT);
        return DateTimeFormatter.ofPattern(timeStampFormat).format(LocalDateTime.now());
    }
}
