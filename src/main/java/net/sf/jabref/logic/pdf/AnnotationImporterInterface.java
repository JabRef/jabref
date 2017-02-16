package net.sf.jabref.logic.pdf;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.pdf.FileAnnotation;

public interface AnnotationImporterInterface {

    List<FileAnnotation> importAnnotations(final String path, final BibDatabaseContext context);
}
