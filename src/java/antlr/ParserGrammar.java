package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;


/** Parser-specific grammar subclass */
class ParserGrammar extends Grammar {


    ParserGrammar(String className_, Tool tool_, String superClass) {
        super(className_, tool_, superClass);
    }

    /** Top-level call to generate the code for this grammar */
    public void generate() throws IOException {
        generator.gen(this);
    }

    // Get name of class from which generated parser/lexer inherits
    protected String getSuperClass() {
        // if debugging, choose the debugging version of the parser
        if (debuggingOutput)
            return "debug.LLkDebuggingParser";
        return "LLkParser";
    }

    /**Process command line arguments.
     * -trace			have all rules call traceIn/traceOut
     * -traceParser		have parser rules call traceIn/traceOut
     * -debug			generate debugging output for parser debugger
     */
    public void processArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-trace")) {
                traceRules = true;
                antlrTool.setArgOK(i);
            }
            else if (args[i].equals("-traceParser")) {
                traceRules = true;
                antlrTool.setArgOK(i);
            }
            else if (args[i].equals("-debug")) {
                debuggingOutput = true;
                antlrTool.setArgOK(i);
            }
        }
    }

    /** Set parser options -- performs action on the following options:
     */
    public boolean setOption(String key, Token value) {
        String s = value.getText();
        if (key.equals("buildAST")) {
            if (s.equals("true")) {
                buildAST = true;
            }
            else if (s.equals("false")) {
                buildAST = false;
            }
            else {
                antlrTool.error("buildAST option must be true or false", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("interactive")) {
            if (s.equals("true")) {
                interactive = true;
            }
            else if (s.equals("false")) {
                interactive = false;
            }
            else {
                antlrTool.error("interactive option must be true or false", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("ASTLabelType")) {
            super.setOption(key, value);
            return true;
        }
        if (super.setOption(key, value)) {
            return true;
        }
        antlrTool.error("Invalid option: " + key, getFilename(), value.getLine(), value.getColumn());
        return false;
    }
}
