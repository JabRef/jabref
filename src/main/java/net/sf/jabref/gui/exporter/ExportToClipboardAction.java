package net.sf.jabref.gui.exporter;

import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.exporter.ExportFormats;
import net.sf.jabref.logic.exporter.IExportFormat;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Dec 12, 2006
 * Time: 6:22:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExportToClipboardAction extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(ExportToClipboardAction.class);

    private final JabRefFrame frame;

    /**
     * written by run() and read by update()
     */
    private String message;


    public ExportToClipboardAction(JabRefFrame frame) {
        this.frame = Objects.requireNonNull(frame);
    }

    @Override
    public void run() {
        BasePanel panel = frame.getCurrentBasePanel();
        if (panel == null) {
            return;
        }
        if (panel.getSelectedEntries().isEmpty()) {
            message = Localization.lang("This operation requires one or more entries to be selected.");
            getCallBack().update();
            return;
        }

        List<IExportFormat> exportFormats = new LinkedList<>(ExportFormats.getExportFormats().values());
        Collections.sort(exportFormats, (e1, e2) -> e1.getDisplayName().compareTo(e2.getDisplayName()));
        String[] exportFormatDisplayNames = new String[exportFormats.size()];
        for (int i = 0; i < exportFormats.size(); i++) {
            IExportFormat exportFormat = exportFormats.get(i);
            exportFormatDisplayNames[i] = exportFormat.getDisplayName();
        }

        JList<String> list = new JList<>(exportFormatDisplayNames);
        list.setBorder(BorderFactory.createEtchedBorder());
        list.setSelectionInterval(0, 0);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int answer = JOptionPane.showOptionDialog(frame, list, Localization.lang("Select export format"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[] {Localization.lang("Export"),
                        Localization.lang("Cancel")},
                Localization.lang("Export"));
        if (answer == JOptionPane.NO_OPTION) {
            return;
        }

        IExportFormat format = exportFormats.get(list.getSelectedIndex());

        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFileDirectory(Globals.prefs.getFileDirectoryPreferences());

        File tmp = null;
        try {
            // To simplify the exporter API we simply do a normal export to a temporary
            // file, and read the contents afterwards:
            tmp = File.createTempFile("jabrefCb", ".tmp");
            tmp.deleteOnExit();
            List<BibEntry> entries = panel.getSelectedEntries();

            // Write to file:
            format.performExport(panel.getBibDatabaseContext(), tmp.getPath(),
                    panel.getBibDatabaseContext().getMetaData().getEncoding()
                            .orElse(Globals.prefs.getDefaultEncoding()),
                    entries);
            // Read the file and put the contents on the clipboard:
            StringBuilder sb = new StringBuilder();
            try (Reader reader = new InputStreamReader(new FileInputStream(tmp),
                    panel.getBibDatabaseContext().getMetaData().getEncoding()
                            .orElse(Globals.prefs.getDefaultEncoding()))) {
                int s;
                while ((s = reader.read()) != -1) {
                    sb.append((char) s);
                }
            }
            ClipboardOwner owner = (clipboard, content) -> {
                // Do nothing
            };
            RtfSelection rs = new RtfSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(rs, owner);
            message = Localization.lang("Entries exported to clipboard") + ": " + entries.size();

        } catch (Exception e) {
            LOGGER.error("Error exporting to clipboard", e); //To change body of catch statement use File | Settings | File Templates.
            message = Localization.lang("Error exporting to clipboard");
        } finally {
            // Clean up:
            if ((tmp != null) && !tmp.delete()) {
                LOGGER.info("Cannot delete temporary clipboard file");
            }
        }
    }

    @Override
    public void update() {
        frame.output(message);
    }

}
