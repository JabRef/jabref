package net.sf.jabref.model.pdf;


import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class PdfComment {

    private String commentId;
    private String author;
    private String date;
    private int page;
    private String content;
    private String text;
    private final static int ABBREVIATED_ANNOTATION_NAME_LENGTH = 45;

    public PdfComment(String commentId, String author, String date, int page, String content, String text) {
        this.author = author;
        this.date = date;
        this.page = page;
        this.content = content;
        this.text = text;
        this.commentId = commentId;
    }
    public PdfComment(PDAnnotation annotation, int page){
        COSDictionary dict = annotation.getDictionary();
        this.author = dict.getString(COSName.T);
        this.commentId = annotation.getAnnotationName();
        this.date = annotation.getModifiedDate();
        this.page = page;
        this.content = annotation.getContents();

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
