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
package net.sf.jabref.bibtex.comparator;

import java.util.Comparator;
import java.util.List;

/**
 * This class represents a list of comparators. The first Comparator takes precedence,
 * and each time a Comparator returns 0, the next one is attempted. If all comparators
 * return 0 the final result will be 0.
 */
public class FieldComparatorStack<T> implements Comparator<T> {

    private final List<? extends Comparator<? super T>> comparators;


    public FieldComparatorStack(List<? extends Comparator<? super T>> comparators) {
        this.comparators = comparators;
    }

    @Override
    public int compare(T o1, T o2) {
        for (Comparator<? super T> comp : comparators) {
            int res = comp.compare(o1, o2);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }
}
