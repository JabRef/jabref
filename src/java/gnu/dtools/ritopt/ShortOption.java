package gnu.dtools.ritopt;

/**
 * ShortOption.java
 *
 * Version:
 *    $Id$
 */

/**
 * This class is used for options with short values.
 *
 * <hr>
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

public class ShortOption extends Option {

    /**
     * The value of this short option.
     */

    private short value;

    /**
     * Constructs a short option that is initially set to zero.
     */

    public ShortOption() {
	this( (short)0 );
    }

    /**
     * Constructs a short option by copying the short option passed.
     *
     * @param op     The character option to copy.
     */

    public ShortOption( ShortOption op ) {
	super( op );
	op.value = op.getValue();
    }

    /**
     * Constructs a short option initialized with the value passed.
     *
     * @param value    The initial value of this short option.
     */

    public ShortOption( short value ) {
	this( value, null );
    }

    /**
     * Constructs a short option initialized with the value and
     * long option passed.
     *
     * @param value      The initial value of this short option.
     * @param longOption The long option associated with short option.
     */

    public ShortOption( short value, String longOption ) {
	this( value, longOption, '\0' );
    }

    /**
     * Constructs a character option initialized with the value and
     * short option passed.
     *
     * @param value       The initial value of this short option.
     * @param shortOption The short option associated with this option.
     */

    public ShortOption( short value, char shortOption ) {
	this( value, null, shortOption );
    }

    /**
     * Constructs a short option initialized with the value, short
     * and long option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param longOption  The long option associated with this option.
     * @param value       The initial value of this short option.
     */

    public ShortOption( short value, String longOption, char shortOption ) {
	super( longOption, shortOption );
	this.value = value;
    }

    /**
     * Return the value as an object.
     *
     * @return This value as an option.
     */

    public Object getObject() {
	return new Short( value );
    }

    /**
     * Modify this option based on a string representation.
     *
     * @param     value String representation of the object.
     * @exception OptionModificationException Thrown if an error occurs
     *                                  during modification of an option.
     */

    public void modify( String value ) throws OptionModificationException {
	try {
	    this.value = Short.parseShort( value );
	}
	catch ( NumberFormatException e ) {
	    throw new OptionModificationException( "Error. A short must be"
						   + " specified, not '"
						   + value + "'." );
	}
    }

    /**
     * Modify this option based on a string representation.
     *
     * @param     value String representation of the object.
     * @exception OptionModificationException Thrown if an error occurs
     *                                  during modification of an option.
     */

    public void setValue( String value ) throws OptionModificationException {
	modify( value );
    }

    /**
     * Modify this option using a short value.
     *
     * @param     value A short value.
     */

    public void setValue( short value ) {
	this.value = value;
    }

    /**
     * Return this option as a short.
     *
     * @return This option as a short.
     */

    public short getValue() {
	return value;
    }

    /**
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getStringValue() {
	return Short.toString( value );
    }

    /**
     * Returns the type name of this option. For a ShortOption, "SHORT"
     * is returned.
     *
     * @return The type name of this option.
     */

    public String getTypeName() {
	return "SHORT";
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */

    public String toString() {
	return getStringValue();
    }

} /** ShortOption */
