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
// function : a class for wrapping a IntegrityCheck message
//
// modified :

package net.sf.jabref.wizard.integrity;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

public class IntegrityMessage implements Cloneable
{

    // Hints and Infos < 1000 :-)
    public static final int
            GENERIC_HINT = 1,
            UPPER_AND_LOWER_HINT = 10,
            FOUR_DIGITS_HINT = 11

            ;

    // > 1000 Warnings
    public static final int
            GENERIC_WARNING = 1001,
            NAME_START_WARNING = 1010,
            NAME_END_WARNING = 1011,
            NAME_SEMANTIC_WARNING = 1012
            ;

    // > 2000 Failure Messages
    public static final int
            UNKNONW_FAILURE = 2001,
            UNEXPECTED_CLOSING_BRACE_FAILURE = 2010
            ;

    public static final int
            FULL_MODE = 1; // print with Bibtex Entry
            public static final int SINLGE_MODE = 2 // print only Message
            ;

    private static int printMode = IntegrityMessage.SINLGE_MODE;

    private final int type;
    private final BibtexEntry entry;
    private final String fieldName;
    private final Object additionalInfo;
    private final String msg;
    private boolean fixed; // the user has changed sometings on BibtexEntry


    public synchronized static void setPrintMode(int newMode)
    {
        IntegrityMessage.printMode = newMode;
    }

    public IntegrityMessage(int pType, BibtexEntry pEntry, String pFieldName, Object pAdditionalInfo)
    {
        this.type = pType;
        this.entry = pEntry;
        this.fieldName = pFieldName;
        this.additionalInfo = pAdditionalInfo;
        fixed = false;

        msg = getMessage();
    }

    private String getMessage()
    {
        String back = Globals.getIntegrityMessage("ITEXT_" + type);
        if ((back != null) && (fieldName != null))
        {
            back = back.replaceAll("\\$FIELD", fieldName);
        }
        return back;
    }

    @Override
    public String toString()
    {
        String back = msg;
        if (IntegrityMessage.printMode == IntegrityMessage.FULL_MODE)
        {
            back = "[" + entry.getCiteKey() + "] " + msg;
        }
        return back;
    }

    public int getType()
    {
        return type;
    }

    public BibtexEntry getEntry()
    {
        return entry;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public Object getAdditionalInfo()
    {
        return additionalInfo;
    }

    public boolean getFixed()
    {
        return fixed;
    }

    public void setFixed(boolean pFixed)
    {
        this.fixed = pFixed;
    }
}
