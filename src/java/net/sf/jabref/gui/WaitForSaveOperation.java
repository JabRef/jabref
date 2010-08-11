package net.sf.jabref.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog shown when closing of application needs to wait for a save operation to finish.
 */
public class WaitForSaveOperation implements ActionListener {

    JabRefFrame frame;
    JDialog diag;
    JProgressBar prog;
    JButton cancel;
    Timer t = new Timer(500, this);
    boolean cancelled = false;

    public WaitForSaveOperation(JabRefFrame frame) {
        this.frame = frame;

        cancel = new JButton(Globals.lang("Cancel"));
        prog = new JProgressBar(0);
        prog.setIndeterminate(true);
        prog.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag = new JDialog(frame, Globals.lang("Please wait"), true);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(cancel);
        bb.addGlue();
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                cancelled = true;
                t.stop();
                diag.dispose();
            }
        });

        JLabel message = new JLabel(Globals.lang("Waiting for save operation to finish")+"...");
        message.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
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

    public boolean cancelled() {
        return cancelled;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        boolean anySaving = false;
        for (int i=0; i<frame.baseCount(); i++) {
            if (frame.baseAt(i).isSaving()) {
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
