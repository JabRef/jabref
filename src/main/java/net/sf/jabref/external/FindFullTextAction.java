package net.sf.jabref.external;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
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
    private final Map<Optional<URL>, BibEntry> downloads = new ConcurrentHashMap<>();

    public FindFullTextAction(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Override
    public void init() throws Throwable {
        basePanel.output(Localization.lang("Looking for full text document..."));
    }

    @Override
    public void run() {
        for (BibEntry entry : basePanel.getSelectedEntries()) {
            FulltextFetchers fft = new FulltextFetchers();
            downloads.put(fft.findFullTextPDF(entry), entry);
        }
    }

    @Override
    public void update() {
        List<Optional<URL>> remove = new ArrayList<>();
        for (Entry<Optional<URL>, BibEntry> download : downloads.entrySet()) {
            BibEntry entry = download.getValue();
            Optional<URL> result = download.getKey();
            if (result.isPresent()) {
                List<String> dirs = basePanel.getBibDatabaseContext()
                        .getFileDirectory(Globals.prefs.getFileDirectoryPreferences());
                if (dirs.isEmpty()) {
                    JOptionPane.showMessageDialog(basePanel.frame(),
                            Localization.lang("Main file directory not set!") + " " + Localization.lang("Preferences")
                                    + " -> " + Localization.lang("External programs"),
                            Localization.lang("Directory not found"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // TODO: this needs its own thread as it blocks the UI!
                DownloadExternalFile def = new DownloadExternalFile(basePanel.frame(),
                        basePanel.getBibDatabaseContext(), entry);
                try {
                    def.download(result.get(), file -> {
                        FileListTableModel tm = new FileListTableModel();
                        entry.getField(FieldName.FILE).ifPresent(tm::setContent);
                        tm.addEntry(tm.getRowCount(), file);
                        String newValue = tm.getStringRepresentation();
                        UndoableFieldChange edit = new UndoableFieldChange(entry, FieldName.FILE,
                                entry.getField(FieldName.FILE).orElse(null), newValue);
                        entry.setField(FieldName.FILE, newValue);
                        basePanel.getUndoManager().addEdit(edit);
                        basePanel.markBaseChanged();
                    });
                } catch (IOException e) {
                    LOGGER.warn("Problem downloading file", e);
                }
                basePanel.output(Localization.lang("Finished downloading full text document for entry %0",
                        entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            } else {
                String title = Localization.lang("Full text document download failed");
                String message = Localization.lang("Full text document download failed for entry %0",
                        entry.getCiteKeyOptional().orElse(Localization.lang("undefined")));

                basePanel.output(message);
                JOptionPane.showMessageDialog(basePanel.frame(), message, title, JOptionPane.ERROR_MESSAGE);
            }
            remove.add(result);
        }
        for (Optional<URL> result : remove) {
            downloads.remove(result);
        }
    }
}
