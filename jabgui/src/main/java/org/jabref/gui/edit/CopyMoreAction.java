package org.jabref.gui.edit;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.os.OS;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyMoreAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyMoreAction.class);
    private final StandardActions action;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ClipBoardManager clipBoardManager;
    private final GuiPreferences preferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    public CopyMoreAction(StandardActions action,
                          DialogService dialogService,
                          StateManager stateManager,
                          ClipBoardManager clipBoardManager,
                          GuiPreferences preferences,
                          JournalAbbreviationRepository abbreviationRepository) {
        this.action = action;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.clipBoardManager = clipBoardManager;
        this.preferences = preferences;
        this.abbreviationRepository = abbreviationRepository;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty() || stateManager.getSelectedEntries().isEmpty()) {
            return;
        }

        switch (action) {
            case COPY_CITATION_KEY ->
                    copyKey();
            case COPY_AS_CITE_COMMAND ->
                    copyCiteKey();
            case COPY_CITATION_KEY_AND_TITLE ->
                    copyKeyAndTitle();
            case COPY_CITATION_KEY_AND_LINK ->
                    copyKeyAndLink();
            case COPY_DOI,
                 COPY_DOI_URL ->
                    copyDoi();
            case COPY_FIELD_AUTHOR ->
                    copyField(StandardField.AUTHOR, Localization.lang("Author"));
            case COPY_FIELD_TITLE ->
                    copyField(StandardField.TITLE, Localization.lang("Title"));
            case COPY_FIELD_JOURNAL ->
                    copyField(StandardField.JOURNAL, Localization.lang("Journal"));
            case COPY_FIELD_DATE ->
                    copyField(StandardField.DATE, Localization.lang("Date"));
            case COPY_FIELD_KEYWORDS ->
                    copyField(StandardField.KEYWORDS, Localization.lang("Keywords"));
            case COPY_FIELD_ABSTRACT ->
                    copyField(StandardField.ABSTRACT, Localization.lang("Abstract"));
            default ->
                    LOGGER.info("Unknown copy command.");
        }
    }

    private void copyDoi() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // Collect all non-null DOI or DOI urls
        if (action == StandardActions.COPY_DOI_URL) {
            copyDoiList(entries.stream()
                               .filter(entry -> entry.getDOI().isPresent())
                               .map(entry -> entry.getDOI().get().getURIAsASCIIString())
                               .toList(), entries.size());
        } else {
            copyDoiList(entries.stream()
                               .filter(entry -> entry.getDOI().isPresent())
                               .map(entry -> entry.getDOI().get().asString())
                               .toList(), entries.size());
        }
    }

    private void copyDoiList(List<String> dois, int size) {
        if (dois.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have DOIs."));
            return;
        }

        final String copiedDois = String.join(",", dois);
        clipBoardManager.setContent(copiedDois);

        if (dois.size() == size) {
            // All entries had DOIs.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(copiedDois)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined DOIs.",
                    Integer.toString(size - dois.size()), Integer.toString(size)));
        }
    }

    private void doCopyKey(Function<List<String>, String> mapKeyList) {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // Collect all non-null keys.
        List<String> keys = entries.stream()
                                   .filter(entry -> entry.getCitationKey().isPresent())
                                   .map(entry -> entry.getCitationKey().get())
                                   .toList();

        if (keys.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have citation keys."));
            return;
        }

        String clipBoardContent = mapKeyList.apply(keys);

        clipBoardManager.setContent(clipBoardContent);

        if (keys.size() == entries.size()) {
            // All entries had keys.
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(clipBoardContent)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                    Integer.toString(entries.size() - keys.size()), Integer.toString(entries.size())));
        }
    }

    private void copyCiteKey() {
        doCopyKey(keys -> {
            CitationCommandString citeCommand = preferences.getPushToApplicationPreferences().getCiteCommand();
            return citeCommand.prefix() + String.join(citeCommand.delimiter(), keys) + citeCommand.suffix();
        });
    }

    private void copyKey() {
        doCopyKey(keys -> String.join(preferences.getPushToApplicationPreferences().getCiteCommand().delimiter(), keys));
    }

    private void copyKeyAndTitle() {
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // ToDo: this string should be configurable to allow arbitrary exports
        Reader layoutString = Reader.of("\\citationkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
        Layout layout;
        try {
            layout = new LayoutHelper(layoutString, preferences.getLayoutFormatterPreferences(), abbreviationRepository).getLayoutFromText();
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
                stateManager.getActiveDatabase()
                            .map(BibDatabaseContext::getDatabase)
                            .ifPresent(bibDatabase -> keyAndTitle.append(layout.doLayout(entry, bibDatabase)));
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
                                               .toList();

        if (entriesWithKey.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have citation keys."));
            return;
        }

        for (BibEntry entry : entriesWithKey) {
            String key = entry.getCitationKey().orElse("");
            if (LOGGER.isDebugEnabled() && key.isEmpty()) {
                LOGGER.debug("entry {} had no citation key, but it should have had one", entry);
            }
            String url = entry.getField(StandardField.URL).orElse("");
            keyAndLink.append(url.isEmpty() ? key : "<a href=\"%s\">%s</a>".formatted(url, key));
            keyAndLink.append(OS.NEWLINE);
            fallbackString.append(url.isEmpty() ? key : "%s - %s".formatted(key, url));
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

    private void copyField(StandardField field, String fieldDisplayName) {
        List<BibEntry> selectedBibEntries = stateManager.getSelectedEntries();

        List<String> fieldValues = selectedBibEntries.stream()
                                                     .filter(bibEntry -> bibEntry.getFieldOrAlias(field).isPresent())
                                                     .map(bibEntry -> LatexToUnicodeAdapter.format(bibEntry.getFieldOrAlias(field).orElse("")))
                                                     .filter(value -> !value.isEmpty())
                                                     .toList();

        if (fieldValues.isEmpty()) {
            dialogService.notify(Localization.lang("None of the selected entries have %0.", fieldDisplayName));
            return;
        }

        final String copiedContent = fieldValues.stream().collect(Collectors.joining("\n"));
        clipBoardManager.setContent(copiedContent);

        if (fieldValues.size() == selectedBibEntries.size()) {
            dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                    JabRefDialogService.shortenDialogMessage(copiedContent)));
        } else {
            dialogService.notify(Localization.lang("Warning: %0 out of %1 entries have undefined %2.",
                    Integer.toString(selectedBibEntries.size() - fieldValues.size()),
                    Integer.toString(selectedBibEntries.size()),
                    fieldDisplayName));
        }
    }
}
