package org.jabref.logic.pdf;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.pdf.FileAnnotation;

public interface AnnotationImporter {

    List<FileAnnotation> importAnnotations(final String path, final BibDatabaseContext context);
}
