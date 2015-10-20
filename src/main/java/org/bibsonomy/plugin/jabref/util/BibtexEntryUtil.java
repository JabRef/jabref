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

package org.bibsonomy.plugin.jabref.util;

import java.util.Set;

import net.sf.jabref.BibtexEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper methods for BibtexEntry objects (the internal jabref representation)
 * 
 * @author Dominik Benz <benz@cs.uni-kassel.de>
 * 
 */
public class BibtexEntryUtil {

	private static final Log LOG = LogFactory.getLog(BibtexEntryUtil.class);

	/**
	 * Check the (string) equality of two BibTex entries
	 * 
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static boolean areEqual(final BibtexEntry b1, final BibtexEntry b2) {
		final Set<String> commonFields = b1.getAllFields();
		commonFields.addAll(b2.getAllFields());
		LOG.debug("Total nr. of common fields: "
				+ commonFields.size());
		for (final String field : commonFields) {
			BibtexEntryUtil.LOG.debug("Comparing field: " + field);

			// fields that should be ignored
			if ((field != null) && !field.startsWith("__")
					&& !"id".equals(field) && !"".equals(field)
					&& !"timestamp".equals(field)
					&& !"owner".equals(field)) {
				// check if b1 lacks a field that b2 has
				if (StringUtil.isEmpty(b1.getField(field))
						&& !StringUtil.isEmpty(b2.getField(field))) {
					LOG.debug("field " + field
							+ " is null for b1 but not for b2");
					return false;
				}
				// check if b2 lacks a field that b1 has
				if (StringUtil.isEmpty(b2.getField(field))
						&& !StringUtil.isEmpty(b1.getField(field))) {
					LOG.debug("field " + field
							+ " is null for b2 but not for b1");
					return false;
				}
				// check if both are empty/null -> OK
				if (StringUtil.isEmpty(b1.getField(field))
						&& StringUtil.isEmpty(b2.getField(field))) {
					continue;
				}
				// check for fields of b1 if they are the same in b2
				if (!b1.getField(field).equals(b2.getField(field))) {
					LOG.debug("Found inequality for field: "
							+ field);
					return false;
				}
			}
		}
		return true;
	}
}