package antlr.preprocessor;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.IndexedVector;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

class Grammar {
    protected String name;
    protected String fileName;		// where does it come from?
    protected String superGrammar;	// null if no super class
    protected String type;				// lexer? parser? tree parser?
    protected IndexedVector rules;	// text of rules as they were read in
    protected IndexedVector options;// rule options
    protected String tokenSection;	// the tokens{} stuff
    protected String preambleAction;// action right before grammar
    protected String memberAction;	// action inside grammar
    protected Hierarchy hier;			// hierarchy of grammars
    protected boolean predefined = false;	// one of the predefined grammars?
    protected boolean alreadyExpanded = false;
    protected boolean specifiedVocabulary = false;	// found importVocab option?

	/** if not derived from another grammar, might still specify a non-ANTLR
	 *  class to derive from like this "class T extends Parser(MyParserClass);"
	 */
	protected String superClass = null;

    protected String importVocab = null;
    protected String exportVocab = null;
    protected antlr.Tool antlrTool;

    public Grammar(antlr.Tool tool, String name, String superGrammar, IndexedVector rules) {
        this.name = name;
        this.superGrammar = superGrammar;
        this.rules = rules;
        this.antlrTool = tool;
    }

    public void addOption(Option o) {
        if (options == null) {	// if not already there, create it
            options = new IndexedVector();
        }
        options.appendElement(o.getName(), o);
    }

    public void addRule(Rule r) {
        rules.appendElement(r.getName(), r);
    }

    /** Copy all nonoverridden rules, vocabulary, and options into this grammar from
     *  supergrammar chain.  The change is made in place; e.g., this grammar's vector
     *  of rules gets bigger.  This has side-effects: all grammars on path to
     *  root of hierarchy are expanded also.
     */
    public void expandInPlace() {
        // if this grammar already expanded, just return
        if (alreadyExpanded) {
            return;
        }

        // Expand super grammar first (unless it's a predefined or subgrammar of predefined)
        Grammar superG = getSuperGrammar();
        if (superG == null)
            return; // error (didn't provide superclass)
        if (exportVocab == null) {
            // if no exportVocab for this grammar, make it same as grammar name
            exportVocab = getName();
        }
        if (superG.isPredefined())
            return; // can't expand Lexer, Parser, ...
        superG.expandInPlace();

        // expand current grammar now.
        alreadyExpanded = true;
        // track whether a grammar file needed to have a grammar expanded
        GrammarFile gf = hier.getFile(getFileName());
        gf.setExpanded(true);

        // Copy rules from supergrammar into this grammar
        IndexedVector inhRules = superG.getRules();
        for (Enumeration e = inhRules.elements(); e.hasMoreElements();) {
            Rule r = (Rule)e.nextElement();
            inherit(r, superG);
        }

        // Copy options from supergrammar into this grammar
        // Modify tokdef options so that they point to dir of enclosing grammar
        IndexedVector inhOptions = superG.getOptions();
        if (inhOptions != null) {
            for (Enumeration e = inhOptions.elements(); e.hasMoreElements();) {
                Option o = (Option)e.nextElement();
                inherit(o, superG);
            }
        }

        // add an option to load the superGrammar's output vocab
        if ((options != null && options.getElement("importVocab") == null) || options == null) {
            // no importVocab found, add one that grabs superG's output vocab
            Option inputV = new Option("importVocab", superG.exportVocab + ";", this);
            addOption(inputV);
            // copy output vocab file to current dir
            String originatingGrFileName = superG.getFileName();
            String path = antlrTool.pathToFile(originatingGrFileName);
            String superExportVocabFileName = path + superG.exportVocab +
                antlr.CodeGenerator.TokenTypesFileSuffix +
                antlr.CodeGenerator.TokenTypesFileExt;
            String newImportVocabFileName = antlrTool.fileMinusPath(superExportVocabFileName);
            if (path.equals("." + System.getProperty("file.separator"))) {
                // don't copy tokdef file onto itself (must be current directory)
                // System.out.println("importVocab file same dir; leaving as " + superExportVocabFileName);
            }
            else {
                try {
                    antlrTool.copyFile(superExportVocabFileName, newImportVocabFileName);
                }
                catch (IOException io) {
                    antlrTool.toolError("cannot find/copy importVocab file " + superExportVocabFileName);
                    return;
                }
            }
        }

        // copy member action from supergrammar into this grammar
        inherit(superG.memberAction, superG);
    }

    public String getFileName() {
        return fileName;
    }

    public String getName() {
        return name;
    }

    public IndexedVector getOptions() {
        return options;
    }

    public IndexedVector getRules() {
        return rules;
    }

    public Grammar getSuperGrammar() {
        if (superGrammar == null) return null;
        Grammar g = (Grammar)hier.getGrammar(superGrammar);
        return g;
    }

    public String getSuperGrammarName() {
        return superGrammar;
    }

    public String getType() {
        return type;
    }

    public void inherit(Option o, Grammar superG) {
        // do NOT inherit importVocab/exportVocab options under any circumstances
        if (o.getName().equals("importVocab") ||
            o.getName().equals("exportVocab")) {
            return;
        }

        Option overriddenOption = null;
        if (options != null) {	// do we even have options?
            overriddenOption = (Option)options.getElement(o.getName());
        }
        // if overridden, do not add to this grammar
        if (overriddenOption == null) { // not overridden
            addOption(o);	// copy option into this grammar--not overridden
        }
    }

    public void inherit(Rule r, Grammar superG) {
        // if overridden, do not add to this grammar
        Rule overriddenRule = (Rule)rules.getElement(r.getName());
        if (overriddenRule != null) {
            // rule is overridden in this grammar.
            if (!overriddenRule.sameSignature(r)) {
                // warn if different sig
                antlrTool.warning("rule " + getName() + "." + overriddenRule.getName() +
                                   " has different signature than " +
                                   superG.getName() + "." + overriddenRule.getName());
            }
        }
        else {  // not overridden, copy rule into this
            addRule(r);
        }
    }

    public void inherit(String memberAction, Grammar superG) {
        if (this.memberAction != null) return;	// do nothing if already have member action
        if (memberAction != null) { // don't have one here, use supergrammar's action
            this.memberAction = memberAction;
        }
    }

    public boolean isPredefined() {
        return predefined;
    }

    public void setFileName(String f) {
        fileName = f;
    }

    public void setHierarchy(Hierarchy hier) {
        this.hier = hier;
    }

    public void setMemberAction(String a) {
        memberAction = a;
    }

    public void setOptions(IndexedVector options) {
        this.options = options;
    }

    public void setPreambleAction(String a) {
        preambleAction = a;
    }

    public void setPredefined(boolean b) {
        predefined = b;
    }

    public void setTokenSection(String tk) {
        tokenSection = tk;
    }

    public void setType(String t) {
        type = t;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(10000);
        if (preambleAction != null) {
            s.append(preambleAction);
        }
        if (superGrammar == null) {
			return "class " + name + ";";
        }
		if ( superClass!=null ) {
			// replace with specified superclass not actual grammar
			// user must make sure that the superclass derives from super grammar class
			s.append("class " + name + " extends " + superClass + ";");
		}
		else {
			s.append("class " + name + " extends " + type + ";");
		}
		s.append(
			System.getProperty("line.separator") +
            System.getProperty("line.separator"));
        if (options != null) {
            s.append(Hierarchy.optionsToString(options));
        }
        if (tokenSection != null) {
            s.append(tokenSection + "\n");
        }
        if (memberAction != null) {
            s.append(memberAction + System.getProperty("line.separator"));
        }
        for (int i = 0; i < rules.size(); i++) {
            Rule r = (Rule)rules.elementAt(i);
            if (!getName().equals(r.enclosingGrammar.getName())) {
                s.append("// inherited from grammar " + r.enclosingGrammar.getName() + System.getProperty("line.separator"));
            }
            s.append(r +
                System.getProperty("line.separator") +
                System.getProperty("line.separator"));
        }
        return s.toString();
    }
}
