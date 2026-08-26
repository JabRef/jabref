package org.jabref.logic.ocr;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class OcrPreferences {
    private final ObjectProperty<EngineSelection> engineSelection;
    private final StringProperty ocrEnginePath;
    private final ObjectProperty<PagesWithTextHandling> pagesWithTextHandling;
    private final ListProperty<OcrLanguage> ocrLanguages;

    private OcrPreferences() {
        this("ocrmypdf", PagesWithTextHandling.SKIP, EngineSelection.OCRMYPDF, List.of(OcrLanguage.ENGLISH));
    }

    public OcrPreferences(String ocrEnginePath, PagesWithTextHandling pagesWithTextHandling, EngineSelection engineSelection, List<OcrLanguage> ocrLanguages) {
        this.ocrEnginePath = new SimpleStringProperty(ocrEnginePath);
        this.pagesWithTextHandling = new SimpleObjectProperty<>(pagesWithTextHandling);
        this.engineSelection = new SimpleObjectProperty<>(engineSelection);
        this.ocrLanguages = new SimpleListProperty<>(FXCollections.observableArrayList(ocrLanguages));
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

    public ObservableList<OcrLanguage> getOcrLanguages() {
        return ocrLanguages.get();
    }

    public ListProperty<OcrLanguage> ocrLanguagesProperty() {
        return ocrLanguages;
    }

    public void setOcrLanguages(ObservableList<OcrLanguage> ocrLanguages) {
        this.ocrLanguages.set(ocrLanguages);
    }
}
