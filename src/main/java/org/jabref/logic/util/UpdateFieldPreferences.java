package org.jabref.logic.util;

import org.jabref.model.entry.field.Field;

public class UpdateFieldPreferences {

    private final boolean useOwner;
    private final boolean useTimeStamp;
    private final boolean overwriteOwner;
    private final boolean overwriteTimeStamp;
    private final Field timeStampField;
    private final String timeStampFormat;
    private final String defaultOwner;


    public UpdateFieldPreferences(boolean useOwner, boolean overwriteOwner, String defaultOwner, boolean useTimeStamp,
                                  boolean overwriteTimeStamp, Field timeStampField,
                                  String timeStampFormat) {
        this.useOwner = useOwner;
        this.overwriteOwner = overwriteOwner;
        this.defaultOwner = defaultOwner;
        this.useTimeStamp = useTimeStamp;
        this.overwriteTimeStamp = overwriteTimeStamp;
        this.timeStampField = timeStampField;
        this.timeStampFormat = timeStampFormat;
    }

    public boolean isUseOwner() {
        return useOwner;
    }

    public boolean isUseTimeStamp() {
        return useTimeStamp;
    }

    public Field getTimeStampField() {
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
