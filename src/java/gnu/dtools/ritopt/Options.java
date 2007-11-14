package gnu.dtools.ritopt;

/**
 * Options.java
 *
 * Version:
 *    $Id$

 */

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.jabref.Globals;

/**
 * This class functions as a repository for options and their modules. It
 * facilitates registration of options and modules, as well as processing of
 * arguments.<p>
 *
 * Information such as help, usage, and versions are displayed
 * when the respective --help and --version options are specified.
 * The --menu option will invoke the built-in menu.<p>
 *
 * In the example below, the program processes three simple options.
 *
 * <pre>
 * public class AboutMe {
 *
 *    private static StringOption name = new StringOption( "Ryan" );
 *    private static IntOption age = new IntOption( 19 );
 *    private static DoubleOption bankBalance = new DoubleOption( 15.15 );
 *
 *    public static void main( String args[] ) {
 *       Options repo = new Options( "java AboutMe" );
 *       repo.register( "name", 'n', name, "The person's name." );
 *       repo.register( "age", 'a', age, "The person's age." );
 *       repo.register( "balance", 'b', "The person's bank balance.",
 *                       bankBalance );
 *       repo.process( args );
g *       System.err.println( "" + name + ", age " + age + " has a " +
 *                           " bank balance of " + bankBalance + "." );
 *    }
 * }
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
 *
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

public class Options implements OptionRegistrar, OptionModuleRegistrar,
                                OptionListener {

    /**
     * The default verbosity.
     */

    public static final int DEFAULT_VERBOSITY = 3;

    /**
     * This boolean defines whether options are deprecated by default.
     */

    public static final boolean DEFAULT_DEPRECATED = false;

    /**
     * The default reason for deprecation.
     */

    public static final String DEFAULT_REASON = "No reason given.";

    /**
     * The default general module name.
     */

    public static final String DEFAULT_GENERAL_MODULE_NAME = "General";

    /**
     * This boolean defines whether usage should be displayed.
     */

    public static final boolean DEFAULT_DISPLAY_USAGE = false; // Mod. Morten A.

    /**
     * This boolean defines whether the menu should be used.
     */

    public static final boolean DEFAULT_USE_MENU = false; // Mod. Morten A.

    /**
     * The default program name that is display in the usage.
     */

    public static final String DEFAULT_PROGRAM_NAME = "java program";

    /**
     * The default option file.
     */

    public static final String DEFAULT_OPTION_FILENAME = "default.opt";

    /**
     * The program to display in the usage.
     */

    private String usageProgram;

    /**
     * The version to display in the usage.
     */

    private String version;

    /**
     * The default option filename if an option file is not specified.
     */

    private String defaultOptionFilename;

    /**
     * This flag defines whether to display usage when help is displayed.
     */

    private boolean displayUsage;

    /**
     * This boolean defines whether the menu should be used.
     */

    private boolean useMenu;

    /**
     * When this flag is true, debugging information is displayed.
     */

    private boolean debugFlag;

    /**
     * The current module being processed.
     */

    private OptionModule currentModule;

    /**
     * The general option module.
     */

    private OptionModule generalModule;

    /**
     * A map of option modules.
     */

    private java.util.HashMap<String, OptionModule> modules;

    /**
     * Version information is displayed when this option is specified.
     */

    private NotifyOption versionOption;

    /**
     * Create an option repository.
     */

    public Options() {
        this( DEFAULT_PROGRAM_NAME );
    }

    /**
     * Create an option repository and associated it with a program name.
     *
     * @param programName A program name like "java Balloons".
     */

    public Options( String programName ) {
        displayUsage = DEFAULT_DISPLAY_USAGE;
        useMenu = DEFAULT_USE_MENU;
        defaultOptionFilename = DEFAULT_OPTION_FILENAME;
        usageProgram = programName;
        modules = new HashMap<String, OptionModule>();
        versionOption = new NotifyOption( this, "version", "" );
        version = "Version 1.0";
        generalModule = new OptionModule( DEFAULT_GENERAL_MODULE_NAME );
        currentModule = generalModule;

        // Mod. Morten A. ------------------------------------------------
        register( "version", 'v',
                  "Displays version information.", versionOption );
        /*register( "help", 'h', "Displays help for each option.", helpOption );
        register( "menu", 'm', "Displays the built-in interactive menu.",
                  menuOption );*/
        // End mod. Morten A. ------------------------------------------------
    }

    /**
     * Returns the help information as a string.
     *
     * @return The help information.
     */

    public String getHelp() {
        String retval = (displayUsage ? getUsage() + "\n\n" : "" ) +
            // Mod. Morten A.
            //"Use --menu to invoke the interactive built-in menu.\n\n" +
            Option.getHelpHeader() + "\n\n" + generalModule.getHelp();
        Iterator<OptionModule> it = modules.values().iterator();
        while ( it.hasNext() ) {
            OptionModule module = it.next();
            retval += "\n\nOption Listing for " + module.getName() + "\n";
            retval += module.getHelp() + "\n";
        }
        return retval;
    }

    /**
     * Returns usage information of this program.
     *
     * @return The usage information.
     */

    public String getUsage() {
        return getUsageProgram()
            + " @optionfile :module: OPTIONS ... :module: OPTIONS";
    }

    /**
     * Returns the program name displayed in the usage.
     *
     * @returns The program name.
     */

    public String getUsageProgram() {
        return usageProgram;
    }

    /**
     * Returns the version of the program.
     *
     * @returns The version.
     */

    public String getVersion() {
        return version;
    }

    /**
     * Returns the option filename to load or write to if one is not
     * specified.
     *
     * @return The default option filename.
     */

    public String getDefaultOptionFilename() {
        return defaultOptionFilename;
    }

    /**
     * Returns whether debugging information should be displayed.
     *
     * @return A boolean indicating whether to display help information.
     */

    public boolean getDebugFlag() {
        return debugFlag;
    }

    /**
     * Returns whether the help information should display usage.
     *
     * @return A boolean indicating whether help should display usage.
     */

    public boolean shouldDisplayUsage() {
        return displayUsage;
    }

    /**
     * Returns whether the built-in menu system can be invoked.
     *
     * @return A boolean indicating whether the build-in menu system
     *         can be invoked.
     */

    public boolean shouldUseMenu() {
        return useMenu;
    }

    /**
     * Sets whether usage can be displayed.
     *
     * @param b     A boolean value indicating that usage can be displayed.
     */

    public void setDisplayUsage( boolean b ) {
        displayUsage = b;
    }

    /**
     * Sets whether the built-in menu system can be used.
     *
     * @param b      A boolean value indicating whether the built-in menu
     *               system can be used.
     */

    public void setUseMenu( boolean b ) {
        useMenu = b;
    }

    /**
     * Sets the program to display when the usage is displayed.
     *
     * @param program The program displayed during usage.
     */

    public void setUsageProgram( String program ) {
        usageProgram = program;
    }

    /**
     * Sets the version of the program.
     *
     * @param version The version.
     */

    public void setVersion( String version ) {
        this.version = version;
    }

    /**
     * Sets the option file to use when an option file is not specified.
     *
     * @param fn      The filename of the default option file.
     */

    public void setDefaultOptionFilename( String fn ) {
        defaultOptionFilename = fn;
    }


    /**
     * Displays the program's help which includes a description of each
     * option. The usage is display if the usage flag is set to true.
     */

    public void displayHelp() {
        System.err.println( getHelp() );
    }

    /**
     * Displays the version of the program.
     */

    public void displayVersion() {
        System.err.println( getVersion() +" (build " +Globals.BUILD +")");
    }

    /**
     * Register an option into the repository as a long option.
     *
     * @param longOption  The long option name.
     * @param option      The option to register.
     */

    public void register( String longOption, Option option ) {
        generalModule.register( longOption, option );
    }

    /**
     * Register an option into the repository as a short option.
     *
     * @param shortOption The short option name.
     * @param option      The option to register.
     */

    public void register( char shortOption, Option option ) {
        generalModule.register( shortOption, option );
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
        generalModule.register( longOption, shortOption, option );
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
        generalModule.register( longOption, shortOption, description, option );
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
        generalModule.register( longOption, shortOption, description, option,
                                deprecated );
    }

    /**
     * Register an option module based on its name.
     *
     * @param module The option module to register.
     */

    public void register( OptionModule module ) {
        register( module.getName(), module );
    }

    /**
     * Register an option module and associate it with the name passed.
     *
     * @param name   The name associated with the option module.
     * @param module The option module to register.
     */

    public void register( String name, OptionModule module ) {
        modules.put( name.toLowerCase(), module );
    }

    /**
     * Process a string of values representing the invoked options. After
     * all the options are processed, any leftover arguments are returned.
     *
     * @param args The arguments to process.
     *
     * @return The leftover arguments.
     */

    public String[] process( String args[] )
    {
        String []retval = new String[0];
        try {
            retval = processOptions( args );
        }
        catch ( OptionException e ) {
            System.err.println( "Error: " + e.getMessage() );
        }
        /**
        catch ( Exception e ) {
            System.err.println( "Error: Unexpected Error in ritopt Processing." +
                                "Check syntax." );
                                }**/
        return retval;
    }

    /**
     * Retrieves an option module based on the name passed.
     *
     * @param name The name referring to the option module.
     *
     * @return The option module. Null is returned if the module does not
     *         exist.
     */

    public OptionModule getModule( String name ) {
        return modules.get( name.toLowerCase() );
    }

    /**
     * Returns a boolean indicating whether an option module exists.
     *
     * @param name The name referring to the option module.
     *
     * @return A boolean value indicating whether the module exists.
     */

    public boolean moduleExists( String name ) {
        return getModule( name ) != null;
    }

    /**
     * Receives NotifyOption events. If the event command equals "help"
     * or "version", the appropriate display methods are invoked.
     *
     * @param event         The event object containing information about the
     *                      invocation.
     */

    public void optionInvoked( OptionEvent event ) {
        if ( event.getCommand().equals( "help" ) ) {
            displayHelp();
        }
        else if ( event.getCommand().equals( "version" ) ) {
            displayVersion();
        }
    }

    /**
     * Process a string representing the invoked options. This string
     * gets split according to how they would be split when passed to
     * a main method. The split array of options gets passed to a
     * private method for processing. After all the options are processed,
     * any leftover arguments are returned.
     *
     * @param str The arguments to process.
     *
     * @return The leftover arguments.
     */

    public String[] process( String str ) {
        return process( split( str ) );
    }

    /**
     * Splits a string representing command line arguments into several
     * strings.
     *
     * @param str   The string to split.
     *
     * @return  The splitted string.
     */

    public String[] split( String str ) {
        StringBuffer buf = new StringBuffer( str.length() );
        java.util.List<String> l = new java.util.ArrayList<String>();
        int scnt = Utility.count( str, '"' );
        boolean q = false;
        if ( (scnt) / 2.0 != (scnt / 2) ) {
            throw new OptionProcessingException( "Expecting an end quote." );
        }
        for ( int n = 0; n < str.length(); n++ ) {
            if ( str.charAt( n ) == '"' ) {
                q = !q;
            }
            else if ( str.charAt( n ) == ' ' && !q ) {
                l.add( buf.toString() );
                buf = new StringBuffer( str.length() );
            }
            else {
                buf.append( str.charAt( n ) );
            }
        }
        if ( buf.length() != 0 ) {
            l.add( buf.toString() );
        }
        Iterator<String> it = l.iterator();
        String retval[] = new String[ l.size() ];
        int n = 0;
        while ( it.hasNext() ) {
            retval[ n++ ] = it.next();
        }
        return retval;
    }

    /**
     * Writes all options and their modules out to an options file.
     *
     * @param filename  The options filename to write.
     */

    public void writeOptionFile( String filename ) {
        BufferedOutputStream writer = null;
        Iterator<OptionModule> it = null;
        currentModule = generalModule;
        try {
            writer =
                new BufferedOutputStream( new FileOutputStream( filename ) );
            PrintStream ps = new PrintStream( writer );
            generalModule.writeFileToPrintStream( ps );
            it = modules.values().iterator();
            while ( it.hasNext() ) {
                OptionModule module = it.next();
                module.writeFileToPrintStream( ps );
            }
        }
        catch ( IOException e ) {
            throw new OptionProcessingException( e.getMessage() );
        }
        finally {
            try {
                if ( writer != null )
                    writer.close();
            }
            catch( IOException e ) {
                throw new OptionProcessingException( e.getMessage() );
            }
        }
    }

    /**
     * Loads all options and their modules from an options file.
     *
     * @param filename  The options filename to write.
     */

    public void loadOptionFile( String filename ) {
        BufferedReader reader = null;
        String line = null;
        currentModule = generalModule;
        try {
            reader = new BufferedReader( new FileReader( filename ) );
            while ( ( line = reader.readLine() ) != null ) {
                line = Utility.stripComments( line, '\"', ';' );
                process( line );
            }
        }
        catch ( IOException e ) {
            throw new OptionProcessingException( e.getMessage() );
        }
        finally {
            try {
                if ( reader != null )
                    reader.close();
            }
            catch( IOException e ) {
                throw new OptionProcessingException( e.getMessage() );
            }
        }
    }

    /**
     * Processes an array of strings representing command line arguments.
     *
     * @param args arguments to process.
     *
     * @return The leftover arguments.
     */

    private String[] processOptions( String args[] ) {
        String retval[] = null;
        String moduleName = "general";
        String optionFile = "";
        char shortOption = '\0';
        String longOption = "";
        for ( int n = 0; n < args.length && retval == null; n++ ) {
            boolean moduleInvoked = false;
            boolean shortOptionInvoked = false;
            boolean longOptionInvoked = false;
            boolean readOptionFileInvoked = false;
            boolean writeOptionFileInvoked = false;
            if ( args[ n ].length() >= 1 ) {
                char fc = args[ n ].charAt( 0 );
                moduleInvoked = fc == ':';
                readOptionFileInvoked = fc == '@';
                writeOptionFileInvoked = fc == '%';
            }
            if ( args[ n ].length() >= 2 ) {
                String s = args[ n ].substring( 0, 2 );
                shortOptionInvoked = ( !s.equals( "--" ) &&
                           s.charAt( 0 ) == '-' );
                longOptionInvoked = ( s.equals( "--" ) );
            }
            if ( debugFlag ) {
                System.err.println( "Short Option: " + shortOptionInvoked );
                System.err.println( "Long Option: " + longOptionInvoked );
                System.err.println( "Module: " + moduleInvoked );
                System.err.println( "Load Option File: " +
                                    readOptionFileInvoked );
                System.err.println( "Write Option File: "
                                    + writeOptionFileInvoked );
            }
            if ( moduleInvoked ) {
                if (  args[ n ].charAt( args[ n ].length() - 1 ) != ':' ) {
                    System.err.println( args[ n ] );
                    throw new
                        OptionProcessingException(
                                                  "Module arguments must start"
                                                  + " with : and end with :."
                                                  );
                }
                else {
                    moduleName = args[n].substring( 1,
                                                    args[n].length() - 1
                                                    ).toLowerCase();
                    if ( moduleName.length() == 0
                         || moduleName.equals( "general" ) ) {
                        moduleName = "general";
                        currentModule = generalModule;
                    }
                    else {
                        currentModule = getModule( moduleName );
                    }
                    if ( currentModule == null )
                        throw new OptionProcessingException( "Module '" +
                                                             moduleName +
                                                         "' does not exist." );
                    if ( debugFlag ) {
                        System.err.println( "Module: " + moduleName );
                    }
                }
                moduleInvoked = false;
            }
            else if ( readOptionFileInvoked ) {
                optionFile = Utility.trim( args[ n ].substring( 1 ) );
                if ( optionFile.equals( "@" )
                     || optionFile.length() == 0 )
                    optionFile = defaultOptionFilename;
                if ( debugFlag ) {
                    System.err.println( "Option file: '" + optionFile + "'." );
                }
                loadOptionFile( optionFile );
            }
            else if ( shortOptionInvoked ) {
                shortOption = args[ n ].charAt( 1 );
                if ( !Utility.isAlphaNumeric( shortOption ) ) {
                    throw new OptionProcessingException(
                      "A short option must be alphanumeric. -" + shortOption
                      + " is not acceptable." );
                }
                if ( debugFlag ) {
                    System.err.println( "Short option text: " + shortOption );
                }
                char delim = ( args[ n ].length() >= 3 ) ?
                    args[ n ].charAt( 2 ) : '\0';
                if ( delim == '+' || delim == '-' ) {
                    currentModule.action( shortOption, delim );
                }
                else if ( delim == '=' ) {
                    currentModule.action( shortOption,
                                          args[ n ].substring( 3 ) );
                }
                else if ( delim == '\0' ) {
                    String dtext = "+";
                    if ( n < args.length - 1 ) {
                        if ( !Utility.contains( args[ n + 1 ].charAt( 0 ),
                                                "-[@" ) ) {
                            dtext = args[ n + 1 ];
                            n++;
                        }
                    }
                    currentModule.action( shortOption, dtext );
                }
                else if ( Utility.isAlphaNumeric( delim ) ) {
                    for ( int j = 1; j < args[ n ].length(); j++ ) {
                        if ( Utility.isAlphaNumeric( args[ n ].charAt( j ) ) ) {
                            currentModule.action( shortOption, "+" );
                        }
                        else {
                            throw new OptionProcessingException(
                              "A short option must be alphanumeric. -"
                              + shortOption + " is not acceptable." );
                        }
                    }
                }
            }
            else if ( longOptionInvoked ) {
                char lastchar = args[ n ].charAt( args[ n ].length() - 1 );
                int eqindex = args[ n ].indexOf( "=" );
                if ( eqindex != -1 ) {
                    longOption = args[ n ].substring( 2, eqindex );
                    String value = args[ n ].substring( eqindex + 1 );
                    currentModule.action( longOption, value );
                }
                else if ( Utility.contains( lastchar, "+-" ) ) {
                    longOption = args[ n ].substring( 2,
                                                      args[ n ].length() - 1 );
                    currentModule.action( longOption, lastchar );
                }
                else {
                    longOption = args[ n ].substring( 2 );
                    String dtext = "+";
                    if ( n < args.length - 1 && args[ n + 1 ].length() > 0 ) {
                        if ( !Utility.contains( args[ n + 1 ].charAt( 0 ),
                                                "-[@" ) ) {
                            dtext = args[ n + 1 ];
                            n++;
                        }
                    }
                    currentModule.action( longOption, dtext );
                }
                if ( debugFlag ) {
                    System.err.println( "long option: " + longOption );
                }
            }
            else if ( writeOptionFileInvoked ) {
                optionFile = Utility.trim( args[ n ].substring( 1 ) );
                if ( optionFile.equals( "%" )
                     || optionFile.length() == 0 )
                    optionFile = defaultOptionFilename;
                if ( debugFlag ) {
                    System.err.println( "Option file: '" + optionFile + "'." );
                }
                writeOptionFile( optionFile );
            }
            else {
                retval = new String[ args.length - n ];
                for ( int j = n; j < args.length; j++ ) {
                    retval[ j - n ] = args[ j ];
                }
            }
        }
        if ( retval == null ) retval = new String[ 0 ];
        return retval;
    }
}
