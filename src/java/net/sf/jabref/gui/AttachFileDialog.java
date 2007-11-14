package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFilePanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: May 18, 2005
 * Time: 9:59:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class AttachFileDialog extends JDialog {

    AttachFileDialog ths = this;
    FieldEditor editor;
    String fieldName;
    JPanel main;
    JButton browse = new JButton(Globals.lang("Browse")),
        download = new JButton(Globals.lang("Download")),
        auto = new JButton(Globals.lang("Auto")),
        ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    BibtexEntry entry;
    MetaData metaData;
    private boolean cancelled = true; // Default to true, so a pure close operation implies Cancel.

    public AttachFileDialog(Frame parent, MetaData metaData, BibtexEntry entry, String fieldName) {
        super(parent, true);
        this.metaData = metaData;
        this.entry = entry;
        this.fieldName = fieldName;
        this.editor = new FieldTextField(fieldName, entry.getField(fieldName), false);

        initGui();
    }

    public AttachFileDialog(Dialog parent, MetaData metaData, BibtexEntry entry, String fieldName) {
        super(parent, true);
        this.metaData = metaData;
        this.entry = entry;
        this.fieldName = fieldName;
        this.editor = new FieldTextField(fieldName, entry.getField(fieldName), false);

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
                      editor, Util.getFileFilterForField(fieldName));

        browse.addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent event) {
                extPan.browseFile(fieldName, editor);
            }
        });

        download.addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent event) {
                extPan.downLoadFile(fieldName, editor, ths);
            }
        });

        auto.addActionListener(new ActionListener () {
                    public void actionPerformed(ActionEvent event) {
                        extPan.autoSetFile(fieldName, editor);
                    }
                });


        ActionListener okListener = new ActionListener () {
            public void actionPerformed(ActionEvent event) {
                cancelled = false;
                dispose();
            }
        };

        ok.addActionListener(okListener);
        ((JTextField)editor.getTextComponent()).addActionListener(okListener);

        AbstractAction cancelListener = new AbstractAction () {
            public void actionPerformed(ActionEvent event) {
                cancelled = true;
                dispose();
            }
        };

        cancel.addActionListener(cancelListener);
        editor.getTextComponent().getInputMap().put(Globals.prefs.getKey("Close dialog"), "close");
	    editor.getTextComponent().getActionMap().put("close", cancelListener);

        FormLayout layout = new FormLayout("fill:160dlu, 4dlu, fill:pref","");
	    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        //builder.append(Util.nCase(fieldName));//(editor.getLabel());
        builder.appendSeparator(Util.nCase(fieldName));
        builder.append(editor.getTextComponent());
        builder.append(browse);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGridded(download);
        bb.addGridded(auto);
        builder.nextLine();
        builder.append(bb.getPanel());
        builder.nextLine();
        builder.appendSeparator();

        main = builder.getPanel();

        main.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();


        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();
    }
}
