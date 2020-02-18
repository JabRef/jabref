package org.jabref.gui.edit;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyMoreAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyMoreAction.class);
    private StandardActions action;
    private DialogService dialogService;
    private StateManager stateManager;
    private final ClipBoardManager clipBoardManager;
    private PreferencesService preferencesService;

    public CopyMoreAction(StandardActions action, DialogService dialogService, StateManager stateManager, ClipBoardManager clipBoardManager, PreferencesService preferencesService) {
        this.action = action;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.clipBoardManager = clipBoardManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty() || stateManager.getSelectedEntries().isEmpty()) {
            return;
        }

        if (!Arrays.asList(
                StandardActions.COPY_TITLE,
                StandardActions.COPY_KEY,
                StandardActions.COPY_CITE_KEY,
                StandardActions.COPY_KEY_AND_TITLE,
                StandardActions.COPY_KEY_AND_LINK)
                  .contains(action)) {
            return;
        }

        switch (action) {
            case COPY_TITLE: copyTitle(); break;
            case COPY_KEY: copyKey(); break;
            case COPY_CITE_KEY: copyCiteKey(); break;
            case COPY_KEY_AND_TITLE: copyKeyAndTitle(); break;
            case COPY_KEY_AND_LINK: copyKeyAndLink(); break;
        }
    }

    private void copyTitle() {
        List<BibEntry> selectedBibEntries = stateManager.getSelectedEntries();

        List<String> titles = selectedBibEntries.stream()
                                                .filter(bibEntry -> bibEntry.getTitle().isPresent())
                                                .map(bibEntry -> bibEntry.getTitle().get())
                                                .collect(Collectors.toList());

        if (titles.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have titles."));
            return;
        }

        final String copiedTitles = String.join("\n", titles);
        clipBoardManager.setContent(copiedTitles);

        if (titles.size() == selectedBibEntries.size()) {
            // All entries had titles.
            dialogService.notify(Localization.lang("Copied") + " '" + JabRefDialogService.shortenDialogMessage(copiedTitles) + "'.");
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined title.", Integer.toString(selectedBibEntries.size() - titles.size()), Integer.toString(selectedBibEntries.size())));
        }
    }

    private void copyKey() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

            List<String> keys = new ArrayList<>(entries.size());
            // Collect all non-null keys.
            for (BibEntry entry : entries) {
                entry.getCiteKeyOptional().ifPresent(keys::add);
            }
            if (keys.isEmpty()) {
                dialogService.notify(Localization.lang("None of the selected entries have BibTeX keys."));
                return;
            }

            final String copiedKeys = String.join(",", keys);
            clipBoardManager.setContent(copiedKeys);

            if (keys.size() == entries.size()) {
                // All entries had keys.
                dialogService.notify(Localization.lang("Copied") + " '" + JabRefDialogService.shortenDialogMessage(copiedKeys) + "'.");
            } else {
                dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(entries.size() - keys.size()), Integer.toString(entries.size())));
            }
    }

    private void copyCiteKey() {
        List<BibEntry> entries = stateManager.getSelectedEntries();
        List<String> keys = new ArrayList<>(entries.size());

        // Collect all non-null keys.
        for (BibEntry entry : entries) {
            entry.getCiteKeyOptional().ifPresent(keys::add);
        }
        if (keys.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have BibTeX keys."));
            return;
        }

        String citeCommand = Optional.ofNullable(Globals.prefs.get(JabRefPreferences.CITE_COMMAND))
                                     .filter(cite -> cite.contains("\\")) // must contain \
                                     .orElse("\\cite");

        final String copiedCiteCommand = citeCommand + "{" + String.join(",", keys) + '}';
        clipBoardManager.setContent(copiedCiteCommand);

        if (keys.size() == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied") + " '" + JabRefDialogService.shortenDialogMessage(copiedCiteCommand) + "'.");
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(entries.size() - keys.size()), Integer.toString(entries.size())));
        }
    }

    private void copyKeyAndTitle() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // ToDo: in a future version, this string should be configurable to allow arbitrary exports
        StringReader layoutString = new StringReader("\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
        Layout layout;
        try {
            layout = new LayoutHelper(layoutString, preferencesService.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader))
                    .getLayoutFromText();
        } catch (IOException e) {
            LOGGER.info("Could not get layout", e);
            return;
        }

        StringBuilder keyAndTitle = new StringBuilder();

        int copied = 0;
        // Collect all non-null keys.
        for (BibEntry entry : entries) {
            if (entry.hasCiteKey()) {
                copied++;
                keyAndTitle.append(layout.doLayout(entry, stateManager.getActiveDatabase().get().getDatabase()));
            }
        }

        if (copied == 0) {
            dialogService.notify(Localization.lang("None of the selected entries have BibTeX keys."));
            return;
        }

        clipBoardManager.setContent(keyAndTitle.toString());

        if (copied == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied") + " '" + JabRefDialogService.shortenDialogMessage(keyAndTitle.toString()) + "'.");
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.", Integer.toString(entries.size() - copied), Integer.toString(entries.size())));
        }
    }

    /**
     * This method will copy each selected entry's BibTeX key as a hyperlink to its url to the clipboard.
     * In case an entry doesn't have a BibTeX key it will not be copied.
     * In case an entry doesn't have an url this will only copy the BibTeX key.
     */
    private void copyKeyAndLink() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        StringBuilder keyAndLink = new StringBuilder();

        List<BibEntry> entriesWithKey = entries.stream().filter(BibEntry::hasCiteKey).collect(Collectors.toList());

        if (entriesWithKey.isEmpty()) {
            JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("None of the selected entries have BibTeX keys."));
            return;
        }

        for (BibEntry entry : entriesWithKey) {
            String key = entry.getCiteKeyOptional().get();
            String url = entry.getField(StandardField.URL).orElse("");
            keyAndLink.append(url.isEmpty() ? key : String.format("<a href=\"%s\">%s</a>", url, key));
            keyAndLink.append(OS.NEWLINE);
        }

        clipBoardManager.setHtmlContent(keyAndLink.toString());

        int copied = entriesWithKey.size();
        int toCopy = entries.size();
        if (copied == toCopy) {
            // All entries had keys.
            JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Copied") + " '" + JabRefDialogService.shortenDialogMessage(keyAndLink.toString()) + "'.");
        } else {
            JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Warning: %0 out of %1 entries have undefined BibTeX key.",
                    Long.toString(toCopy - copied), Integer.toString(toCopy)));
        }
    }
}
