package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.util.XMPUtil;

/**
 * Wraps the XMPUtility function to be used as an ImportFormat.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class PdfXmpImporter extends ImportFormat {

	public String getFormatName() {
		return Globals.lang("XMP-annotated PDF");
	}

	/**
	 * Returns a list of all BibtexEntries found in the inputstream.
	 */
	public List importEntries(InputStream in) throws IOException {
		return XMPUtil.readXMP(in);
	}

	/**
	 * Returns whether the given stream contains data that is a.) a pdf and b.)
	 * contains at least one BibtexEntry.
	 * 
	 * @override
	 */
	public boolean isRecognizedFormat(InputStream in) throws IOException {
		return XMPUtil.hasMetadata(in);
	}

	/**
	 * String used to identify this import filter on the command line.
	 * 
	 * @override
	 * @return "xmp"
	 */
	public String getCLIid() {
		return "xmp";
	}

}
