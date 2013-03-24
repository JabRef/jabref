/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.export;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;


/**
 * Exception thrown if saving goes wrong. If caused by a specific
 * entry, keeps track of which entry caused the problem.
 */
public class SaveException extends Exception
{
    //~ Instance fields ////////////////////////////////////////////////////////

    public static final SaveException FILE_LOCKED = new SaveException
            (Globals.lang("Could not save, file locked by another JabRef instance."));
    public static final SaveException BACKUP_CREATION = new SaveException
            (Globals.lang("Unable to create backup"));

    private BibtexEntry entry;
    private int status = 0;
    //~ Constructors ///////////////////////////////////////////////////////////

    public SaveException(String message)
    {
        super(message);
        entry = null;
    }

    public SaveException(String message, int status)
        {
            super(message);
            entry = null;
            this.status = status;
        }


    public SaveException(String message, BibtexEntry entry)
    {
        super(message);
        this.entry = entry;
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public int getStatus() {
        return status;
    }

    public BibtexEntry getEntry()
    {
        return entry;
    }

    public boolean specificEntry()
    {
        return (entry != null);
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
