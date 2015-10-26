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
package net.sf.jabref.gui;

import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class FileHistory extends JMenu implements ActionListener {

    private final JabRefPreferences prefs;
    private final LinkedList<String> history = new LinkedList<>();
    private final JabRefFrame frame;


    public FileHistory(JabRefPreferences prefs, JabRefFrame frame) {
        String name = Localization.menuTitle("Recent files");
        int i = name.indexOf('&');
        if (i >= 0) {
            setText(name.substring(0, i) + name.substring(i + 1));
            char mnemonic = Character.toUpperCase(name.charAt(i + 1));
            setMnemonic((int) mnemonic);
        } else {
            setText(name);
        }

        this.prefs = prefs;
        this.frame = frame;
        String[] old = prefs.getStringArray("recentFiles");
        if ((old != null) && (old.length > 0)) {
            for (i = 0; i < old.length; i++) {
                history.addFirst(old[i]);
            }
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
        int i = 0;
        while (i < history.size()) {
            if (history.get(i).equals(filename)) {
                history.remove(i);
                i--;
            }
            i++;
        }
        history.addFirst(filename);
        while (history.size() > prefs.getInt(JabRefPreferences.HISTORY_SIZE)) {
            history.removeLast();
        }
        setItems();
        if (!isEnabled()) {
            setEnabled(true);
        }
    }

    private void setItems() {
        removeAll();
        Iterator<String> i = history.iterator();
        int count = 1;
        while (i.hasNext()) {
            addItem(i.next(), count);
            count++;
        }
    }

    private void addItem(String filename, int num) {
        String number = num + "";
        JMenuItem item = new JMenuItem(number + ". " + filename);
        char mnemonic = Character.toUpperCase(number.charAt(0));
        item.setMnemonic((int) mnemonic);
        item.addActionListener(this);
        add(item);
        //history.addFirst(item);
    }

    private void removeItem(String filename) {
        int i = 0;
        while (i < history.size()) {
            if (history.get(i).equals(filename)) {
                history.remove(i);
                setItems();
                return;
            }
            i++;
        }
    }

    public void storeHistory() {
        if (!history.isEmpty()) {
            String[] names = new String[history.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = history.get(i);
            }
            prefs.putStringArray("recentFiles", names);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = ((JMenuItem) e.getSource()).getText();
        int pos = name.indexOf(" ");
        name = name.substring(pos + 1);
        //Util.pr("'"+name+"'");
        final File fileToOpen = new File(name);

        if (!fileToOpen.exists()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File not found") + ": " + fileToOpen.getName(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            removeItem(name);
            return;
        }
        JabRefExecutorService.INSTANCE.execute(new Runnable() {

            @Override
            public void run() {
                frame.open.openIt(fileToOpen, true);
            }
        });

    }

}
