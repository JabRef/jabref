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

// created by : r.nagel 14.09.2004
//
// function : import from plain text => simple mark/copy/paste into bibtex entry
//
// todo     : - change colors and fonts
//            - delete selected text
//            - make textarea editable
//            - create several bibtex entries in dialog
//            - if the dialog works with an existing entry (right click menu item)
//              the cancel option doesn't work well
//
// modified :
//            28.07.2005
//            - fix: insert button doesnt work
//            - append a author with "and"
//            04.11.2004
//            - experimental: text-input-area with underlying infotext
//            02.11.2004
//            - integrity check, which reports errors and warnings for the fields
//            22.10.2004
//            - little help box
//

package net.sf.jabref.wizard.text.gui ;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.wizard.integrity.gui.IntegrityMessagePanel;
import net.sf.jabref.wizard.text.TagToMarkedTextStore;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class TextInputDialog
    extends JDialog implements ActionListener
{
  private JButton okButton = new JButton() ;
  private JButton cancelButton = new JButton() ;
  private JButton insertButton = new JButton() ;
  private JPanel panel1 = new JPanel() ;
  private JPanel buttons = new JPanel() ;
  private JPanel rawPanel = new JPanel() ;
  private JPanel sourcePanel = new JPanel() ;
  private IntegrityMessagePanel warnPanel;
  private JList fieldList ;
  private JRadioButton overRadio, appRadio ;

  private OverlayPanel testPanel ;

  private BibtexEntry entry ;

  public JPopupMenu inputMenu = new JPopupMenu() ;
  private StyledDocument doc ; // content from inputPane
  private JTextPane textPane ;
  private JTextArea preview ;

  private boolean inputChanged ; // input changed, fired by insert buttons

  private TagToMarkedTextStore marked ;

  private JabRefFrame _frame ;

  private boolean okPressed = false ;

  public TextInputDialog( JabRefFrame frame, BasePanel panel, String title, boolean modal,
                          BibtexEntry bibEntry )
  {
    super( frame, title, modal ) ;

    warnPanel = new IntegrityMessagePanel(panel);
    inputChanged = true ;  // for a first validCheck


    _frame = frame ;

    entry = bibEntry ;
    marked = new TagToMarkedTextStore() ;

    try
    {
      jbInit( frame ) ;
      pack() ;
    }
    catch ( Exception ex )
    {
      ex.printStackTrace() ;
    }

    updateSourceView() ;
  }

  private void jbInit( JabRefFrame parent ) {
    this.setModal( true ) ;
    //this.setResizable( false ) ;
    getContentPane().setLayout(new BorderLayout());
    String typeStr = Globals.lang( "for" ) ;
    if ( entry != null )
    {
      if ( entry.getType() != null )
      {
        typeStr = typeStr + " " + entry.getType().getName() ;
      }
    }

    this.setTitle( Globals.lang( "Plain_text_import" ) + " " + typeStr ) ;
    getContentPane().add( panel1, BorderLayout.CENTER ) ;

    initRawPanel() ;
    initButtonPanel() ;
    initSourcePanel() ;

    JTabbedPane tabbed = new JTabbedPane() ;
    tabbed.addChangeListener(
        new ChangeListener()
    {
      public void stateChanged( ChangeEvent e )
      {
        if ( inputChanged )
        {
          warnPanel.updateView( entry ) ;
        }
      }
    } ) ;

    tabbed.add( rawPanel, Globals.lang( "Raw_source" ) ) ;
    tabbed.add( sourcePanel, Globals.lang( "BibTeX_source" ) ) ;
    tabbed.add( warnPanel, Globals.lang( "Messages_and_Hints" ) ) ;

    // Panel Layout
    panel1.setLayout( new BorderLayout() ) ;
    panel1.add( tabbed, BorderLayout.CENTER ) ;
    panel1.add( buttons, BorderLayout.SOUTH ) ;

    // Key bindings:
    ActionMap am = buttons.getActionMap() ;
    InputMap im = buttons.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ) ;
    im.put( parent.prefs().getKey( "Close dialog" ), "close" ) ;
    am.put( "close", new AbstractAction()
    {
      public void actionPerformed( ActionEvent e )
      {
        dispose() ;
      }
    } ) ;
  }

// ---------------------------------------------------------------------------
  // Panel with text import functionality
  private void initRawPanel()
  {

    rawPanel.setLayout( new BorderLayout() ) ;

        // Textarea
        textPane = new JTextPane() ;

        textPane.setEditable( false ) ;

        doc = textPane.getStyledDocument() ;
        addStylesToDocument( doc ) ;

        try
        {
          doc.insertString( 0, "", doc.getStyle( "regular" ) ) ;
        }
        catch ( Exception e )
        {}

        testPanel = new OverlayPanel(textPane,
                                     Globals.lang("Text_Input_Area") ) ;

        testPanel.setPreferredSize( new Dimension( 450, 255) );
        testPanel.setMaximumSize( new Dimension(450, Integer.MAX_VALUE) );

    // copy/paste Menu
    PasteAction pasteAction = new PasteAction() ;
    JMenuItem pasteMI = new JMenuItem( pasteAction ) ;
    inputMenu.add( new MenuHeaderAction() ) ;
    inputMenu.addSeparator() ;
    inputMenu.add( pasteMI ) ;

    //Add listener to components that can bring up popup menus.
    MouseListener popupListener = new PopupListener( inputMenu ) ;
    textPane.addMouseListener( popupListener );
    testPanel.addMouseListener( popupListener );

    // Toolbar
    JToolBar toolBar = new JToolBar() ;
    toolBar.add( new ClearAction() ) ;
    toolBar.setBorderPainted( false ) ;
    toolBar.addSeparator() ;
    toolBar.add( pasteAction ) ;
    toolBar.add( new LoadAction() ) ;

    JPanel leftPanel = new JPanel( new BorderLayout() ) ;

    leftPanel.add( toolBar, BorderLayout.NORTH ) ;
    leftPanel.add( testPanel, BorderLayout.CENTER ) ;


    // ----------------------------------------------------------------
    JPanel inputPanel = new JPanel() ;

    // Panel Layout
    GridBagLayout gbl = new GridBagLayout() ;
    GridBagConstraints con = new GridBagConstraints() ;
    con.weightx = 0 ;
    con.insets = new Insets( 5, 5, 0, 5 ) ;
    con.fill = GridBagConstraints.HORIZONTAL ;

    inputPanel.setLayout( gbl ) ;

    // Border
    TitledBorder titledBorder1 = new TitledBorder(
        BorderFactory.createLineBorder(
            new Color( 153, 153, 153 ), 2 ),
        Globals.lang( "Input" ) ) ;
    inputPanel.setBorder( titledBorder1 ) ;
    //inputPanel.setPreferredSize( new Dimension( 200, 255 ) ) ;
    inputPanel.setMinimumSize( new Dimension( 10, 10 ) ) ;

    fieldList = new JList( getAllFields() ) ;
    fieldList.setCellRenderer( new SimpleCellRenderer( fieldList.getFont() ) ) ;
    ListSelectionModel listSelectionModel = fieldList.getSelectionModel() ;
    listSelectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION ) ;
    listSelectionModel.addListSelectionListener( new FieldListSelectionHandler() ) ;
    fieldList.addMouseListener( new FieldListMouseListener() ) ;

    JScrollPane fieldScroller = new JScrollPane( fieldList ) ;
    fieldScroller.setVerticalScrollBarPolicy(
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED ) ;
    //fieldScroller.setPreferredSize( new Dimension( 180, 190 ) ) ;
    //fieldScroller.setMinimumSize( new Dimension( 180, 190 ) ) ;

    // insert buttons
    insertButton.setText( Globals.lang( "Insert" ) ) ;
    insertButton.addActionListener( this ) ;

    // Radio buttons
    appRadio = new JRadioButton( Globals.lang( "Append" ) ) ;
    appRadio.setToolTipText( Globals.lang( "append_the_selected_text_to_bibtex_key") ) ;
    appRadio.setMnemonic( KeyEvent.VK_A ) ;
    appRadio.setSelected( true ) ;

    overRadio = new JRadioButton( Globals.lang( "Override" ) ) ;
    overRadio.setToolTipText( Globals.lang( "override_the_bibtex_key_by_the_selected_text") ) ;
    overRadio.setMnemonic( KeyEvent.VK_O ) ;
    overRadio.setSelected( false ) ;

    //Group the radio buttons.
    ButtonGroup group = new ButtonGroup() ;
    group.add( appRadio ) ;
    group.add( overRadio ) ;

    JPanel radioPanel = new JPanel( new GridLayout( 0, 1 ) ) ;
    radioPanel.add( appRadio ) ;
    radioPanel.add( overRadio ) ;

    // insert sub components
    JLabel label1 = new JLabel( Globals.lang( "Available fields" ) ) ;
    con.gridwidth = GridBagConstraints.REMAINDER ;
    gbl.setConstraints( label1, con ) ;
    inputPanel.add( label1 ) ;

    con.gridwidth = GridBagConstraints.REMAINDER ;
    con.gridheight = 8 ;
    con.weighty = 1;
    con.fill = GridBagConstraints.BOTH;
    gbl.setConstraints( fieldScroller, con ) ;
    inputPanel.add( fieldScroller ) ;

    con.fill = GridBagConstraints.HORIZONTAL ;
    con.weighty = 0;
    con.gridwidth = 2 ;
    gbl.setConstraints( radioPanel, con ) ;
    inputPanel.add( radioPanel ) ;

    con.gridwidth = GridBagConstraints.REMAINDER ;
    gbl.setConstraints( insertButton, con ) ;
    inputPanel.add( insertButton ) ;

    // ----------------------------------------------------------------------
    rawPanel.add( leftPanel, BorderLayout.CENTER ) ;
    rawPanel.add( inputPanel, BorderLayout.EAST ) ;

    // ----------------------------------------------------------------------
    // add a short info, if available
    // load the info text from a help-file, the "normal" translation is
    // to long and unpractical for the properties file (single line)
    // => try to load the help file text, if it fails, show a short text

    //infoText.setText(Globals.lang("This_is_a_simple_copy_and_paste_dialog._First_load_or_paste_some_"
    //        +"text_into_the_text_input_area._After_that,_you_can_mark_text_and_assign_it_to_a_bibtex_field."));

    JEditorPane infoText =  new JEditorPane() ;

    boolean loaded = false ;

    URL infoURL = JabRef.class.getResource(GUIGlobals.getLocaleHelpPath()
                                           +GUIGlobals.shortPlainImport);

    if (infoURL != null)
    {
      try
      {
         // get the info text from help file
        infoText.setPage( infoURL ) ;
        //infoText.setContentType("text/html");

        loaded = true ; // text successfully loaded
      }
      catch (Exception e) {}
    }

    if (!loaded) // only if no help available
    {
      infoText.setText(
         Globals.lang("This_is_a_simple_copy_and_paste_dialog_for_import_some_fields_from_normal_text.") ) ;
    }

    infoText.setEditable(false);
    infoText.setBackground(GUIGlobals.infoField);
    infoText.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    infoText.setPreferredSize( new Dimension(220, 50));
    infoText.setMinimumSize( new Dimension(180, 50));

    rawPanel.add( infoText, BorderLayout.SOUTH ) ;
  }

// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------

  private void initButtonPanel()
  {

    okButton.setText( Globals.lang( "Accept" ) ) ;
    okButton.addActionListener( this ) ;
    cancelButton.setText( Globals.lang( "Cancel" ) ) ;
    cancelButton.addActionListener( this ) ;

    ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
    buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    bb.addGlue();
    bb.addGridded(okButton);
    bb.addGridded(cancelButton);
    bb.addGlue();

  }

// ---------------------------------------------------------------------------

  // Panel with bibtex source code
  private void initSourcePanel()
  {
//    preview =  new PreviewPanel(entry) ;
    preview = new JTextArea() ;
    preview.setEditable( false ) ;

    JScrollPane paneScrollPane = new JScrollPane( preview ) ;
    paneScrollPane.setVerticalScrollBarPolicy(
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) ;
    paneScrollPane.setPreferredSize( new Dimension( 500, 255 ) ) ;
    paneScrollPane.setMinimumSize( new Dimension( 10, 10 ) ) ;

    sourcePanel.setLayout( new BorderLayout() ) ;
    sourcePanel.add( paneScrollPane, BorderLayout.CENTER ) ;
  }

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
  protected void addStylesToDocument( StyledDocument doc )
  {
    //Initialize some styles.
    Style def = StyleContext.getDefaultStyleContext().
        getStyle( StyleContext.DEFAULT_STYLE ) ;

    Style regular = doc.addStyle( "regular", def ) ;
    StyleConstants.setFontFamily( def, "SansSerif" ) ;
    StyleConstants.setFontSize( def, 12 ) ;

    Style s = doc.addStyle( "oldused", regular ) ;
    StyleConstants.setItalic( s, true ) ;
    StyleConstants.setForeground( s, Color.blue ) ;

    s = doc.addStyle( "used", regular ) ;
    StyleConstants.setBold( s, true ) ;
    StyleConstants.setForeground( s, Color.blue ) ;

    s = doc.addStyle( "marked", regular ) ;
    StyleConstants.setBold( s, true ) ;
    StyleConstants.setForeground( s, Color.red ) ;

    s = doc.addStyle( "small", regular ) ;
    StyleConstants.setFontSize( s, 10 ) ;

    s = doc.addStyle( "large", regular ) ;
    StyleConstants.setFontSize( s, 16 ) ;
  }

// ---------------------------------------------------------------------------
  private void insertTextForTag()
  {
    String type = ( String ) fieldList.getSelectedValue() ;
    if ( type != null )
    {
      String txt = textPane.getSelectedText() ;

      if ( txt != null )
      {
        int selStart = textPane.getSelectionStart() ;
        int selEnd = textPane.getSelectionEnd() ;

        // unselect text
        textPane.setSelectionEnd( selStart ) ;

        // mark the selected text as "used"
        doc.setCharacterAttributes( selStart, selEnd - selStart,
                                    doc.getStyle( "marked" ), true ) ;

        // override an existing entry
        if ( overRadio.isSelected() )
        {
          entry.setField( type, txt ) ;
          // erase old text selection
          marked.setStyleForTag( type, "regular", doc ) ; // delete all previous styles
          marked.insertPosition( type, selStart, selEnd ) ; // insert new selection style
        }
        else // append text
        {
          // memorize the selection for text highlighting
          marked.appendPosition( type, selStart, selEnd ) ;

          // get old text from bibtex tag
          String old = entry.getField( type ) ;

          // merge old and selected text
          if ( old != null )
          {
            // insert a new author with an additional "and"
            if (type.hashCode() == "author".hashCode())
            {
              entry.setField(type, old +" and " +txt);
            }
            else entry.setField( type, old + txt ) ;
          }
          else // "null"+"txt" Strings forbidden
          {
            entry.setField( type, txt ) ;
          }
        }
        // make the new data in bibtex source code visible
        updateSourceView() ;
      }
    }
  }

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
  public boolean okPressed()
  {
    return okPressed ;
  }

// ---------------------------------------------------------------------------

//  ActionListener
//  handling of buttons-click actions
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource() ;

    if (source == this.okButton)
    {
      okPressed = true ;
      dispose() ;
    }
    else if (source == this.cancelButton)
    {
      dispose() ;
    }
    else if (source == this.insertButton)
    {
      insertTextForTag() ;
    }
  }

// ---------------------------------------------------------------------------
  // update the bibtex source view and available List
  private final void updateSourceView()
  {
    StringWriter sw = new StringWriter( 200 ) ;
    try
    {
      entry.write( sw, new net.sf.jabref.export.LatexFieldFormatter(), false ) ;
      String srcString = sw.getBuffer().toString() ;
      preview.setText( srcString ) ;
    }
    catch ( IOException ex )
    {}

    fieldList.clearSelection() ;
  }

// ---------------------------------------------------------------------------
  private final String[] getAllFields()
  {
    int len = 0 ;
    String dummy[][] = new String[3][] ;

    // fill
    if ( entry != null )
    {
      dummy[0] = entry.getRequiredFields() ;
      dummy[1] = entry.getGeneralFields() ;
      dummy[2] = entry.getOptionalFields() ;
    }

    // get size of new result array
    for ( int t = 0 ; t < 3 ; t++ )
    {
      if ( dummy[t] != null )
      {
        len = len + dummy[t].length ;
      }
    }

    String back[] = new String[len] ;
    int count = 0 ;

    // put
    for ( int t = 0 ; t < 3 ; t++ )
    {
      if ( dummy[t] != null )
      {
        System.arraycopy( dummy[t], 0, back, count, dummy[t].length ) ;
        count += dummy[t].length ;
      }
    }
    return back ;
  }

// ---------------------------------------------------------------------------
  class PasteAction
      extends BasicAction
  {
    public PasteAction()
    {
      super( "Paste", "Paste from clipboard", GUIGlobals.getIconUrl("paste")) ;
    }

    public void actionPerformed( ActionEvent e )
    {
      String data = ClipBoardManager.clipBoard.getClipboardContents() ;
      if ( data != null )
      {
        int selStart = textPane.getSelectionStart() ;
        int selEnd = textPane.getSelectionEnd() ;
        if ( selEnd - selStart > 0 )
        {
          textPane.replaceSelection( "" ) ;
        }
        int cPos = textPane.getCaretPosition() ;
        try
        {
          doc.insertString( cPos, data, doc.getStyle( "regular" ) ) ;
        }
        catch ( Exception ex )
        {}
      }
    }
  }

// ---------------------------------------------------------------------------
  class LoadAction
      extends BasicAction
  {
    public LoadAction()
    {
      super( "Open", "Open_file", GUIGlobals.getIconUrl("open")) ;
    }

    public void actionPerformed( ActionEvent e )
    {
      try
      {
        String chosen = null ;
        chosen = FileDialogs.getNewFile( _frame, null, null,
                                     ".txt",
                                     JFileChooser.OPEN_DIALOG, false ) ;
        if ( chosen != null )
        {
          File newFile = new File( chosen ) ;
          doc.remove( 0, doc.getLength() ) ;
          EditorKit eKit = textPane.getEditorKit() ;
          if ( eKit != null )
          {
            eKit.read( new FileInputStream( newFile ), doc, 0 ) ;
            doc.setLogicalStyle( 0, doc.getStyle( "regular" ) ) ;
          }
        }
      }
      catch ( Exception ex )
      {}
    }
  }

// ---------------------------------------------------------------------------
  class ClearAction
      extends BasicAction
  {
    public ClearAction()
    {
      super( "Clear", "Clear_inputarea", GUIGlobals.getIconUrl("new")) ;
    }

    public void actionPerformed( ActionEvent e )
    {
      textPane.setText( "" ) ;
    }
  }

// ---------------------------------------------------------------------------
  class MenuHeaderAction
      extends BasicAction
  {
    public MenuHeaderAction()
    {
      super( "Edit" ) ;
      this.setEnabled( false ) ;
    }

    public void actionPerformed( ActionEvent e )
    {}
  }

// ---------------------------------------------------------------------------

  class FieldListSelectionHandler
      implements ListSelectionListener
  {
    private int lastIndex = -1 ;

    public void valueChanged( ListSelectionEvent e )
    {
      ListSelectionModel lsm = ( ListSelectionModel ) e.getSource() ;

      int index = lsm.getAnchorSelectionIndex() ;
      if ( index != lastIndex )
      {

        boolean isAdjusting = e.getValueIsAdjusting() ;

        if ( !isAdjusting ) // if selection is finished
        {
//            System.out.println( "Event for index" + index ) ;
          if ( lastIndex > -1 )
          {
            String tag1 = fieldList.getModel().getElementAt( lastIndex ).
                toString() ;
            marked.setStyleForTag( tag1, "used", doc ) ;
          }

          String tag2 = fieldList.getModel().getElementAt( index ).toString() ;
          marked.setStyleForTag( tag2, "marked", doc ) ;

          lastIndex = index ;
        }
      }
    }
  }

// ---------------------------------------------------------------------------

  // simple JList Renderer
  // based on : Advanced JList Programming at developers.sun.com
  class SimpleCellRenderer
      extends DefaultListCellRenderer
  {
    private Font baseFont ;
    private Font usedFont ;
    private ImageIcon okIcon = GUIGlobals.getImage("complete");
    private ImageIcon needIcon = GUIGlobals.getImage("wrong");

    public SimpleCellRenderer( Font normFont )
    {
      baseFont = normFont ;
      usedFont = baseFont.deriveFont( Font.ITALIC ) ;
    }

    /* This is the only method defined by ListCellRenderer.  We just
     * reconfigure the Jlabel each time we're called.
     */
    public Component getListCellRendererComponent(
        JList list,
        Object value, // value to display
        int index, // cell index
        boolean iss, // is the cell selected
        boolean chf ) // the list and the cell have the focus
    {
      /* The DefaultListCellRenderer class will take care of
       * the JLabels text property, it's foreground and background
       * colors, and so on.
       */
      super.getListCellRendererComponent( list, value, index, iss, chf ) ;

      /* We additionally set the JLabels icon property here.
       */
      String s = value.toString() ;
//        setIcon((s.length > 10) ? longIcon : shortIcon);
      if ( entry.getField( s ) != null )
      {
        this.setForeground( Color.gray ) ;
        this.setFont( usedFont ) ;
        this.setIcon( okIcon ) ;
        this.setToolTipText( "filled" ) ;
      }
      else
      {
        this.setIcon( needIcon ) ;
        this.setToolTipText( "field is missing" ) ;
      }
      return this ;
    }
  }

//---------------------------------------------------------------

  class FieldListMouseListener
      extends MouseAdapter
  {
    public void mouseClicked( MouseEvent e )
    {
      if ( e.getClickCount() == 2 )
      {
        insertTextForTag() ;
      }
    }
  }
}

//---------------------------------------------------------------
class PopupListener
    extends MouseAdapter
{
  private JPopupMenu popMenu ;

  public PopupListener( JPopupMenu menu )
  {
    popMenu = menu ;
  }

  public void mousePressed( MouseEvent e )
  {
    maybeShowPopup( e ) ;
  }

  public void mouseReleased( MouseEvent e )
  {
    maybeShowPopup( e ) ;
  }

  private void maybeShowPopup( MouseEvent e )
  {
    if ( e.isPopupTrigger() )
    {
//      System.out.println("show "
//                         + e.getComponent() +"  x =" + e.getX() +"y =" + e.getY() ) ;
//      popMenu.setVisible(true);
      popMenu.show( e.getComponent(), e.getX(), e.getY() ) ;
    }
  }
}

//---------------------------------------------------------------

abstract class BasicAction
    extends AbstractAction
{
  public BasicAction( String text, String description, URL icon )
  {
    super( Globals.lang( text ), new ImageIcon( icon ) ) ;
    putValue( SHORT_DESCRIPTION, Globals.lang( description ) ) ;
  }

  public BasicAction( String text, String description, URL icon, KeyStroke key )
  {
    super( Globals.lang( text ), new ImageIcon( icon ) ) ;
    putValue( ACCELERATOR_KEY, key ) ;
    putValue( SHORT_DESCRIPTION, Globals.lang( description ) ) ;
  }

  public BasicAction( String text )
  {
    super( Globals.lang( text ) ) ;
  }

  public BasicAction( String text, KeyStroke key )
  {
    super( Globals.lang( text ) ) ;
    putValue( ACCELERATOR_KEY, key ) ;
  }

  public abstract void actionPerformed( ActionEvent e ) ;
}
//---------------------------------------------------------------


//---------------------------------------------------------------

