package net.sf.jabref.model.pdf;


import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class FileAnnotation {

    private String commentId;
    private String author;
    private String date;
    private int page;
    private String content;
    private String annotationType;
    private FileAnnotation linkedFileAnnotation;
    private final static int ABBREVIATED_ANNOTATION_NAME_LENGTH = 45;

    private boolean linkedComment;

    public FileAnnotation(final String commentId, final String author, final String date, final int page,
                          final String content, final String annotationType) {
        this.author = author;
        this.date = date;
        this.page = page;
        this.content = content;
        this.commentId = commentId;
        this.annotationType = annotationType;
    }

    public FileAnnotation(final PDAnnotation annotation, final int page ){
        COSDictionary dict = annotation.getDictionary();
        this.author = dict.getString(COSName.T);
        this.commentId = annotation.getAnnotationName();
        this.date = annotation.getModifiedDate();
        this.page = page;
        this.content = annotation.getContents();
        this.annotationType = annotation.getSubtype();

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

    public void linkComments(FileAnnotation commentToLinkTo){
        linkedFileAnnotation = commentToLinkTo;
        commentToLinkTo.setLinkedFileAnnotation(this);
        commentToLinkTo.setLinkedComment(true);
        linkedComment = true;
    }

    @Override
    public String toString() {
        return abbreviateAnnotationName(content);
    }

    public FileAnnotation getLinkedFileAnnotation() {
        return linkedFileAnnotation;
    }

    public void setLinkedFileAnnotation(FileAnnotation linkedFileAnnotation) {
        this.linkedFileAnnotation = linkedFileAnnotation;
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

    public boolean hasLinkedComment() {
        return linkedComment;
    }

    public void setLinkedComment(boolean linkedComment) {
        this.linkedComment = linkedComment;
    }
}
