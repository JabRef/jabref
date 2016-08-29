package net.sf.jabref.gui.exporter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.exporter.ExportFormat;
import net.sf.jabref.logic.exporter.ExportFormats;
import net.sf.jabref.logic.exporter.IExportFormat;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExportAction {

    private static final Log LOGGER = LogFactory.getLog(ExportAction.class);


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

        class InternalExportAction extends MnemonicAwareAction {

            private final JabRefFrame frame;

            private final boolean selectedOnly;


            public InternalExportAction(JabRefFrame frame, boolean selectedOnly) {
                this.frame = frame;
                this.selectedOnly = selectedOnly;
                putValue(Action.NAME, selectedOnly ? Localization.menuTitle("Export selected entries") : Localization
                        .menuTitle("Export"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Map<String, ExportFormat> customFormats = Globals.prefs.customExports.getCustomExportFormats(Globals.prefs,
                        Globals.journalAbbreviationLoader);
                LayoutFormatterPreferences layoutPreferences = Globals.prefs
                        .getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
                SavePreferences savePreferences = SavePreferences.loadForExportFromPreferences(Globals.prefs);
                ExportFormats.initAllExports(customFormats, layoutPreferences, savePreferences);
                JFileChooser fc = ExportAction
                        .createExportFileChooser(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));
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
                            .getFileDirectory(Globals.prefs.getFileDirectoryPreferences());

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
                                        finFile.getPath(),
                                        frame.getCurrentBasePanel().getBibDatabaseContext().getMetaData().getEncoding()
                                                .orElse(Globals.prefs.getDefaultEncoding()),
                                        finEntries);
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
                                frame.output(Localization.lang("Could not save file.") + " - " + errorMessage);
                                // Need to warn the user that saving failed!
                                JOptionPane.showMessageDialog(frame,
                                        Localization.lang("Could not save file.") + "\n" + errorMessage,
                                        Localization.lang("Save database"), JOptionPane.ERROR_MESSAGE);
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

        return new InternalExportAction(frame, selectedOnly);
    }

    private static JFileChooser createExportFileChooser(String currentDir) {
        String lastUsedFormat = Globals.prefs.get(JabRefPreferences.LAST_USED_EXPORT);
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        Set<FileFilter> filters = new TreeSet<>();
        for (Map.Entry<String, IExportFormat> e : ExportFormats.getExportFormats().entrySet()) {
            String formatName = e.getKey();
            IExportFormat format = e.getValue();
            ExportFileFilter exportFileFilter = new ExportFileFilter(format);
            filters.add(exportFileFilter);
            if (formatName.equals(lastUsedFormat)) {
                defaultFilter = exportFileFilter;
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

}
