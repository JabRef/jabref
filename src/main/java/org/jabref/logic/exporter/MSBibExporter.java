package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.msbib.MSBibDatabase;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;

/**
 * TemplateExporter for exporting in MSBIB XML format.
 */
class MSBibExporter extends Exporter {

    private final TransformerFactory transformerFactory;

    public MSBibExporter() {
        super("MSBib", "MS Office 2007", StandardFileType.XML);
        transformerFactory = TransformerFactory.newInstance();
    }

    @Override
    public void export(@NonNull BibDatabaseContext databaseContext,
                       @NonNull Path file,
                       @NonNull List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext); // required by test case
        if (entries.isEmpty()) {
            return;
        }

        MSBibDatabase msBibDatabase = new MSBibDatabase(databaseContext.getDatabase(), entries);

        // forcing to use UTF8 output format for some problems with XML export in other encodings
        try (AtomicFileWriter ps = new AtomicFileWriter(file, StandardCharsets.UTF_8)) {
            try {
                DOMSource source = new DOMSource(msBibDatabase.getDomForExport());
                StreamResult result = new StreamResult(ps);
                Transformer trans = transformerFactory.newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(source, result);
            } catch (TransformerException | IllegalArgumentException | TransformerFactoryConfigurationError e) {
                throw new SaveException(e);
            }
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }
}
