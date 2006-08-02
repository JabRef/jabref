/*
 Copyright (C) 2003-2006 Morten O. Alver, JabRef-Team
 
 All programs in this directory and subdirectories are published 
 under the GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.export.layout;

/**
 * The LayoutFormatter is used for a Filter design-pattern.
 * 
 * Implementing classes have to accept a String and returned a formatted version of it.
 * 
 * Example:
 * 
 *   "John von Neumann" => "von Neumann, John"
 *
 * @version 1.2 - Documentation CO
 */
public interface LayoutFormatter {
	/**
	 * Failure Mode:
	 * <p>
	 * Formatters should be robust in the sense that they always return some
	 * relevant string.
	 * <p>
	 * If the formatter can detect an invalid input it should return the
	 * original string otherwise it may simply return a wrong output.
	 * 
	 * @param fieldText
	 *            The text to layout.
	 * @return The layouted text.
	 */
	public String format(String fieldText);
}
