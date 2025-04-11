package org.jabref.logic.ocr.models;

import java.util.Objects;

/**
 * Represents a language for OCR processing.
 * <p>
 * This class encapsulates language information with ISO code and display name.
 * Follows JabRef's domain model pattern.
 */
public class OcrLanguage {
    private final String isoCode;
    private final String displayName;
    private final boolean isAncient; // Special flag for ancient languages that might need special handling

    /**
     * Create a new OCR language.
     *
     * @param isoCode ISO 639-2 language code (e.g., "eng" for English)
     * @param displayName Human-readable display name
     * @param isAncient Whether this is an ancient language
     */
    public OcrLanguage(String isoCode, String displayName, boolean isAncient) {
        this.isoCode = Objects.requireNonNull(isoCode);
        this.displayName = Objects.requireNonNull(displayName);
        this.isAncient = isAncient;
    }

    /**
     * Create a new modern language for OCR.
     *
     * @param isoCode ISO 639-2 language code (e.g., "eng" for English)
     * @param displayName Human-readable display name
     * @return New OcrLanguage object
     */
    public static OcrLanguage createModernLanguage(String isoCode, String displayName) {
        return new OcrLanguage(isoCode, displayName, false);
    }

    /**
     * Create a new ancient language for OCR.
     *
     * @param isoCode ISO 639-2 language code (e.g., "grc" for Ancient Greek)
     * @param displayName Human-readable display name
     * @return New OcrLanguage object
     */
    public static OcrLanguage createAncientLanguage(String isoCode, String displayName) {
        return new OcrLanguage(isoCode, displayName, true);
    }

    /**
     * Get the ISO 639-2 language code.
     *
     * @return ISO language code
     */
    public String getIsoCode() {
        return isoCode;
    }

    /**
     * Get the human-readable display name.
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this is an ancient language.
     *
     * @return true if this is an ancient language
     */
    public boolean isAncient() {
        return isAncient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OcrLanguage that = (OcrLanguage) o;
        return Objects.equals(isoCode, that.isoCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoCode);
    }

    @Override
    public String toString() {
        return displayName + " (" + isoCode + ")";
    }
}