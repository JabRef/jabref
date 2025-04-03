package org.jabref.logic.ocr.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration options for OCR engines.
 * <p>
 * This class follows JabRef's domain model pattern and provides a consistent
 * way to configure different OCR engines through a common interface.
 */
public class OcrEngineConfig {
    /**
     * Quality presets for OCR processing.
     */
    public enum QualityPreset {
        FAST("Optimize for speed (lower accuracy)"),
        BALANCED("Balanced speed and accuracy"),
        ACCURATE("Optimize for accuracy (slower processing)");

        private final String description;

        QualityPreset(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final String engineName;
    private final OcrLanguage language;
    private final Map<String, String> engineSettings;
    private final boolean preprocessImages;
    private final int dpi;
    private final QualityPreset qualityPreset;

    private OcrEngineConfig(Builder builder) {
        this.engineName = Objects.requireNonNull(builder.engineName);
        this.language = Objects.requireNonNull(builder.language);
        this.engineSettings = Collections.unmodifiableMap(new HashMap<>(builder.engineSettings));
        this.preprocessImages = builder.preprocessImages;
        this.dpi = builder.dpi;
        this.qualityPreset = builder.qualityPreset;
    }

    /**
     * Get the OCR engine name.
     *
     * @return Engine name
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Get the OCR language.
     *
     * @return OCR language
     */
    public OcrLanguage getLanguage() {
        return language;
    }

    /**
     * Get engine-specific settings.
     *
     * @return Map of settings
     */
    public Map<String, String> getEngineSettings() {
        return engineSettings;
    }

    /**
     * Get a specific engine setting.
     *
     * @param key Setting key
     * @return Optional containing the setting value if present
     */
    public Optional<String> getEngineSetting(String key) {
        return Optional.ofNullable(engineSettings.get(key));
    }

    /**
     * Check if image preprocessing is enabled.
     *
     * @return true if preprocessing is enabled
     */
    public boolean isPreprocessImages() {
        return preprocessImages;
    }

    /**
     * Get the DPI (dots per inch) setting for OCR.
     *
     * @return DPI value
     */
    public int getDpi() {
        return dpi;
    }

    /**
     * Get the quality preset.
     *
     * @return Quality preset
     */
    public QualityPreset getQualityPreset() {
        return qualityPreset;
    }

    /**
     * Builder for OcrEngineConfig.
     */
    public static class Builder {
        private final String engineName;
        private OcrLanguage language;
        private final Map<String, String> engineSettings = new HashMap<>();
        private boolean preprocessImages = true;
        private int dpi = 300;
        private QualityPreset qualityPreset = QualityPreset.BALANCED;

        /**
         * Create a new builder.
         *
         * @param engineName OCR engine name
         */
        public Builder(String engineName) {
            this.engineName = engineName;
        }

        /**
         * Set the OCR language.
         *
         * @param language OCR language
         * @return This builder
         */
        public Builder withLanguage(OcrLanguage language) {
            this.language = language;
            return this;
        }

        /**
         * Add an engine-specific setting.
         *
         * @param key Setting key
         * @param value Setting value
         * @return This builder
         */
        public Builder withEngineSetting(String key, String value) {
            this.engineSettings.put(key, value);
            return this;
        }

        /**
         * Add multiple engine settings.
         *
         * @param settings Map of settings
         * @return This builder
         */
        public Builder withEngineSettings(Map<String, String> settings) {
            this.engineSettings.putAll(settings);
            return this;
        }

        /**
         * Set whether to preprocess images before OCR.
         *
         * @param preprocessImages true to enable preprocessing
         * @return This builder
         */
        public Builder withPreprocessImages(boolean preprocessImages) {
            this.preprocessImages = preprocessImages;
            return this;
        }

        /**
         * Set the DPI (dots per inch) for OCR.
         *
         * @param dpi DPI value
         * @return This builder
         */
        public Builder withDpi(int dpi) {
            this.dpi = dpi;
            return this;
        }

        /**
         * Set the quality preset.
         *
         * @param qualityPreset Quality preset
         * @return This builder
         */
        public Builder withQualityPreset(QualityPreset qualityPreset) {
            this.qualityPreset = qualityPreset;
            return this;
        }

        /**
         * Build the OcrEngineConfig.
         *
         * @return New OcrEngineConfig instance
         */
        public OcrEngineConfig build() {
            return new OcrEngineConfig(this);
        }
    }
}