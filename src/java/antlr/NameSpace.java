package antlr;

/**
 * ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * Container for a C++ namespace specification.  Namespaces can be
 * nested, so this contains a vector of all the nested names.
 *
 * @author David Wagner (JPL/Caltech) 8-12-00
 *
 * $Id$
 */

import java.util.Vector;
import java.util.Enumeration;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class NameSpace {
    private Vector names = new Vector();
    private String _name;

    public NameSpace(String name) {
    	  _name = new String(name);
        parse(name);
    }

	 public String getName()
	 {
	 	return _name;
	 }
	
    /**
     * Parse a C++ namespace declaration into seperate names
     * splitting on ::  We could easily parameterize this to make
     * the delimiter a language-specific parameter, or use subclasses
     * to support C++ namespaces versus java packages. -DAW
     */
    protected void parse(String name) {
        StringTokenizer tok = new StringTokenizer(name, "::");
        while (tok.hasMoreTokens())
            names.addElement(tok.nextToken());
    }

    /**
     * Method to generate the required C++ namespace declarations
     */
    void emitDeclarations(PrintWriter out) {
        for (Enumeration n = names.elements(); n.hasMoreElements();) {
            String s = (String)n.nextElement();
            out.println("ANTLR_BEGIN_NAMESPACE(" + s + ")");
        }
    }

    /**
     * Method to generate the required C++ namespace closures
     */
    void emitClosures(PrintWriter out) {
        for (int i = 0; i < names.size(); ++i)
            out.println("ANTLR_END_NAMESPACE");
    }
}
