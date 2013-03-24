package gnu.dtools.ritopt;

/**
 * The NotifyOption class is used to register options that when invoked
 * notify a listener. This provides an interface for event-driven
 * options processing. In order for a class to be notified, it must implement
 * the OptionListener interface.<p>
 *
 * When the option is invoked, the corresponding short, long, and option
 * values are put in an OptionEvent object, and passed to all registered
 * listeners.<p>
 *
 * A class must implement the OptionListener interface in order to receive
 * notification of option events.<p>
 *
 * For a more detailed explanation please refer to the tutorial. The following
 * is a simple example of how a NotifyOption is used.
 * <pre>
 *
 * import gnu.dtools.ritopt.*;
 *
 * public class TellMe implements OptionListener {
 *
 *    public static void main( String args[] ) {
 *       TellMe m = new TellMe();
 *       Options processor = new Options();
 *       NotifyOption say = new NotifyOption( m );
 *       processor.register( "say", 's', say );
 *       processor.process();
 *    }
 *
 *    public void optionInvoked( OptionEvent e ) {
 *       if ( e.getCommand().equals( "say" ) ) {
 *           String say = e.getValue();
 *           if ( Utility.trim( say ).length() == 0 ) say = "nothing";
 *           System.err.println( "You told me to say " + nothing + "." );
 *       }
 *    }
 * }
 *
 * cookies@crazymonster$ javac TellMe.java
 * cookies@crazymonster$ java TellMe
 * cookies@crazymonster$ java TellMe
 * cookies@crazymonster$ java TellMe --say -s
 * You told me to say nothing.
 * You told me to say nothing.
 * cookies@crazymonster$ java TellMe --say hello
 * You told me to say hello.
 * cookies@crazymonster$ java TellMe --say "I'm sorry"
 * You told me to say I'm sorry.
 * cookies@crazymonster$ java TellMe --say="not until tomorrow" -s "I'm crazy"
 * You told me to say not until tomorrow.
 * You told me to say I'm crazy.
 * </pre>
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

public class NotifyOption extends Option implements OptionNotifier {

    /**
     * The current value of the notify option.
     */

    private String value = "";

    /**
     * The default command if a command is not specified.
     */

    private String command = "Default";

    /**
     * A list of listeners to notify whenever a modification event occurs.
     */

    private java.util.List<OptionListener> listeners;

    /**
     * Construct a NotifyOption with an empty list of listeners. Set the
     * initial value to null.
     */

    public NotifyOption() {
	this( "" );
    }

    /**
     * Construct a NotifyOption and register the passed listener.
     *
     * @param listener    The listener to register.
     */

    public NotifyOption( OptionListener listener ) {
	this( listener, "Default" );
    }

    /**
     * Construct a NotifyOption and register the passed listener. Initialize
     * the command to the value passed.
     *
     * @param listener    The listener to register.
     * @param command     The value of the command.
     */

    public NotifyOption( OptionListener listener, String command ) {
	this( listener, command, "" );
    }

    /**
     * Construct a NotifyOption and register the passed listener. Initialize
     * the command to the value passed.
     *
     * @param listener    The listener to register.
     * @param command     The value of the command.
     * @param value       The default value of the option.
     */

    public NotifyOption( OptionListener listener, String command,
			 String value ) {
	this( value );
	this.command = command;
	listeners.add( listener );
    }

    /**
     * Construct a NotifyOption by copying the NotifyOption passed.
     *
     * @param op   The notify option to copy.
     */

    public NotifyOption( NotifyOption op ) {
	super( op );
	op.value = op.getValue();
	listeners = new java.util.ArrayList<OptionListener>( op.listeners );
    }

    /**
     * Construct a NotifyOption, and initialize its default value to the
     * value passed.
     *
     * @param value   The default value of this option.
     */

    public NotifyOption( String value ) {
	this( value, null );
    }

    /**
     * Constructs a NotifyOption option initialized with the value and
     * long option passed.
     *
     * @param value      The initial value of this notify option.
     * @param longOption The long option associated with this notify option.
     */

    public NotifyOption( String value, String longOption ) {
	this( value, longOption, '\0' );
    }

    /**
     * Constructs a character option initialized with the value and
     * short option passed.
     *
     * @param value       The initial value of this NotifyOption option.
     * @param shortOption The short option associated with this option.
     */

    public NotifyOption( String value, char shortOption ) {
	this( value, null, shortOption );
    }

    /**
     * Constructs an NotifyOption option initialized with the value, short
     * and long option passed.
     *
     * @param shortOption The short option associated with this option.
     * @param longOption  The long option associated with this option.
     * @param value       The initial value of this NotifyOption option.
     */

    public NotifyOption( String value, String longOption, char shortOption ) {
	super( longOption, shortOption );
	this.value = value;
	listeners = new java.util.ArrayList<OptionListener>();
    }

    /**
     * Return the value as an object.
     *
     * @return This value as an option.
     */

    public Object getObject() {
	return value;
    }

    /**
     * Modify this option based on a string representation.
     *
     * @param     value String representation of the object.
     * @exception OptionModificationException Thrown if an error occurs
     *                                  during modification of an option.
     */

    public void modify( String value ) throws OptionModificationException {
	this.value = value;
	java.util.Iterator<OptionListener> iterator = listeners.iterator();
	OptionEvent event = new OptionEvent( command, value, this );
	while ( iterator.hasNext() ) {
	    OptionListener listener = iterator.next();
	    listener.optionInvoked( event );
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
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getValue() {
	return value;
    }

    /**
     * Return this option as a string.
     *
     * @return This option as a string.
     */

    public String getStringValue() {
	return value;
    }

    /**
     * Returns the type name of this option. For an NotifyOption, "NOTIFY"
     * is returned.
     *
     * @return The type name of this option.
     */

    public String getTypeName() {
	return "NOTIFY";
    }

    /**
     * Adds an OptionListener to the notification list.
     *
     * @param listener The OptionListener to add.
     */

    public void addOptionListener( OptionListener listener ) {
	listeners.add( listener );
    }

    /**
     * Removes an OptionListener from the notification list.
     *
     * @param listener The OptionListener to remove.
     */

    public void removeOptionListener( OptionListener listener ) {
	listeners.remove( listener );
    }

    /**
     * Sets the command sent when an option is invoked.
     *
     * @param command  The command to send.
     */

    public void setOptionCommand( String command ) {
	this.command = command;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */

    public String toString() {
	return value;
    }
} /** NotifyOption */
