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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Uses as input the fields (author or editor) in the LastFirst format.
 * 
 * This formater enables to abbreviate the authors name in the following way:
 * 
 * Ex: Someone, Van Something will be abbreviated as Someone, V.S.
 * 
 * @author Carlos Silla
 * @author Christopher Oezbek <oezi@oezi.de>
 * 
 * @version 1.0 Created on 12/10/2004
 * @version 1.1 Fixed bug
 *          http://sourceforge.net/tracker/index.php?func=detail&aid=1466924&group_id=92314&atid=600306
 */
public class AuthorLastFirstAbbreviator implements LayoutFormatter {

    /**
     * @see net.sf.jabref.logic.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {

        /**
         * This formatter is a duplicate of AuthorAbbreviator, so we simply
         * call that one.
         */
        return new AuthorAbbreviator().format(fieldText);

    }
}
