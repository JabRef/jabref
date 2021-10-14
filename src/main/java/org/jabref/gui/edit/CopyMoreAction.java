package org.jabref.gui.edit;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
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
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyMoreAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyMoreAction.class);
    private final StandardActions action;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ClipBoardManager clipBoardManager;
    private final PreferencesService preferencesService;

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

        switch (action) {
            case COPY_TITLE -> copyTitle();
            case COPY_KEY -> copyKey();
            case COPY_CITE_KEY -> copyCiteKey();
            case COPY_KEY_AND_TITLE -> copyKeyAndTitle();
            case COPY_KEY_AND_LINK -> copyKeyAndLink();
            case COPY_DOI -> copyDoi();
            default -> LOGGER.info("Unknown copy command.");
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
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(copiedTitles)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined title.",
                    Integer.toString(selectedBibEntries.size() - titles.size()), Integer.toString(selectedBibEntries.size())));
        }
    }

    private void copyKey() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // Collect all non-null keys.
        List<String> keys = entries.stream()
                                   .filter(entry -> entry.getCitationKey().isPresent())
                                   .map(entry -> entry.getCitationKey().get())
                                   .collect(Collectors.toList());

        if (keys.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have citation keys."));
            return;
        }

        final String copiedKeys = String.join(",", keys);
        clipBoardManager.setContent(copiedKeys);

        if (keys.size() == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(copiedKeys)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                    Integer.toString(entries.size() - keys.size()), Integer.toString(entries.size())));
        }
    }

    private void copyDoi() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // Collect all non-null DOIs.
        List<String> dois = entries.stream()
                                   .filter(entry -> entry.getDOI().isPresent())
                                   .map(entry -> entry.getDOI().get().getDOI())
                                   .collect(Collectors.toList());

        if (dois.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have DOIs."));
            return;
        }

        final String copiedDois = String.join(",", dois);
        clipBoardManager.setContent(copiedDois);

        if (dois.size() == entries.size()) {
            // All entries had DOIs.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(copiedDois)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined DOIs.",
                    Integer.toString(entries.size() - dois.size()), Integer.toString(entries.size())));
        }
    }

    private void copyCiteKey() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // Collect all non-null keys.
        List<String> keys = entries.stream()
                                   .filter(entry -> entry.getCitationKey().isPresent())
                                   .map(entry -> entry.getCitationKey().get())
                                   .collect(Collectors.toList());

        if (keys.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have citation keys."));
            return;
        }

        String citeCommand = Optional.ofNullable(preferencesService.getExternalApplicationsPreferences().getCiteCommand())
                                     .filter(cite -> cite.contains("\\")) // must contain \
                                     .orElse("\\cite");

        final String copiedCiteCommand = citeCommand + "{" + String.join(",", keys) + '}';
        clipBoardManager.setContent(copiedCiteCommand);

        if (keys.size() == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(copiedCiteCommand)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                    Integer.toString(entries.size() - keys.size()), Integer.toString(entries.size())));
        }
    }

    private void copyKeyAndTitle() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // ToDo: this string should be configurable to allow arbitrary exports
        StringReader layoutString = new StringReader("\\citationkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
        Layout layout;
        try {
            layout = new LayoutHelper(layoutString, preferencesService.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository)).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.info("Could not get layout.", e);
            return;
        }

        StringBuilder keyAndTitle = new StringBuilder();

        int entriesWithKeys = 0;
        // Collect all non-null keys.
        for (BibEntry entry : entries) {
            if (entry.hasCitationKey()) {
                entriesWithKeys++;
                keyAndTitle.append(layout.doLayout(entry, stateManager.getActiveDatabase().get().getDatabase()));
            }
        }

        if (entriesWithKeys == 0) {
            dialogService.notify(Localization.lang("None of the selected entries have citation keys."));
            return;
        }

        clipBoardManager.setContent(keyAndTitle.toString());

        if (entriesWithKeys == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(keyAndTitle.toString())));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                    Integer.toString(entries.size() - entriesWithKeys), Integer.toString(entries.size())));
        }
    }

    /**
     * This method will copy each selected entry's citation key as a hyperlink to its url to the clipboard. In case an
     * entry doesn't have a citation key it will not be copied. In case an entry doesn't have an url this will only copy
     * the citation key.
     */
    private void copyKeyAndLink() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        StringBuilder keyAndLink = new StringBuilder();
        StringBuilder fallbackString = new StringBuilder();

        List<BibEntry> entriesWithKey = entries.stream()
                                               .filter(BibEntry::hasCitationKey)
                                               .collect(Collectors.toList());

        if (entriesWithKey.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have citation keys."));
            return;
        }

        for (BibEntry entry : entriesWithKey) {
            String key = entry.getCitationKey().get();
            String url = entry.getField(StandardField.URL).orElse("");
            keyAndLink.append(url.isEmpty() ? key : String.format("<a href=\"%s\">%s</a>", url, key));
            keyAndLink.append(OS.NEWLINE);
            fallbackString.append(url.isEmpty() ? key : String.format("%s - %s", key, url));
            fallbackString.append(OS.NEWLINE);
        }

        clipBoardManager.setHtmlContent(keyAndLink.toString(), fallbackString.toString());

        if (entriesWithKey.size() == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(keyAndLink.toString())));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                    Long.toString(entries.size() - entriesWithKey.size()), Integer.toString(entries.size())));
        }
    }
}
