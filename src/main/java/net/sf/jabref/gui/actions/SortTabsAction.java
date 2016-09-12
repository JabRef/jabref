package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Action;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This action rearranges all tabs in the main tabbed pane of the given JabRefFrame
 * in alphabetical order.
 */
public class SortTabsAction extends MnemonicAwareAction implements Comparator<String> {
    private final JabRefFrame frame;

    public SortTabsAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Sort tabs"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Rearrange tabs alphabetically by title"));
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Make a sorted Map that compares case-insensitively:
        Map<String, BasePanel> map = new TreeMap<>(this);

        for (BasePanel panel : frame.getBasePanelList()) {
            map.put(panel.getTabTitle(), panel);
        }

        frame.getTabbedPane().removeAll();
        for (Map.Entry<String, BasePanel> entry : map.entrySet()) {
            frame.addTab(entry.getValue(), false);
        }
    }

    @Override
    public int compare(String o1, String o2) {
        return o1.toLowerCase().compareTo(o2.toLowerCase());
    }
}
