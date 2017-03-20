package org.jabref.gui.entryeditor;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

public class FileAnnotationViewModel extends FileAnnotation {

    public FileAnnotationViewModel(FileAnnotation annotation) {
        super(annotation.getAuthor(), annotation.getTimeModified(), annotation.getPage(), annotation.getContent(),
                annotation.getAnnotationType(), annotation.hasLinkedAnnotation() ? Optional.of(annotation.getLinkedFileAnnotation()) : Optional.empty());
    }

    @Override
    public String toString() {
        if (this.hasLinkedAnnotation() && this.getContent().isEmpty()) {
            if (FileAnnotationType.UNDERLINE.equals(this.getAnnotationType())) {
                return Localization.lang("Empty Underline");
            }
            if (FileAnnotationType.HIGHLIGHT.equals(this.getAnnotationType())) {
                return Localization.lang("Empty Highlight");
            }
            return Localization.lang("Empty Marking");
        }

        if (FileAnnotationType.UNDERLINE.equals(this.getAnnotationType())) {
            return Localization.lang("Underline") + ": " + this.getContent();
        }
        if (FileAnnotationType.HIGHLIGHT.equals(this.getAnnotationType())) {
            return Localization.lang("Highlight") + ": " + this.getContent();
        }

        return super.toString();
    }
}
