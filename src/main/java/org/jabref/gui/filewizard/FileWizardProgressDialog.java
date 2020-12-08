package org.jabref.gui.filewizard;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * Represents the dialog window showing the progress of the running File Wizard.
 */
public class FileWizardProgressDialog extends BaseDialog<Void> {
    @FXML ProgressBar progressBar;
    @FXML ButtonType cancelButton;
    @FXML Label progressLabel;
    int numberOfBibEntries;
    boolean running;

    public FileWizardProgressDialog(int numberOfBibEntries) {
        running = true;
        this.numberOfBibEntries = numberOfBibEntries;

        this.setTitle("File Wizard");

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(cancelButton, getDialogPane(), event -> stopRunning());
    }

    public void updateProgress(BibEntry entry, int progress) {
        double percent;
        if(progress == 0) {
            percent = 0;
        } else {
            percent = (double) progress / numberOfBibEntries;
            progressBar.setProgress(percent);
        }
        Platform.runLater(() -> progressLabel.setText((int) (percent * 100) + "% complete, currently handling " + entry.getAuthorTitleYear(30)));
    }

    /**
     * Quits the File Wizard. The manager reacts to this method being executed and after completing the currently handled
     * entry, it terminates.
     */
    public void stopRunning() {
        running = false;
        Platform.runLater(() -> progressLabel.setText(Localization.lang("Cancelling, please wait...")));
    }

    public boolean isRunning() {
        return running;
    }
}
