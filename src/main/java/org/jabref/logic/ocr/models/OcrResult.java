package org.jabref.logic.ocr.models;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain model representing the result of an OCR processing operation.
 * <p>
 * Contains the extracted text, confidence scores, and metadata.
 * Follows JabRef's domain model patterns with immutability and builder pattern.
 */
public class OcrResult {
    private final String extractedText;
    private final double averageConfidence;
    private final Map<Integer, Double> pageConfidenceMap;
    private final Path sourceFile;
    private final String engineName;
    private final LocalDateTime processingTime;
    private final Map<String, String> metadata;
    private final OcrLanguage language;

    private OcrResult(Builder builder) {
        this.extractedText = Objects.requireNonNull(builder.extractedText);
        this.averageConfidence = builder.averageConfidence;
        this.pageConfidenceMap = Collections.unmodifiableMap(new HashMap<>(builder.pageConfidenceMap));
        this.sourceFile = builder.sourceFile;
        this.engineName = builder.engineName;
        this.processingTime = builder.processingTime != null ? builder.processingTime : LocalDateTime.now();
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
        this.language = builder.language;
    }

    /**
     * Get the text extracted from the document.
     *
     * @return Extracted text
     */
    public String getExtractedText() {
        return extractedText;
    }

    /**
     * Get the average confidence score for the OCR result.
     *
     * @return Confidence score (0-100%)
     */
    public double getAverageConfidence() {
        return averageConfidence;
    }

    /**
     * Get confidence scores for individual pages.
     *
     * @return Map of page numbers to confidence scores
     */
    public Map<Integer, Double> getPageConfidenceMap() {
        return pageConfidenceMap;
    }

    /**
     * Get the source file that was processed.
     *
     * @return Optional containing the source file path if available
     */
    public Optional<Path> getSourceFile() {
        return Optional.ofNullable(sourceFile);
    }

    /**
     * Get the OCR engine name.
     *
     * @return Name of the OCR engine
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Get the time when processing was completed.
     *
     * @return Processing completion time
     */
    public LocalDateTime getProcessingTime() {
        return processingTime;
    }

    /**
     * Get additional metadata from OCR processing.
     *
     * @return Map of metadata key-value pairs
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Get the language used for OCR.
     *
     * @return Optional containing the language if available
     */
    public Optional<OcrLanguage> getLanguage() {
        return Optional.ofNullable(language);
    }

    /**
     * Builder for OcrResult.
     */
    public static class Builder {
        private String extractedText = "";
        private double averageConfidence;
        private final Map<Integer, Double> pageConfidenceMap = new HashMap<>();
        private Path sourceFile;
        private String engineName;
        private LocalDateTime processingTime;
        private final Map<String, String> metadata = new HashMap<>();
        private OcrLanguage language;

        /**
         * Set the extracted text.
         *
         * @param extractedText Text extracted by OCR
         * @return This builder
         */
        public Builder withExtractedText(String extractedText) {
            this.extractedText = extractedText;
            return this;
        }

        /**
         * Set the average confidence score.
         *
         * @param averageConfidence Confidence score (0-100%)
         * @return This builder
         */
        public Builder withAverageConfidence(double averageConfidence) {
            this.averageConfidence = averageConfidence;
            return this;
        }

        /**
         * Set confidence scores for individual pages.
         *
         * @param pageConfidenceMap Map of page numbers to confidence scores
         * @return This builder
         */
        public Builder withPageConfidenceMap(Map<Integer, Double> pageConfidenceMap) {
            this.pageConfidenceMap.putAll(pageConfidenceMap);
            return this;
        }

        /**
         * Add a confidence score for a specific page.
         *
         * @param pageNumber Page number (1-based)
         * @param confidence Confidence score for the page
         * @return This builder
         */
        public Builder withPageConfidence(int pageNumber, double confidence) {
            this.pageConfidenceMap.put(pageNumber, confidence);
            return this;
        }

        /**
         * Set the source file.
         *
         * @param sourceFile Path to the source file
         * @return This builder
         */
        public Builder withSourceFile(Path sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        /**
         * Set the OCR engine name.
         *
         * @param engineName Name of the OCR engine
         * @return This builder
         */
        public Builder withEngineName(String engineName) {
            this.engineName = engineName;
            return this;
        }

        /**
         * Set the processing time.
         *
         * @param processingTime Time when processing was completed
         * @return This builder
         */
        public Builder withProcessingTime(LocalDateTime processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        /**
         * Add a metadata entry.
         *
         * @param key Metadata key
         * @param value Metadata value
         * @return This builder
         */
        public Builder withMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Set multiple metadata entries.
         *
         * @param metadata Map of metadata entries
         * @return This builder
         */
        public Builder withMetadata(Map<String, String> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * Set the language used for OCR.
         *
         * @param language OCR language
         * @return This builder
         */
        public Builder withLanguage(OcrLanguage language) {
            this.language = language;
            return this;
        }

        /**
         * Build the OcrResult.
         *
         * @return New OcrResult instance
         */
        public OcrResult build() {
            return new OcrResult(this);
        }
    }
}