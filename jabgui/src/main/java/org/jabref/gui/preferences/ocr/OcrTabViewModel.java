package org.jabref.gui.preferences.ocr;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.ocr.OcrPreferences;

public class OcrTabViewModel implements PreferenceTabViewModel {
    private final StringProperty ocrPath = new SimpleStringProperty();

    private final OcrPreferences ocrPreferences;

    public OcrTabViewModel(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
    }

    @Override
    public void setValues() {
        ocrPath.setValue(ocrPreferences.getOcrPath());
    }

    @Override
    public void storeSettings() {
        ocrPreferences.setOcrPath(ocrPath.getValue());
    }

    public StringProperty ocrPathProperty() {
        return ocrPath;
    }
}
