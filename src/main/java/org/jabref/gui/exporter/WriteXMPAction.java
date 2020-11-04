package org.jabref.gui.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class WriteXMPAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;

    private OptionsDialog optionsDialog;

    private BibDatabase database;
    private Collection<BibEntry> entries;

    private boolean shouldContinue = true;
    private int skipped;
    private int entriesChanged;
    private int errors;

    public WriteXMPAction(StateManager stateManager, DialogService dialogService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        init();
        BackgroundTask.wrap(this::writeXMP)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    public void init() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        database = stateManager.getActiveDatabase().get().getDatabase();
        // Get entries and check if it makes sense to perform this operation
        entries = stateManager.getSelectedEntries();

        if (entries.isEmpty()) {

            entries = database.getEntries();

            if (entries.isEmpty()) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Write XMP metadata"),
                        Localization.lang("This operation requires one or more entries to be selected."));
                shouldContinue = false;
                return;
            } else {
                boolean confirm = dialogService.showConfirmationDialogAndWait(
                        Localization.lang("Write XMP metadata"),
                        Localization.lang("Write XMP metadata for all PDFs in current library?"));
                if (confirm) {
                    shouldContinue = false;
                    return;
                }
            }
        }

        errors = entriesChanged = skipped = 0;

        if (optionsDialog == null) {
            optionsDialog = new OptionsDialog();
        }
        optionsDialog.open();

        dialogService.notify(Localization.lang("Writing XMP metadata..."));
    }

    private void writeXMP() {
        if (!shouldContinue || stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        for (BibEntry entry : entries) {
            // Make a list of all PDFs linked from this entry:
            List<Path> files = entry.getFiles().stream()
                                    .filter(file -> file.getFileType().equalsIgnoreCase("pdf"))
                                    .map(file -> file.findIn(stateManager.getActiveDatabase().get(), Globals.prefs.getFilePreferences()))
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());

            Platform.runLater(() -> optionsDialog.getProgressArea()
                                                 .appendText(entry.getCitationKey().orElse(Localization.lang("undefined")) + "\n"));

            if (files.isEmpty()) {
                skipped++;
                Platform.runLater(() -> optionsDialog.getProgressArea()
                                                     .appendText("  " + Localization.lang("Skipped - No PDF linked") + ".\n"));
            } else {
                for (Path file : files) {
                    if (Files.exists(file)) {
                        try {
                            XmpUtilWriter.writeXmp(file, entry, database, Globals.prefs.getXmpPreferences());
                            Platform.runLater(
                                    () -> optionsDialog.getProgressArea().appendText("  " + Localization.lang("OK") + ".\n"));
                            entriesChanged++;
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                optionsDialog.getProgressArea().appendText("  " + Localization.lang("Error while writing") + " '"
                                        + file.toString() + "':\n");
                                optionsDialog.getProgressArea().appendText("    " + e.getLocalizedMessage() + "\n");
                            });
                            errors++;
                        }
                    } else {
                        skipped++;
                        Platform.runLater(() -> {
                            optionsDialog.getProgressArea()
                                         .appendText("  " + Localization.lang("Skipped - PDF does not exist") + ":\n");
                            optionsDialog.getProgressArea().appendText("    " + file.toString() + "\n");
                        });
                    }
                }
            }

            if (optionsDialog.isCanceled()) {
                Platform.runLater(
                        () -> optionsDialog.getProgressArea().appendText("\n" + Localization.lang("Operation canceled.") + "\n"));
                break;
            }
        }
        Platform.runLater(() -> {
            optionsDialog.getProgressArea()
                         .appendText("\n"
                                 + Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).", String
                                 .valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
            optionsDialog.done();
        });

        if (!shouldContinue) {
            return;
        }

        dialogService.notify(Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).",
                String.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
    }

    class OptionsDialog extends FXDialog {

        private final Button okButton = new Button(Localization.lang("OK"));
        private final Button cancelButton = new Button(Localization.lang("Cancel"));

        private boolean isCancelled;

        private final TextArea progressArea;

        public OptionsDialog() {
            super(AlertType.NONE, Localization.lang("Writing XMP metadata for selected entries..."), false);
            okButton.setDisable(true);
            okButton.setOnAction(e -> dispose());
            okButton.setPrefSize(100, 30);
            cancelButton.setOnAction(e -> isCancelled = true);
            cancelButton.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    isCancelled = true;
                }
            });
            cancelButton.setPrefSize(100, 30);
            progressArea = new TextArea();
            ScrollPane scrollPane = new ScrollPane(progressArea);
            progressArea.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
            progressArea.setEditable(false);
            progressArea.setText("");

            GridPane tmpPanel = new GridPane();
            getDialogPane().setContent(tmpPanel);
            tmpPanel.setHgap(450);
            tmpPanel.setVgap(10);
            tmpPanel.add(scrollPane, 0, 0, 2, 1);
            tmpPanel.add(okButton, 0, 1);
            tmpPanel.add(cancelButton, 1, 1);
            tmpPanel.setGridLinesVisible(false);
            this.setResizable(false);
        }

        private void dispose() {
            ((Stage) (getDialogPane().getScene().getWindow())).close();
        }

        public void done() {
            okButton.setDisable(false);
            cancelButton.setDisable(true);
        }

        public void open() {
            progressArea.setText("");
            isCancelled = false;

            okButton.setDisable(true);
            cancelButton.setDisable(false);

            okButton.requestFocus();

            optionsDialog.show();
        }

        public boolean isCanceled() {
            return isCancelled;
        }

        public TextArea getProgressArea() {
            return progressArea;
        }
    }
}
