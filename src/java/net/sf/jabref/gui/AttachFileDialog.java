package net.sf.jabref.gui;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFilePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;

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
    private boolean cancelled = true; // Default to true, so a pure close operation implies Cancel.

    public AttachFileDialog(Frame parent, BibtexEntry entry, String fieldName) {
        super(parent, true);
        this.entry = entry;
        this.fieldName = fieldName;
        this.editor = new FieldTextField(fieldName, (String)entry.getField(fieldName), false);

        initGui();
    }

    public AttachFileDialog(Dialog parent, BibtexEntry entry, String fieldName) {
        super(parent, true);
        this.entry = entry;
        this.fieldName = fieldName;
        this.editor = new FieldTextField(fieldName, (String)entry.getField(fieldName), false);

        initGui();
    }

    public boolean cancelled() {
        return cancelled;
    }

    public String getValue() {
        return editor.getText();
    }

    private void initGui() {

        final ExternalFilePanel extPan = new ExternalFilePanel(fieldName, entry);
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


        ok.addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent event) {
                cancelled = false;
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent event) {
                cancelled = true;
                dispose();
            }
        });

        FormLayout layout = new FormLayout("fill:pref, 4dlu, fill:160dlu","");
	    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(browse);
        builder.nextLine();
        builder.append(download);
        builder.nextLine();
        builder.append(auto);
        builder.nextLine();
        builder.append(Util.nCase(fieldName));//(editor.getLabel());
        builder.append(editor.getTextComponent());
        main = builder.getPanel();

        main.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGridded(ok);
        bb.addGridded(cancel);


        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();
    }
}
