/*
 * OpenOfficeDocumentCreator.java
 *
 * Created on February 16, 2005, 8:04 PM
 */

package net.sf.jabref.export;

import java.io.*;
import java.net.URL;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
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
public class OpenDocumentSpreadsheetCreator extends ExportFormat {

    /**
     * Creates a new instance of OpenOfficeDocumentCreator
     */
    public OpenDocumentSpreadsheetCreator() {
        super(Globals.lang("OpenDocument Spreadsheet"), "ods", null, null, ".ods");
    }

    public void performExport(final BibtexDatabase database, final MetaData metaData,
                              final String file, final String encoding, Set<String> keySet) throws Exception {
        exportOpenDocumentSpreadsheet(new File(file), database, keySet);
    }

    public static void storeOpenDocumentSpreadsheetFile(File file, InputStream source) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        try {
            
            //addResourceFile("mimetype", "/resource/ods/mimetype", out);
            ZipEntry ze = new ZipEntry("mimetype");
            String mime = "application/vnd.oasis.opendocument.spreadsheet";
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(mime.length());
            CRC32 crc = new CRC32();
            crc.update(mime.getBytes());
            ze.setCrc(crc.getValue());
            out.putNextEntry(ze);
            for (int i=0; i<mime.length(); i++) {
                out.write(mime.charAt(i));
            }
            out.closeEntry();

            ZipEntry zipEntry = new ZipEntry("content.xml");
            //zipEntry.setMethod(ZipEntry.DEFLATED);
            out.putNextEntry(zipEntry);
            int c = -1;
            while ((c = source.read()) >= 0) {
                out.write(c);
            }
            out.closeEntry();

            // Add manifest (required for OOo 2.0) and "meta.xml": These are in the
            // resource/ods directory, and are copied verbatim into the zip file.
            addResourceFile("meta.xml", "/resource/ods/meta.xml", out);

            addResourceFile("META-INF/manifest.xml", "/resource/ods/manifest.xml", out);

            //zipEntry = new ZipEntry()

        } finally {
            out.close();
        }
    }

    public static void exportOpenDocumentSpreadsheet(File file, BibtexDatabase database, Set<String> keySet) throws Exception {

        // First store the xml formatted content to a temporary file.
        File tmpFile = File.createTempFile("opendocument", null);
        exportOpenDocumentSpreadsheetXML(tmpFile, database, keySet);

        // Then add the content to the zip file:
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile));
        storeOpenDocumentSpreadsheetFile(file, in);

        // Delete the temporary file:
        tmpFile.delete();
    }

    public static void exportOpenDocumentSpreadsheetXML(File tmpFile, BibtexDatabase database, Set<String> keySet) {
        OpenDocumentRepresentation od = new OpenDocumentRepresentation(database, keySet);

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
        URL url = OpenDocumentSpreadsheetCreator.class.getResource(resource);
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
