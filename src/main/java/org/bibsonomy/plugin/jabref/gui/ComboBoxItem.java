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

package org.bibsonomy.plugin.jabref.gui;

/**
 * {@link ComboBoxItem} is a simple class to represent a key-value store in a combo box
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 * @param <K> Type of the key
 */
public class ComboBoxItem<K> {

	private K key;
	private String value;
	
	public void setKey(K key) {
		this.key = key;
	}
	public K getKey() {
		return key;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	
	public ComboBoxItem(K key, String value) {
		
		setKey(key);
		setValue(value);
	}
	
	@Override
	public String toString() {
		
		return getValue();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ComboBoxItem<?>) {
			ComboBoxItem<?> cbi = (ComboBoxItem<?>) obj;
			return (cbi.getValue().equals(this.getValue()));
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.getValue() != null ? this.getValue().hashCode() : 0);
		return hash;
	}
}
