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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author alver
 */
public class OpenDocumentSpreadsheetCreator extends ExportFormat {

    private static final Log LOGGER = LogFactory.getLog(OpenDocumentSpreadsheetCreator.class);


    /**
     * Creates a new instance of OpenOfficeDocumentCreator
     */
    public OpenDocumentSpreadsheetCreator() {
        super(Localization.lang("OpenDocument spreadsheet"), "ods", null, null, ".ods");
    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, final String file,
            final Charset encoding, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (!entries.isEmpty()) { // Only export if entries exists
            OpenDocumentSpreadsheetCreator.exportOpenDocumentSpreadsheet(new File(file), databaseContext.getDatabase(), entries);
        }
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

    private static void exportOpenDocumentSpreadsheet(File file, BibDatabase database, List<BibEntry> entries)
            throws Exception {

        // First store the xml formatted content to a temporary file.
        File tmpFile = File.createTempFile("opendocument", null);
        OpenDocumentSpreadsheetCreator.exportOpenDocumentSpreadsheetXML(tmpFile, database, entries);

        // Then add the content to the zip file:
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile))) {
            OpenDocumentSpreadsheetCreator.storeOpenDocumentSpreadsheetFile(file, in);
        }
        // Delete the temporary file:
        if (!tmpFile.delete()) {
            LOGGER.info("Cannot delete temporary export file");
        }
    }

    private static void exportOpenDocumentSpreadsheetXML(File tmpFile, BibDatabase database, List<BibEntry> entries) {
        OpenDocumentRepresentation od = new OpenDocumentRepresentation(database, entries);

        try (Writer ps = new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8)) {

            DOMSource source = new DOMSource(od.getDOMrepresentation());
            StreamResult result = new StreamResult(ps);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(source, result);
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
            LOGGER.warn("Cannot get resource", e);
        }
    }
}
