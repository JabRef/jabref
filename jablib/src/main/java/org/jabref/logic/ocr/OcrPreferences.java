package org.jabref.logic.ocr;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.ocr.docling.DoclingEngine;

public class OcrPreferences {
    private final ObjectProperty<EngineSelection> engineSelection;
    private final StringProperty ocrEnginePath;
    private final ObjectProperty<PagesWithTextHandling> pagesWithTextHandling;

    private OcrPreferences() {
        this("ocrmypdf", PagesWithTextHandling.SKIP, EngineSelection.OCRMYPDF);
    }

    public OcrPreferences(String ocrEnginePath, PagesWithTextHandling pagesWithTextHandling, EngineSelection engineSelection) {
        this.ocrEnginePath = new SimpleStringProperty(ocrEnginePath);
        this.pagesWithTextHandling = new SimpleObjectProperty<>(pagesWithTextHandling);
        this.engineSelection = new SimpleObjectProperty<>(engineSelection);
    }

    public EngineSelection getEngineSelection() {
        return engineSelection.get();
    }

    public ObjectProperty<EngineSelection> engineSelectionProperty() {
        return engineSelection;
    }

    public void setEngineSelection(EngineSelection engineSelection) {
        this.engineSelection.set(engineSelection);
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

    public OcrEngine getOcrEngine() {
        if (engineSelection.get() == EngineSelection.OCRMYPDF) {
            return new OcrMyPdfEngine(this);
        } else {
            return new DoclingEngine(this);
        }
    }
}
