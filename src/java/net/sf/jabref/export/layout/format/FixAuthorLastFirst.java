///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner, Morten O. Alver
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

package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.ImportFormatReader;

/**
 * Changes {\^o} or {\^{o}} to ô
 *
 * @author $author$
 * @version $Revision$
 */
public class FixAuthorLastFirst implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////
    public String format(String fieldText)
    {
	ConvertSpecialCharactersForHTML conv = new ConvertSpecialCharactersForHTML();
	return conv.format(ImportFormatReader.fixAuthor_lastnameFirst(fieldText));
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
