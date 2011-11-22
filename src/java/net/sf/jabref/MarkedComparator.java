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
package net.sf.jabref;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 1, 2005
 * Time: 11:35:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarkedComparator implements Comparator<BibtexEntry> {

    Comparator<BibtexEntry> next;

    public MarkedComparator(Comparator<BibtexEntry> next) {
        this.next = next;
    }
    public int compare(BibtexEntry e1, BibtexEntry e2) {

        if (e1 == e2)
            return 0;

        int mrk1 = Util.isMarked(e1),
                mrk2 = Util.isMarked(e2);

        if (mrk1 == mrk2)
            return (next != null ? next.compare(e1, e2) : idCompare(e1, e2));

        else return mrk2-mrk1;
        
    }

    private int idCompare(BibtexEntry b1, BibtexEntry b2) {
	    return ((b1.getId())).compareTo((b2.getId()));
    }
}
