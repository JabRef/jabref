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

/**
 * A class implementing this interface can provided as a receiver for error messages originating
 * in a thread that can't return any value or throw any exceptions. E.g. net.sf.jabref.DatabaseSearch.
 *
 * The point is that the worker thread doesn't need to know what interface it is working against,
 * since the ErrorMessageDisplay implementer will be responsible for displaying the error message.
 */
public interface ErrorMessageDisplay {

    /**
     * An error has occured.
     * @param errorMessage Error message.
     */
    public void reportError(String errorMessage);

    /**
     * An error has occured.
     * @param errorMessage Error message.
     * @param exception Exception representing the error condition.
     */
    public void reportError(String errorMessage, Exception exception);

}
