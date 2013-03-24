package gnu.dtools.ritopt;

/**
 * OptionModificationException.java
 *
 * Version:
 *    $Id$
 */

/**
 * Instances of this class are thrown if there is an error during modification
 * of an option's value.
 *
 * <hr>
 *
 * <pre>
 * Copyright (C) Damian Ryan Eads, 2001. All Rights Reserved.
 *
 * ritopt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * ritopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ritopt; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </pre>
 *
 * @author Damian Eads
 */

public class OptionModificationException extends OptionException {

    /**
     * Construct an OptionModificationException.
     *
     * @param msg The exception message.
     */

    OptionModificationException( String msg ) {
	super( msg );
    }

}
