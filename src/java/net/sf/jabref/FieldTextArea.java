/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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
package net.sf.jabref;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
//import java.awt.Color;
import java.awt.event.*;

public class FieldTextArea extends JTextArea implements FieldEditor {

    Dimension PREFERRED_SIZE;
    protected JScrollPane sp;
    protected FieldNameLabel label;
    protected String fieldName;
    //protected Completer completer;

    public FieldTextArea(String fieldName_, String content) {
        super(content);

        // Add the global focus listener, so a menu item can see if this field was focused when
        // an action was called.
        addFocusListener(Globals.focusListener);

        sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setLineWrap(true);
        setWrapStyleWord(true);
        fieldName = fieldName_;

        label = new FieldNameLabel(" "+Util.nCase(fieldName)+" ");
        //label.setBorder(BorderFactory.createEtchedBorder
        //		 (GUIGlobals.lightGray, Color.gray));
        //label.setBorder(BorderFactory.createEtchedBorder());
        //label.setOpaque(true);
        //label.setBackground(GUIGlobals.lightGray);
        //label.setForeground(Color.gray);
        setBackground(GUIGlobals.validFieldBackground);
        //if ((content != null) && (content.length() > 0))
        //label.setForeground(GUIGlobals.validFieldColor);
        // At construction time, the field can never have an invalid value.
        //else
        //    label.setForeground(GUIGlobals.nullFieldColor);

        FieldTextMenu popMenu = new FieldTextMenu(this) ;
        this.addMouseListener( popMenu );
        label.addMouseListener( popMenu);
    }

    /*
    public void setAutoComplete(Completer completer) {
        addKeyListener(new AutoCompListener(completer));
    }
    */

    /*public Dimension getPreferredSize() {
        return PREFERRED_SIZE;
        }*/

    public Dimension getPreferredScrollableViewportSize() {
        return PREFERRED_SIZE;
    }

  public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        RenderingHints rh = g2.getRenderingHints();
        rh.put(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_RENDERING,
               RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHints(rh);
        super.paintComponent(g2);
  }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String newName) { fieldName = newName ; }
    public JLabel getLabel() { return label; }
    public void setLabelColor(Color c) { label.setForeground(c); }
    public JComponent getPane() { return sp; }
    public JComponent getTextComponent() { return this; }

    public void paste(String textToInsert)
    {
      int sel =  getSelectionEnd() - getSelectionStart() ;
      if (sel > 0) // selected text available
        replaceSelection(textToInsert);
      else
      {
       int cPos = this.getCaretPosition() ;
       this.insert(textToInsert, cPos);
      }
    }
}
