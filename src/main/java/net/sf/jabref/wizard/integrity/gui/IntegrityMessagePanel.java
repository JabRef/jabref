/*
Copyright (C) 2004 R. Nagel

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

// created by : r.nagel 09.12.2004
//
// function : shows the IntegrityMessages produced by IntegrityCheck
//
//     todo : several entries not supported
//
// modified :

package net.sf.jabref.wizard.integrity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.wizard.integrity.IntegrityCheck;
import net.sf.jabref.wizard.integrity.IntegrityMessage;
import net.sf.jabref.wizard.text.gui.HintListModel;

public class IntegrityMessagePanel
        extends JPanel
        implements ListSelectionListener, KeyListener, ActionListener

{

    private final JList warnings;
    private final HintListModel warningData;

    private final IntegrityCheck validChecker;

    private final JTextField content;
    private final JButton applyButton;
    private final BasePanel basePanel;


    public IntegrityMessagePanel(BasePanel basePanel)
    {
        this.basePanel = basePanel;
        validChecker = new IntegrityCheck(); // errors, warnings, hints

        // JList --------------------------------------------------------------
        warningData = new HintListModel();
        warnings = new JList(warningData);
        warnings.setCellRenderer(new IntegrityListRenderer());
        warnings.addListSelectionListener(this);

        JScrollPane paneScrollPane = new JScrollPane(warnings);
        paneScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        paneScrollPane.setPreferredSize(new Dimension(540, 255));
        paneScrollPane.setMinimumSize(new Dimension(10, 10));

        // Fix Panel ---------------------------------------------------------
        JPanel fixPanel = new JPanel();
        //    BoxLayout box = new BoxLayout(fixPanel, BoxLayout.LINE_AXIS) ;

        JLabel label1 = new JLabel(Localization.lang("Field_content"));

        content = new JTextField(40);
        content.addKeyListener(this);
        applyButton = new JButton(Localization.lang("Apply"));
        applyButton.addActionListener(this);
        applyButton.setEnabled(false);
        JButton fixButton = new JButton(Localization.lang("Suggest"));
        fixButton.setEnabled(false);

        fixPanel.add(label1);
        fixPanel.add(content);
        fixPanel.add(applyButton);
        fixPanel.add(fixButton);

        // Main Panel --------------------------------------------------------
        this.setLayout(new BorderLayout());
        this.add(paneScrollPane, BorderLayout.CENTER);
        this.add(fixPanel, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------------

    public void updateView(BibtexEntry entry)
    {
        warningData.clear();
        IntegrityMessage.setPrintMode(IntegrityMessage.SINLGE_MODE);
        warningData.setData(validChecker.checkBibtexEntry(entry));
    }

    public void updateView(BibtexDatabase base)
    {
        warningData.clear();
        IntegrityMessage.setPrintMode(IntegrityMessage.FULL_MODE);
        warningData.setData(validChecker.checkBibtexDatabase(base));
    }

    // ------------------------------------------------------------------------
    //This method is required by ListSelectionListener.
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting())
        {
            Object obj = warnings.getSelectedValue();
            String str = "";
            if (obj != null)
            {
                IntegrityMessage msg = (IntegrityMessage) obj;
                BibtexEntry entry = msg.getEntry();

                if (entry != null)
                {
                    str = entry.getField(msg.getFieldName());
                    basePanel.highlightEntry(entry);
                    // make the "invalid" field visible  ....
                    //          EntryEditor editor = basePanel.getCurrentEditor() ;
                    //          editor.
                }
            }
            content.setText(str);
            applyButton.setEnabled(false);
        }
    }

    // --------------------------------------------------------------------------
    // This methods are required by KeyListener
    @Override
    public void keyPressed(KeyEvent e)
    {
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        applyButton.setEnabled(true);
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            applyButton.doClick();
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object obj = e.getSource();
        if (obj == applyButton)
        {
            Object data = warnings.getSelectedValue();
            if (data != null)
            {
                IntegrityMessage msg = (IntegrityMessage) data;
                BibtexEntry entry = msg.getEntry();

                if (entry != null)
                {
                    //          System.out.println("update") ;
                    String oldContent = entry.getField(msg.getFieldName());
                    UndoableFieldChange edit = new UndoableFieldChange(entry, msg.getFieldName(), oldContent,
                            content.getText());
                    entry.setField(msg.getFieldName(), content.getText());
                    basePanel.undoManager.addEdit(edit);
                    basePanel.markBaseChanged();
                    msg.setFixed(true);
                    //          updateView(entry) ;
                    warningData.valueUpdated(warnings.getSelectedIndex());
                }
            }

            applyButton.setEnabled(false);
        }
    }


    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
    class IntegrityListRenderer extends DefaultListCellRenderer
    {

        final ImageIcon warnIcon = IconTheme.getImage("integrityWarn");
        final ImageIcon infoIcon = IconTheme.getImage("integrityInfo");
        final ImageIcon failIcon = IconTheme.getImage("integrityFail");
        final ImageIcon fixedIcon = IconTheme.getImage("complete");


        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value, // value to display
                int index, // cell index
                boolean iss, // is the cell selected
                boolean chf) // the list and the cell have the focus
        {
            super.getListCellRendererComponent(list, value, index, iss, chf);

            if (value != null)
            {
                IntegrityMessage msg = (IntegrityMessage) value;
                if (msg.getFixed())
                {
                    setIcon(fixedIcon);
                }
                else
                {
                    int id = msg.getType();
                    if (id < 1000) {
                        setIcon(infoIcon);
                    } else if (id < 2000) {
                        setIcon(warnIcon);
                    } else {
                        setIcon(failIcon);
                    }
                }
            }
            return this;
        }
    }

}
