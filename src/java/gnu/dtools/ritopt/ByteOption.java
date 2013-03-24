package gnu.dtools.ritopt;

/**
 * ByteOption.java
 *
 * Version:
 *     $Id$
 */

/**
 * This class is used for options with byte values.
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

public class ByteOption extends Option {

    /**
     * The value of this byte option.
     */

    private byte value;

    /**
     * Constructs a byte option that is set to the null character.
     */

    public ByteOption() {
	this( (byte)0 );
    }

    /**
     * Constructs a byte option by copying the byte option passed.
     *
     * @param op     The byte option to copy.
     */

    public ByteOption( ByteOption op ) {
	super( op );
	op.value = op.getValue();
    }

    /**
     * Constructs a byte option initialized with the value passed.
     *
     * @param value    The initial value of this byte option.
     */

    public ByteOption( byte value ) {
	this( value, null );
    }

    /**
     * Constructs a byte option initialized with the value and
     * long option passed.
     *
     * @param value      The initial value of this byte option.
     * @param longOption The long option associated with this option.
     */

    public ByteOption( byte value, String longOption ) {
	this( value, longOption, '\0' );
    }

    /**
     * Constructs a byte option initialized with the value and short
     * option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param value       The initial value of this byte option.
     */

    public ByteOption( byte value, char shortOption ) {
	this( value, null, shortOption );
    }

    /**
     * Constructs a byte option initialized with the value, short
     * and long option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param longOption  The long option associated with this option.
     * @param value       The initial value of this byte option.
     */

    public ByteOption( byte value, String longOption, char shortOption ) {
	super( longOption, shortOption );
	this.value = value;
    }

    /**
     * Return the value as an object.
     *
     * @return This value as an option.
     */

    public Object getObject() {
	return new Byte( value );
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
	    this.value = Byte.parseByte( value );
	}
	catch ( NumberFormatException e ) {
	    throw new OptionModificationException( "Error. A byte must be"
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
     * Modify this option using a byte value.
     *
     * @param     value A byte value.
     */

    public void setValue( byte value ) {
	this.value = value;
    }

    /**
     * Return this option as a boolean.
     *
     * @return This option as a boolean.
     */

    public byte getValue() {
	return value;
    }

    /**
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getStringValue() {
	return Byte.toString( value );
    }

    /**
     * Returns the type name of this option. For a ByteOption, "BYTE"
     * is returned.
     *
     * @return The type name of this option.
     */

    public String getTypeName() {
	return "BYTE";
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */

    public String toString() {
	return getStringValue();
    }

} /** ByteOption */
