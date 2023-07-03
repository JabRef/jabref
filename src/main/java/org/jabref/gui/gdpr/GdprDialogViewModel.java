package org.jabref.gui.gdpr;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.preferences.PreferencesService;

public class GdprDialogViewModel {
    private final BooleanProperty versionEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty webSearchEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty pdfMetaDataParserEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty relatedArticlesEnabledProperty = new SimpleBooleanProperty();

    private final PreferencesService preferencesService;

    GdprDialogViewModel(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;

        setValues();
    }

    public void setValues() {
        versionEnabledProperty.set(preferencesService.getInternalPreferences().isVersionCheckEnabled());
        webSearchEnabledProperty.set(preferencesService.getImporterPreferences().areImporterEnabled());
        pdfMetaDataParserEnabledProperty.set(preferencesService.getGrobidPreferences().isGrobidEnabled());
        relatedArticlesEnabledProperty.set(preferencesService.getMrDlibPreferences().shouldAcceptRecommendations());
    }

    public void storeSettings() {
        preferencesService.getInternalPreferences().setVersionCheckEnabled(versionEnabledProperty.get());
        preferencesService.getImporterPreferences().setImporterEnabled(webSearchEnabledProperty.get());
        preferencesService.getGrobidPreferences().setGrobidEnabled(pdfMetaDataParserEnabledProperty.get());
        preferencesService.getMrDlibPreferences().setAcceptRecommendations(relatedArticlesEnabledProperty.get());
    }

    public void selectAll() {
        versionEnabledProperty.set(true);
        webSearchEnabledProperty.set(true);
        pdfMetaDataParserEnabledProperty.set(true);
        relatedArticlesEnabledProperty.set(true);
    }

    public BooleanProperty versionEnabledProperty() {
        return versionEnabledProperty;
    }

    public BooleanProperty webSearchEnabledProperty() {
        return webSearchEnabledProperty;
    }

    public BooleanProperty pdfMetaDataParserEnabledProperty() {
        return pdfMetaDataParserEnabledProperty;
    }

    public BooleanProperty relatedArticlesEnabledProperty() {
        return relatedArticlesEnabledProperty;
    }
}
