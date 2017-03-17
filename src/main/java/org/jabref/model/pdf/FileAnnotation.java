package org.jabref.model.pdf;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public final class FileAnnotation {

    private final static int ABBREVIATED_ANNOTATION_NAME_LENGTH = 45;
    private final String author;
    private final LocalDateTime timeModified;
    private final int page;
    private final String content;
    private final String annotationType;
    private final Optional<FileAnnotation> linkedFileAnnotation;


    /**
     * A flexible constructor, mainly used as dummy if there is actually no annotation.
     *
     * @param author         The authors of the annotation
     * @param timeModified   The last time this annotation was modified
     * @param pageNumber     The page of the pdf where the annotation occurs
     * @param content        the actual content of the annotation
     * @param annotationType the type of the annotation
     */
    public FileAnnotation(final String author, final LocalDateTime timeModified, final int pageNumber,
                          final String content, final String annotationType, final Optional<FileAnnotation> linkedFileAnnotation) {
        this.author = author;
        this.timeModified = timeModified;
        this.page = pageNumber;
        this.content = parseContent(content);
        this.annotationType = annotationType;
        this.linkedFileAnnotation = linkedFileAnnotation;
    }

    /**
     * Creating a normal FileAnnotation from a PDAnnotation.
     *
     * @param annotation The actual annotation that holds the information
     * @param pageNumber The page of the pdf where the annotation occurs
     */
    public FileAnnotation(final PDAnnotation annotation, final int pageNumber) {
        this(annotation.getDictionary().getString(COSName.T),
                extractModifiedTime(annotation.getModifiedDate()),
                pageNumber, annotation.getContents(), annotation.getSubtype(), Optional.empty());
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
        this(annotation.getDictionary().getString(COSName.T), extractModifiedTime(annotation.getModifiedDate()),
                pageNumber, annotation.getContents(), annotation.getSubtype(), Optional.of(linkedFileAnnotation));
    }

    /**
     * Parses a String into a LocalDateTime.
     *
     * @param dateTimeString In this case of format yyyyMMddHHmmss.
     * @return a LocalDateTime parsed from the dateTimeString
     */
    public static LocalDateTime extractModifiedTime(String dateTimeString) {
        if (dateTimeString == null) {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(dateTimeString.substring(2), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String parseContent(final String content) {
        if (content == null) {
            return "";
        }
        if (content.trim().equals("þÿ")) {
            return "";
        }

        return content.trim();
    }

    /**
     * Abbreviate annotation names when they are longer than {@code ABBREVIATED_ANNOTATION_NAME_LENGTH} chars
     *
     * @param annotationName annotation to be shortened
     * @return the abbreviated name
     */
    private String abbreviateAnnotationName(final String annotationName) {
        if (annotationName.length() > ABBREVIATED_ANNOTATION_NAME_LENGTH) {
            return annotationName.subSequence(0, ABBREVIATED_ANNOTATION_NAME_LENGTH).toString() + "...";
        }
        return annotationName;
    }


    @Override
    public String toString() {
        if (this.hasLinkedAnnotation()) {
            if (this.content.isEmpty()) {
                return "Empty Highlight";
            }
            return abbreviateAnnotationName("Highlight: " + content);
        }

        return abbreviateAnnotationName(content);
    }


    public String getAuthor() {
        return author;
    }

    public LocalDateTime getTimeModified() {
        return timeModified;
    }

    public int getPage() {
        return page;
    }

    public String getContent() {
        return content;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public boolean hasLinkedAnnotation() {
        return this.linkedFileAnnotation.isPresent();
    }

    public FileAnnotation getLinkedFileAnnotation() {
        return linkedFileAnnotation.get();
    }
}
