package org.jabref.gui;

import java.awt.Frame;

import javax.swing.JDialog;

import org.jabref.Globals;

public class JabRefDialog extends JDialog {

    public <T extends JabRefDialog> JabRefDialog(Frame owner, boolean modal, Class<T> clazz) {
        super(owner, modal);

        trackDialogOpening(clazz);
    }

    private <T extends JabRefDialog> void trackDialogOpening(Class<T> clazz) {
        Globals.getTelemetryClient().trackPageView(clazz.getName());
    }

    public <T  extends JabRefDialog> JabRefDialog(Frame owner, String title, boolean modal, Class<T> clazz) {
        super(owner, title, modal);

        trackDialogOpening(clazz);
    }
}
