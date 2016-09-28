package net.sf.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.logic.mods.MODSDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;

/**
 * ExportFormat for exporting in MODS XML format.
 */
class ModsExportFormat extends ExportFormat {

    public ModsExportFormat() {
        super("MODS", "mods", null, null, ".xml");
    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, final String file,
            final Charset encoding, List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }

        SaveSession ss = new FileSaveSession(StandardCharsets.UTF_8, false);
        try (VerifyingWriter ps = ss.getWriter()) {
            MODSDatabase md = new MODSDatabase(databaseContext.getDatabase(), entries);

            try {
                DOMSource source = new DOMSource(md.getDOMrepresentation());
                StreamResult result = new StreamResult(ps);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(source, result);
            } catch (TransformerException | IllegalArgumentException | TransformerFactoryConfigurationError e) {
                throw new Error(e);
            }
            finalizeSaveSession(ss, Paths.get(file));
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }
}
