///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner
//  Version:  $Revision$
//            $Date$
//            $Author$
//
//  Copyright (c) Dept. Computer Architecture, University of Tuebingen, Germany
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
///////////////////////////////////////////////////////////////////////////////
package wsi.ra.tool;


/*==========================================================================*
 * IMPORTS
 *========================================================================== */
import java.util.StringTokenizer;
import java.util.Vector;


/*==========================================================================*
 * CLASS DECLARATION
 *========================================================================== */

/**
 * JabRef helper methods.
 *
 * @author     wegnerj
 * @license GPL
 * @cvsversion    $Revision$, $Date$
 */
public class WSITools
{
    //~ Constructors ///////////////////////////////////////////////////////////

    private WSITools()
    {
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    /**
     * @param  vcr  {@link java.util.Vector} of <tt>String</tt>
     * @param  buf  Description of the Parameter
     * @return      Description of the Return Value
     */
    public static boolean tokenize(Vector vcr, String buf)
    {
        return tokenize(vcr, buf, " \t\n");
    }

    /**
     * @param  vcr       {@link java.util.Vector} of <tt>String</tt>
     * @param  buf       Description of the Parameter
     * @param  delimstr  Description of the Parameter
     * @return           Description of the Return Value
     */
    public static boolean tokenize(Vector vcr, String buf, String delimstr)
    {
        vcr.clear();
        buf = new String(buf + "\n");

        StringTokenizer st = new StringTokenizer(buf, delimstr);

        while (st.hasMoreTokens())
        {
            vcr.add(st.nextToken());
        }

        return true;
    }

    /**
     * @param  vcr       {@link java.util.Vector} of <tt>String</tt>
     * @param  s         Description of the Parameter
     * @param  delimstr  Description of the Parameter
     * @param  limit     Description of the Parameter
     * @return           Description of the Return Value
     */
    public static boolean tokenize(Vector vcr, String s, String delimstr,
        int limit)
    {
        System.out.println("Warning: tokenize \"" + s + "\"");
        vcr.clear();
        s = new String(s + "\n");

        int endpos = 0;
        int matched = 0;

        StringTokenizer st = new StringTokenizer(s, delimstr);

        while (st.hasMoreTokens())
        {
            String tmp = st.nextToken();
            vcr.add(tmp);

            matched++;

            if (matched == limit)
            {
                endpos = s.lastIndexOf(tmp);
                vcr.add(s.substring(endpos + tmp.length()));

                break;
            }
        }

        return true;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
