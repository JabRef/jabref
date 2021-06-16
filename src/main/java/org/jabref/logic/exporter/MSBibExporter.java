package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
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

/**
 * TemplateExporter for exporting in MSBIB XML format.
 */
class MSBibExporter extends Exporter {

    public MSBibExporter() {
        super("MSBib", "MS Office 2007", StandardFileType.XML);
    }

    @Override
    public void export(final BibDatabaseContext databaseContext, final Path file,
                       final Charset encoding, List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        MSBibDatabase msBibDatabase = new MSBibDatabase(databaseContext.getDatabase(), entries);

        // forcing to use UTF8 output format for some problems with xml export in other encodings
        try (AtomicFileWriter ps = new AtomicFileWriter(file, StandardCharsets.UTF_8)) {
            try {
                DOMSource source = new DOMSource(msBibDatabase.getDomForExport());
                StreamResult result = new StreamResult(ps);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
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
