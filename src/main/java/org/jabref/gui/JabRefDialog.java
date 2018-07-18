package org.jabref.gui;

import java.awt.Frame;

import javax.swing.JDialog;

import org.jabref.Globals;

public class JabRefDialog extends JDialog {

    public <T extends JabRefDialog> JabRefDialog(boolean modal, Class<T> clazz) {
        this("JabRef", modal, clazz);
    }

    public <T extends JabRefDialog> JabRefDialog(Class<T> clazz) {
        this(true, clazz);
    }

    public <T extends JabRefDialog> JabRefDialog(String title, Class<T> clazz) {
        this(title, true, clazz);
    }

    public <T extends JabRefDialog> JabRefDialog(String title, boolean modal, Class<T> clazz) {
        super((Frame) null, title, modal);

        trackDialogOpening(clazz);
    }

    public <T  extends JabRefDialog> JabRefDialog(java.awt.Dialog owner, String title, Class<T> clazz) {
        this(owner, title, true, clazz);
    }

    public <T  extends JabRefDialog> JabRefDialog(java.awt.Dialog owner, String title, boolean modal, Class<T> clazz) {
        super(owner, title, modal);

        trackDialogOpening(clazz);
    }

    private <T extends JabRefDialog> void trackDialogOpening(Class<T> clazz) {
        Globals.getTelemetryClient().ifPresent(client -> client.trackPageView(clazz.getName()));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            // FIXME: Ugly hack to ensure that new dialogs are not hidden behind the main window
            setAlwaysOnTop(true);
            requestFocus();
            setAlwaysOnTop(false);
        }
    }
}
