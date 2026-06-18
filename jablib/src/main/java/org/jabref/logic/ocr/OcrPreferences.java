package org.jabref.logic.ocr;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OcrPreferences {
    private final StringProperty ocrPath;

    public OcrPreferences(String ocrPath) {
        this.ocrPath = new SimpleStringProperty(ocrPath);
    }

    public String getOcrPath() {
        return ocrPath.get();
    }

    public StringProperty ocrPathProperty() {
        return ocrPath;
    }

    public void setOcrPath(String ocrPath) {
        this.ocrPath.set(ocrPath);
    }

    public static OcrPreferences getDefault() {
        return new OcrPreferences("");
    }

    public void setAll(OcrPreferences preferences) {
        this.ocrPath.set(preferences.getOcrPath());
    }
}
