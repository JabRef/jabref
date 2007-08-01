package net.sf.jabref.gui;

import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JTabbedPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;

/**
 * This action rearranges all tabs in the main tabbed pane of the given JabRefFrame
 * in alphabetical order.
 */
public class SortTabsAction extends MnemonicAwareAction implements Comparator {
    private JabRefFrame frame;

    public SortTabsAction(JabRefFrame frame) {
        putValue(NAME, "Sort tabs");
        putValue(SHORT_DESCRIPTION, Globals.lang("Rearrange tabs alphabetically by title"));
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        JTabbedPane tabbedPane = frame.getTabbedPane();
       // Make a sorted Map that compares case-insensitively:
        TreeMap map = new TreeMap(this);

        for (int i=0; i<tabbedPane.getTabCount(); i++) {
            BasePanel panel = (BasePanel)tabbedPane.getComponent(i);
            map.put(tabbedPane.getTitleAt(i), panel);
        }
        tabbedPane.removeAll();
        for (Iterator i=map.keySet().iterator(); i.hasNext();) {
            String title = (String)i.next();
            BasePanel panel = (BasePanel)map.get(title);
            tabbedPane.addTab(title, panel);
        }
    }

    public int compare(Object o1, Object o2) {
        return ((String)o1).toLowerCase().compareTo(((String)o2).toLowerCase());
    }
}
