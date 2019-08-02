package org.jabref.gui.edit;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.JabRefGUI;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

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
                JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("None of the selected entries have BibTeX keys."));
                return;
            }

            for (BibEntry entry : entriesWithKey) {
                String key = entry.getCiteKeyOptional().get();
                String url = entry.getField(StandardField.URL).orElse("");
                sb.append(url.isEmpty() ? key : String.format("<a href=\"%s\">%s</a>", url, key));
                sb.append(OS.NEWLINE);
            }
            final String keyAndLink = sb.toString();
            DefaultTaskExecutor.runInJavaFXThread(() -> clipboardManager.setHtmlContent(keyAndLink));

            int copied = entriesWithKey.size();
            int toCopy = entries.size();
            if (copied == toCopy) {
                // All entries had keys.
                JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Copied") + " '" + JabRefDialogService.shortenDialogMessage(keyAndLink) + "'.");
            } else {
                JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                        Long.toString(toCopy - copied), Integer.toString(toCopy)));
            }
        }
    }
}
