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

import java.io.*;
import java.net.URL;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;

/**
 * @author alver
 */
public class OpenOfficeDocumentCreator extends ExportFormat {

    /**
     * Creates a new instance of OpenOfficeDocumentCreator
     */
    public OpenOfficeDocumentCreator() {
        super(Globals.lang("OpenOffice Calc"), "oocalc", null, null, ".sxc");
    }

    public void performExport(final BibtexDatabase database, final MetaData metaData,
                              final String file, final String encoding, Set<String> keySet) throws Exception {
        exportOpenOfficeCalc(new File(file), database, keySet);
    }

    public static void storeOpenOfficeFile(File file, InputStream source) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            ZipEntry zipEntry = new ZipEntry("content.xml");
            out.putNextEntry(zipEntry);
            int c = -1;
            while ((c = source.read()) >= 0) {
                out.write(c);
            }
            out.closeEntry();

            // Add manifest (required for OOo 2.0), "meta.xml", "mimetype" files. These are in the
            // resource/openoffice directory, and are copied verbatim into the zip file.
            addResourceFile("meta.xml", "/resource/openoffice/meta.xml", out);
            addResourceFile("mimetype", "/resource/openoffice/mimetype", out);
            addResourceFile("META-INF/manifest.xml", "/resource/openoffice/manifest.xml", out);

            //zipEntry = new ZipEntry()

        } finally {
            out.close();
        }
    }

    public static void exportOpenOfficeCalc(File file, BibtexDatabase database,
        Set<String> keySet) throws Exception {


        // First store the xml formatted content to a temporary file.
        File tmpFile = File.createTempFile("oocalc", null);
        exportOpenOfficeCalcXML(tmpFile, database, keySet);

        // Then add the content to the zip file:
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile));
        storeOpenOfficeFile(file, in);

        // Delete the temporary file:
        tmpFile.delete();
    }

    public static void exportOpenOfficeCalcXML(File tmpFile, BibtexDatabase database, Set<String> keySet) {
        OOCalcDatabase od = new OOCalcDatabase(database, keySet);

        try {
            Writer ps = new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF8");
            try {

                //            Writer ps = new FileWriter(tmpFile);
                DOMSource source = new DOMSource(od.getDOMrepresentation());
                StreamResult result = new StreamResult(ps);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(source, result);
            } finally {
                ps.close();
            }
        } catch (Exception e) {
            throw new Error(e);
        }

        return;
    }

    private static void addResourceFile(String name, String resource, ZipOutputStream out) throws IOException {
        ZipEntry zipEntry = new ZipEntry(name);
        out.putNextEntry(zipEntry);
        addFromResource(resource, out);
        out.closeEntry();
    }

    private static void addFromResource(String resource, OutputStream out) {
        URL url = OpenOfficeDocumentCreator.class.getResource(resource);
        try {
            InputStream in = url.openStream();
            byte[] buffer = new byte[256];
            synchronized (in) {
                synchronized (out) {
                    while (true) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) break;
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
