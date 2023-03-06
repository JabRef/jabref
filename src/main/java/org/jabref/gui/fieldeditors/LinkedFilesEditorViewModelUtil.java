package org.jabref.gui.fieldeditors;

import java.util.Optional;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class LinkedFilesEditorViewModelUtil {

    private final DialogService dialogService;
    private final BibEntry entry;

    public LinkedFilesEditorViewModelUtil(DialogService dialogService, BibEntry entry) {
        this.dialogService = dialogService;
        this.entry = entry;
    }

    public Optional<String> getUrlForDownloadFromClipBoardOrEntry() {
        String clipText = ClipBoardManager.getContents();
        Optional<String> urlText;
        String urlField = entry.getField(StandardField.URL).orElse("");
        if (clipText.startsWith("http://") || clipText.startsWith("https://") || clipText.startsWith("ftp://")) {
            urlText = dialogService.showInputDialogWithDefaultAndWait(
                    Localization.lang("Download file"), Localization.lang("Enter URL to download"), clipText);
        } else if (urlField.startsWith("http://") || urlField.startsWith("https://") || urlField.startsWith("ftp://")) {
            urlText = dialogService.showInputDialogWithDefaultAndWait(
                    Localization.lang("Download file"), Localization.lang("Enter URL to download"), urlField);
        } else {
            urlText = dialogService.showInputDialogAndWait(
                    Localization.lang("Download file"), Localization.lang("Enter URL to download"));
        }
       return urlText;
    }
}