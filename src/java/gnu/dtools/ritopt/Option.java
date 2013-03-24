package gnu.dtools.ritopt;

/**
 * Option.java
 *
 * Version
 *    $Id$
 */


/**
 * This is the principal base class for all Option classes. It contains
 * constructors for short and long option initialization, utility members
 * for help reporting and file writing, and deprecation facilities.<p>
 *
 * Options that provide array support should inherit from the ArrayOption
 * class, and follow the guidelines defined both in the Option and
 * ArrayOption class descriptions.<p>
 *
 * Non-abstract subclasses should implement the modify method. When an option
 * is invoked, the value of the option is passed to the modify method.<p>
 *
 * Subclasses should provide several constructors so that registration is
 * simple and uniform. Recommended constructors include a default constructor,
 * an interface for initialization of short and long options,
 * and one that allows both short and long option fields to be
 * initialized. If the subclass implementation provides constructors which
 * initialize its members then the member parameters must be before
 * the short and long option initialization parameters.<p>
 *
 * Event driven option processing is provided in the NotifyOption class. In
 * order to use a NotifyOption, the recipient object must implement the
 * OptionListener class. Although it is not required, subclass implementations
 * of NotifyOption should implement the OptionNotifier interface.<p>
 *
 * By default, the Option class considers the width of an output device
 * to be eighty characters. It initializes the width of the help fields
 * based on this figure. If a field exceeds its field width, it is
 * truncated. The width constraints can be changed by invoking the appropriate
 * static mutators.<p>
 *
 * Similar to the help reporting facilities, the same constraints are placed
 * on the listing of options provided by the built-in menu interface. These
 * constraints can be modified by executing the appropriate static mutators.
 * <p>
 *
 * The Option class provides a facility for writing options files.
 * For option file writing, there are only two field width constraints; the
 * assignment and the comment.
 * <pre>
 * Assignment:                           Comment:
 * --longOrShortOption=optionValue       ;description goes here [d]
 * </pre>
 * As shown above, an assignment includes the long or short option text,
 * an equal sign, and the option's value. The comment includes the
 * description, and "[d]" if the option is deprecated.<p>
 *
 * If the assignment exceeds its field width, the comment is placed before
 * the assignment on a separate line. The comment is truncated if it
 * exceeds eighty characters when it is placed before the assignment.
 * However, if the assignment does not exceeds its field width and the comment
 * does, the comment is truncated, and continued on the next line at the
 * columnar position defined by the assignment's field width. Field widths
 * may be modified by invoking the appropriate static mutator.<p>
 *
 * This class also provides a facility for deprecating options. An option is
 * deprecated to discourage its use without removing the functionality it
 * provides. An option is deprecated by invoking the deprecate method.
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

public abstract class Option implements OptionModifiable {

    /**
     * The default width of the option field when the help usage is displayed.
     */

    public static final int DEFAULT_HELP_OPTION_SIZE = 22;

    /**
     * The default width of the type name field when the help usage is
     * display.
     */

    public static final int DEFAULT_HELP_TYPENAME_SIZE = 10;

    /**
     * The default width of the description when the help usage is displayed.
     */

    public static final int DEFAULT_HELP_DESCRIPTION_SIZE = 48;

    /**
     * The default width of the deprecated field when the help usage is
     * displayed.
     */

    public static final int DEFAULT_HELP_DEPRECATED_SIZE = 3;

    /**
     * The default width of the option field when the menu usage is displayed.
     */

    public static final int DEFAULT_MENU_OPTION_SIZE = 15;

    /**
     * The default width of the type name field when the menu usage is
     * displayed.
     */

    public static final int DEFAULT_MENU_TYPENAME_SIZE = 10;

    /**
     * The default width of the description field when the menu usage is
     * displayed.
     */

    public static final int DEFAULT_MENU_DESCRIPTION_SIZE = 48;

    /**
     * The default width of the deprecated field when the menu usage is
     * displayed.
     */

    public static final int DEFAULT_MENU_DEPRECATED_SIZE = 3;

    /**
     * The default width of the option assignment in an option file.
     */

    public static final int DEFAULT_FILE_COMPLETE_OPTION_SIZE = 60;

    /**
     * The default width of the comment in an option file. If the option
     * and the comment exceeds the width of the device, the comment is
     * truncated to the next line at the same columnar position of the
     * previous comment line. If the option assignment line is longer than
     * the width, the comment line is put before the option assignment it
     * refers.
     */

    public static final int DEFAULT_FILE_COMMENT_SIZE = 16;

    /**
     * The String holding the value of the long option. If there is no
     * long option, this value is set to null.
     */

    private String longOption;

    /**
     * The character holding the value of the short option. If there is no
     * short option,t his value is set to '\0'.
     */

    private char shortOption;

    /**
     * The String holding the description of this option.
     */

    private String description;

    /**
     * A flag identifying whether this option is deprecated.
     */

    private boolean deprecated;

    /**
     * The field width for the option specification that is reporter for
     * help.
     */

    private static int helpOptionSpecificationSize = DEFAULT_HELP_OPTION_SIZE;

    /**
     * The field width for the type name that is reported for help.
     */

    private static int helpTypenameSize = DEFAULT_HELP_TYPENAME_SIZE;

    /**
     * The field width for the description that is reported during help.
     */

    private static int helpDescriptionSize = DEFAULT_HELP_DESCRIPTION_SIZE;

    /**
     * The field width for the deprecated flag that is reported during
     * help.
     */

    private static int helpDeprecatedSize = DEFAULT_HELP_DEPRECATED_SIZE;

    /**
     * The field width for the option specification that is reported when
     * the options are listed in the built-in menu.
     */

    private static int menuOptionSpecificationSize = DEFAULT_MENU_OPTION_SIZE;

    /**
     * The field width for the type name that is reported when the options
     * are listed in the built-in menu.
     */

    private static int menuTypenameSize = DEFAULT_MENU_TYPENAME_SIZE;

    /**
     * The field width for the description that is reported when the options
     * are listed in the built-in menu.
     */

    private static int menuDescriptionSize = DEFAULT_MENU_DESCRIPTION_SIZE;

    /**
     * The field width for the deprecated flag that is reported when the
     * options are listed in the build-in menu.
     */

    private static int menuDeprecatedSize = DEFAULT_MENU_DEPRECATED_SIZE;

    /**
     * The field width for the assignment portion of an option that is 
     * written to a file.
     */

    private static int fileCompleteOptionSize =
                                            DEFAULT_FILE_COMPLETE_OPTION_SIZE;

    /**
     * The field width for the comment portion of an option that is written
     * to a file.
     */

    private static int fileCommentSize = DEFAULT_FILE_COMMENT_SIZE;

    /**
     * A field indicating whether an option has been invoked.
     */

    protected boolean invoked;

    /**
     * Returns this option's value as an Object.
     *
     * @return An object representation of this option.
     */

    public abstract Object getObject();

    /**
     * Returns the option's value as a String. This String should conform
     * to the formatting requirements prescribed by a modify method.
     *
     * @return The option's value as a String conforming to formatting
     *         requirements.
     */

    public abstract String getStringValue();

    /**
     * Constructs an option with no initial short or long option value,
     * and is by default uninvoked and undeprecated, and has a description
     * initialized to the empty string.
     */

    public Option() {
	super();
	description = "";
    }

    /**
     * Constructs an option by copying the option passed.
     *
     * @param option  The option to copy for this object's construction.
     */

    public Option( Option option ) {
	longOption = option.getLongOption();
	shortOption = option.getShortOption();
	description = option.getDescription();
	deprecated = option.isDeprecated();
    }

    /**
     * Constructs an option by initializing its long option with the
     * value passed. The short option is equal to the null character,
     * and the description is equal to the empty string.
     *
     * @param longOption The value of the long option
     */

    public Option( String longOption ) {
	this.longOption = longOption;
	this.shortOption = '\0';
	description = "";
    }

    /**
     * Constructs an option by initializing its short option with the
     * value passed. The long option is equal to null, and the description
     * is equal to the empty string.
     *
     * @param shortOption The value of the short option.
     */

    public Option( char shortOption ) {
	this.shortOption = shortOption;
	this.longOption = null;
	description = "";
    }

    /**
     * Constructs an option by initializing its short and long options
     * with the values passed. The description is set to the empty string.
     *
     * @param longOption The value of the long option.
     * @param shortOption The value of the short option.
     */

    public Option( String longOption, char shortOption ) {
	this.longOption = longOption;
	this.shortOption = shortOption;
	description = "";
    }

    /**
     * Sets the long option.
     *
     * @param longOption The value to set the long option.
     */

    public void setKey( String longOption ) {
	this.longOption = longOption;
    }

    /**
     * Sets the short option.
     *
     * @param shortOption The value to set the short option.
     */

    public void setKey( char shortOption ) {
	this.shortOption = shortOption;
    }

    /**
     * Sets the short option.
     *
     * @param shortOption The value to set the short option.
     */

    public void setShortOption( char shortOption ) {
	setKey( shortOption );
    }

    /**
     * Sets the long option.
     *
     * @param longOption The value to set the long option.
     */

    public void setLongOption( String longOption ) {
	setKey( longOption );
    }

    /**
     * Sets the description of this option.
     *
     * @param description The description of this option.
     */

    public void setDescription( String description ) {
	this.description = description;
    }

    /**
     * Sets the deprecated flag to the value passed.
     *
     * @param deprecated A flag indicating whether the option is deprecated.
     */

    public void setDeprecated( boolean deprecated ) {
	this.deprecated = deprecated;
    }

    /**
     * Sets the field width for the option specification displayed
     * in the help report.
     *
     * @param newSize The size to set the field width.
     */

    public static void setHelpOptionSpecificationSize( int newSize ) {
	helpOptionSpecificationSize = newSize;
    }

    /**
     * Sets the field width for the type name displayed in the help report.
     *
     * @param newSize The size to set the field width.
     */

    public static void setHelpTypenameSize( int newSize ) {
	helpTypenameSize = newSize;
    }

    /**
     * Sets the field width for the description displayed in the help report.
     *
     * @param newSize The size to set the field width.
     */

    public static void setHelpDescriptionSize( int newSize ) {
	helpDescriptionSize = newSize;
    }

    /**
     * Sets the field width for the deprecated flag displayed in the
     * help report.
     *
     * @param newSize The size to set the field width.
     */

    public static void setHelpDeprecatedSize( int newSize ) {
	helpDeprecatedSize = newSize;
    }

    /**
     * Sets the field width for the option specification displayed
     * in the menu listing of options.
     *
     * @param newSize The size to set the field width.
     */

    public static void setMenuOptionSpecificationSize( int newSize ) {
	menuOptionSpecificationSize = newSize;
    }

    /**
     * Sets the field width for the type name displayed in the menu
     * listing of options.
     *
     * @param newSize The size to set the field width.
     */

    public static void setMenuTypenameSize( int newSize ) {
	menuTypenameSize = newSize;
    }

    /**
     * Sets the field width for the option description displayed
     * in the menu listing of options.
     *
     * @param newSize The size to set the field width.
     */

    public static void setMenuDescriptionSize( int newSize ) {
	menuDescriptionSize = newSize;
    }

    /**
     * Sets the field width for the deprecated flag displayed
     * in the menu listing of options.
     *
     * @param newSize The size to set the field width.
     */

    public static void setMenuDeprecatedSize( int newSize ) {
	menuDeprecatedSize = newSize;
    }

    /**
     * Sets the assignment field width used when options files are written.
     *
     * @param newSize The size to set the field width.
     */

    public static void setFileCompleteOptionSize( int newSize ) {
	fileCompleteOptionSize = newSize;
    }

    /**
     * Sets the assignment field width used when options files are written.
     *
     * @param newSize The size to set the field width.
     */

    public static void setFileCommentSize( int newSize ) {
	fileCommentSize = newSize;
    }

    /**
     * Sets whether this option has been invoked.
     *
     * @param A boolean indicating whether this option has been invoked.
     */

    public void setInvoked( boolean b ) {
	invoked = b;
    }

    /**
     * Deprecates this option.
     */

    public void deprecate() {
	setDeprecated( true );
    }

    /**
     * Return the name of this option. This method returns the same value as
     * the getLongOption accessor.
     *
     * @return The name of this otpion.
     */

    public String getName() {
	return longOption;
    }

    /**
     * Return the short option key. There is no short option when this
     * character is the null character.
     *
     * @return The short option key of this option.
     */

    public char getShortOption() {
	return shortOption;
    }

    /**
     * Return the long option key. There is no long option when this value
     * is null.
     *
     * @return The long option key of this option.
     */

    public String getLongOption() {
	return longOption;
    }


    /**
     * Return a line used for help reporting.
     *
     * @return A line used for help reporting.
     */

    public String getHelp() {
	return getHelpOptionSpecification() + " " + getHelpTypeName() + " "
	    + getHelpDescription() + " " + getHelpDeprecated();
    }

    /**
     * Return the option specification field used during help reporting.
     *
     * @return The option specification field.
     */

    public String getHelpOptionSpecification() {
	return Utility.expandString(
	    ( ( ( shortOption != '\0' ) ? ( "-" + getShortOption() ) : "  " )
	    + ( ( longOption != null && shortOption != '\0' ) ? ", " : "  " )
	    + ( ( longOption != null ) ? "--" + getLongOption(): "" ) ),
			     helpOptionSpecificationSize );
    }

    /**
     * Return the type name field used during help reporting.
     *
     * @return The type name field.
     */

    public String getHelpTypeName() {
	return Utility.expandString( "<" + getTypeName() + ">",
				     helpTypenameSize );
    }

    /**
     * Return the description field used during help reporting.
     *
     * @return The description field.
     */

    public String getHelpDescription() {
	return Utility.expandString( getDescription(),
			     helpDescriptionSize );
    }

    /**
     * Return the deprecated field used during help reporting.
     *
     * @return The deprecated field.
     */

    public String getHelpDeprecated() {
	return Utility.expandString( isDeprecated() ? "[d]" : "",
			     helpDeprecatedSize );
    }

    /**
     * Return the header displayed at the top of the help report.
     *
     * @return The header displayed at the top of the help report.
     */

    public static String getHelpHeader() {
	return Utility.expandString( "Option Name",
				     helpOptionSpecificationSize ) + " "
	    + Utility.expandString( "Type", helpTypenameSize ) + " "
	    + Utility.expandString( "Description", helpDescriptionSize );
    }

    /**
     * The description explaining the meaning of this option.
     *
     * @return This options description.
     */

    public String getDescription() {
	return description;
    }

    /**
     * The hash key of this option. This is used by classes that implement
     * the option registrar class. This method should <b>not</b> be overrided.
     *
     * @return The hash key of this option.
     */

    public String getHashKey() {
	return Option.getHashKey( longOption, shortOption );
    }

    /**
     * The hash key of an option if there is no short option. This method
     * should <b>not</b> be overrided.
     *
     * @param longOption The long option.
     *
     * @return The hash key of this option based on the long option.
     */

    public static String getHashKey( String longOption ) {
	return "," + ( ( longOption != null ) ? longOption : "" );
    }

    /**
     * The hash key of an option if there is no long option. This method
     * should <b>not</b> be overrided.
     *
     * @param shortOption The short option.
     *
     * @return The hash key of this option based on the short option.
     */

    public static String getHashKey( char shortOption ) {
	return "" + ( shortOption != '\0' ) + ",";
    }

    /**
     * The hash key of an option if there both short and long options are
     * defined.
     *
     * @param shortOption The short option.
     * @param longOption  The long option.
     *
     * @return The hash key of this option based on both the short and long
     *         options.
     */

    public static String getHashKey( String longOption, char shortOption ) {
	return ( ( shortOption == '\0' ) ? "" : "" + shortOption ) +
	    ( ( longOption == null ) ? "," : "," + longOption );
    }

    /**
     * Returns whether this option is deprecated.
     *
     * @return A boolean indicating whether this option is deprecated.
     */

    public boolean isDeprecated() {
	return deprecated;
    }

    /**
     * Returns whether this option has been invoked.
     *
     * @return A boolean indicating whether this option has been invoked.
     */

    public boolean isInvoked() {
	return invoked;
    }

    /**
     * Returns (a) line(s) representing this option. This line is usually
     * later written to an options file.
     *
     * @return Line(s) representing this option.
     */

    public String getOptionFileLine() {
	boolean descriptionPrinted = false;
	String retval = "";
	String optionText = "";
	String strval = getStringValue();
	if ( longOption != null ) {
	    optionText += "--" + longOption;
	}
	else if ( shortOption != '\0' ) {
	    optionText += "-" + shortOption;
	}
	if ( optionText.length() > 0
	     && Utility.trim( strval ).length() >= 0 ) {
	    optionText += "=" + strval;
	}
	if ( optionText.length() <= fileCompleteOptionSize ) {
	    retval += Utility.expandString( optionText,
					    fileCompleteOptionSize );
	}
	else {
	    retval += "; " + description + "\n";
	    retval += optionText;
	    descriptionPrinted = true;
	}
	if ( !descriptionPrinted ) { 
	    StringBuffer descsplit = new StringBuffer( description );
	    boolean tmp = false;
	    while ( descsplit.length() > 0 ) {
		String st = "";
		int size = 0;
		if ( tmp ) {
		    st += Utility.getSpaces( fileCompleteOptionSize );
		}
		size = ( descsplit.length() >= fileCommentSize )
		    ? fileCommentSize : descsplit.length();
		st += "; " + descsplit.substring( 0, size );
		descsplit.delete( 0, size );
		retval += st + "\n";
		tmp = true;
	    }
	    descriptionPrinted = true;
	}
	return retval;
    }

    /**
     * Returns the field width for the option specification displayed in the
     * help report.
     *
     * @return The field width.
     */

    public static int getHelpOptionSpecificationSize() {
	return helpOptionSpecificationSize;
    }

    /**
     * Returns the field width for the type name displayed in the help report.
     *
     * @return The field width.
     */

    public static int getHelpTypenameSize() {
	return helpTypenameSize;
    }

    /**
     * Returns the field width for the description displayed in the help
     * report.
     *
     * @return The field width.
     */

    public static int getHelpDescriptionSize() {
	return helpDescriptionSize;
    }

    /**
     * Returns the field width for the deprecated flag displayed in the
     * help report.
     *
     * @return The field width.
     */

    public static int getHelpDeprecatedSize() {
	return helpDeprecatedSize;
    }

    /**
     * Returns the field width for the option specification displayed in the
     * menu listing of options.
     *
     * @return The field width.
     */

    public static int getMenuOptionSpecificationSize() {
	return menuOptionSpecificationSize;
    }

    /**
     * Returns the field width for the type name displayed in the
     * menu listing of options.
     *
     * @return The field width.
     */

    public static int getMenuTypenameSize() {
	return menuTypenameSize;
    }

    /**
     * Returns the field width for the description displayed in the
     * menu listing of options.
     *
     * @return The field width.
     */

    public static int getMenuDescriptionSize() {
	return menuDescriptionSize;
    }

    /**
     * Returns the field width for the deprecated flag displayed in the
     * menu listing of options.
     *
     * @return The field width.
     */

    public static int getMenuDeprecatedSize() {
	return menuDeprecatedSize;
    }

    /**
     * Returns the field width for assignment portion of a option file line.
     *
     * @return The field width.
     */

    public static int getFileCompleteOptionSize() {
	return fileCompleteOptionSize;
    }

    /**
     * Returns the field width for assignment portion of a option file line.
     *
     * @return The field width.
     */

    public static int getFileCommentSize() {
	return fileCommentSize;
    }

    /**
     * Returns the type name of this option.
     *
     * @return The type name of this option.
     */

    public abstract String getTypeName();

    /**
     * Prepares the option for modification.
     */

    public void action() {
	if ( deprecated ) {
	    System.err.print( "Warning: " );
	    if ( longOption != null ) {
		System.err.print( "--" + longOption );
	    }
	    if ( shortOption != '\0' && longOption != null ) {
		System.err.print( " or " );
	    }
	    if ( shortOption != '\0' ) {
		System.err.println( "-" + shortOption + " is deprecated." );
	    }
	}
    }

} /** Option */





