package net.sf.jabref.export;

import java.util.List;

/**
 * @author kariem
 */
public interface IExportFormatProvider {

	/**
	 * @return a list of export formats
	 */
	List<IExportFormat> getExportFormats();
}
