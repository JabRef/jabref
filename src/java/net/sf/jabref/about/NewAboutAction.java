/*
 animated about dialog

Copyright (C) 2005 Raik Nagel <kiar@users.sourceforge.net>
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

// created by : r.nagel 05.05.2005
//
// function : about action, used in JabrefFrame
//
// todo     :
//
// modified :

package net.sf.jabref.about ;

import java.awt.event.* ;
import javax.swing.* ;

import net.sf.jabref.* ;

public class NewAboutAction
    extends MnemonicAwareAction
{

  private String type = null ; // The type of item to create.
  private KeyStroke keyStroke = null ; // Used for the specific instances.

  public NewAboutAction()
  {
    // This action leads to a dialog asking for entry type.
    super( new ImageIcon( GUIGlobals.helpIconFile ) ) ;
    putValue( NAME, "About JabRef" ) ;
//    putValue( ACCELERATOR_KEY, key ) ;
    putValue( SHORT_DESCRIPTION, Globals.lang( "About JabRef" ) ) ;
  }

  public void actionPerformed( ActionEvent e )
  {
    About2 dialog = new About2((JFrame) null) ;
  }
}
