package org.jabref.logic.ocr;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OcrPreferences {
    private final StringProperty ocrEnginePath;

    private OcrPreferences() {
        this("ocrmypdf");
    }

    public OcrPreferences(String ocrEnginePath) {
        this.ocrEnginePath = new SimpleStringProperty(ocrEnginePath);
    }

    public String getOcrEnginePath() {
        return ocrEnginePath.get();
    }

    public StringProperty ocrEnginePathProperty() {
        return ocrEnginePath;
    }

    public void setOcrEnginePath(String ocrEnginePath) {
        this.ocrEnginePath.set(ocrEnginePath);
    }

    public static OcrPreferences getDefault() {
        return new OcrPreferences();
    }

    public void setAll(OcrPreferences preferences) {
        this.ocrEnginePath.set(preferences.getOcrEnginePath());
    }
}
