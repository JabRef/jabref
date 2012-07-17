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
package net.sf.jabref.util;

/**
 * This little contraption is used if a generic type is needed that is either a
 * S or a T.
 * 
 * @author oezbek
 * 
 * @param <S>
 * @param <T>
 */
public class TypeOr<S, T> {

	public S s;

	public T t;

	public TypeOr(S s, T t) {
		if (!(s == null ^ t == null))
			throw new IllegalArgumentException("Either s or t need to be null");

		this.s = s;
		this.t = t;
	}

	public boolean isS() {
		return s != null;
	}

	public boolean isT() {
		return t != null;
	}

}
