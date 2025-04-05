package org.jabref.logic.ocr.pdf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a searchable text layer for a PDF.
 * <p>
 * This class stores the text content and position information
 * needed to add a searchable layer to a PDF.
 */
public class SearchableTextLayer {
    
    private final String sourceText;
    private final Map<Integer, String> pageTextMap;
    private final boolean isInvisible;
    
    /**
     * Create a new searchable text layer.
     *
     * @param sourceText Source text from OCR
     * @param pageTextMap Map of page numbers to page text
     * @param isInvisible Whether the text should be invisible
     */
    private SearchableTextLayer(Builder builder) {
        this.sourceText = Objects.requireNonNull(builder.sourceText);
        this.pageTextMap = Collections.unmodifiableMap(new HashMap<>(builder.pageTextMap));
        this.isInvisible = builder.isInvisible;
    }
    
    /**
     * Get the original source text.
     *
     * @return Source text
     */
    public String getSourceText() {
        return sourceText;
    }
    
    /**
     * Get the text for a specific page.
     *
     * @param pageNumber Page number (1-based)
     * @return Text for the page, or empty string if no text for that page
     */
    public String getPageText(int pageNumber) {
        return pageTextMap.getOrDefault(pageNumber, "");
    }
    
    /**
     * Get the page text map.
     *
     * @return Map of page numbers to page text
     */
    public Map<Integer, String> getPageTextMap() {
        return pageTextMap;
    }
    
    /**
     * Check if the text layer should be invisible.
     *
     * @return true if the text should be invisible
     */
    public boolean isInvisible() {
        return isInvisible;
    }
    
    /**
     * Builder for SearchableTextLayer.
     */
    public static class Builder {
        private String sourceText = "";
        private final Map<Integer, String> pageTextMap = new HashMap<>();
        private boolean isInvisible = true;
        
        /**
         * Set the source text.
         *
         * @param sourceText Source text
         * @return This builder
         */
        public Builder withSourceText(String sourceText) {
            this.sourceText = sourceText;
            return this;
        }
        
        /**
         * Add text for a specific page.
         *
         * @param pageNumber Page number (1-based)
         * @param text Text for the page
         * @return This builder
         */
        public Builder withPageText(int pageNumber, String text) {
            this.pageTextMap.put(pageNumber, text);
            return this;
        }
        
        /**
         * Set the page text map.
         *
         * @param pageTextMap Map of page numbers to page text
         * @return This builder
         */
        public Builder withPageTextMap(Map<Integer, String> pageTextMap) {
            this.pageTextMap.clear();
            this.pageTextMap.putAll(pageTextMap);
            return this;
        }
        
        /**
         * Set whether the text should be invisible.
         *
         * @param isInvisible true if the text should be invisible
         * @return This builder
         */
        public Builder withInvisible(boolean isInvisible) {
            this.isInvisible = isInvisible;
            return this;
        }
        
        /**
         * Build the searchable text layer.
         *
         * @return New SearchableTextLayer instance
         */
        public SearchableTextLayer build() {
            return new SearchableTextLayer(this);
        }
    }
}