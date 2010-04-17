package gnu.dtools.ritopt;

/**
 * OptionMenu.java
 *
 * Version:
 *    $Id$
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class facilitates the built-in menu feature for ritopt. When the
 * --menu long option is invoked, an instance of this class is notified,
 * and the menu system starts.
 *
 * Here is an example run of the built-in menu.
 * <pre>
 * -&gt; ?
 *         - Options Delimiter
 *         ? Help
 *         = Run program and return to menu
 *         ! Shell to Operating System
 *         $ Exit menu
 *         + Additional options
 *         &#64;&lt;filename&gt; Get options from file [default.opt]
 *         &#64;&#64; Get options from file [default.opt]
 *         %&lt;filename&gt; Put options in file
 *         %% Put options in file [default.opt]
 *         . Quit
 * -&gt; =ls
 * -&gt; =uname
 * CYGWIN_ME-4.90
 * Exit status: 0
 * Press enter to continue...
 *
 * -&gt; =ls
 * CVS
 * Makefile
 * Makefile.am
 * Makefile.in
 * edu
 * gnu
 * ritopt.jar
 * Exit status: 0
 * Press enter to continue..
 *
 * -&gt; --help
 * java FavoriteFood @optionfile [module] OPTIONS ... [module] OPTIONS
 *
 * Use --menu to invoke the interactive built-in menu.
 *
 * Option Name     Type       Description
 *
 * -h, --help      &lt;NOTIFY&gt;   Displays help for each option.
 * -m, --menu      &lt;NOTIFY&gt;   Displays the built-in interactive menu.
 *     --fatfree   &lt;BOOLEAN&gt;  No description given
 * -v, --version   &lt;NOTIFY&gt;   Displays version information.
 *     --name      &lt;STRING&gt;   No description given
 * -g, --grub      &lt;STRING&gt;   Favorite Food
 * -f, --food      &lt;STRING&gt;   Favorite Food
 * -c, --food-coun &lt;STRING&gt;   No description given
 *
 * -&gt; --grub=tacos
 * Warning: --grub or -g is deprecated.
 * -&gt; --grubb
 * Error: Option --grubb does not exist in module 'General'.
 * -&gt; .
 *
 * </pre>
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

public class OptionMenu implements OptionListener {

    /**
     * Commands starting with this substring are option invocations.
     */

    public static final String OPTION_COMMAND_CHAR = "-";

    /**
     * This command without any arguments displays the command summary.
     * Otherwise, specific help information is provided based on the
     * argument.
     */

    public static final String HELP_COMMAND_CHAR = "?";

    /**
     * This command must have an argument. When invoked, it executes
     * the argument at the command shell.
     */

    public static final String RUN_COMMAND_CHAR = "=";

    /**
     * This command spawns a shell.
     */

    public static final String SHELL_COMMAND_CHAR = "!";

    /**
     * This command exits the built-in menu system.
     */

    public static final String EXIT_MENU_COMMAND_CHAR = "$";

    /**
     * This command lists registered option modules.
     */

    public static final String LIST_MODULES_COMMAND_CHAR = "+";

    /**
     * This command without any arguments loads the default option file.
     * Otherwise, the file as the argument is loaded.
     */

    public static final String FILE_READ_COMMAND_CHAR = "@";

    /**
     * This command without any arguments writes to the default option file.
     * Otherwise, the file as the argument is written.
     */

    public static final String FILE_WRITE_COMMAND_CHAR = "%";

    /**
     * Loads an option module. Invoking this command without any arguments,
     * the default option module is loaded.
     */

    public static final String FILE_MODULE_COMMAND_CHAR = ":";

    /**
     * The menu prompt used for the built-in menu system.
     */

    public static final String MENU_PROMPT = "-> ";

    /**
     * The options registrar associated with this option menu.
     */

    private Options options;

    /**
     * A reader which is connected to standard input.
     */

    private BufferedReader reader;

    /**
     * Constructs a new built-in menu attaching it to the options registrar
     * passed.
     *
     * @param options The option registrar associated with this built-in
     *                menu.
     */

    public OptionMenu( Options options ) {
	this.options = options;
	reader = new BufferedReader( new InputStreamReader( System.in ) );
    }

    /**
     * Starts the built-in menu system.
     */

    public void startMenu() {
	String command = "";
	while ( !command.equals( "$" ) ) {
	    System.out.print( MENU_PROMPT );
	    try {
		command = reader.readLine();
	    }
	    catch ( IOException e ) {
		return;
	    }
	    boolean commandEntered = command != null && command.length() > 0;
	    if (commandEntered && command.equals( "?" ) ) {
		System.err.println( "\t- Options Delimiter" );
		System.err.println( "\t? Help" );
		System.err.println( "\t= Run program and return to menu" );
		System.err.println( "\t! Shell to Operating System" );
		System.err.println( "\t$ Exit menu" );
		System.err.println( "\t+ Additional options" );
		System.err.println( "\t@<filename> Get options from file ["
				    + options.getDefaultOptionFilename()
				    + "]" );
		System.err.println( "\t@@ Get options from file ["
				    + options.getDefaultOptionFilename()
				    + "]" );
                System.err.println( "\t%<filename> Put options in file" );
		System.err.println( "\t%% Put options in file ["
				    + options.getDefaultOptionFilename()
				    + "]" );
                System.err.println( "\t. Quit" );
	    }
	    else if ( commandEntered &&
		      ( command.substring( 0, 1 ).equals(
					    FILE_READ_COMMAND_CHAR )
		      || command.substring( 0, 1 ).equals(
					    FILE_WRITE_COMMAND_CHAR )
		      || command.substring( 0, 1 ).equals(
                                            OPTION_COMMAND_CHAR )
		      || command.substring( 0, 1 ).equals(
                                            FILE_MODULE_COMMAND_CHAR ) ) ) {
		options.process( command );
	    }
	    else if ( commandEntered &&
		      command.substring( 0, 1 ).equals( SHELL_COMMAND_CHAR ) ) {

	    }
	    else if ( commandEntered &&
		      command.substring( 0, 1 ).equals( RUN_COMMAND_CHAR ) ) {
		try {
		    SimpleProcess p
			= new SimpleProcess( Runtime.getRuntime().exec( command.substring( 1 ) ) );
		    System.err.println( "Exit status: " + p.waitFor() );
		}
		catch ( Exception e ) {
		    System.err.println( "ritopt: An Error Occurred During Process Execution" );
		    e.printStackTrace();
		}
		finally {
		    System.out.println( "Press enter to continue..." );
		    try {
			reader.readLine();
		    } catch ( IOException e ) { }
		}
	    }
	    else {
		System.err.println( "(Type ? for Help)" );
	    }
	}
    }

    /**
     * This method is notified when the --menu option is specified.
     *
     * @param event    The event associated.
     */

    public void optionInvoked( OptionEvent event ) {
	if ( event.getCommand().equals( "menu" ) ) {
	    startMenu();
	}
    }
} /** OptionMenu **/
