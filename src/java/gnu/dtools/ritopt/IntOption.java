package gnu.dtools.ritopt;

/**
 * IntOption.java
 *
 * Version:
 *    $Id$
 */

/**
 * This class is used for options with integer values.
 *
 * <pre>
 *
 * <hr>
 *
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

public class IntOption extends Option {

    /**
     * The value of this integer option.
     */

    private int value;

    /**
     * Constructs an integer option that is initially set to zero.
     */

    public IntOption() {
	this( 0 );
    }

    /**
     * Constructs an integer option by copying the integer option passed.
     *
     * @param op     The character option to copy.
     */

    public IntOption( IntOption op ) {
	super( op );
	op.value = op.getValue();
    }

    /**
     * Constructs an integer option initialized with the value passed.
     *
     * @param value    The initial value of this integer option.
     */

    public IntOption( int value ) {
	this( value, null );
    }

    /**
     * Constructs an integer option initialized with the value and
     * long option passed.
     *
     * @param value      The initial value of this integer option.
     * @param longOption The long option associated with integer option.
     */

    public IntOption( int value, String longOption ) {
	this( value, longOption, '\0' );
    }

    /**
     * Constructs a character option initialized with the value and
     * short option passed.
     *
     * @param value       The initial value of this integer option.
     * @param shortOption The short option associated with this option.
     */

    public IntOption( int value, char shortOption ) {
	this( value, null, shortOption );
    }

    /**
     * Constructs an integer option initialized with the value, short
     * and long option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param longOption  The long option associated with this option.
     * @param value       The initial value of this integer option.
     */

    public IntOption( int value, String longOption, char shortOption ) {
	super( longOption, shortOption );
	this.value = value;
    }

    /**
     * Return the value as an object.
     *
     * @return This value as an option.
     */

    public Object getObject() {
	return new Integer( value );
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
	    this.value = Integer.parseInt( value );
	}
	catch ( NumberFormatException e ) {
	    throw new OptionModificationException( "Error. An integer must be"
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
     * Modify this option using an integer value.
     *
     * @param     value an integer value.
     */

    public void setValue( int value ) {
	this.value = value;
    }

    /**
     * Return this option as an integer.
     *
     * @return This option as an integer.
     */

    public int getValue() {
	return value;
    }

    /**
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getStringValue() {
	return Integer.toString( value );
    }

    /**
     * Returns the type name of this option. For an IntegerOption, "INTEGER"
     * is returned.
     *
     * @return The type name of this option.
     */

    public String getTypeName() {
	return "INTEGER";
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */

    public String toString() {
	return getStringValue();
    }

}
