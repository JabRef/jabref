package org.jabref.model.pdf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileAnnotation {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileAnnotation.class);

    private final static int ABBREVIATED_ANNOTATION_NAME_LENGTH = 45;
    private static final String DATE_TIME_STRING = "^D:\\d{14}$";
    private static final String DATE_TIME_STRING_WITH_TIME_ZONE = "^D:\\d{14}.+";
    private static final String ANNOTATION_DATE_FORMAT = "yyyyMMddHHmmss";

    private final String author;
    private final LocalDateTime timeModified;
    private final int page;
    private final String content;
    private final FileAnnotationType annotationType;
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
                          final String content, final FileAnnotationType annotationType, final Optional<FileAnnotation> linkedFileAnnotation) {
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
        this(annotation.getCOSObject().getString(COSName.T),
                extractModifiedTime(annotation.getModifiedDate()),
                pageNumber, annotation.getContents(), FileAnnotationType.parse(annotation), Optional.empty());
    }

    /**
     * For creating a FileAnnotation that has a connection to another FileAnnotation. Needed when creating a text
     * highlighted or underlined annotation with a sticky note.
     *
     * @param annotation           The actual annotation that holds the information
     * @param pageNumber           The page of the pdf where the annotation occurs
     * @param linkedFileAnnotation The corresponding note of a marked text area.
     */
    public FileAnnotation(final PDAnnotation annotation, final int pageNumber, FileAnnotation linkedFileAnnotation) {
        this(annotation.getCOSObject().getString(COSName.T), extractModifiedTime(annotation.getModifiedDate()),
                pageNumber, annotation.getContents(), FileAnnotationType.parse(annotation), Optional.of(linkedFileAnnotation));
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

        if (dateTimeString.matches(DATE_TIME_STRING_WITH_TIME_ZONE)) {
            dateTimeString = dateTimeString.substring(2, 16);
        } else if (dateTimeString.matches(DATE_TIME_STRING)) {
            dateTimeString = dateTimeString.substring(2);
        }

        try {
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(ANNOTATION_DATE_FORMAT));
        } catch (DateTimeParseException e) {
            LOGGER.info(String.format("Expected a parseable date string! However, this text could not be parsed: '%s'", dateTimeString));
            return LocalDateTime.now();
        }
    }

    private String parseContent(final String content) {
        if (content == null) {
            return "";
        }

        final String unreadableContent = "þÿ";
        if (content.trim().equals(unreadableContent)) {
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
        return abbreviateAnnotationName(content);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        FileAnnotation that = (FileAnnotation) other;
        return Objects.equals(this.annotationType, that.annotationType)
                && Objects.equals(this.author, that.author)
                && Objects.equals(this.content, that.content)
                && Objects.equals(this.page, that.page)
                && Objects.equals(this.linkedFileAnnotation, that.linkedFileAnnotation)
                && Objects.equals(this.timeModified, that.timeModified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotationType, author, content, page, linkedFileAnnotation, timeModified);
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

    public FileAnnotationType getAnnotationType() {
        return annotationType;
    }

    public boolean hasLinkedAnnotation() {
        return this.linkedFileAnnotation.isPresent();
    }

    /**
     * Before this getter is called the presence of the linked annotation must be checked via hasLinkedAnnotation()!
     *
     * @return the note attached to the annotation
     */
    public FileAnnotation getLinkedFileAnnotation() {
        return linkedFileAnnotation.get();
    }
}
