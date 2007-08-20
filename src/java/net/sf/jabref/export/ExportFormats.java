package net.sf.jabref.export;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.ExportFormatTemplateExtension;

/**
 * User: alver
 * 
 * Date: Oct 18, 2006 
 * 
 * Time: 9:35:08 PM 
 */
public class ExportFormats {

	private static Map<String,ExportFormat> exportFormats = new TreeMap<String,ExportFormat>();

    public static void initAllExports() {
        exportFormats.clear();

        // Initialize Build-In Export Formats
        putFormat(new ExportFormat(
                Globals.lang("HTML"), "html", "html", null, ".html"));
        putFormat(new ExportFormat(
                Globals.lang("Simple HTML"), "simplehtml", "simplehtml", null, ".html"));
        putFormat(new ExportFormat(Globals.lang("Docbook"), "docbook", "docbook", null, ".xml"));
        putFormat(new ExportFormat(Globals.lang("BibTeXML"), "bibtexml", "bibtexml", null, ".xml"));
        putFormat(new ModsExportFormat());
        putFormat(new ExportFormat(Globals.lang("HTML table"),
                "tablerefs", "tablerefs", "tablerefs", ".html"));
        putFormat(new ExportFormat(Globals.lang("HTML table (with Abstract & BibTeX)"),
                "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", ".html"));
        putFormat(new ExportFormat(Globals.lang("Harvard RTF"), "harvard", "harvard",
                "harvard", ".rtf"));
        putFormat(new ExportFormat(Globals.lang("Endnote"), "endnote", "EndNote",
                "endnote", ".txt"));
        putFormat(new OpenOfficeDocumentCreator());
        putFormat(new OpenDocumentSpreadsheetCreator());
        putFormat(new MSBibExportFormat());
    
        // Add Export Formats contributed by Plugins
        JabRefPlugin plugin = JabRefPlugin.getInstance(PluginCore.getManager());
		if (plugin != null){
			for (ExportFormatTemplateExtension e : plugin.getExportFormatTemplateExtensions()){
				ExportFormat format = PluginBasedExportFormat.getFormat(e);
				if (format != null){
					putFormat(format);
				}
			}
		}
        
        // Now add custom export formats
        for (ExportFormat format : Globals.prefs.customExports.getCustomExportFormats().values()){
            putFormat(format);
        }
    }

	/**
	 * Build a string listing of all available export formats.
	 * 
	 * @param maxLineLength
	 *            The max line length before a line break must be added.
	 * @param linePrefix
	 *            If a line break is added, this prefix will be inserted at the
	 *            beginning of the next line.
	 * @return The string describing available formats.
	 */
	public static String getConsoleExportList(int maxLineLength, int firstLineSubtr,
		String linePrefix) {
		StringBuffer sb = new StringBuffer();
		int lastBreak = -firstLineSubtr;

		for (Iterator<String> i = exportFormats.keySet().iterator(); i.hasNext();) {
			String name = i.next();
			if (sb.length() + 2 + name.length() - lastBreak > maxLineLength) {
				sb.append(",\n");
				lastBreak = sb.length();
				sb.append(linePrefix);
			} else if (sb.length() > 0)
				sb.append(", ");
			sb.append(name);
		}

		return sb.toString();
	}

    /**
     * Get a Map of all export formats.
     * @return A Map containing all export formats, mapped to their console names.
     */
    public static Map<String, ExportFormat> getExportFormats() {
        // It is perhaps overly paranoid to make a defensive copy in this case:
        return Collections.unmodifiableMap(exportFormats);
    } 

    /**
	 * Look up the named export format.
	 * 
	 * @param consoleName
	 *            The export name given in the JabRef console help information.
	 * @return The ExportFormat, or null if no exportformat with that name is
	 *         registered.
	 */
	public static ExportFormat getExportFormat(String consoleName) {
		return exportFormats.get(consoleName);
	}

	/**
	 * Create an AbstractAction for performing an export operation.
	 * 
	 * @param frame
	 *            The JabRefFrame of this JabRef instance.
	 * @param selectedOnly
	 *            true indicates that only selected entries should be exported,
	 *            false indicates that all entries should be exported.
	 * @return The action.
	 */
	public static AbstractAction getExportAction(JabRefFrame frame, boolean selectedOnly) {

		class ExportAction extends MnemonicAwareAction {

			private static final long serialVersionUID = 639463604530580554L;

			private JabRefFrame frame;

			private boolean selectedOnly;

			public ExportAction(JabRefFrame frame, boolean selectedOnly) {
				this.frame = frame;
				this.selectedOnly = selectedOnly;
				putValue(NAME, selectedOnly ? "Export selected entries" : "Export");
			}

			public void actionPerformed(ActionEvent e) {
				ExportFormats.initAllExports();
				JFileChooser fc = ExportFormats.createExportFileChooser(Globals.prefs
					.get("exportWorkingDirectory"));
				fc.showSaveDialog(frame);
				File file = fc.getSelectedFile();
				if (file == null)
					return;
				FileFilter ff = fc.getFileFilter();
				if (ff instanceof ExportFileFilter) {
					try {
						ExportFileFilter eff = (ExportFileFilter) ff;
						String path = file.getPath();
						if (!path.endsWith(eff.getExportFormat().getExtension()))
							path = path + eff.getExportFormat().getExtension();
						file = new File(path);
						if (file.exists()) {
							// Warn that the file exists:
							if (JOptionPane.showConfirmDialog(frame, "'" + file.getName() + "' "
								+ Globals.lang("exists. Overwrite file?"), Globals.lang("Export"),
								JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
								return;
						}
						ExportFormat format = eff.getExportFormat();
						Set<String> entryIds = null;
						if (selectedOnly) {
							BibtexEntry[] selected = frame.basePanel().getSelectedEntries();
							entryIds = new HashSet<String>();
							for (int i = 0; i < selected.length; i++) {
								BibtexEntry bibtexEntry = selected[i];
								entryIds.add(bibtexEntry.getId());
							}
						}
						
						// Make sure we remember which filter was used, to set
						// the default for next time:
						Globals.prefs.put("lastUsedExport", format.getConsoleName());
						Globals.prefs.put("exportWorkingDirectory", file.getParent());
						
						format.performExport(frame.basePanel().database(), file.getPath(), frame
							.basePanel().getEncoding(), entryIds);
						
					} catch (Exception ex) {
						ex.printStackTrace();

						frame.output(Globals.lang("Could not save file") + " - " + ex.getMessage());
						
						// Need to warn the user that saving failed!
						JOptionPane.showMessageDialog(frame, Globals.lang("Could not save file")
							+ ".\n" + ex.getMessage(), Globals.lang("Save database"),
							JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}

		return new ExportAction(frame, selectedOnly);
	}

    
    public static JFileChooser createExportFileChooser(String currentDir) {
		String lastUsedFormat = Globals.prefs.get("lastUsedExport");
		FileFilter defaultFilter = null;
		JFileChooser fc = new JFileChooser(currentDir);
		TreeSet<FileFilter> filters = new TreeSet<FileFilter>();
		for (Iterator<String> i = exportFormats.keySet().iterator(); i.hasNext();) {
			String formatName = i.next();
			ExportFormat format = exportFormats.get(formatName);
			filters.add(format.getFileFilter());
			if (formatName.equals(lastUsedFormat))
				defaultFilter = format.getFileFilter();
		}
		for (Iterator<FileFilter> i = filters.iterator(); i.hasNext();) {
			fc.addChoosableFileFilter((ExportFileFilter) i.next());
		}
		fc.setAcceptAllFileFilterUsed(false);
		if (defaultFilter != null)
			fc.setFileFilter(defaultFilter);
		return fc;
	}

	private static void putFormat(ExportFormat format) {
		exportFormats.put(format.getConsoleName(), format);
	}

}