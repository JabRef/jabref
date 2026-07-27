package org.jabref.logic.ocr;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OcrPreferences {
    private final StringProperty ocrEnginePath;
    private final ObjectProperty<PagesWithTextHandling> pagesWithTextHandling;

    private OcrPreferences() {
        this("ocrmypdf", PagesWithTextHandling.SKIP);
    }

    public OcrPreferences(String ocrEnginePath, PagesWithTextHandling pagesWithTextHandling) {
        this.ocrEnginePath = new SimpleStringProperty(ocrEnginePath);
        this.pagesWithTextHandling = new SimpleObjectProperty<>(pagesWithTextHandling);
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

    public PagesWithTextHandling getPagesHaveText() {
        return pagesWithTextHandling.get();
    }

    public ObjectProperty<PagesWithTextHandling> pagesHaveTextProperty() {
        return pagesWithTextHandling;
    }

    public void setPagesHaveText(PagesWithTextHandling pagesHaveText) {
        this.pagesWithTextHandling.set(pagesHaveText);
    }

    public static OcrPreferences getDefault() {
        return new OcrPreferences();
    }
}
