package org.jabref.gui.menus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     *
     * @param filename a <code>String</code> value
     */
    public void newFile(String filename) {
        history.newFile(filename);
        setItems();
        setDisable(false);
    }

    private void setItems() {
        getItems().clear();
        for (int count = 0; count < history.size(); count++) {
            addItem(history.getFileName(count), count + 1);
        }
    }

    private void addItem(String fileName, int num) {
        String number = Integer.toString(num);
        MenuItem item = new MenuItem(number + ". " + fileName);
        item.setOnAction(event -> openFile(fileName));
        getItems().add(item);
    }

    public void storeHistory() {
        preferences.storeFileHistory(history);
    }

    public void openFile(String fileName) {
        final Path fileToOpen = Paths.get(fileName);

        // the existence check has to be done here (and not in open.openIt) as we have to call "removeItem" if the file does not exist
        if (!Files.exists(fileToOpen)) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("File not found"),
                    Localization.lang("File not found") + ": " + fileName);
            history.removeItem(fileName);
            setItems();
            return;
        }
        JabRefExecutorService.INSTANCE.execute(() -> frame.getOpenDatabaseAction().openFile(fileToOpen, true));

    }

}
