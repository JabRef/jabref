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
    private final ListProperty<String> ocrLanguages;

    private OcrPreferences() {
        this("ocrmypdf", PagesWithTextHandling.SKIP, EngineSelection.OCRMYPDF, List.of("eng"));
    }

    public OcrPreferences(String ocrEnginePath, PagesWithTextHandling pagesWithTextHandling, EngineSelection engineSelection, List<String> ocrLanguages) {
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

    public ObservableList<String> getOcrLanguages() {
        return ocrLanguages.get();
    }

    public ListProperty<String> ocrLanguagesProperty() {
        return ocrLanguages;
    }

    public void setOcrLanguages(ObservableList<String> ocrLanguages) {
        this.ocrLanguages.set(ocrLanguages);
    }
}
