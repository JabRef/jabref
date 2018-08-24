package org.jabref.gui.push;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class PushToApplicationSettingsDialog {
    public static void showSettingsDialog(JFrame parent, PushToApplicationSettings toApp, int n) {
        final JDialog diag = new JDialog(parent, Localization.lang("Settings"), true);
        JPanel options = toApp.getSettingsPanel(n);
        options.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(options, BorderLayout.CENTER);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        ok.addActionListener(e -> {
            // If the user pressed Ok, ask the PushToApplication implementation to store its settings:
            toApp.storeSettings();
            diag.dispose();
        });
        cancel.addActionListener(e -> diag.dispose());

        // Key bindings:
        ActionMap am = bb.getPanel().getActionMap();
        InputMap im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });
        diag.pack();
        diag.setLocationRelativeTo(parent);

        // Show the dialog:
        diag.setVisible(true);
    }
}
