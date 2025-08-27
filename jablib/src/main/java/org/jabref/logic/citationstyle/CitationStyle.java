package org.jabref.logic.citationstyle;

import java.nio.file.Path;
import java.util.Objects;

import org.jabref.logic.openoffice.style.OOStyle;

import org.jspecify.annotations.NonNull;

/**
 * Representation of a CitationStyle. Stores its name, the file path and the style itself.
 * This is a pure model class. For loading/parsing functionality, see {@link CSLStyleUtils} and {@link CSLStyleLoader}.
 */
public class CitationStyle implements OOStyle {

    // Currently, we have support for only one alphanumeric style, so we hardcode it
    private static final String ALPHANUMERIC_STYLE = "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690";

    private final String filePath;
    private final String title;
    private final String shortTitle;
    private final boolean isNumericStyle;
    private final boolean hasBibliography;
    private final boolean usesHangingIndent;
    private final String source;
    private final boolean isInternalStyle;

    public CitationStyle(@NonNull String filePath, @NonNull String title, @NonNull String shortTitle, boolean isNumericStyle, boolean hasBibliography, boolean usesHangingIndent, @NonNull String source, boolean isInternalStyle) {
        this.filePath = Path.of(filePath).toString(); // wrapping with Path.of takes care of extra slashes in path due to subsequent storage and retrieval (observed on Windows)
        this.title = title;
        this.shortTitle = shortTitle;
        this.isNumericStyle = isNumericStyle;
        this.hasBibliography = hasBibliography;
        this.usesHangingIndent = hasBibliography && usesHangingIndent;
        this.source = source;
        this.isInternalStyle = isInternalStyle;
    }

    /**
     * Creates a new citation style with an auto-determined internal/external state.
     */
    public CitationStyle(@NonNull String filePath, @NonNull String title, @NonNull String shortTitle, boolean isNumericStyle, boolean hasBibliography, boolean usesHangingIndent, @NonNull String source) {
        this(filePath, title, shortTitle, isNumericStyle, hasBibliography, usesHangingIndent, source, !Path.of(filePath).isAbsolute());
    }

    public String getTitle() {
        return title;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public boolean isNumericStyle() {
        return isNumericStyle;
    }

    public boolean hasBibliography() {
        return hasBibliography;
    }

    public boolean usesHangingIndent() {
        return usesHangingIndent;
    }

    /**
     * Currently, we have support for one alphanumeric CSL style.
     * There is no tag or field in .csl style files that can be parsed to determine if it is an alphanumeric style.
     * Thus, to determine alphanumeric nature, we currently manually check for equality with "DIN 1505-2".
     */
    public boolean isAlphanumericStyle() {
        return ALPHANUMERIC_STYLE.equals(this.title);
    }

    public String getSource() {
        return source;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        CitationStyle other = (CitationStyle) o;
        return Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public boolean isInternalStyle() {
        return isInternalStyle;
    }

    @Override
    public String getPath() {
        return getFilePath();
    }
}
