package org.jabref.model.pdf;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

/**
 * Our representation of the type of the FileAnnotation. This is needed as some FileAnnotationTypes require special
 * handling (e.g., Highlight or Underline), because of the linked FileAnnotations.
 */

public enum FileAnnotationType {
    TEXT("Text"),
    HIGHLIGHT("Highlight"),
    UNDERLINE("Underline"),
    POLYGON("Polygon"),
    POPUP("Popup"),
    LINE("Line"),
    CIRCLE("Circle"),
    FREETEXT("FreeText"),
    STRIKEOUT("Strikeout"),
    LINK("Link"),
    INK("Ink"),
    UNKNOWN("Unknown"),
    NONE("None");

    private static final Log LOGGER = LogFactory.getLog(FileAnnotationType.class);

    private final String name;

    FileAnnotationType(String name) {
        this.name = name;
    }

    /**
     * Determines the FileAnnotationType of a raw PDAnnotation. Returns 'UNKNOWN' if the type is currently not in our
     * list of FileAnnotationTypes.
     *
     * @param annotation the raw PDAnnotation
     * @return The determined FileAnnotationType
     */
    public static FileAnnotationType parse(PDAnnotation annotation) {
        try {
            return FileAnnotationType.valueOf(annotation.getSubtype().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.info(String.format("FileAnnotationType %s is not supported and was converted into 'Unknown'!", annotation.getSubtype()));
            return UNKNOWN;
        }
    }

    public String toString() {
        return this.name;
    }
}
