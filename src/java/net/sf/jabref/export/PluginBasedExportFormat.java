package net.sf.jabref.export;

import java.io.*;
import java.net.URL;

import net.sf.jabref.Globals;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.ExportFormatTemplateExtension;

/**
 * Class for export formats defined in plugins.
 * 
 * Needed since resources might be loaded from a plugin-jar.
 * 
 */
public class PluginBasedExportFormat extends ExportFormat {

	public ExportFormatTemplateExtension extension;

	/**
	 * Load the plugin from the given extension. Might be null if extension could not be loaded.
	 * 
	 * @param extension
	 * @return
	 */
	public static PluginBasedExportFormat getFormat(
			ExportFormatTemplateExtension extension) {

		String consoleName = extension.getConsoleName();
		String displayName = extension.getDisplayName();
		String layoutFilename = extension.getLayoutFilename();
		String fileExtension = extension.getExtension();

		if ("".equals(fileExtension) || "".equals(displayName)
				|| "".equals(consoleName) || "".equals(layoutFilename)) {
			Globals.logger("Could not load extension " + extension.getId());
			return null;
		}

		return new PluginBasedExportFormat(displayName, consoleName,
				layoutFilename, fileExtension, extension);
	}

	public PluginBasedExportFormat(String displayName, String consoleName,
			String layoutFileName, String fileExtension, ExportFormatTemplateExtension extension) {
		super(displayName, consoleName, layoutFileName, null, fileExtension);
		this.extension = extension;
	}

	public Reader getReader(String filename) throws IOException {
		URL reso = extension.getDirAsUrl(filename);

		Globals.logger(reso.toExternalForm());

		Reader reader;

		// If that didn't work, try loading as a normal file URL:
		if (reso != null) {
			try {
				reader = new InputStreamReader(reso.openStream());
			} catch (FileNotFoundException ex) {
				throw new IOException(Globals
						.lang("Could not find layout file")
						+ ": '" + filename + "'.");
			}
		} else {
			File f = new File(filename);
			try {
				reader = new FileReader(f);
			} catch (FileNotFoundException ex) {
				throw new IOException(Globals
						.lang("Could not find layout file")
						+ ": '" + filename + "'.");
			}
		}

		return reader;
	}
}
