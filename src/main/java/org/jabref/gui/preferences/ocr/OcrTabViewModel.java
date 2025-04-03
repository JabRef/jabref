package org.jabref.gui.preferences.ocr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.models.OcrEngineConfig;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

/**
 * View model for the OCR tab in JabRef preferences.
 * <p>
 * This class demonstrates how to implement a preference tab view model
 * following JabRef's MVVM pattern.
 */
public class OcrTabViewModel implements PreferenceTabViewModel {
    
    // Preference properties
    private final BooleanProperty ocrEnabled;
    private final StringProperty defaultOcrEngine;
    private final StringProperty defaultLanguage;
    private final BooleanProperty preprocessImages;
    private final ObjectProperty<OcrEngineConfig.QualityPreset> qualityPreset;
    private final StringProperty tesseractPath;
    
    // Available options
    private final ListProperty<String> availableEngines;
    private final ListProperty<String> availableLanguages;
    private final ListProperty<OcrEngineConfig.QualityPreset> availableQualityPresets;
    
    // Validators
    private final Validator tesseractPathValidator;
    
    private final GuiPreferences preferences;
    
    /**
     * Create a new OCR tab view model.
     *
     * @param preferences GUI preferences
     */
    public OcrTabViewModel(GuiPreferences preferences) {
        this.preferences = preferences;
        
        // Initialize properties with default values
        ocrEnabled = new SimpleBooleanProperty(false);
        defaultOcrEngine = new SimpleStringProperty("Tesseract");
        defaultLanguage = new SimpleStringProperty("eng");
        preprocessImages = new SimpleBooleanProperty(true);
        qualityPreset = new SimpleObjectProperty<>(OcrEngineConfig.QualityPreset.BALANCED);
        tesseractPath = new SimpleStringProperty("");
        
        // Initialize available options
        availableEngines = new SimpleListProperty<>(FXCollections.observableArrayList(
                "Tesseract", "Google Vision", "ABBYY Cloud OCR"
        ));
        
        availableLanguages = new SimpleListProperty<>(FXCollections.observableArrayList(
                "eng", "deu", "fra", "spa", "ita", "lat", "grc", "san", "rus", "jpn", "chi_sim", "chi_tra"
        ));
        
        availableQualityPresets = new SimpleListProperty<>(FXCollections.observableArrayList(
                OcrEngineConfig.QualityPreset.values()
        ));
        
        // Setup validators
        tesseractPathValidator = new FunctionBasedValidator<>(
                tesseractPath,
                this::validateTesseractPath,
                ValidationMessage.error(Localization.lang("The Tesseract path must be valid"))
        );
    }
    
    /**
     * Validate the Tesseract path.
     *
     * @param path Path to validate
     * @return true if path is valid (either empty or existing directory)
     */
    private boolean validateTesseractPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return true; // Empty path is allowed (will use system default)
        }
        
        Path tesseractDir = Paths.get(path);
        return Files.exists(tesseractDir) && Files.isDirectory(tesseractDir);
    }
    
    @Override
    public void setValues() {
        // In a real implementation, these values would be loaded from JabRef preferences
        // For this demonstration, we just use the default values set in the constructor
    }
    
    @Override
    public void storeSettings() {
        // In a real implementation, these values would be stored in JabRef preferences
        // For this demonstration, we just log the values
        System.out.println("OCR settings stored:");
        System.out.println("OCR enabled: " + ocrEnabled.get());
        System.out.println("Default OCR engine: " + defaultOcrEngine.get());
        System.out.println("Default language: " + defaultLanguage.get());
        System.out.println("Preprocess images: " + preprocessImages.get());
        System.out.println("Quality preset: " + qualityPreset.get());
        System.out.println("Tesseract path: " + tesseractPath.get());
    }
    
    @Override
    public boolean validateSettings() {
        return tesseractPathValidator.getValidationStatus().isValid();
    }
    
    @Override
    public List<String> getRestartWarnings() {
        // No restart needed for OCR settings
        return new ArrayList<>();
    }
    
    // Property getters
    
    public BooleanProperty ocrEnabledProperty() {
        return ocrEnabled;
    }
    
    public StringProperty defaultOcrEngineProperty() {
        return defaultOcrEngine;
    }
    
    public StringProperty defaultLanguageProperty() {
        return defaultLanguage;
    }
    
    public BooleanProperty preprocessImagesProperty() {
        return preprocessImages;
    }
    
    public ObjectProperty<OcrEngineConfig.QualityPreset> qualityPresetProperty() {
        return qualityPreset;
    }
    
    public StringProperty tesseractPathProperty() {
        return tesseractPath;
    }
    
    public ListProperty<String> availableEnginesProperty() {
        return availableEngines;
    }
    
    public ListProperty<String> availableLanguagesProperty() {
        return availableLanguages;
    }
    
    public ListProperty<OcrEngineConfig.QualityPreset> availableQualityPresetsProperty() {
        return availableQualityPresets;
    }
    
    public ValidationStatus getTesseractPathValidationStatus() {
        return tesseractPathValidator.getValidationStatus();
    }
}