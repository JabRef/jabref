package org.jabref.gui;

import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;

import org.jabref.Globals;

public class JabRefDialog extends JDialog {

    public <T extends JabRefDialog> JabRefDialog(Frame owner, boolean modal, Class<T> clazz) {
        super(owner, modal);

        trackDialogOpening(clazz);
    }

    public <T extends JabRefDialog> JabRefDialog(Class<T> clazz) {
        super();

        trackDialogOpening(clazz);
    }

    public <T extends JabRefDialog> JabRefDialog(Frame owner, Class<T> clazz) {
        super(owner);

        trackDialogOpening(clazz);
    }

    public <T  extends JabRefDialog> JabRefDialog(Frame owner, String title, Class<T> clazz) {
        this(owner, title, true, clazz);
    }

    public <T  extends JabRefDialog> JabRefDialog(Frame owner, String title, boolean modal, Class<T> clazz) {
        super(owner, title, modal);

        trackDialogOpening(clazz);
    }

    public <T  extends JabRefDialog> JabRefDialog(java.awt.Dialog owner, String title, Class<T> clazz) {
        this(owner, title, true, clazz);
    }

    public <T  extends JabRefDialog> JabRefDialog(java.awt.Dialog owner, String title, boolean modal, Class<T> clazz) {
        super(owner, title, modal);

        trackDialogOpening(clazz);
    }

    public <T extends JabRefDialog> JabRefDialog(Window owner, String title, Class<T> clazz) {
        super(owner, title);

        trackDialogOpening(clazz);
    }

    private <T extends JabRefDialog> void trackDialogOpening(Class<T> clazz) {
        Globals.getTelemetryClient().ifPresent(client -> client.trackPageView(clazz.getName()));
    }
}
