package gnu.dtools.ritopt;

/**
 * OptionRegistrationException.java
 *
 * Version:
 *   $Id$
 */

/**
 * This exception indicates that an error has occurred during registration
 * of an option, registrar, or module.
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

public class OptionRegistrationException extends OptionException {

    /**
     * The target option associated with the registration failure.
     */

    private Option target;

    /**
     * Construct an OptionRegistrationException.
     *
     * @param msg The exception message.
     */


    public OptionRegistrationException( String msg ) {
	super( msg );
    }

    /**
     * Construct an OptionRegisrationException and initialize its members
     * with the message and target passed.
     *
     * @param msg     An exception message.
     * @param target  The target option that caused the registration failure.
     */

    public OptionRegistrationException( String msg, Option target ) {
	super( msg );
	this.target = target;
    }

    /**
     * Returns the target option associated with the registration failure.
     *
     * @return The target option.
     */

    public Option getTarget() {
	return target;
    }


}
