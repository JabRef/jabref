package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import org.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Dialog shown when closing of application needs to wait for a save operation to finish.
 */
public class WaitForSaveOperation implements ActionListener {

    private final JabRefFrame frame;
    private final JDialog diag;
    private final Timer t = new Timer(500, this);
    private boolean canceled;


    public WaitForSaveOperation(JabRefFrame frame) {
        this.frame = frame;

        JButton cancel = new JButton(Localization.lang("Cancel"));
        JProgressBar prog = new JProgressBar(0);
        prog.setIndeterminate(true);
        prog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag = new JDialog(frame, Localization.lang("Please wait..."), true);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(cancel);
        bb.addGlue();
        cancel.addActionListener(e -> {
            canceled = true;
            t.stop();
            diag.dispose();
        });

        JLabel message = new JLabel(Localization.lang("Waiting for save operation to finish") + "...");
        message.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        diag.getContentPane().add(message, BorderLayout.NORTH);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.getContentPane().add(prog, BorderLayout.CENTER);
        diag.pack();
    }

    public void show() {
        diag.setLocationRelativeTo(frame);
        t.start();
        diag.setVisible(true);

    }

    public boolean canceled() {
        return canceled;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        boolean anySaving = false;
        for (BasePanel basePanel : frame.getBasePanelList()) {
            if (basePanel.isSaving()) {
                anySaving = true;
                break;
            }
        }
        if (!anySaving) {
            t.stop();
            diag.dispose();
        }
    }
}
