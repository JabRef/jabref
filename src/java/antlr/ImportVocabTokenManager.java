package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.io.*;
import java.util.Hashtable;
import java.util.Enumeration;

import antlr.collections.impl.Vector;

/** Static implementation of the TokenManager, used for importVocab option  */
class ImportVocabTokenManager extends SimpleTokenManager implements Cloneable {
    private String filename;
    protected Grammar grammar;

    // FIXME: it would be nice if the path to the original grammar file was
    // also searched.
    ImportVocabTokenManager(Grammar grammar, String filename_, String name_, Tool tool_) {
        // initialize
        super(name_, tool_);

        this.grammar = grammar;
        this.filename = filename_;

        // Figure out exactly where the file lives.  Check $PWD first,
        // and then search in -o <output_dir>.
        //
        File grammarFile = new File(filename);

        if (!grammarFile.exists()) {
            grammarFile = new File(antlrTool.getOutputDirectory(), filename);

            if (!grammarFile.exists()) {
                antlrTool.panic("Cannot find importVocab file '" + filename + "'");
            }
        }

        setReadOnly(true);

        // Read a file with lines of the form ID=number
        try {
            Reader fileIn = new BufferedReader(new FileReader(grammarFile));
            ANTLRTokdefLexer tokdefLexer = new ANTLRTokdefLexer(fileIn);
            ANTLRTokdefParser tokdefParser = new ANTLRTokdefParser(tokdefLexer);
            tokdefParser.setTool(antlrTool);
            tokdefParser.setFilename(filename);
            tokdefParser.file(this);
        }
        catch (FileNotFoundException fnf) {
            antlrTool.panic("Cannot find importVocab file '" + filename + "'");
        }
        catch (RecognitionException ex) {
            antlrTool.panic("Error parsing importVocab file '" + filename + "': " + ex.toString());
        }
        catch (TokenStreamException ex) {
            antlrTool.panic("Error reading importVocab file '" + filename + "'");
        }
    }

    public Object clone() {
        ImportVocabTokenManager tm;
        tm = (ImportVocabTokenManager)super.clone();
        tm.filename = this.filename;
        tm.grammar = this.grammar;
        return tm;
    }

    /** define a token. */
    public void define(TokenSymbol ts) {
        super.define(ts);
    }

    /** define a token.  Intended for use only when reading the importVocab file. */
    public void define(String s, int ttype) {
        TokenSymbol ts = null;
        if (s.startsWith("\"")) {
            ts = new StringLiteralSymbol(s);
        }
        else {
            ts = new TokenSymbol(s);
        }
        ts.setTokenType(ttype);
        super.define(ts);
        maxToken = (ttype + 1) > maxToken ? (ttype + 1) : maxToken;	// record maximum token type
    }

    /** importVocab token manager is read-only if output would be same as input */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** Get the next unused token type. */
    public int nextTokenType() {
        return super.nextTokenType();
    }
}
