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
package net.sf.jabref.logic.layout;

/**
 * This interface extends LayoutFormatter, adding the capability of taking
 * and additional parameter. Such a parameter is specified in the layout file
 * by the following construct: \format[MyFormatter(argument){\field}
 * If and only if MyFormatter is a class that implements ParamLayoutFormatter,
 * it will be set up with the argument given in the parenthesis by way of the
 * method setArgument(String). If no argument is given, the formatter will be
 * invoked without the setArgument() method being called first.
 */
public interface ParamLayoutFormatter extends LayoutFormatter {

    /**
     * Method for setting the argument of this formatter.
     * @param arg A String argument.
     */
    void setArgument(String arg);

}
