/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFilePanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.TextField;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.EntryUtil;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: May 18, 2005
 * Time: 9:59:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class AttachFileDialog extends JDialog {

    private final AttachFileDialog ths = this;
    private final FieldEditor editor;
    private final String fieldName;
    private final JButton browse = new JButton(Localization.lang("Browse"));
    private final JButton download = new JButton(Localization.lang("Download"));
    private final JButton auto = new JButton(Localization.lang("Auto"));
    private final JButton ok = new JButton(Localization.lang("Ok"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final BibtexEntry entry;
    private final MetaData metaData;
    private boolean cancelled = true; // Default to true, so a pure close operation implies Cancel.

    public AttachFileDialog(Dialog parent, MetaData metaData, BibtexEntry entry, String fieldName) {
        super(parent, true);
        this.metaData = metaData;
        this.entry = entry;
        this.fieldName = fieldName;
        this.editor = new TextField(fieldName, entry.getField(fieldName), false);

        initGui();
    }

    public boolean cancelled() {
        return cancelled;
    }

    public String getValue() {
        return editor.getText();
    }

    private void initGui() {

        final ExternalFilePanel extPan = new ExternalFilePanel(fieldName, metaData, entry,
                editor, net.sf.jabref.util.Util.getFileFilterForField(fieldName));

        browse.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                extPan.browseFile(fieldName, editor);
            }
        });

        download.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                extPan.downLoadFile(fieldName, editor, ths);
            }
        });

        auto.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                JabRefExecutorService.INSTANCE.execute(extPan.autoSetFile(fieldName, editor));
            }
        });

        ActionListener okListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                cancelled = false;
                dispose();
            }
        };

        ok.addActionListener(okListener);
        ((JTextField) editor.getTextComponent()).addActionListener(okListener);

        AbstractAction cancelListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                cancelled = true;
                dispose();
            }
        };

        cancel.addActionListener(cancelListener);
        editor.getTextComponent().getInputMap().put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        editor.getTextComponent().getActionMap().put("close", cancelListener);

        FormLayout layout = new FormLayout("fill:160dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        //builder.append(Util.capitalizeFirst(fieldName));//(editor.getLabel());
        builder.appendSeparator(EntryUtil.capitalizeFirst(fieldName));
        builder.append(editor.getTextComponent());
        builder.append(browse);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addButton(download);
        bb.addButton(auto);
        builder.nextLine();
        builder.append(bb.getPanel());
        builder.nextLine();
        builder.appendSeparator();

        JPanel main = builder.getPanel();

        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();
    }
}
