/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User: alver
 *
 * Date: Oct 18, 2006
 *
 * Time: 9:35:08 PM
 */
public class ExportFormats {

    private static final Log LOGGER = LogFactory.getLog(ExportFormats.class);

    private static final Map<String, IExportFormat> EXPORT_FORMATS = new TreeMap<>();

    // Global variable that is used for counting output entries when exporting:
    public static int entryNumber;


    public static void initAllExports() {

        ExportFormats.EXPORT_FORMATS.clear();

        // Initialize Build-In Export Formats
        ExportFormats.putFormat(new ExportFormat("HTML", "html", "html", null, ".html"));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("Simple HTML"), "simplehtml", "simplehtml", null, ".html"));
        ExportFormats.putFormat(new ExportFormat("DocBook 4.4", "docbook", "docbook", null, ".xml"));
        ExportFormats.putFormat(new ExportFormat("DIN 1505", "din1505", "din1505winword", "din1505", ".rtf"));
        ExportFormats.putFormat(new ExportFormat("BibTeXML", "bibtexml", "bibtexml", null, ".xml"));
        ExportFormats.putFormat(new ExportFormat("BibO RDF", "bibordf", "bibordf", null, ".rdf"));
        ExportFormats.putFormat(new ModsExportFormat());
        ExportFormats.putFormat(new ExportFormat(Localization.lang("HTML table"), "tablerefs", "tablerefs", "tablerefs", ".html"));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("HTML list"),
                "listrefs", "listrefs", "listrefs", ".html"));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("HTML table (with Abstract & BibTeX)"),
                "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", ".html"));
        ExportFormats.putFormat(new ExportFormat("Harvard RTF", "harvard", "harvard",
                "harvard", ".rtf"));
        ExportFormats.putFormat(new ExportFormat("ISO 690", "iso690rtf", "iso690RTF", "iso690rtf", ".rtf"));
        ExportFormats.putFormat(new ExportFormat("ISO 690", "iso690txt", "iso690", "iso690txt", ".txt"));
        ExportFormats.putFormat(new ExportFormat("Endnote", "endnote", "EndNote", "endnote", ".txt"));
        ExportFormats.putFormat(new ExportFormat("OpenOffice/LibreOffice CSV", "oocsv", "openoffice-csv",
                "openoffice", ".csv"));
        ExportFormat ef = new ExportFormat("RIS", "ris", "ris", "ris", ".ris");
        ef.setEncoding(StandardCharsets.UTF_8);
        ExportFormats.putFormat(ef);
        ExportFormats.putFormat(new ExportFormat("MIS Quarterly", "misq", "misq", "misq", ".rtf"));

        ExportFormats.putFormat(new OpenOfficeDocumentCreator());
        ExportFormats.putFormat(new OpenDocumentSpreadsheetCreator());
        ExportFormats.putFormat(new MSBibExportFormat());
        ExportFormats.putFormat(new ModsExportFormat());

        // Now add custom export formats
        Map<String, ExportFormat> customFormats = Globals.prefs.customExports.getCustomExportFormats();
        for (IExportFormat format : customFormats.values()) {
            ExportFormats.putFormat(format);
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
        StringBuilder sb = new StringBuilder();
        int lastBreak = -firstLineSubtr;

        for (String name : ExportFormats.EXPORT_FORMATS.keySet()) {
            if (((sb.length() + 2 + name.length()) - lastBreak) > maxLineLength) {
                sb.append(",\n");
                lastBreak = sb.length();
                sb.append(linePrefix);
            } else if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }

        return sb.toString();
    }

    /**
     * Get a Map of all export formats.
     * @return A Map containing all export formats, mapped to their console names.
     */
    public static Map<String, IExportFormat> getExportFormats() {
        // It is perhaps overly paranoid to make a defensive copy in this case:
        return Collections.unmodifiableMap(ExportFormats.EXPORT_FORMATS);
    }

    /**
     * Look up the named export format.
     *
     * @param consoleName
     *            The export name given in the JabRef console help information.
     * @return The ExportFormat, or null if no exportformat with that name is
     *         registered.
     */
    public static IExportFormat getExportFormat(String consoleName) {
        return ExportFormats.EXPORT_FORMATS.get(consoleName);
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

            private final JabRefFrame frame;

            private final boolean selectedOnly;


            public ExportAction(JabRefFrame frame, boolean selectedOnly) {
                this.frame = frame;
                this.selectedOnly = selectedOnly;
                putValue(Action.NAME, selectedOnly ? Localization.menuTitle("Export selected entries") :
                    Localization.menuTitle("Export"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ExportFormats.initAllExports();
                JFileChooser fc = ExportFormats.createExportFileChooser(
                        Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));
                fc.showSaveDialog(frame);
                File file = fc.getSelectedFile();
                if (file == null) {
                    return;
                }
                FileFilter ff = fc.getFileFilter();
                if (ff instanceof ExportFileFilter) {

                    ExportFileFilter eff = (ExportFileFilter) ff;
                    String path = file.getPath();
                    if (!path.endsWith(eff.getExtension())) {
                        path = path + eff.getExtension();
                    }
                    file = new File(path);
                    if (file.exists()) {
                        // Warn that the file exists:
                        if (JOptionPane.showConfirmDialog(frame,
                                Localization.lang("'%0' exists. Overwrite file?", file.getName()),
                                Localization.lang("Export"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                            return;
                        }
                    }
                    final IExportFormat format = eff.getExportFormat();
                    List<BibEntry> entries;
                    if (selectedOnly) {
                        // Selected entries
                        entries = frame.getCurrentBasePanel().getSelectedEntries();
                    } else {
                        // All entries
                        entries = frame.getCurrentBasePanel().getDatabase().getEntries();
                    }

                    // Set the global variable for this database's file directory before exporting,
                    // so formatters can resolve linked files correctly.
                    // (This is an ugly hack!)
                    Globals.prefs.fileDirForDatabase = frame.getCurrentBasePanel().getBibDatabaseContext()
                            .getFileDirectory();

                    // Make sure we remember which filter was used, to set
                    // the default for next time:
                    Globals.prefs.put(JabRefPreferences.LAST_USED_EXPORT, format.getConsoleName());
                    Globals.prefs.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, file.getParent());

                    final File finFile = file;
                    final List<BibEntry> finEntries = entries;
                    AbstractWorker exportWorker = new AbstractWorker() {

                        String errorMessage;


                        @Override
                        public void run() {
                            try {
                                format.performExport(frame.getCurrentBasePanel().getBibDatabaseContext(),
                                        finFile.getPath(), frame.getCurrentBasePanel().getEncoding(), finEntries);
                            } catch (Exception ex) {
                                LOGGER.warn("Problem exporting", ex);
                                if (ex.getMessage() == null) {
                                    errorMessage = ex.toString();
                                } else {
                                    errorMessage = ex.getMessage();
                                }
                            }
                        }

                        @Override
                        public void update() {
                            // No error message. Report success:
                            if (errorMessage == null) {
                                frame.output(Localization.lang("%0 export successful", format.getDisplayName()));
                            }
                            // ... or show an error dialog:
                            else {
                                frame.output(Localization.lang("Could not save file.")
                                        + " - " + errorMessage);
                                // Need to warn the user that saving failed!
                                JOptionPane.showMessageDialog(frame, Localization.lang("Could not save file.")
                                        + "\n" + errorMessage, Localization.lang("Save database"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };

                    // Run the export action in a background thread:
                    exportWorker.getWorker().run();
                    // Run the update method:
                    exportWorker.update();
                }
            }
        }

        return new ExportAction(frame, selectedOnly);
    }

    private static JFileChooser createExportFileChooser(String currentDir) {
        String lastUsedFormat = Globals.prefs.get(JabRefPreferences.LAST_USED_EXPORT);
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        Set<FileFilter> filters = new TreeSet<>();
        for (Map.Entry<String, IExportFormat> e : ExportFormats.EXPORT_FORMATS.entrySet()) {
            String formatName = e.getKey();
            IExportFormat format = e.getValue();
            filters.add(format.getFileFilter());
            if (formatName.equals(lastUsedFormat)) {
                defaultFilter = format.getFileFilter();
            }
        }
        for (FileFilter ff : filters) {
            fc.addChoosableFileFilter(ff);
        }
        fc.setAcceptAllFileFilterUsed(false);
        if (defaultFilter != null) {
            fc.setFileFilter(defaultFilter);
        }
        return fc;
    }

    private static void putFormat(IExportFormat format) {
        ExportFormats.EXPORT_FORMATS.put(format.getConsoleName(), format);
    }

}
