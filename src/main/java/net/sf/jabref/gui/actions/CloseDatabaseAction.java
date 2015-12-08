package net.sf.jabref.gui.actions;

import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.exporter.SaveDatabaseAction;
import net.sf.jabref.gui.*;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class CloseDatabaseAction extends MnemonicAwareAction {
    private DragDropPopupPane frame;
    private CloseMode mode;

    public CloseDatabaseAction(DragDropPopupPane pane) {
        this.frame = pane;
        mode = CloseMode.CURRENT;
    }

    public CloseDatabaseAction(DragDropPopupPane pane, CloseMode mode) {
        this.frame = pane;
        this.mode = mode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final BasePanel active = (BasePanel) frame.getSelectedComponent();
        final Component[] panels = frame.getComponents();

        if(mode == CloseMode.CURRENT) {
            closeTab(active);
        } else if(mode == CloseMode.OTHER) {
            for(Component p : panels) {
                if(p != active) {
                    closeTab((BasePanel) p);
                }
            }
        } else {
            while((BasePanel) frame.getSelectedComponent() != null) {
                closeTab((BasePanel) frame.getSelectedComponent());
            }
        }

    }

    private void closeTab(BasePanel panel) {
        // TODO: this menu should be tab based not on the DragDropPopupPane
        // empty tab without database
        if (panel == null) {
            return;
        }

        if (panel.isModified()) {
            if(confirmClose(panel)) {
                close(panel);
            }
        } else {
            close(panel);
        }
    }

    // Ask if the user really wants to close, if the base has not been saved
    private boolean confirmClose(BasePanel panel) {
        boolean close = false;
        String filename;

        if (panel.getDatabaseFile() != null) {
            filename = panel.getDatabaseFile().getAbsolutePath();
        } else {
            filename = GUIGlobals.untitledTitle;
        }

        int answer = JabRefFrame.showSaveDialog(filename);
        if (answer == JOptionPane.YES_OPTION) {
            // The user wants to save.
            try {
                SaveDatabaseAction saveAction = new SaveDatabaseAction(panel);
                saveAction.runCommand();
                if (saveAction.isSuccess()) {
                    close = true;
                }
            } catch (Throwable ex) {
                // do not close
            }

        } else if(answer == JOptionPane.NO_OPTION) {
            // discard changes
            close = true;
        }
        return close;
    }

    private void close(BasePanel panel) {
        panel.cleanUp();
        AutoSaveManager.deleteAutoSaveFile(panel);
        frame.remove(panel);
        if (frame.getTabCount() > 0) {
            frame.markActiveBasePanel();
        }
        frame.setWindowTitle();
        frame.updateEnabledState(); // FIXME: Man, this is what I call a bug that this is not called.
        frame.output(Localization.lang("Closed database") + '.');
        // update tab titles
        frame.updateAllTabTitles();
    }

    public enum CloseMode {
        CURRENT,
        OTHER,
        ALL
    }
}
