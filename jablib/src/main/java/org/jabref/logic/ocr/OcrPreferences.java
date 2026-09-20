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
    private final ObjectProperty<OcrMyPdfPlugin> pluginSelection;

    private OcrPreferences(OcrMyPdfPlugin pluginSelection) {
        this("ocrmypdf", PagesWithTextHandling.SKIP, EngineSelection.OCRMYPDF, List.of(OcrLanguage.ENGLISH), pluginSelection);
    }

    public OcrPreferences(String ocrEnginePath, PagesWithTextHandling pagesWithTextHandling, EngineSelection engineSelection, List<OcrLanguage> ocrLanguages, OcrMyPdfPlugin pluginSelection) {
        this.ocrEnginePath = new SimpleStringProperty(ocrEnginePath);
        this.pagesWithTextHandling = new SimpleObjectProperty<>(pagesWithTextHandling);
        this.engineSelection = new SimpleObjectProperty<>(engineSelection);
        this.ocrLanguages = new SimpleListProperty<>(FXCollections.observableArrayList(ocrLanguages));
        this.pluginSelection = new SimpleObjectProperty<>(pluginSelection);
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

    public ObservableList<OcrLanguage> getOcrLanguages() {
        return ocrLanguages.get();
    }

    public ListProperty<OcrLanguage> ocrLanguagesProperty() {
        return ocrLanguages;
    }

    public void setOcrLanguages(ObservableList<OcrLanguage> ocrLanguages) {
        this.ocrLanguages.setAll(ocrLanguages);
    }

    public void setPluginSelection(OcrMyPdfPlugin pluginSelection) {
        this.pluginSelection.set(pluginSelection);
    }

    public OcrMyPdfPlugin getPluginSelection() {
        return pluginSelection.get();
    }

    public ObjectProperty<OcrMyPdfPlugin> pluginSelectionProperty() {
        return pluginSelection;
    }

    public static OcrPreferences getDefault() {
        return new OcrPreferences(OcrMyPdfPlugin.TESSERACT);
    }
}
