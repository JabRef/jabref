/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileHistory;

public class FileHistoryMenu extends JMenu implements ActionListener {

    private final FileHistory history;
    private final JabRefFrame frame;


    public FileHistoryMenu(JabRefPreferences prefs, JabRefFrame frame) {
        String name = Localization.menuTitle("Recent files");
        int i = name.indexOf('&');
        if (i >= 0) {
            setText(name.substring(0, i) + name.substring(i + 1));
            char mnemonic = Character.toUpperCase(name.charAt(i + 1));
            setMnemonic((int) mnemonic);
        } else {
            setText(name);
        }

        this.frame = frame;
        history = new FileHistory(prefs);
        if (history.size() > 0) {
            setItems();
        } else {
            setEnabled(false);
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
        history.storeHistory();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = ((JMenuItem) e.getSource()).getText();
        int pos = name.indexOf(' ');
        name = name.substring(pos + 1);
        final File fileToOpen = new File(name);

        // the existence check has to be done here (and not in open.openIt) as we have to call "removeItem" if the file does not exist
        if (!fileToOpen.exists()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File not found") + ": " + fileToOpen.getName(),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            history.removeItem(name);
            setItems();
            return;
        }
        JabRefExecutorService.INSTANCE.execute(() -> frame.getOpenDatabaseAction().openFile(fileToOpen, true));

    }

}
