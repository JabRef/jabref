package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
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
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewDroppedFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewDroppedFileHandler.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final ExternalFileTypes externalFileTypes;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final MoveFilesCleanup moveFilesCleanup;
    private final DialogService dialogService;

    public NewDroppedFileHandler(DialogService dialogService, BibDatabaseContext bibDatabaseContext, ExternalFileTypes externalFileTypes, FileDirectoryPreferences fileDirectoryPreferences, String fileDirPattern) {
        this.dialogService = dialogService;
        this.externalFileTypes = externalFileTypes;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
        this.bibDatabaseContext = bibDatabaseContext;
        this.moveFilesCleanup = new MoveFilesCleanup(bibDatabaseContext, fileDirPattern, fileDirectoryPreferences);
    }

    public void addFilesToEntry(BibEntry entry, List<Path> files) {

        for (Path file : files) {
            FileUtil.getFileExtension(file).ifPresent(ext -> {

                ExternalFileType type = externalFileTypes.getExternalFileTypeByExt(ext)
                        .orElse(new UnknownExternalFileType(ext));
                Path relativePath = FileUtil.shortenFileName(file, bibDatabaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences));
                LinkedFile linkedfile = new LinkedFile("", relativePath.toString(), type.getName());
                entry.addFile(linkedfile);
            });

        }

    }

    public void addNewEntryFromXMPorPDFContent(List<Path> files) {

        for (Path file : files) {

            if (FileUtil.getFileExtension(file).filter(ext -> ext.equals("pdf")).isPresent()) {

                try {

                    List<BibEntry> xmpEntriesInFile = XmpUtilReader.readXmp(file, Globals.prefs.getXMPPreferences());

                    if (showImportDialogAndShouldImport(xmpEntriesInFile, file.toString())) {

                        bibDatabaseContext.getDatabase().insertEntries(xmpEntriesInFile);
                        for (BibEntry entry : xmpEntriesInFile) {


                        }

                    }
                    //PdfContentImporter
                    //List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

                } catch (IOException e) {
                    LOGGER.warn("Problem reading XMP", e);
                }

            }
        }
    }

    private boolean showImportDialogAndShouldImport(List<BibEntry> entries, String fileName) {

        if (entries.isEmpty()) {
            return false;
        }
        DialogPane pane = new DialogPane();

        VBox vbox = new VBox();
        Text text = new Text(Localization.lang("The PDF contains one or several BibTeX-records.")
                + "\n" + Localization.lang("Do you want to import these as new entries into the current library?"));
        vbox.getChildren().add(text);

        entries.forEach(entry -> {
            TextArea textArea = new TextArea(entry.toString());
            textArea.setEditable(false);
            vbox.getChildren().add(textArea);
        });
        pane.setContent(vbox);

        ButtonType importButton = new ButtonType("Import", ButtonData.OK_DONE);
        Optional<ButtonType> buttonPressed = dialogService.showCustomDialogAndWait(Localization.lang("XMP-metadata found in PDF: %0", fileName), pane, importButton, ButtonType.CANCEL);

        if (buttonPressed.isPresent() && buttonPressed.get().equals(importButton)) {
            return true;
        }
        return false;
    }

    public void addToEntryAndMoveToFileDir(BibEntry entry, List<Path> files) {
        addFilesToEntry(entry, files);
        moveFilesCleanup.cleanup(entry);

    }

}
