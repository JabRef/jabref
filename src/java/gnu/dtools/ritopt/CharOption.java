package gnu.dtools.ritopt;

/**
 * CharOption.java
 *
 * Version:
 *    $Id$
 */

/**
 * This class is used for options with character values.
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

public class CharOption extends Option {

    /**
     * The value of this character option.
     */

    private char value;

    /**
     * Constructs a character option that is set to the space character.
     */

    public CharOption() {
	this( ' ' );
    }

    /**
     * Constructs a character option by copying the character option passed.
     *
     * @param op     The character option to copy.
     */

    public CharOption( CharOption op ) {
	super( op );
	op.value = op.getValue();
    }

    /**
     * Constructs a character option initialized with the value passed.
     *
     * @param value    The initial value of this character option.
     */

    public CharOption( char value ) {
	this( value, null );
    }

    /**
     * Constructs a character option initialized with the value and
     * long option passed.
     *
     * @param value      The initial value of this character option.
     * @param longOption The long option associated with character option.
     */

    public CharOption( char value, String longOption ) {
	this( value, longOption, '\0' );
    }

    /**
     * Constructs a character option initialized with the value and
     * short option passed.
     *
     * @param value       The initial value of this character option.
     * @param shortOption The short option associated with this option.
     */

    public CharOption( char value, char shortOption ) {
	this( value, null, shortOption );
    }

    /**
     * Constructs a character option initialized with the value, short
     * and long option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param longOption  The long option associated with this option.
     * @param value       The initial value of this character option.
     */

    public CharOption( char value, String longOption, char shortOption ) {
	super( longOption, shortOption );
	this.value = value;
    }

    /**
     * Return the value as an object.
     *
     * @return This value as an option.
     */

    public Object getObject() {
	return new Character( value );
    }

    /**
     * Modify this option based on a string representation.
     *
     * @param     value String representation of the object.
     * @exception OptionModificationException Thrown if an error occurs
     *                                  during modification of an option.
     */

    public void modify( String value ) throws OptionModificationException {
	this.value = ( value.length() > 0 ) ? value.charAt( 0 ) : ' ';
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
     * Modify this option using a character value.
     *
     * @param     value A character value.
     */

    public void setValue( char value ) {
	this.value = value;
    }

    /**
     * Return this option as a character.
     *
     * @return This option as a character.
     */

    public char getValue() {
	return value;
    }

    /**
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getStringValue() {
	return "" + value;
    }

    /**
     * Returns the type name of this option. For a ByteOption, "BYTE"
     * is returned.
     *
     * @return The type name of this option.
     */

    public String getTypeName() {
	return "CHAR";
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */

    public String toString() {
	return getStringValue();
    }

} /** CharOption */
