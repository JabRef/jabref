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
package wsi.ra.types;


/*==========================================================================*
 * IMPORTS
 *========================================================================== */
/*==========================================================================*
 * CLASS DECLARATION
 *========================================================================== */

/**
 * String and integer value.
 *
 * @author     wegnerj
 * @license GPL
 * @cvsversion    $Revision$, $Date$
 */
public class StringInt implements java.io.Serializable
{
    //~ Instance fields ////////////////////////////////////////////////////////

    /*-------------------------------------------------------------------------*
     * public member variables
     *------------------------------------------------------------------------- */

    /**
     *  Description of the Field
     */
    public String s;

    /**
     *  Description of the Field
     */
    public int i;

    //~ Constructors ///////////////////////////////////////////////////////////

    /*-------------------------------------------------------------------------*
     * constructor
     *------------------------------------------------------------------------- */

    /**
     *  Constructor for the StringString object
     *
     * @param  _s1  Description of the Parameter
     * @param  _s2  Description of the Parameter
     */
    public StringInt(String _s, int _i)
    {
        s = _s;
        i = _i;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
