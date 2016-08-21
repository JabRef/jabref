package net.sf.jabref.external;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.importer.FulltextFetchers;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Try to download fulltext PDF for selected entry(ies) by following URL or DOI link.
 */
public class FindFullTextAction extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(FindFullTextAction.class);

    private final BasePanel basePanel;
    private BibEntry entry;
    private Optional<URL> result;

    public FindFullTextAction(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Override
    public void init() throws Throwable {
        basePanel.output(Localization.lang("Looking for full text document..."));
    }

    @Override
    public void run() {
        if (basePanel.getSelectedEntries().size() != 1) {
            basePanel.output(Localization.lang("This operation requires exactly one item to be selected."));
            result = Optional.empty();
        } else {
            entry = basePanel.getSelectedEntries().get(0);
            FulltextFetchers fft = new FulltextFetchers();
            result = fft.findFullTextPDF(entry);
        }
    }

    @Override
    public void update() {
        if (result.isPresent()) {
            List<String> dirs = basePanel.getBibDatabaseContext().getFileDirectory();
            if (dirs.isEmpty()) {
                JOptionPane.showMessageDialog(basePanel.frame(),
                        Localization.lang("Main file directory not set!") + " " + Localization.lang("Preferences")
                                + " -> " + Localization.lang("External programs"),
                        Localization.lang("Directory not found"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            String bibtexKey = entry.getCiteKey();
            // TODO: this needs its own thread as it blocks the UI!
            DownloadExternalFile def = new DownloadExternalFile(basePanel.frame(), basePanel.getBibDatabaseContext(), bibtexKey);
            try {
                def.download(result.get(), file -> {
                    FileListTableModel tm = new FileListTableModel();
                    entry.getFieldOptional(FieldName.FILE).ifPresent(tm::setContent);
                    tm.addEntry(tm.getRowCount(), file);
                    String newValue = tm.getStringRepresentation();
                    UndoableFieldChange edit = new UndoableFieldChange(entry, FieldName.FILE,
                            entry.getFieldOptional(FieldName.FILE).orElse(null), newValue);
                    entry.setField(FieldName.FILE, newValue);
                    basePanel.getUndoManager().addEdit(edit);
                    basePanel.markBaseChanged();
                });
            } catch (IOException e) {
                LOGGER.warn("Problem downloading file", e);
            }
            basePanel.output(Localization.lang("Finished downloading full text document"));
        }
        else {
            String message = Localization.lang("Full text document download failed");
            basePanel.output(message);
            JOptionPane.showMessageDialog(basePanel.frame(), message, message, JOptionPane.ERROR_MESSAGE);
        }
    }
}
