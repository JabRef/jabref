package net.sf.jabref.export;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.msbib.MSBibDatabase;

/**
 * ExportFormat for exporting in MSBIB XML format.
 */
class MSBibExportFormat extends ExportFormat {

	public MSBibExportFormat() {
        super(Globals.lang("MS Office 2007"), "MSBib", null, null, ".xml");
    }

    public void performExport(final BibtexDatabase database, final MetaData metaData,
                              final String file, final String encoding, Set<String> keySet) throws IOException {
    	// forcing to use UTF8 output format for some problems with
    	// xml export in other encodings
        SaveSession ss = getSaveSession("UTF8", new File(file));
        VerifyingWriter ps = ss.getWriter();
        MSBibDatabase md = new MSBibDatabase(database, keySet);

        // PS: DOES NOT SUPPORT EXPORTING ONLY A SET OF ENTRIES

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
