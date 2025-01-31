package org.jabref.gui.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.util.StandardFileType;

public class ConsistencyCheckDialogViewModel extends AbstractViewModel {
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibliographyConsistencyCheck.Result result;

    private final List<String> citationKeys;

    public ConsistencyCheckDialogViewModel(BibliographyConsistencyCheck.Result result, List<String> citationKeys, DialogService dialogService, GuiPreferences preferences) {
        this.result = result;
        this.citationKeys = citationKeys;
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    public List<String> getCitationKeys() {
        return citationKeys;
    }

    public void startExportAsTxt() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.TXT)
                .withDefaultExtension(StandardFileType.TXT)
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        if (exportPath.isEmpty()) {
            return;
        }

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(exportPath.get()))) {
            BibliographyConsistencyCheckResultTxtWriter bibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer);
            bibliographyConsistencyCheckResultTxtWriter.writeFindings();
        } catch (IOException e) {
            dialogService.notify(Localization.lang("Failed to export file!"));
        }
    }

    public void startExportAsCsv() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .addExtensionFilter(StandardFileType.TXT)
                .withDefaultExtension(StandardFileType.TXT)
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(exportPath.get()))) {
            BibliographyConsistencyCheckResultCsvWriter bibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer);
            bibliographyConsistencyCheckResultTxtWriter.writeFindings();
        } catch (IOException e) {
            dialogService.notify(Localization.lang("Failed to export file!"));
        }
    }
}
