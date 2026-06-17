package org.jabref.logic.ocr;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.os.OS;

public class OcrPreferences {
    private final StringProperty ocrPath;

    public OcrPreferences() {
        String path;
        if (OS.LINUX) {
            path = "/usr/bin/ocrmypdf";
        } else if (OS.WINDOWS) {
            path = "C:\\Program Files\\OCRmyPDF\\ocrmypdf.exe";
        } else {
            path = "/usr/local/bin/ocrmypdf";
        }
        this(path);
    }

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
        return new OcrPreferences();
    }

    public void setAll(OcrPreferences preferences) {
        this.ocrPath.set(preferences.getOcrPath());
    }
}
