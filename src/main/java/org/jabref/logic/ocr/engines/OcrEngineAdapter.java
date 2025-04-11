package org.jabref.logic.ocr.engines;

import java.util.HashSet;
import java.util.Set;

import org.jabref.logic.ocr.OcrService;
import org.jabref.logic.ocr.exception.OcrProcessException;
import org.jabref.logic.ocr.models.OcrEngineConfig;
import org.jabref.logic.ocr.models.OcrLanguage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base adapter class for OCR engines.
 * <p>
 * This abstract class provides common functionality for OCR engine adapters,
 * allowing engine-specific implementations to focus on their unique aspects.
 */
public abstract class OcrEngineAdapter implements OcrService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrEngineAdapter.class);
    
    protected OcrLanguage currentLanguage;
    protected OcrEngineConfig currentConfig;
    protected final Set<OcrLanguage> supportedLanguages = new HashSet<>();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setLanguage(OcrLanguage language) throws OcrProcessException {
        if (!supportedLanguages.contains(language)) {
            throw new OcrProcessException(getEngineName(), 
                    String.format("Language '%s' is not supported", language.getDisplayName()));
        }
        
        LOGGER.debug("Setting OCR language to: {}", language);
        this.currentLanguage = language;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<OcrLanguage> getSupportedLanguages() {
        return new HashSet<>(supportedLanguages);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void applyConfig(OcrEngineConfig config) throws OcrProcessException {
        if (!config.getEngineName().equals(getEngineName())) {
            throw new OcrProcessException(getEngineName(),
                    String.format("Configuration is for engine '%s', not compatible with '%s'",
                            config.getEngineName(), getEngineName()));
        }
        
        LOGGER.debug("Applying configuration to {}: {}", getEngineName(), config);
        this.currentConfig = config;
        setLanguage(config.getLanguage());
    }
    
    /**
     * Initialize supported languages.
     * <p>
     * Subclasses should call this method to populate the supported languages.
     *
     * @param languages Set of supported languages
     */
    protected void initializeSupportedLanguages(Set<OcrLanguage> languages) {
        this.supportedLanguages.clear();
        this.supportedLanguages.addAll(languages);
    }
    
    /**
     * Add a supported language.
     *
     * @param language Language to add
     */
    protected void addSupportedLanguage(OcrLanguage language) {
        this.supportedLanguages.add(language);
    }
    
    /**
     * Get the current configuration.
     *
     * @return Current configuration or null if not configured
     */
    protected OcrEngineConfig getCurrentConfig() {
        return currentConfig;
    }
}