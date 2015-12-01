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
package net.sf.jabref.exporter;

import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.l10n.Localization;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author alver
 */
public class OpenDocumentSpreadsheetCreator extends ExportFormat {

    /**
     * Creates a new instance of OpenOfficeDocumentCreator
     */
    public OpenDocumentSpreadsheetCreator() {
        super(Localization.lang("OpenDocument Spreadsheet"), "ods", null, null, ".ods");
    }

    @Override
    public void performExport(final BibtexDatabase database, final MetaData metaData, final String file,
            final Charset encoding, Set<String> keySet) throws Exception {
        OpenDocumentSpreadsheetCreator.exportOpenDocumentSpreadsheet(new File(file), database, keySet);
    }

    private static void storeOpenDocumentSpreadsheetFile(File file, InputStream source) throws Exception {

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {

            //addResourceFile("mimetype", "/resource/ods/mimetype", out);
            ZipEntry ze = new ZipEntry("mimetype");
            String mime = "application/vnd.oasis.opendocument.spreadsheet";
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(mime.length());
            CRC32 crc = new CRC32();
            crc.update(mime.getBytes());
            ze.setCrc(crc.getValue());
            out.putNextEntry(ze);
            for (int i = 0; i < mime.length(); i++) {
                out.write(mime.charAt(i));
            }
            out.closeEntry();

            ZipEntry zipEntry = new ZipEntry("content.xml");
            //zipEntry.setMethod(ZipEntry.DEFLATED);
            out.putNextEntry(zipEntry);
            int c;
            while ((c = source.read()) >= 0) {
                out.write(c);
            }
            out.closeEntry();

            // Add manifest (required for OOo 2.0) and "meta.xml": These are in the
            // resource/ods directory, and are copied verbatim into the zip file.
            OpenDocumentSpreadsheetCreator.addResourceFile("meta.xml", "/resource/ods/meta.xml", out);

            OpenDocumentSpreadsheetCreator.addResourceFile("META-INF/manifest.xml", "/resource/ods/manifest.xml", out);
        }
    }

    private static void exportOpenDocumentSpreadsheet(File file, BibtexDatabase database, Set<String> keySet)
            throws Exception {

        // First store the xml formatted content to a temporary file.
        File tmpFile = File.createTempFile("opendocument", null);
        OpenDocumentSpreadsheetCreator.exportOpenDocumentSpreadsheetXML(tmpFile, database, keySet);

        // Then add the content to the zip file:
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile))) {
            OpenDocumentSpreadsheetCreator.storeOpenDocumentSpreadsheetFile(file, in);
        }
        // Delete the temporary file:
        tmpFile.delete();
    }

    private static void exportOpenDocumentSpreadsheetXML(File tmpFile, BibtexDatabase database, Set<String> keySet) {
        OpenDocumentRepresentation od = new OpenDocumentRepresentation(database, keySet);

        try {
            try (Writer ps = new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8)) {

                DOMSource source = new DOMSource(od.getDOMrepresentation());
                StreamResult result = new StreamResult(ps);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(source, result);
            }
        } catch (Exception e) {
            throw new Error(e);
        }

    }

    private static void addResourceFile(String name, String resource, ZipOutputStream out) throws IOException {
        ZipEntry zipEntry = new ZipEntry(name);
        out.putNextEntry(zipEntry);
        OpenDocumentSpreadsheetCreator.addFromResource(resource, out);
        out.closeEntry();
    }

    private static void addFromResource(String resource, OutputStream out) {
        URL url = OpenDocumentSpreadsheetCreator.class.getResource(resource);
        try (InputStream in = url.openStream()) {
            byte[] buffer = new byte[256];
            synchronized (out) {
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
