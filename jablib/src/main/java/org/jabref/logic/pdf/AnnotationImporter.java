package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.pdf.FileAnnotation;

public interface AnnotationImporter {

    List<FileAnnotation> importAnnotations(final Path path);
}
