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


//
// ANTLR C# Code Generator by Micheal Jordan
//                            Kunle Odutola       : kunle UNDERSCORE odutola AT hotmail DOT com
//                            Anthony Oguntimehin
//
// With many thanks to Eric V. Smith from the ANTLR list.
//

// HISTORY:
//
// 17-May-2002 kunle    Original version
//

import java.util.Vector;
import java.util.Enumeration;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class CSharpNameSpace extends NameSpace
{
    public CSharpNameSpace(String name)
    {
		super(name);
    }

    /**
     * Method to generate the required CSharp namespace declarations
     */
    void emitDeclarations(PrintWriter out) {
    	  out.println("namespace " + getName() );
    	  out.println("{");
    }

    /**
     * Method to generate the required CSharp namespace closures
     */
    void emitClosures(PrintWriter out) {
    	  out.println("}");
    }
}
