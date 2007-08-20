package net.sf.jabref;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class FileHistory extends JMenu implements ActionListener {

    JabRefPreferences prefs;
    LinkedList<String> history = new LinkedList<String>();
    JabRefFrame frame;

    public FileHistory(JabRefPreferences prefs, JabRefFrame frame) {
        String name = Globals.menuTitle("Recent files");
        int i = name.indexOf('&');
        if (i >= 0) {
            setText(name.substring(0, i) + name.substring(i + 1));
            char mnemonic = Character.toUpperCase(name.charAt(i + 1));
            setMnemonic((int) mnemonic);
        } else
            setText(name);

        this.prefs = prefs;
        this.frame = frame;
        String[] old = prefs.getStringArray("recentFiles");
        if ((old != null) && (old.length > 0)) {
            for (i = 0; i < old.length; i++) {
                history.addFirst(old[i]);
            }
            setItems();
        } else
            setEnabled(false);
    }

    /**
     * Adds the file name to the top of the menu. If it already is in
     * the menu, it is merely moved to the top.
     *
     * @param filename a <code>String</code> value
     */
    public void newFile(String filename) {
        int i = 0;
        while (i < history.size()) {
            if (history.get(i).equals(filename))
                history.remove(i--);
            i++;
        }
        history.addFirst(filename);
        while (history.size() > prefs.getInt("historySize")) {
            history.removeLast();
        }
        setItems();
        if (!isEnabled())
            setEnabled(true);
    }

    private void setItems() {
        removeAll();
        Iterator<String> i = history.iterator();
        int count = 1;
        while (i.hasNext()) {
            addItem(i.next(), count++);
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
        int i=0;
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
        if (history.size() > 0) {
            String[] names = new String[history.size()];
            for (int i = 0; i < names.length; i++)
                names[i] = history.get(i);
            prefs.putStringArray("recentFiles", names);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String name = ((JMenuItem) e.getSource()).getText();
        int pos = name.indexOf(" ");
        name = name.substring(pos + 1);
        //Util.pr("'"+name+"'");
        final File fileToOpen = new File(name);

        if (!fileToOpen.exists()) {
            JOptionPane.showMessageDialog(frame, Globals.lang("File not found")+": "+fileToOpen.getName(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            removeItem(name);
            return;
        }
        (new Thread() {
            public void run() {
                frame.open.openIt(fileToOpen, true);
            }
        }).start();

    }


}
