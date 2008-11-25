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
	 * Load the plugin from the given extension. Might be null if extension
	 * could not be loaded.
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
        String encoding = extension.getEncoding();
		if ("".equals(fileExtension) || "".equals(displayName)
			|| "".equals(consoleName) || "".equals(layoutFilename)) {
			Globals.logger("Could not load extension " + extension.getId());
			return null;
		}

		return new PluginBasedExportFormat(displayName, consoleName,
			layoutFilename, fileExtension, encoding, extension);
	}

	public PluginBasedExportFormat(String displayName, String consoleName,
		String layoutFileName, String fileExtension, String encoding,
		ExportFormatTemplateExtension extension) {
		super(displayName, consoleName, layoutFileName, null, fileExtension);
        // Set the overriding encoding, if the plugin supplied one:
        if (encoding != null)
            setEncoding(encoding);
		this.extension = extension;
	}

	@Override
	public Reader getReader(String filename) throws IOException {
		URL reso = extension.getDirAsUrl(filename);

		if (reso != null) {
			try {
				return new InputStreamReader(reso.openStream());
			} catch (FileNotFoundException ex) {
				// If that didn't work, try below
			}
		}

		try {
			return new FileReader(new File(filename));
		} catch (FileNotFoundException ex) {
			// If that did not work, throw the IOException below
		}
		throw new IOException(Globals.lang("Could not find layout file")
			+ ": '" + filename + "'.");
	}
}
