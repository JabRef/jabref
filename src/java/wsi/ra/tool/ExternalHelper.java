///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner, Gerd Mueller
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


/**
 * Some helper methods for calling external programs.
 *
 * @author     wegnerj
 * @license GPL
 * @cvsversion    $Revision$, $Date$
 */
public class ExternalHelper
{
    //~ Static fields/initializers /////////////////////////////////////////////

    public static final String OS_WINDOWS = "windows";
    public static final String OS_LINUX = "linux";
    public static final String OS_SOLARIS = "solaris";

    //~ Constructors ///////////////////////////////////////////////////////////

    /*-------------------------------------------------------------------------*
     * constructor
     *-------------------------------------------------------------------------*/

    /** Don't let anyone instantiate this class */
    private ExternalHelper()
    {
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    /*-------------------------------------------------------------------------*
     * private static methods
     *-------------------------------------------------------------------------*/

    /**
     * Returns the name of the operation system.
     *
     *   @todo maybe move this method to a more common class */
    public static String getOperationSystemName()
    {
        String osName = System.getProperty("os.name");

        // determine name of operation system and convert it into lower caps without blanks
        if (osName.indexOf("Windows") != -1)
        {
            osName = OS_WINDOWS;
        }
        else if (osName.indexOf("Linux") != -1)
        {
            osName = OS_LINUX;
        }
        else if (osName.indexOf("Solaris") != -1)
        {
            osName = OS_SOLARIS;
        }

        return osName;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
