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

package net.sf.jabref.wizard.integrity.gui ;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.*;
import net.sf.jabref.undo.UndoableFieldChange;
import net.sf.jabref.wizard.integrity.IntegrityCheck;
import net.sf.jabref.wizard.integrity.IntegrityMessage;
import net.sf.jabref.wizard.text.gui.HintListModel;

public class IntegrityMessagePanel
    extends JPanel
    implements ListSelectionListener, KeyListener, ActionListener

{
  private JList warnings ;
  private HintListModel warningData ;

  private IntegrityCheck validChecker ;

  private JTextField content  ;
  private JButton applyButton ;
  private JButton fixButton ;
  private BasePanel basePanel;

  public IntegrityMessagePanel(BasePanel basePanel)
  {
    this.basePanel = basePanel;
    validChecker = new IntegrityCheck() ; // errors, warnings, hints

  // JList --------------------------------------------------------------
    warningData = new HintListModel() ;
    warnings = new JList( warningData ) ;
    warnings.setCellRenderer( new IntegrityListRenderer() );
    warnings.addListSelectionListener(this);

    JScrollPane paneScrollPane = new JScrollPane( warnings ) ;
    paneScrollPane.setVerticalScrollBarPolicy(
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) ;
    paneScrollPane.setPreferredSize( new Dimension( 540, 255 ) ) ;
    paneScrollPane.setMinimumSize( new Dimension( 10, 10 ) ) ;

  // Fix Panel ---------------------------------------------------------
    JPanel fixPanel = new JPanel() ;
//    BoxLayout box = new BoxLayout(fixPanel, BoxLayout.LINE_AXIS) ;

    JLabel label1 = new JLabel(Globals.lang("Field_content")) ;

    content = new JTextField(40) ;
    content.addKeyListener(this);
    applyButton = new JButton(Globals.lang("Apply")) ;
    applyButton.addActionListener(this) ;
    applyButton.setEnabled(false);
    fixButton = new JButton(Globals.lang("Suggest")) ;
    fixButton.setEnabled(false);

    fixPanel.add(label1) ;
    fixPanel.add(content) ;
    fixPanel.add(applyButton) ;
    fixPanel.add(fixButton) ;

  // Main Panel --------------------------------------------------------
    this.setLayout( new BorderLayout() );
    this.add( paneScrollPane, BorderLayout.CENTER ) ;
    this.add( fixPanel, BorderLayout.SOUTH) ;
  }

  // ------------------------------------------------------------------------

  public void updateView( BibtexEntry entry )
  {
    warningData.clear();
    IntegrityMessage.setPrintMode( IntegrityMessage.SINLGE_MODE) ;
    warningData.setData( validChecker.checkBibtexEntry( entry ) ) ;
  }

  public void updateView( BibtexDatabase base )
  {
    warningData.clear();
    IntegrityMessage.setPrintMode( IntegrityMessage.FULL_MODE) ;
    warningData.setData( validChecker.checkBibtexDatabase( base ) ) ;
  }


  // ------------------------------------------------------------------------
  //This method is required by ListSelectionListener.
  public void valueChanged( ListSelectionEvent e )
  {
    if ( e.getValueIsAdjusting() )
    {
      Object obj = warnings.getSelectedValue() ;
      String str = "" ;
      if (obj != null)
      {
        IntegrityMessage msg = (IntegrityMessage) obj ;
        BibtexEntry entry = msg.getEntry() ;

        if (entry != null)
        {
          str = entry.getField(msg.getFieldName()) ;
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
  public void keyPressed( KeyEvent e )
  {
  }

  public void keyReleased( KeyEvent e )
  {
    applyButton.setEnabled(true);
    if (e.getKeyCode() == KeyEvent.VK_ENTER)
    {
      applyButton.doClick();
    }
  }

  public void keyTyped( KeyEvent e )
  {
  }

  public void actionPerformed( ActionEvent e )
  {
    Object obj = e.getSource() ;
    if (obj == applyButton)
    {
      Object data = warnings.getSelectedValue() ;
      if (data != null)
      {
        IntegrityMessage msg = (IntegrityMessage) data ;
        BibtexEntry entry = msg.getEntry() ;

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
          warningData.valueUpdated(warnings.getSelectedIndex()) ;
        }
      }

      applyButton.setEnabled(false);
    }
  }
  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------
  class IntegrityListRenderer extends DefaultListCellRenderer
  {
    final ImageIcon warnIcon = GUIGlobals.getImage("integrityWarn");
    final ImageIcon infoIcon = GUIGlobals.getImage("integrityInfo");
    final ImageIcon failIcon = GUIGlobals.getImage("integrityFail");
    final ImageIcon fixedIcon = GUIGlobals.getImage("complete");

    public Component getListCellRendererComponent(
        JList list,
        Object value, // value to display
        int index, // cell index
        boolean iss, // is the cell selected
        boolean chf ) // the list and the cell have the focus
    {
      super.getListCellRendererComponent( list, value, index, iss, chf ) ;

      if (value != null)
      {
        IntegrityMessage msg = (IntegrityMessage) value ;
        if (msg.getFixed())
        {
          setIcon(fixedIcon) ;
        }
        else
        {
          int id = msg.getType() ;
          if ( id < 1000 )
            setIcon( infoIcon ) ;
          else if ( id < 2000 )
            setIcon( warnIcon ) ;
          else setIcon( failIcon ) ;
        }
      }
      return this ;
    }
  }

}
