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
package net.sf.jabref.oo;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog for adding citation with page number info.
 */
public class AdvancedCiteDialog {

    static boolean defaultInPar = true;
    boolean okPressed = false;
    JDialog diag;
    JRadioButton inPar = new JRadioButton(Globals.lang("Cite selected entries")),
        inText = new JRadioButton(Globals.lang("Cite selected entries with in-text citation"));
    JTextField pageInfo = new JTextField(15);
    JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));

    public AdvancedCiteDialog(JabRefFrame parent) {
        diag = new JDialog(parent, Globals.lang("Cite special"), true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(inPar);
        bg.add(inText);
        if (defaultInPar)
            inPar.setSelected(true);
        else
            inText.setSelected(true);
        
        inPar.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                defaultInPar = inPar.isSelected();
            }
        });
        
        DefaultFormBuilder b = new DefaultFormBuilder
                (new FormLayout("left:pref, 4dlu, fill:pref", ""));
        b.append(inPar, 3);
        b.nextLine();
        b.append(inText, 3);
        b.nextLine();
        b.append(Globals.lang("Extra information (e.g. page number)")+":");
        b.append(pageInfo);

        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.getContentPane().add(b.getPanel(), BorderLayout.CENTER);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        diag.pack();

        Action okAction = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = true;
                diag.dispose();
            }
        };
        ok.addActionListener(okAction);
        pageInfo.addActionListener(okAction);
        inPar.addActionListener(okAction);
        inText.addActionListener(okAction);
        Action cancelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = false;
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);
        b.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.prefs.getKey("Close dialog"), "close");
        b.getPanel().getActionMap().put("close", cancelAction);
        
    }

    public void showDialog() {
        diag.setLocationRelativeTo(diag.getParent());
        diag.setVisible(true);
    }

    public boolean cancelled() {
        return !okPressed;
    }

    public String getPageInfo() {
        return pageInfo.getText().trim();
    }

    public boolean isInParenthesisCite() {
        return inPar.isSelected();
    }
}
