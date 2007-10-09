package net.sf.jabref.export;

import net.sf.jabref.Globals;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.MetaData;
import net.sf.jabref.mods.MODSDatabase;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import java.util.Set;
import java.io.IOException;
import java.io.File;

/**
 * ExportFormat for exporting in MODS XML format.
 */
class ModsExportFormat extends ExportFormat {
    public ModsExportFormat() {
        super(Globals.lang("MODS"), "mods", null, null, ".xml");
    }

    public void performExport(final BibtexDatabase database, final MetaData metaData,
                              final String file, final String encoding, Set<String> keySet) throws IOException {
        SaveSession ss = getSaveSession("UTF8", new File(file));
        VerifyingWriter ps = ss.getWriter();
        MODSDatabase md = new MODSDatabase(database, keySet);

        try {
            DOMSource source = new DOMSource(md.getDOMrepresentation());
            StreamResult result = new StreamResult(ps);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(source, result);
        }
        catch (Exception e) {
            throw new Error(e);
        }

        try {
            finalizeSaveSession(ss);
        } catch (SaveException ex) {
            throw new IOException(ex.getMessage());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return;
    }
}
