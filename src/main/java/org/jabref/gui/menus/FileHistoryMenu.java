package org.jabref.gui.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.preferences.JabRefPreferences;

public class FileHistoryMenu extends JMenu implements ActionListener {

    private final FileHistory history;
    private final JabRefFrame frame;
    private final JabRefPreferences prefs;

    public FileHistoryMenu(JabRefPreferences prefs, JabRefFrame frame) {
        String name = Localization.menuTitle("Recent libraries");
        int i = name.indexOf('&');
        if (i >= 0) {
            setText(name.substring(0, i) + name.substring(i + 1));
            char mnemonic = Character.toUpperCase(name.charAt(i + 1));
            setMnemonic((int) mnemonic);
        } else {
            setText(name);
        }

        this.frame = frame;
        this.prefs = prefs;
        history = prefs.getFileHistory();
        if (history.isEmpty()) {
            setEnabled(false);
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
        if (!isEnabled()) {
            setEnabled(true);
        }
    }

    private void setItems() {
        removeAll();
        for (int count = 0; count < history.size(); count++) {
            addItem(history.getFileName(count), count + 1);
        }
    }

    private void addItem(String filename, int num) {
        String number = Integer.toString(num);
        JMenuItem item = new JMenuItem(number + ". " + filename);
        char mnemonic = Character.toUpperCase(number.charAt(0));
        item.setMnemonic((int) mnemonic);
        item.addActionListener(this);
        add(item);
        //history.addFirst(item);
    }

    public void storeHistory() {
        prefs.storeFileHistory(history);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = ((JMenuItem) e.getSource()).getText();
        int pos = name.indexOf(' ');
        name = name.substring(pos + 1);
        final Path fileToOpen = Paths.get(name);

        // the existence check has to be done here (and not in open.openIt) as we have to call "removeItem" if the file does not exist
        if (!Files.exists(fileToOpen)) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File not found") + ": " + fileToOpen.getFileName(),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            history.removeItem(name);
            setItems();
            return;
        }
        JabRefExecutorService.INSTANCE.execute(() -> frame.getOpenDatabaseAction().openFile(fileToOpen, true));

    }

}
