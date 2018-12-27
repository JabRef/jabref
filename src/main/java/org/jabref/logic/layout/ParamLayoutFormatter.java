package org.jabref.logic.layout;

/**
 * This interface extends LayoutFormatter, adding the capability of taking
 * and additional parameter. Such a parameter is specified in the layout file
 * by the following construct: \format[MyFormatter(argument){\field}
 * If and only if MyFormatter is a class that implements ParamLayoutFormatter,
 * it will be set up with the argument given in the parenthesis by way of the
 * method setArgument(String). If no argument is given, the formatter will be
 * invoked without the setArgument() method being called first.
 */
public interface ParamLayoutFormatter extends LayoutFormatter {

    /**
     * Method for setting the argument of this formatter.
     * @param arg A String argument.
     */
    void setArgument(String arg);

}
