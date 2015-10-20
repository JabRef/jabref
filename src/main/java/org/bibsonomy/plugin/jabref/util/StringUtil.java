/**
 *  
 *  JabRef Bibsonomy Plug-in - Plugin for the reference management 
 * 		software JabRef (http://jabref.sourceforge.net/) 
 * 		to fetch, store and delete entries from BibSonomy.
 *   
 *  Copyright (C) 2008 - 2011 Knowledge & Data Engineering Group, 
 *                            University of Kassel, Germany
 *                            http://www.kde.cs.uni-kassel.de/
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * 
 */
package org.bibsonomy.plugin.jabref.util;

import java.io.UnsupportedEncodingException;

/**
 * @author Waldemar Biller <wbi@cs.uni-kassel.de>
 *
 */
public class StringUtil {

	/**
	 * Encodes a string to UTF8
	 * @param s the string which should be encoded
	 * @return the encoded string or null
	 */
	public static String toUTF8(String s) {
		if(s != null) {
			try {
				// FIXME: what is this? why do we want to introduce platform dependency here?
				// This should only be correct if an error from somewhere else has to be corrected.
				return new String(s.getBytes("UTF8"));
			} catch (UnsupportedEncodingException e) {}
		}
		return null;
	}
	
	public static boolean isEmpty(String s) {
		return s == null || "".equals(s) || "".equals(s.trim());
	}
}
