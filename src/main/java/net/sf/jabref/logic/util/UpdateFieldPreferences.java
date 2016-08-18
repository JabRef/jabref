package net.sf.jabref.logic.util;

import net.sf.jabref.preferences.JabRefPreferences;

public class UpdateFieldPreferences {

    private final boolean useOwner;
    private final boolean useTimeStamp;
    private final boolean overwriteOwner;
    private final boolean overwriteTimeStamp;
    private final String timeStampField;
    private final String timeStampFormat;
    private final String defaultOwner;


    public UpdateFieldPreferences(boolean useOwner, boolean overwriteOwner, String defaultOwner, boolean useTimeStamp,
            boolean overwriteTimeStamp, String timeStampField,
            String timeStampFormat) {
        this.useOwner = useOwner;
        this.overwriteOwner = overwriteOwner;
        this.defaultOwner = defaultOwner;
        this.useTimeStamp = useTimeStamp;
        this.overwriteTimeStamp = overwriteTimeStamp;
        this.timeStampField = timeStampField;
        this.timeStampFormat = timeStampFormat;
    }

    public static UpdateFieldPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new UpdateFieldPreferences(jabRefPreferences.getBoolean(JabRefPreferences.USE_OWNER),
                jabRefPreferences.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                jabRefPreferences.get(JabRefPreferences.DEFAULT_OWNER),
                jabRefPreferences.getBoolean(JabRefPreferences.USE_TIME_STAMP),
                jabRefPreferences.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP),
                jabRefPreferences.get(JabRefPreferences.TIME_STAMP_FIELD),
                jabRefPreferences.get(JabRefPreferences.TIME_STAMP_FORMAT));
    }

    public boolean isUseOwner() {
        return useOwner;
    }


    public boolean isUseTimeStamp() {
        return useTimeStamp;
    }


    public String getTimeStampField() {
        return timeStampField;
    }


    public String getDefaultOwner() {
        return defaultOwner;
    }

    public String getTimeStampFormat() {
        return timeStampFormat;
    }

    public boolean isOverwriteOwner() {
        return overwriteOwner;
    }

    public boolean isOverwriteTimeStamp() {
        return overwriteTimeStamp;
    }
}
