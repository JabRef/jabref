package gnu.dtools.ritopt;

/**
 * BooleanOption.java
 *
 * Version:
 *     $Id$
 */

/**
 * This class is used for options with boolean values. There are several
 * ways to invoke a boolean option at the command line.<p>
 *
 * For example, a plus (true) or minus (false) sign directly after the short
 * or long option may be used.<p>
 * <pre>
 *  myprogram -a+ -b- --longa+ --longb-
 * </pre>
 * The following keywords may be used to invoke an option using the assignment
 * form.
 * <ul>
 *  <li>+
 *  <li>-
 *  <li>true
 *  <li>false
 *  <li>yes
 *  <li>no
 *  <li>on
 *  <li>off
 *  <li>activated
 *  <li>not activated
 *  <li>active
 *  <li>inactive
 * </ul>
 * To invoke an option using assignment form where <value> is the <value> of
 * the option, use the following syntax.<p>
 * <pre>
 *  myprogram -a=<keyword> -b <keyword> --longa=<keyword> --longb=<keyword>
 * </pre>
 * Invoking a boolean option without using any of the aforementioned
 * keywords and forms will set the option to true by default.
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

public class BooleanOption extends Option {

    /**
     * The value of the boolean option.
     */

    private boolean value;

    /**
     * Constructs a boolean option that is initially false.
     */

    public BooleanOption() {
	this( false );
    }

    /**
     * Constructs a boolean option by copying the boolean option passed.
     *
     * @param op     The boolean option to copy.
     */

    public BooleanOption( BooleanOption op ) {
	super( op );
	op.value = op.getValue();
    }

    /**
     * Constructs a boolean option initialized with the value passed.
     *
     * @param value    The initial value of this boolean option.
     */

    public BooleanOption( boolean value ) {
	this( value, null );
    }

    /**
     * Constructs a boolean option initialized with the value and
     * long option passed.
     *
     * @param value      The initial value of this boolean option.
     * @param longOption The long option associated with this option.
     */

    public BooleanOption( boolean value, String longOption ) {
	this( value, longOption, '\0' );
    }

    /**
     * Constructs a boolean option initialized with the value and short
     * option passed.
     *
     * @param value       The initial value of this boolean option.
     * @param shortOption The short option associated with this option.
     */

    public BooleanOption( boolean value, char shortOption ) {
	this( value, null, shortOption );
    }

    /**
     * Constructs a boolean option initialized with the value, short
     * and long option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param longOption  The long option associated with this option.
     * @param value       The initial value of this boolean option.
     */

    public BooleanOption( boolean value, String longOption, char shortOption ) {
	super( longOption, shortOption );
	this.value = value;
    }

    /**
     * Return the value as an object.
     *
     * @return This value as an option.
     */

    public Object getObject() {
	return Boolean.valueOf(value);
    }

    /**
     * Modify this option based on a string representation. Acceptable values
     * are +, -, true, false, yes, no, on, off, activated, not activated,
     * active, and inactive.
     *
     * @param     value String representation of the object.
     * @exception OptionModificationException Thrown if an error occurs
     *                                  during modification of an option.
     */

    public void modify( String value ) throws OptionModificationException {
	String val = value.toUpperCase();
	this.value = false;
	if ( val.equals( "+" ) || val.equals( "TRUE" ) ||
	     val.equals( "YES" ) || val.equals( "ON" ) ||
	     val.equals( "ACTIVATED" ) || val.equals( "ACTIVE" ) ) {
	    this.value = true;
	}
	else if ( val.equals( "-" ) || val.equals( "FALSE" ) ||
		  val.equals( "NO" ) || val.equals( "OFF" ) ||
		  val.equals( "NOT ACTIVATED" ) ||
		  val.equals( "INACTIVE" ) ) {
	    this.value = false;
	}
	else {
	    throw new OptionModificationException( "Error. A boolean value of\n+/-/true/false/yes/no/on/off/activated/not activated/active/inactive must be\nspecified, not '" + value + "'." );
	}
    }

    /**
     * Modify this option based on a string representation. Acceptable values
     * are +, -, true, false, yes, no, on, off, activated, not activated,
     * active, inactive.
     *
     * @param     value String representation of the object.
     * @exception OptionModificationException Thrown if an error occurs
     *                                  during modification of an option.
     */

    public void setValue( String value ) throws OptionModificationException {
	modify( value );
    }

    /**
     * Modify this option using a boolean value.
     *
     * @param     value A boolean value.
     */

    public void setValue( boolean value ) {
	this.value = value;
    }

    /**
     * Return this option as a boolean.
     *
     * @return This option as a boolean.
     */

    public boolean getValue() {
	return value;
    }

    /**
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getStringValue() {
	return ( value ) ? "TRUE" : "FALSE";
    }

    /**
     * Returns the type name of this option. For a BooleanOption, "BOOLEAN"
     * is returned.
     *
     * @return The type name of this option.
     */

    public String getTypeName() {
	return "BOOLEAN";
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */

    public String toString() {
	return getStringValue();
    }

} /** BooleanOption */
