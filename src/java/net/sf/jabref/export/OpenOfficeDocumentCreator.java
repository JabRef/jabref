/*
 * OpenOfficeDocumentCreator.java
 *
 * Created on February 16, 2005, 8:04 PM
 */

package net.sf.jabref.export;

import net.sf.jabref.*;
import java.io.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 *
 * @author alver
 */
public class OpenOfficeDocumentCreator {
    
    /** Creates a new instance of OpenOfficeDocumentCreator */
    private OpenOfficeDocumentCreator() {
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
            
        } finally {
            out.close();
        }
    }
    
    public static void exportOpenOfficeCalc(File file, BibtexDatabase database) throws Exception {
     
            // First store the xml formatted content to a temporary file.
            File tmpFile = File.createTempFile("oocalc", null);
            exportOpenOfficeCalcXML(tmpFile, database);
            
            // Then add the content to the zip file:
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile));
            storeOpenOfficeFile(file, in);
            
            // Delete the temporary file:
            tmpFile.delete();
    }
    
    public static void exportOpenOfficeCalcXML(File tmpFile, BibtexDatabase database) {
        OOCalcDatabase od = new OOCalcDatabase(database);
        
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
	}
	catch (Exception e) {
            throw new Error(e);
	}
            
	return;
    }
}
