package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Try to download fulltext PDF for selected entry(ies) by following URL or DOI link.
 */
public class FindFullTextAction extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(FindFullTextAction.class);

    private static final int WARNING_LIMIT = 5; // The minimum number of selected entries to ask the user for confirmation

    private final BasePanel basePanel;
    private final Map<Optional<URL>, BibEntry> downloads = new ConcurrentHashMap<>();

    public FindFullTextAction(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Override
    public void init() throws Exception {
        if (!basePanel.getSelectedEntries().isEmpty()) {
            basePanel.output(Localization.lang("Looking for full text document..."));
        } else {
            LOGGER.debug("No entry selected for fulltext download.");
        }
    }

    @Override
    public void run() {
        if (basePanel.getSelectedEntries().size() >= WARNING_LIMIT) {
            String[] options = new String[] {Localization.lang("Look up full text documents"),
                    Localization.lang("Cancel")};
            int answer = JOptionPane.showOptionDialog(basePanel.frame(),
                    Localization.lang(
                            "You are about to look up full text documents for %0 entries.",
                            String.valueOf(basePanel.getSelectedEntries().size())) + "\n"
                            + Localization.lang("JabRef will send at least one request per entry to a publisher.")
                            + "\n"
                            + Localization.lang("Do you still want to continue?"),
                    Localization.lang("Look up full text documents"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (answer != JOptionPane.OK_OPTION) {
                basePanel.output(Localization.lang("Operation canceled."));
                return;
            }
        }
        for (BibEntry entry : basePanel.getSelectedEntries()) {
            FulltextFetchers fft = new FulltextFetchers(Globals.prefs.getImportFormatPreferences());
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
                        .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
                if (dirs.isEmpty()) {
                    JOptionPane.showMessageDialog(basePanel.frame(),
                            Localization.lang("Main file directory not set!") + " " + Localization.lang("Preferences")
                                    + " -> " + Localization.lang("File"),
                            Localization.lang("Directory not found"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                DownloadExternalFile def = new DownloadExternalFile(basePanel.frame(),
                        basePanel.getBibDatabaseContext(), entry);
                try {
                    def.download(result.get(), file -> {
                        Optional<FieldChange> fieldChange = entry.addFile(file);
                        if (fieldChange.isPresent()) {
                            UndoableFieldChange edit = new UndoableFieldChange(entry, FieldName.FILE,
                                    entry.getField(FieldName.FILE).orElse(null), fieldChange.get().getNewValue());
                            basePanel.getUndoManager().addEdit(edit);
                            basePanel.markBaseChanged();
                        }
                    });
                } catch (IOException e) {
                    LOGGER.warn("Problem downloading file", e);
                }
                basePanel.output(Localization.lang("Finished downloading full text document for entry %0.",
                        entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            } else {
                String title = Localization.lang("Full text document download failed");
                String message = Localization.lang("Full text document download failed for entry %0.",
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
