/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
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
