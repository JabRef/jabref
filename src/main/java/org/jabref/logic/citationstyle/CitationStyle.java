package org.jabref.logic.citationstyle;

import java.nio.file.Path;
import java.util.Objects;

import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.StandardFileType;

/**
 * Representation of a CitationStyle. Stores its name, the file path and the style itself.
 * This is a pure model class. For loading/parsing functionality, see {@link CSLStyleUtils} and {@link CSLStyleLoader}.
 */
public class CitationStyle implements OOStyle {

    // Currently, we have support for only one alphanumeric style, so we hardcode it
    private static final String ALPHANUMERIC_STYLE = "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690";

    private final String filePath;
    private final String title;
    private final boolean isNumericStyle;
    private final String source;
    private final boolean isInternalStyle;

    public CitationStyle(String filePath, String title, boolean isNumericStyle, String source, boolean isInternalStyle) {
        this.filePath = Path.of(Objects.requireNonNull(filePath)).toString(); // wrapping with Path.of takes care of extra slashes in path due to subsequent storage and retrieval (observed on Windows)
        this.title = Objects.requireNonNull(title);
        this.isNumericStyle = isNumericStyle;
        this.source = Objects.requireNonNull(source);
        this.isInternalStyle = isInternalStyle;
    }

    /**
     * Creates a new citation style with an auto-determined internal/external state.
     */
    public CitationStyle(String filePath, String title, boolean isNumericStyle, String source) {
        this(filePath, title, isNumericStyle, source, !Path.of(filePath).isAbsolute());
    }

    /**
     * Checks if the given style file is a CitationStyle based on its extension
     */
    public static boolean isCitationStyleFile(String styleFile) {
        return StandardFileType.CITATION_STYLE.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    public String getTitle() {
        return title;
    }

    public boolean isNumericStyle() {
        return isNumericStyle;
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
