package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.swing.AbstractAction;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.JabRefPreferences;

public class ExportLinkedFilesAction extends AbstractAction {

    public ExportLinkedFilesAction() {
        super(Localization.lang("Export linked files"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(
                        Paths.get(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        List<BibEntry> entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();

        DialogService ds = new FXDialogService();
        Optional<Path> path = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration));

        BibDatabaseContext databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();

        System.out.println("Files count " + entries.stream()
                .flatMap(entry -> new TypedBibEntry(entry, databaseContext).getFiles().stream()).count());

        for (BibEntry entry : entries) {
            TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);

            List<ParsedFileField> files = typedEntry.getFiles();
            for (ParsedFileField fileEntry : files) {
                String fileName = fileEntry.getLink();

                Optional<Path> fileToExport = FileUtil.expandFilename(fileName,
                        databaseContext.getFileDirectories(Globals.prefs.getFileDirectoryPreferences()))
                        .map(File::toPath);

                Optional<Path> newPath = OptionalUtil.combine(path, fileToExport, resolvePathFilename);

                newPath.ifPresent(newFile -> {
                    System.out.println(newFile);
                    FileUtil.copyFile(fileToExport.get(), newFile, false);

                });

            }
        }
    }

    BiFunction<Path, Path, Path> resolvePathFilename = (p, f) -> {
        return p.resolve(f.getFileName());
    };

}
