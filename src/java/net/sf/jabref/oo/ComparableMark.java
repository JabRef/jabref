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
package net.sf.jabref.oo;

import com.sun.star.awt.Point;

/**
 *
 */
public class ComparableMark implements Comparable {

    String name;
    Point position;

    public ComparableMark(String name, Point position) {
        this.name = name;
        this.position = position;
    }

    public int compareTo(Object o) {
        ComparableMark other = (ComparableMark)o;
        if (position.Y != other.position.Y)
            return position.Y-other.position.Y;
        else
            return position.X-other.position.X;
    }

    public String getName() {
        return name;
    }

    public Point getPosition() {
        return position;
    }
}
