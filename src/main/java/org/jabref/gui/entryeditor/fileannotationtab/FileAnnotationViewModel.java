package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.formatter.bibtexfields.RemoveHyphenatedNewlinesFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

public class FileAnnotationViewModel {

    private static final String NEWLINE = String.format("%n");
    private final FileAnnotation annotation;
    private StringProperty author = new SimpleStringProperty();
    private StringProperty page = new SimpleStringProperty();
    private StringProperty date = new SimpleStringProperty();
    private StringProperty content = new SimpleStringProperty();
    private StringProperty marking = new SimpleStringProperty();

    public FileAnnotationViewModel(FileAnnotation annotation) {
        this.annotation = annotation;
        author.set(annotation.getAuthor());
        page.set(Integer.toString(annotation.getPage()));
        date.set(annotation.getTimeModified().toString().replace('T', ' '));
        setupContentProperties(annotation);
    }

    private void setupContentProperties(FileAnnotation annotation) {
        if (annotation.hasLinkedAnnotation()) {
            this.content.set(annotation.getLinkedFileAnnotation().getContent());
            String annotationContent = annotation.getContent();
            String illegibleTextMessage = Localization.lang("The marked area does not contain any legible text!");
            String markingContent = (annotationContent.isEmpty() ? illegibleTextMessage : annotationContent);
            // remove newlines && hyphens before linebreaks
            markingContent = markingContent.replaceAll("-" + NEWLINE, "");
            new RemoveHyphenatedNewlinesFormatter().format(markingContent);
            // remove new lines not preceded by '.' or ':'
            markingContent = markingContent.replaceAll("(?<![.|:])" + NEWLINE, " ");
            this.marking.set(markingContent);
        } else {
            String content = annotation.getContent();
            // remove newlines && hyphens before linebreaks
            content = new RemoveHyphenatedNewlinesFormatter().format(content);
            content = new RemoveNewlinesFormatter().format(content);
            this.content.set(content);
            this.marking.set("");
        }
    }

    public String getAuthor() {
        return author.get();
    }

    public String getPage() {
        return page.get();
    }

    public String getDate() {
        return date.get();
    }

    public String getContent() {
        return content.get();
    }

    public StringProperty pageProperty() {
        return page;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public StringProperty contentProperty() {
        return content;
    }

    public StringProperty markingProperty() {
        return marking;
    }

    public StringProperty authorProperty() {
        return author;
    }

    @Override
    public String toString() {
        if (annotation.hasLinkedAnnotation() && this.getContent().isEmpty()) {
            if (FileAnnotationType.UNDERLINE.equals(annotation.getAnnotationType())) {
                return Localization.lang("Empty Underline");
            }
            if (FileAnnotationType.HIGHLIGHT.equals(annotation.getAnnotationType())) {
                return Localization.lang("Empty Highlight");
            }
            return Localization.lang("Empty Marking");
        }

        if (FileAnnotationType.UNDERLINE.equals(annotation.getAnnotationType())) {
            return Localization.lang("Underline") + ": " + this.getContent();
        }
        if (FileAnnotationType.HIGHLIGHT.equals(annotation.getAnnotationType())) {
            return Localization.lang("Highlight") + ": " + this.getContent();
        }

        return super.toString();
    }

    public String getMarking() {
        return marking.get();
    }
}
