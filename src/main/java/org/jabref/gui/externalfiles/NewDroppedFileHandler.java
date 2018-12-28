package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.UpdateFieldPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewDroppedFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewDroppedFileHandler.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final UpdateFieldPreferences updateFieldPreferences;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;

    public NewDroppedFileHandler(DialogService dialogService,
                                 BibDatabaseContext bibDatabaseContext,
                                 ExternalFileTypes externalFileTypes,
                                 FilePreferences filePreferences,
                                 ImportFormatPreferences importFormatPreferences,
                                 UpdateFieldPreferences updateFieldPreferences,
                                 FileUpdateMonitor fileupdateMonitor) {

        this.dialogService = dialogService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.updateFieldPreferences = updateFieldPreferences;
        this.fileUpdateMonitor = fileupdateMonitor;

        this.linker = new ExternalFilesEntryLinker(externalFileTypes, filePreferences, bibDatabaseContext);
        this.contentImporter = new ExternalFilesContentImporter(importFormatPreferences);
    }

    public void addNewEntryFromXMPorPDFContent(BibEntry entry, List<Path> files) {

        for (Path file : files) {

            if (FileUtil.getFileExtension(file).filter(ext -> "pdf".equals(ext)).isPresent()) {

                try {
                    List<BibEntry> pdfResult = contentImporter.importPDFContent(file);
                    //FIXME: Show merge dialog if working again properly
                    List<BibEntry> xmpEntriesInFile = contentImporter.importXMPContent(file);

                    //First try xmp import, if empty try pdf import, otherwise show dialog
                    if (xmpEntriesInFile.isEmpty()) {
                        if (pdfResult.isEmpty()) {
                            addToEntryRenameAndMoveToFileDir(entry, files);
                        } else {
                            showImportOrLinkFileDialog(pdfResult, file, entry);
                        }
                    } else {
                        showImportOrLinkFileDialog(xmpEntriesInFile, file, entry);
                    }

                } catch (IOException e) {
                    LOGGER.warn("Problem reading XMP", e);
                }

            } else {
                addToEntryRenameAndMoveToFileDir(entry, files);
            }
        }
    }

    public void importEntriesFromDroppedBibFiles(Path bibFile) {

        List<BibEntry> entriesToImport = contentImporter.importFromBibFile(bibFile, fileUpdateMonitor);
        bibDatabaseContext.getDatabase().insertEntries(entriesToImport);

        if (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)) {
            // Set owner field to default value
            UpdateField.setAutomaticFields(entriesToImport, true, true, updateFieldPreferences);
        }
    }

    private void showImportOrLinkFileDialog(List<BibEntry> entriesToImport, Path fileName, BibEntry entryToLink) {

        DialogPane pane = new DialogPane();

        VBox vbox = new VBox();
        Text text = new Text(Localization.lang("The PDF contains one or several BibTeX-records.")
                             + "\n" + Localization.lang("Do you want to import these as new entries into the current library or do you want to link the file to the entry?"));
        vbox.getChildren().add(text);

        entriesToImport.forEach(entry -> {
            TextArea textArea = new TextArea(entry.toString());
            textArea.setEditable(false);
            vbox.getChildren().add(textArea);
        });
        pane.setContent(vbox);

        ButtonType importButton = new ButtonType("Import into library", ButtonData.OK_DONE);
        ButtonType linkToEntry = new ButtonType("Link file to entry", ButtonData.OTHER);

        Optional<ButtonType> buttonPressed = dialogService.showCustomDialogAndWait(Localization.lang("XMP-metadata found in PDF: %0", fileName.getFileName().toString()), pane, importButton, linkToEntry, ButtonType.CANCEL);

        if (buttonPressed.equals(Optional.of(importButton))) {
            bibDatabaseContext.getDatabase().insertEntries(entriesToImport);
        }
        if (buttonPressed.equals(Optional.of(linkToEntry))) {
            addToEntryRenameAndMoveToFileDir(entryToLink, Arrays.asList(fileName));
        }
    }

    public void addToEntryRenameAndMoveToFileDir(BibEntry entry, List<Path> files) {
        linker.addFilesToEntry(entry, files);
        linker.moveLinkedFilesToFileDir(entry);
        linker.renameLinkedFilesToPattern(entry);
    }

    public void copyFilesToFileDirAndAddToEntry(BibEntry entry, List<Path> files) {
        for (Path file : files) {
            linker.copyFileToFileDir(file).ifPresent(copiedFile -> {
                linker.addFilesToEntry(entry, files);
            });
        }
    }

    public void addToEntry(BibEntry entry, List<Path> files) {
        linker.addFilesToEntry(entry, files);
    }

}
