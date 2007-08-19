package gnu.dtools.ritopt;

/**
 * OptionModule.java
 *
 * Version:
 *    $Id$
 */

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is used as a repository for options. The Options class maintains
 * an OptionModule repository for general options. The user may create option
 * modules so that their options can overlap and be categorized. Option
 * modules are invoked by specifying the option name delimited with square
 * brackets.<p>
 *
 * For example, suppose we are writing a program called ServerManager
 * that manages both an ftp and http server. One option that both a ftp
 * and http kernel might have in common is the number of seconds before
 * a request times out. Option modules are used to process two different
 * values with the same option name. The shell command below demonstrates
 * how two different modules are invoked.<p>
 * <pre>
 *  java ServerManager :http: --timeout=15 :ftp: --timeout=25
 * </pre>
 *
 * Refer to the tutorial for more information on how to use option modules.
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

public class OptionModule implements OptionRegistrar {

    /**
     * A repository of options registered with this module.
     */

    private HashMap<String, Option> options;

    /**
     * The name of this module.
     */

    private String name;

    /**
     * Returns whether this module is deprecated.
     */

    private boolean deprecated;

    /**
     * The default short option.
     */

    public static final char DEFAULT_SHORT_OPTION = '\0';

    /**
     * The default long option.
     */

    public static final String DEFAULT_LONG_OPTION = null;

    /**
     * The default description.
     */

    public static final String DEFAULT_DESCRIPTION = "No description given";

    /**
     * The default deprecation status.
     */

    public static final boolean DEFAULT_DEPRECATED = false;

    /**
     * The default module name.
     */

    public static final String DEFAULT_MODULE_NAME = "Special";

    /**
     * Constructs an OptionModule with the default name.
     */

    public OptionModule() {
	this( DEFAULT_MODULE_NAME );
    }

    /**
     * Constructs an OptionModule with the name passed.
     *
     * @param name  The name of the module.
     */

    public OptionModule( String name ) {
	options = new HashMap<String, Option>();
	this.name = name;
	deprecated = false;
    }

    /**
     * Register an option into the repository as a long option.
     *
     * @param longOption  The long option name.
     * @param option      The option to register.
     */

    public void register( String longOption, Option option ) {
	register( longOption, DEFAULT_SHORT_OPTION, option );
    }

    /**
     * Register an option into the repository as a short option.
     *
     * @param shortOption The short option name.
     * @param option      The option to register.
     */

    public void register( char shortOption, Option option ) {
	register( DEFAULT_LONG_OPTION, shortOption, option );

    }

    /**
     * Register an option into the repository both as a short and long option.
     *
     * @param longOption  The long option name.
     * @param shortOption The short option name.
     * @param option      The option to register.
     */

    public void register( String longOption, char shortOption,
			  Option option ) {
	register( longOption, shortOption, DEFAULT_DESCRIPTION, option );
    }

    /**
     * Register an option into the repository both as a short and long option.
     * Initialize its description with the description passed.
     *
     * @param longOption  The long option name.
     * @param shortOption The short option name.
     * @param description The description of the option.
     * @param option      The option to register.
     */

    public void register( String longOption, char shortOption,
			  String description, Option option ) {
	register( longOption, shortOption, description, option,
	      DEFAULT_DEPRECATED );
    }

    /**
     * Register an option into the repository both as a short and long option.
     * Initialize its description with the description passed.
     *
     * @param longOption  The long option name.
     * @param shortOption The short option name.
     * @param description The description of the option.
     * @param option      The option to register.
     * @param deprecated  A boolean indicating whether an option should
     *                    be deprecated.
     */

    public void register( String longOption, char shortOption,
			  String description, Option option,
			  boolean deprecated ) {
	if ( optionExists( option ) ) {
	    throw new OptionRegistrationException( "Option Already Registered",
						   option );
	}
	option.setLongOption( longOption );
	option.setShortOption( shortOption );
	option.setDeprecated( deprecated );
	option.setDescription( description );
	options.put( option.getHashKey(), option );
    }

    /**
     * Returns whether the option exists in this module.
     *
     * @param option   The option to check for existance.
     *
     * @return A boolean value indicating whether the option passed exists.
     */

    public boolean optionExists( Option option ) {
	return optionExists( option.getShortOption() ) ||
	       optionExists( option.getLongOption() );
    }

    /**
     * Returns whether the option referred by a short option exists in this
     * module.
     *
     * @param shortOption   The option to check for existance.
     *
     * @return A boolean value indicating whether the option passed exists.
     */
	public boolean optionExists(char shortOption) {
		for (Option next : options.values()) {
			char c = next.getShortOption();
			if (c != 0 && c == shortOption)
				return true;
		}
		return false;
	}

    /**
     * Returns whether the option referred by a long option exists in this
     * module.
     *
     * @param longOption   The option to check for existance.
     *
     * @return A boolean value indicating whether the option passed exists.
     */
	public boolean optionExists(String longOption) {
		for (Option next : options.values()) {
			String s = next.getLongOption();
			if (s != null && s.equals(longOption))
				return true;
		}
		return false;
	}

    /**
	 * Return an iterator over the option repository contained in this module.
	 * 
	 * @return An iterator over the repository.
	 */

	public Iterator<Option> getOptionIterator() {
		return options.values().iterator();
	}

    /**
	 * Returns the option referred by the long option passed.
	 * 
	 * @param shortOption
	 *            The option to retrieve.
	 * 
	 * @return An option referred to by this module. null is returned if it does
	 *         not exist.
	 */

    public Option getOption(char shortOption) {
		Option retval = null;

		for (Option next : options.values()) {
			char c = next.getShortOption();
			if (c != '\0' && c == shortOption)
				retval = next;
		}
		return retval;
	}

    /**
     * Returns the option referred by the long option passed.
     *
     * @param longOption The option to retrieve.
     *
     * @return An option referred to by this module. null is returned
     *         if it does not exist.
     */

    public Option getOption(String longOption) {
		Option retval = null;
		for (Option next : options.values()) {
			String s = next.getLongOption();
			if (s != null && s.equals(longOption))
				retval = next;
		}
		return retval;
	}

    /**
     * Returns the help information as a String.
     *
     * @return The help information as a String.
     */

    public String getHelp() {
		String retval = "";
		for (Option next : options.values()) {
			retval += next.getHelp() + "\n";
		}
		return retval;
	}

    /**
	 * Writes the help information to a print stream.
	 * 
	 * @param ps
	 *            The print stream to write to.
	 */

	public void writeFileToPrintStream(PrintStream ps) {
		ps.println(":" + name + ":");
		for (Option next : options.values()) {
			ps.println(next.getOptionFileLine());
		}
	}

    /**
	 * Returns whether this module is deprecated.
	 * 
	 * @return A boolean value indicating whether this module is deprecated.
	 */

    public boolean isDeprecated() {
	return deprecated;
    }

    /**
     * Sets whether this module is deprecated.
     *
     * @param deprecated The new status.
     */

    public void setDeprecated( boolean deprecated ) {
	this.deprecated = deprecated;
    }

    /**
     * Called by the OptionsProcessor when an option in the target module
     * is invoked.
     *
     * @param shortOption The option to invoke.
     * @param text        The text to pass to the modifier.
     */

    public void action( char shortOption, char text ) {
	action( shortOption, "" + text );
    }

    /**
     * Called by the OptionsProcessor when an option in the target module
     * is invoked.
     *
     * @param longOption The option to invoke.
     * @param text       The text to pass to the modifier.
     */

    public void action( String longOption, char text ) {
	action( longOption, "" + text );
    }

    /**
     * Called by the OptionsProcessor when an option in the target module
     * is invoked.
     *
     * @param shortOption The option to invoke.
     * @param text        The text to pass to the modifier.
     */

    public void action( char shortOption, String text ) {
	Option op = getOption( shortOption );
	if ( op == null )
	    throw new OptionProcessingException( "Option -" + shortOption +
						 " does not"
						 + " exist in module '"
						 + name + "'." );
	op.setInvoked( true );
	op.action();
	op.modify( text );
    }

    /**
     * Called by the OptionsProcessor when an option in the target module
     * is invoked.
     *
     * @param longOption The option to invoke.
     * @param text       The text to pass to the modifier.
     */


    public void action( String longOption, String text ) {
	Option op = getOption( longOption );
	if ( op == null )
	    throw new OptionProcessingException( "Option --" + longOption +
						 " does not"
						 + " exist in module '"
						 + name + "'." );
	op.setInvoked( true );
	op.action();
	op.modify( text );
    }

    /**
     * Set the name of this module.
     *
     * @param name   The new name.
     */

    public void setName( String name ) {
	this.name = name;
    }

    /**
     * Returns the name of this module.
     *
     * @return The name of this module.
     */

    public String getName() {
	return name;
    }

} /** OptionModule **/




