package org.jabref.model.pdf;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public final class FileAnnotation {

    private final static int ABBREVIATED_ANNOTATION_NAME_LENGTH = 45;
    private final static String ANNOTATION_DATE_REGEX = "D:\\d+";
    public final String author;
    public final String date;
    public final int page;
    public final String content;
    public final String annotationType;
    public final Optional<FileAnnotation> linkedFileAnnotation;


    /**
     * A flexible constructor, mainly used as dummy if there is actually no annotation.
     *
     * @param author         The authors of the annotation
     * @param date           The last modified date of the annotation
     * @param pageNumber     The page of the pdf where the annotation occurs
     * @param content        the actual content of the annotation
     * @param annotationType the type of the annotation
     */
    public FileAnnotation(final String author, final String date, final int pageNumber,
                          final String content, final String annotationType) {
        this.author = author;
        this.date = prettyPrint(date);
        this.page = pageNumber;
        this.content = content;
        this.annotationType = annotationType;
        this.linkedFileAnnotation = Optional.empty();
    }

    /**
     * Creating a normal FileAnnotation from a PDAnnotation.
     *
     * @param annotation The actual annotation that holds the information
     * @param pageNumber The page of the pdf where the annotation occurs
     */
    public FileAnnotation(final PDAnnotation annotation, final int pageNumber) {
        COSDictionary dict = annotation.getDictionary();
        this.author = dict.getString(COSName.T);
        this.date = prettyPrint(annotation.getModifiedDate());
        this.page = pageNumber;
        this.content = annotation.getContents();
        this.annotationType = annotation.getSubtype();
        this.linkedFileAnnotation = Optional.empty();
    }

    /**
     * For creating a FileAnnotation that has a connection to another FileAnnotation. Needed when creating a text
     * highlight annotation with a sticky note.
     *
     * @param annotation           The actual annotation that holds the information
     * @param pageNumber           The page of the pdf where the annotation occurs
     * @param linkedFileAnnotation The corresponding note of a highlighted text area.
     */
    public FileAnnotation(final PDAnnotation annotation, final int pageNumber, FileAnnotation linkedFileAnnotation) {
        COSDictionary dict = annotation.getDictionary();
        this.author = dict.getString(COSName.T);
        this.date = prettyPrint(annotation.getModifiedDate());
        this.page = pageNumber;
        this.content = annotation.getContents();
        this.annotationType = annotation.getSubtype();
        this.linkedFileAnnotation = Optional.of(linkedFileAnnotation);
    }

    private String prettyPrint(String date) {
        // Sometimes this is null, not sure why.
        if (date == null) {
            return date;
        }

        // normal case for an imported annotation
        if (date.matches(ANNOTATION_DATE_REGEX)) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.parse(date.substring(2), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }

        return date;
    }

    /**
     * Abbreviate annotation names when they are longer than {@code ABBREVIATED_ANNOTATION_NAME_LENGTH} chars
     *
     * @param annotationName annotation to be shortened
     * @return the abbreviated name
     */
    private String abbreviateAnnotationName(final String annotationName) {

        int abbreviatedContentLengthForName = ABBREVIATED_ANNOTATION_NAME_LENGTH;
        if (annotationName.length() > abbreviatedContentLengthForName) {
            return annotationName.subSequence(0, abbreviatedContentLengthForName).toString() + "...";
        }
        return annotationName;
    }


    @Override
    public String toString() {
        return abbreviateAnnotationName(content);
    }


    public boolean hasLinkedAnnotation() {
        return this.linkedFileAnnotation.isPresent();
    }
}
