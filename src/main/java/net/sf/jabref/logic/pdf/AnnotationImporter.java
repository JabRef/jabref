package net.sf.jabref.logic.pdf;

import java.util.List;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.pdf.FileAnnotation;

public interface AnnotationImporter {

    List<FileAnnotation> importAnnotations(final String path, final BibDatabaseContext context);
}
