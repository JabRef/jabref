package org.jabref.gui.actions;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.JabRefGUI;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

/**
 * This class will copy each selected entry's BibTeX key as a hyperlink to its url to the clipboard.
 * In case an entry doesn't have a BibTeX key it will not be copied.
 * In case an entry doesn't have an url this will only copy the BibTeX key.
 */
public class CopyBibTeXKeyAndLinkAction implements BaseAction {

    private final MainTable mainTable;
    private final ClipBoardManager clipboardManager;

    public CopyBibTeXKeyAndLinkAction(MainTable mainTable, ClipBoardManager clipboardManager) {
        this.mainTable = mainTable;
        this.clipboardManager = clipboardManager;
    }

    @Override
    public void action() throws Exception {
        List<BibEntry> entries = mainTable.getSelectedEntries();
        if (!entries.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            List<BibEntry> entriesWithKey = entries.stream().filter(BibEntry::hasCiteKey).collect(Collectors.toList());

            if (entriesWithKey.isEmpty()) {
                JabRefGUI.getMainFrame().output(Localization.lang("None of the selected entries have BibTeX keys."));
                return;
            }

            for (BibEntry entry : entriesWithKey) {
                String key = entry.getCiteKeyOptional().get();
                String url = entry.getField(FieldName.URL).orElse("");
                sb.append(url.isEmpty() ? key : String.format("<a href=\"%s\">%s</a>", url, key));
                sb.append(OS.NEWLINE);
            }

            DefaultTaskExecutor.runInJavaFXThread(() -> clipboardManager.setHtmlContent(sb.toString()));

            int copied = entriesWithKey.size();
            int toCopy = entries.size();
            if (copied == toCopy) {
                // All entries had keys.
                JabRefGUI.getMainFrame().output((entries.size() > 1 ? Localization.lang("Copied keys") : Localization.lang("Copied key")) + '.');
            } else {
                JabRefGUI.getMainFrame().output(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                        Long.toString(toCopy - copied), Integer.toString(toCopy)));
            }
        }
    }
}
