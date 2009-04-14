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

// A wizard dialog for generating a new sub database from existing TeX aux file
//
// created by : r.nagel 23.08.2004
//
// modified : 18.04.2006 r.nagel
//            insert a "short info" section


package net.sf.jabref.wizard.auximport.gui ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.wizard.auximport.AuxSubGenerator;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class FromAuxDialog
    extends JDialog
{
  private JPanel statusPanel = new JPanel() ;
  private JPanel buttons = new JPanel() ;
  private JButton okButton = new JButton() ;
  private JButton cancelButton = new JButton() ;
  private JButton generateButton = new JButton() ;

  private JComboBox dbChooser = new JComboBox() ;
  private JTextField auxFileField ;
  private JButton browseAuxFileButton ;

  private JList notFoundList ;
  private JTextArea statusInfos ;

  // all open databases from JabRefFrame
  private JTabbedPane parentTabbedPane ;

  private boolean okPressed = false ;

  private AuxSubGenerator auxParser ;

  public FromAuxDialog( JabRefFrame frame, String title, boolean modal,
                        JTabbedPane viewedDBs )
  {
    super( frame, title, modal ) ;

    parentTabbedPane = viewedDBs ;

    auxParser = new AuxSubGenerator( null ) ;

    try
    {
      jbInit( frame ) ;
      pack() ;
    }
    catch ( Exception ex )
    {
      ex.printStackTrace() ;
    }
  }

  private void jbInit( JabRefFrame parent )
  {
    JPanel panel1 = new JPanel() ;

    panel1.setLayout( new BorderLayout() ) ;
    okButton.setText( Globals.lang( "Ok" ) ) ;
    okButton.setEnabled( false ) ;
    okButton.addActionListener( new FromAuxDialog_ok_actionAdapter( this ) ) ;
    cancelButton.setText( Globals.lang( "Cancel" ) ) ;
    cancelButton.addActionListener( new FromAuxDialog_Cancel_actionAdapter( this ) ) ;
    generateButton.setText( Globals.lang( "Generate" ) ) ;
    generateButton.addActionListener( new FromAuxDialog_generate_actionAdapter( this ) ) ;

    initOptionsPanel( parent ) ;

    initStatusPanel() ;

    // insert the buttons
    ButtonBarBuilder bb = new ButtonBarBuilder();
    JPanel buttonPanel = bb.getPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

    bb.addGlue();
    bb.addGridded( generateButton ) ;
    bb.addGridded( okButton ); 
    bb.addGridded( cancelButton );
    bb.addGlue();
    this.setModal( true ) ;
    this.setResizable( false ) ;
    this.setTitle( Globals.lang( "AUX file import" ) ) ;
    getContentPane().add( panel1 ) ;

    JPanel desc = getDescriptionPanel() ;
    // some help is available
    if (desc != null)
    {
      panel1.add( desc, BorderLayout.NORTH ) ;
      panel1.add( buttonPanel, BorderLayout.SOUTH ) ;

      JPanel centerPane = new JPanel( new BorderLayout() ) ;
      centerPane.add( buttons, BorderLayout.NORTH ) ;
      centerPane.add( statusPanel, BorderLayout.CENTER ) ;


      panel1.add( centerPane, BorderLayout.CENTER ) ;
    }
    else  // generate a view without the "short info" area
    {
      panel1.add( buttons, BorderLayout.NORTH ) ;
      panel1.add( buttonPanel, BorderLayout.SOUTH ) ;
      panel1.add( statusPanel, BorderLayout.CENTER ) ;
    }
    // Key bindings:
    ActionMap am = statusPanel.getActionMap() ;
    InputMap im = statusPanel.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ) ;
    im.put( parent.prefs().getKey( "Close dialog" ), "close" ) ;
    am.put( "close", new AbstractAction()
    {
      public void actionPerformed( ActionEvent e )
      {
        dispose() ;
      }
    } ) ;

  }

  private void initOptionsPanel( JabRefFrame parent )
  {
    // collect the names of all open databases
    int len = parentTabbedPane.getTabCount() ;
    for ( int t = 0 ; t < len ; t++ )
    {
      dbChooser.addItem( parentTabbedPane.getTitleAt( t ) ) ;
    }

    // panel view
    GridBagLayout gbl = new GridBagLayout() ;
    GridBagConstraints con = new GridBagConstraints() ;
    con.weightx = 0 ;
    con.insets = new Insets( 5, 10, 0, 10 ) ;
    con.fill = GridBagConstraints.HORIZONTAL ;

    TitledBorder border = new TitledBorder( BorderFactory.createLineBorder(
        new Color( 153, 153, 153 ), 2 ),
                                            Globals.lang( "Options" ) ) ;
    buttons.setBorder( border ) ;
    buttons.setLayout( gbl ) ;

    // Database
    JLabel lab1 = new JLabel( Globals.lang( "Reference database" ) + ":" ) ;
    lab1.setHorizontalAlignment( SwingConstants.LEFT ) ;
    gbl.setConstraints( lab1, con ) ;
    buttons.add( lab1 ) ;
    con.gridwidth = GridBagConstraints.REMAINDER ;
    gbl.setConstraints( dbChooser, con ) ;
    buttons.add( dbChooser ) ;

    // AUX
    con.gridwidth = 1 ;
    con.weightx = 0 ;
    con.insets = new Insets( 5, 10, 15, 10 ) ;
    con.fill = GridBagConstraints.BOTH ;
    lab1 = new JLabel( Globals.lang( "LaTeX AUX file" ) + ":" ) ;
    lab1.setHorizontalAlignment( SwingConstants.LEFT ) ;
    gbl.setConstraints( lab1, con ) ;
    buttons.add( lab1 ) ;
    con.weightx = 1 ;
    auxFileField = new JTextField( "", 25 ) ;
    gbl.setConstraints( auxFileField, con ) ;
    buttons.add( auxFileField ) ;
    con.weightx = 0 ;
    con.insets = new Insets( 5, 10, 15, 2 ) ;
    browseAuxFileButton = new JButton( Globals.lang( "Browse" ) ) ;
    browseAuxFileButton.addActionListener( new BrowseAction(
        auxFileField,
        parent ) ) ;
    gbl.setConstraints( browseAuxFileButton, con ) ;
    buttons.add( browseAuxFileButton ) ;

  }

  private void initStatusPanel()
  {
    GridBagLayout gbl = new GridBagLayout() ;
    GridBagConstraints con = new GridBagConstraints() ;
    con.weightx = 0 ;
    con.insets = new Insets( 5, 10, 0, 10 ) ;
    con.fill = GridBagConstraints.HORIZONTAL ;

    TitledBorder titledBorder1 = new TitledBorder(
        BorderFactory.createLineBorder(
            new Color( 153, 153, 153 ), 2 ),
        Globals.lang( "Results" ) ) ;

    statusPanel.setLayout( gbl ) ;
    statusPanel.setBorder( titledBorder1 ) ;

    JLabel lab1 = new JLabel( Globals.lang( "Unknown bibtex entries" ) + ":" ) ;
    lab1.setHorizontalAlignment( SwingConstants.LEFT ) ;
    con.gridwidth = 1 ;
    gbl.setConstraints( lab1, con ) ;
    statusPanel.add( lab1 ) ;

    lab1 = new JLabel( Globals.lang( "Messages" ) + ":" ) ;
    lab1.setHorizontalAlignment( SwingConstants.LEFT ) ;
    con.gridwidth = GridBagConstraints.REMAINDER ;
    gbl.setConstraints( lab1, con ) ;
    statusPanel.add( lab1 ) ;

    notFoundList = new JList() ;
    JScrollPane listScrollPane = new JScrollPane( notFoundList ) ;
    listScrollPane.setPreferredSize( new Dimension( 250, 120 ) ) ;
    con.gridwidth = 1 ;
    con.weightx = 0 ;
    con.gridheight = 2 ;
    con.insets = new Insets( 5, 10, 15, 10 ) ;
    con.fill = GridBagConstraints.BOTH ;
    gbl.setConstraints( listScrollPane, con ) ;
    statusPanel.add( listScrollPane ) ;

    statusInfos = new JTextArea( "", 5, 20 ) ;
    statusInfos.setBorder( BorderFactory.createEtchedBorder() ) ;
    statusInfos.setEditable( false ) ;
    con.gridheight = 1 ;
    gbl.setConstraints( statusInfos, con ) ;
    statusPanel.add( statusInfos ) ;

  }

// ---------------------------------------------------------------------------

  // returns a "short info" panel, if something is available
    private JPanel getDescriptionPanel() {
        JPanel back = null;

        JEditorPane infoText = null;

        URL infoURL = JabRef.class.getResource(GUIGlobals.getLocaleHelpPath() +
            GUIGlobals.shortAuxImport);
        if (infoURL != null) {
            try {
                infoText = new JEditorPane();
                infoText.setEditable(false);
                infoText.setPreferredSize(new Dimension(240, 50));
                infoText.setMinimumSize(new Dimension(180, 50));
                infoText.setPage(infoURL);
                infoText.setBackground(GUIGlobals.infoField);
                infoText.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

                // content
                back = new JPanel();
                back.setLayout(new BorderLayout());
                back.add(infoText, BorderLayout.PAGE_START);
            } catch (IOException e) {
            }
        }

        return back;
    }

// ---------------------------------------------------------------------------

  void ok_actionPerformed( ActionEvent e )
  {
    okPressed = true ;
    dispose() ;
  }

  void Cancel_actionPerformed( ActionEvent e )
  {
    dispose() ;
  }

  void generate_actionPerformed( ActionEvent e )
  {
    generateButton.setEnabled( false ) ;
    BasePanel bp = ( BasePanel ) parentTabbedPane.getComponentAt(
        dbChooser.getSelectedIndex() ) ;
    notFoundList.removeAll() ;
    statusInfos.setText( null ) ;
    BibtexDatabase refBase = bp.getDatabase() ;
    String auxName = auxFileField.getText() ;

    if ( auxName != null )
    {
      if ( ( refBase != null ) && ( auxName.length() > 0 ) )
      {
        auxParser.clear() ;
        notFoundList.setListData( auxParser.generate( auxName, refBase ) ) ;

        statusInfos.append( Globals.lang( "keys in database" ) + " " +
                            refBase.getEntryCount() ) ;
        statusInfos.append( "\n" + Globals.lang( "found in aux file" ) + " " +
                            auxParser.getFoundKeysInAux() ) ;
        statusInfos.append( "\n" + Globals.lang( "resolved" ) + " " +
                            auxParser.getResolvedKeysCount() ) ;
        statusInfos.append( "\n" + Globals.lang( "not found" ) + " " +
                            auxParser.getNotResolvedKeysCount() ) ;
        statusInfos.append( "\n" + Globals.lang( "crossreferenced entries included" ) + " " +
                            auxParser.getCrossreferencedEntriesCount() ) ;


        int nested = auxParser.getNestedAuxCounter() ;
        if ( nested > 0 )
        {
          statusInfos.append( "\n" + Globals.lang( "nested_aux_files" ) + " " +
                              nested ) ;
        }

        okButton.setEnabled( true ) ;
      }
    }

    // the generated database contains no entries -> no active ok-button
    if ( auxParser.getGeneratedDatabase().getEntryCount() < 1 )
    {
      statusInfos.append( "\n" + Globals.lang( "empty database" ) ) ;
      okButton.setEnabled( false ) ;
    }

    generateButton.setEnabled( true ) ;
  }

  public boolean okPressed()
  {
    return okPressed ;
  }

  public BibtexDatabase getGenerateDB()
  {
    return auxParser.getGeneratedDatabase() ;
  }

// ---------------------------------------------------------------------------

  /**
   * Action used to produce a "Browse" button for one of the text fields.
   */
  class BrowseAction
      extends AbstractAction
  {
    private JTextField comp ;
    private JabRefFrame _frame ;

    public BrowseAction( JTextField tc, JabRefFrame frame )
    {
      super( Globals.lang( "Browse" ) ) ;
      _frame = frame ;
      comp = tc ;
    }

    public void actionPerformed( ActionEvent e )
    {
      String chosen = null ;
      chosen = FileDialogs.getNewFile( _frame,
              new File( comp.getText() ),
                                   ".aux",
                                   JFileChooser.OPEN_DIALOG, false ) ;
      if ( chosen != null )
      {
        File newFile = new File( chosen ) ;
        comp.setText( newFile.getPath() ) ;
      }
    }
  }

}

// ----------- helper class -------------------
class FromAuxDialog_ok_actionAdapter
    implements java.awt.event.ActionListener
{
  FromAuxDialog adaptee ;

  FromAuxDialog_ok_actionAdapter( FromAuxDialog adaptee )
  {
    this.adaptee = adaptee ;
  }

  public void actionPerformed( ActionEvent e )
  {
    adaptee.ok_actionPerformed( e ) ;
  }
}

class FromAuxDialog_Cancel_actionAdapter
    implements java.awt.event.ActionListener
{
  FromAuxDialog adaptee ;

  FromAuxDialog_Cancel_actionAdapter( FromAuxDialog adaptee )
  {
    this.adaptee = adaptee ;
  }

  public void actionPerformed( ActionEvent e )
  {
    adaptee.Cancel_actionPerformed( e ) ;
  }
}

class FromAuxDialog_generate_actionAdapter
    implements java.awt.event.ActionListener
{
  FromAuxDialog adaptee ;

  FromAuxDialog_generate_actionAdapter( FromAuxDialog adaptee )
  {
    this.adaptee = adaptee ;
  }

  public void actionPerformed( ActionEvent e )
  {
    adaptee.generate_actionPerformed( e ) ;
  }
}
