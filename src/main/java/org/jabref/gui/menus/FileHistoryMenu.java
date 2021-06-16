package org.jabref.gui.menus;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.preferences.PreferencesService;

public class FileHistoryMenu extends Menu {

    private final FileHistory history;
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final OpenDatabaseAction openDatabaseAction;

    public FileHistoryMenu(PreferencesService preferences, DialogService dialogService, OpenDatabaseAction openDatabaseAction) {
        setText(Localization.lang("Recent libraries"));

        this.preferences = preferences;
        this.dialogService = dialogService;
        this.openDatabaseAction = openDatabaseAction;
        history = preferences.getFileHistory();
        if (history.isEmpty()) {
            setDisable(true);
        } else {
            setItems();
        }
    }

    /**
     * This method is to use typed letters to access recent libraries in menu.
     * @param keyEvent a KeyEvent.
     * @return false if typed char is invalid or not a number.
     */
    public boolean openFileByKey(KeyEvent keyEvent) {
        if (keyEvent.getCharacter() == null) {
            return false;
        }
        char key = keyEvent.getCharacter().charAt(0);
        int num = Character.getNumericValue(key);
        if (num <= 0 || num > history.getHistory().size()) {
            return false;
        }
        this.openFile(history.getFileAt(Integer.parseInt(keyEvent.getCharacter()) - 1));
        return true;
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
        // By default mnemonic parsing is set to true for anything that is Labeled, if an underscore character
        // is present, it would create a key combination ALT+the succeeding character (at least for Windows OS)
        // and the underscore character will be parsed (deleted).
        // i.e if the file name was called "bib_test.bib", a key combination "ALT+t" will be created
        // so to avoid this, mnemonic parsing should be set to false to print normally the underscore character.
        item.setMnemonicParsing(false);
        item.setOnAction(event -> openFile(file));
        getItems().add(item);
    }

    public void storeHistory() {
        preferences.storeFileHistory(history);
    }

    public void openFile(Path file) {
        if (!Files.exists(file)) {
            this.dialogService.showErrorDialogAndWait(
                    Localization.lang("File not found"),
                    Localization.lang("File not found") + ": " + file);
            history.removeItem(file);
            setItems();
            return;
        }
        openDatabaseAction.openFile(file, true);
    }
}
