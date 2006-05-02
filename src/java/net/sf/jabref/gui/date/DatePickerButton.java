/*
 Copyright (C) 2006 Raik Nagel <kiar@users.sourceforge.net>
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 * Neither the name of the author nor the names of its contributors may be
  used to endorse or promote products derived from this software without
  specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// created by : r.nagel 19.04.2006
//
// function : wrapper and service class for the DatePicker handling at the
//            EntryEditor
//
// todo     :
//
// modified :  r.nagel 25.04.2006
//             check NullPointer at the actionPerformed methode

package net.sf.jabref.gui.date ;

import java.awt.event.* ;
import javax.swing.* ;

import com.michaelbaranov.microba.calendar.* ;
import net.sf.jabref.* ;
import java.util.*;

public class DatePickerButton implements ActionListener
{
  private DatePicker datePicker = new DatePicker() ;
  private FieldEditor editor ;

  public DatePickerButton(FieldEditor pEditor)
  {
    datePicker.showButtonOnly( true ) ;
    datePicker.addActionListener( this ) ;

    editor = pEditor ;
  }

  public void actionPerformed( ActionEvent e )
  {
    Date date = datePicker.getDate() ;
    if (date != null)
    {
      editor.setText( Util.easyDateFormat( date ) ) ;
        // Set focus to editor component after changing its text:
        new FocusRequester(editor.getTextComponent());
    }
  }

  public JComponent getDatePicker()
  {
    return datePicker ;
  }
}
