/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog shown when closing of application needs to wait for a save operation to finish.
 */
public class WaitForSaveOperation implements ActionListener {

    private final JabRefFrame frame;
    private final JDialog diag;
    private final Timer t = new Timer(500, this);
    private boolean cancelled;


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
            cancelled = true;
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

    public boolean cancelled() {
        return cancelled;
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
