package org.jabref.gui.menus;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.preferences.JabRefPreferences;

public class FileHistoryMenu extends Menu {

    private final FileHistory history;
    private final JabRefFrame frame;
    private final JabRefPreferences preferences;
    private final DialogService dialogService;

    public FileHistoryMenu(JabRefPreferences preferences, JabRefFrame frame) {
        setText(Localization.lang("Recent libraries"));

        this.frame = frame;
        this.preferences = preferences;
        this.dialogService = frame.getDialogService();
        history = preferences.getFileHistory();
        if (history.isEmpty()) {
            setDisable(true);
        } else {
            setItems();
        }
    }

    /**
     * Adds the filename to the top of the menu. If it already is in
     * the menu, it is merely moved to the top.
     */
    public void newFile(Path file) {
        history.newFile(file);
        setItems();
        setDisable(false);
    }

    private void setItems() {
        getItems().clear();
        for (int index = 0; index < history.size(); index++) {
            addItem(history.getFileAt(index), index + 1);
        }
    }

    private void addItem(Path file, int num) {
        String number = Integer.toString(num);
        MenuItem item = new MenuItem(number + ". " + file);
        item.setOnAction(event -> openFile(file));
        getItems().add(item);
    }

    public void storeHistory() {
        preferences.storeFileHistory(history);
    }

    public void openFile(Path file) {
        if (!Files.exists(file)) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("File not found"),
                    Localization.lang("File not found") + ": " + file);
            history.removeItem(file);
            setItems();
            return;
        }
        JabRefExecutorService.INSTANCE.execute(() -> frame.getOpenDatabaseAction().openFile(file, true));

    }

}
