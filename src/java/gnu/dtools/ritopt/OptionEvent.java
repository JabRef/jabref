package gnu.dtools.ritopt;
/**
 * OptionEvent.java
 *
 * Version:
 *    $Id$
 */

/**
 * An event indicating that an option has been invoked.
 * When an OptionListener is notified by a NotifyOption, it passes
 * an OptionEvent object to all registered listeners. This includes
 * the target NotifyOption, a command (NotifyOption passes the long
 * option by default), and the option value.<p>
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

public class OptionEvent {

    /**
     * The command string associated with this option processing event.
     */

    private String command;

    /**
     * The value of the option processing event.
     */

    private String value;

    /**
     * The target Option in which the option processing event occurred.
     */

    private Option target;

    /**
     * Constructs an option event with the command set to "Default", the
     * value set to the empty string, and the target set to null.
     */

    public OptionEvent() {
	this( "Default", "", null );
    }

    /**
     * Constructs an option event with the command set to the value passed,
     * the value set to the empty string, and the target set to null.
     *
     * @param command The value to set the command string.
     */

    public OptionEvent( String command ) {
	this( command, "", null );
    }

    /**
     * Constructs an option event with the command and value set to the
     * values passed, and the target set to null.
     *
     * @param command The value to set the command string.
     * @param value   The value to set the option value.
     */

    public OptionEvent( String command, String value ) {
	this( command, value, null );
    }

    /**
     * Constructs an option event with the command set to the long or short
     * option (whichever exists), the value set to the current value of
     * the option, and the target option set to the option passed. If
     * neither the short or long option exist, a value of "Default" is
     * assigned.
     *
     * @param option The option to initialize this OptionEvent.
     */

    public OptionEvent( Option option ) {
	this.target = option;
	this.value = option.getStringValue();
	String longOption = option.getLongOption();
	char shortOption = option.getShortOption();
						      
        if ( longOption != null ) {
	    command = longOption;
	}
	else if ( shortOption != '\0' ) {
	    command = new Character( shortOption ).toString();
	}
	else {
	    command = "Default";
	}
    }

    /**
     * Constructs an option event with the command, value, and target
     * set to the values passed.
     *
     * @param command The value to set the command string.
     * @param value   The value to set the option value.
     * @param target  The target option in which the option processing
     *                event occurred.
     */

    public OptionEvent( String command, String value, Option target ) {
	this.command = command;
	this.value = value;
	this.target = target;
    }

    /**
     * Returns the command string associated with the option.
     *
     * @return The command string associated with the option.
     */

    public String getCommand() {
	return command;
    }

    /**
     * Returns the value associated with the target option.
     *
     * @return The value associated with the target option.
     */

    public String getValue() {
	return value;
    }

    /**
     * Returns the target option of the option processing event.
     *
     * @return The target option.
     */

    public Option getTarget() {
	return target;
    }

    /**
     * Sets the command string to the value passed.
     *
     * @param command The value to set the command string.
     */

    public void setCommand( String command ) {
	this.command = command;
    }

    /**
     * Sets the value of this option event. This value generally should be
     * equal to the value of the target option.
     *
     * @param value   The value of the option event.
     */

    public void setValue( String value ) {
	this.value = value;
    }

    /**
     * Sets the target option of the option processing event.
     *
     * @param target   The target option.
     */

    public void setTarget( Option target ) {
	this.target = target;
    }

}
