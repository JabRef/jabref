package net.sf.jabref.model.pdf;


import java.util.Optional;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class PdfComment {

    private String commentId;
    private String author;
    private String date;
    private int page;
    private String content;
    private Optional<String> annotationTypeInfo;
    private String annotationType;
    private final static int ABBREVIATED_ANNOTATION_NAME_LENGTH = 45;

    public PdfComment(final String commentId, final String author, final String date, final int page,
                      final String content, final Optional<String> annotationTypeInfo, final String annotationType) {
        this.author = author;
        this.date = date;
        this.page = page;
        this.content = content;
        this.annotationTypeInfo = annotationTypeInfo;
        this.commentId = commentId;
        this.annotationType = annotationType;
    }

    public PdfComment(final PDAnnotation annotation, final int page, final Optional<String> annotationTypeInfo){
        COSDictionary dict = annotation.getDictionary();
        this.author = dict.getString(COSName.T);
        this.commentId = annotation.getAnnotationName();
        this.date = annotation.getModifiedDate();
        this.page = page;
        this.content = annotation.getContents();
        this.annotationType = annotation.getSubtype();
        this.annotationTypeInfo = annotationTypeInfo;

    }

    /**
     * Abbreviate annotation names when they are longer than {@code ABBREVIATED_ANNOTATION_NAME_LENGTH} chars
     * @param annotationName
     * @return the abbreviated name
     */
    private String abbreviateAnnotationName(final String annotationName ){

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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public Optional<String> getAnnotationTypeInfo() {
        return annotationTypeInfo;
    }
}
