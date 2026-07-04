package org.jabref.logic.ocr;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.l10n.Localization;

public class OcrPreferences {
    private final StringProperty ocrEnginePath;
    private final StringProperty pagesWithTextHandling;

    private OcrPreferences() {
        this("ocrmypdf", Localization.lang("Skip pages with text"));
    }

    public OcrPreferences(String ocrEnginePath, String pagesWithTextHandling) {
        this.ocrEnginePath = new SimpleStringProperty(ocrEnginePath);
        this.pagesWithTextHandling = new SimpleStringProperty(pagesWithTextHandling);
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

    public String getPagesHaveText() {
        return pagesWithTextHandling.get();
    }

    public StringProperty pagesHaveTextProperty() {
        return pagesWithTextHandling;
    }

    public void setPagesHaveText(String pagesHaveText) {
        this.pagesHaveTextProperty().set(pagesHaveText);
    }

    public static OcrPreferences getDefault() {
        return new OcrPreferences();
    }

    public String getOcrCommand() {
        if (getPagesHaveText().equals(Localization.lang("Skip pages with text"))) {
            return "--skip-text";
        } else {
            return "--force-ocr";
        }
    }
}
