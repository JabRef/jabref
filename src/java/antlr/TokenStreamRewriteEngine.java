package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 */

import antlr.collections.impl.BitSet;
import java.util.*;

/** This token stream tracks the *entire* token stream coming from
 *  a lexer, but does not pass on the whitespace (or whatever else
 *  you want to discard) to the parser.
 *
 *  This class can then be asked for the ith token in the input stream.
 *  Useful for dumping out the input stream exactly after doing some
 *  augmentation or other manipulations.  Tokens are index from 0..n-1
 *
 *  You can insert stuff, replace, and delete chunks.  Note that the
 *  operations are done lazily--only if you convert the buffer to a
 *  String.  This is very efficient because you are not moving data around
 *  all the time.  As the buffer of tokens is converted to strings, the
 *  toString() method(s) check to see if there is an operation at the
 *  current index.  If so, the operation is done and then normal String
 *  rendering continues on the buffer.  This is like having multiple Turing
 *  machine instruction streams (programs) operating on a single input tape. :)
 *
 *  Since the operations are done lazily at toString-time, operations do not
 *  screw up the token index values.  That is, an insert operation at token
 *  index i does not change the index values for tokens i+1..n-1.
 *
 *  Because operations never actually alter the buffer, you may always get
 *  the original token stream back without undoing anything.  Since
 *  the instructions are queued up, you can easily simulate transactions and
 *  roll back any changes if there is an error just by removing instructions.
 *  For example,
 *
 * 		TokenStreamRewriteEngine rewriteEngine =
 * 			new TokenStreamRewriteEngine(lexer);
 *      JavaRecognizer parser = new JavaRecognizer(rewriteEngine);
 *      ...
 *      rewriteEngine.insertAfter("pass1", t, "foobar");}
 * 		rewriteEngine.insertAfter("pass2", u, "start");}
 * 		System.out.println(rewriteEngine.toString("pass1"));
 * 		System.out.println(rewriteEngine.toString("pass2"));
 *
 *  You can also have multiple "instruction streams" and get multiple
 *  rewrites from a single pass over the input.  Just name the instruction
 *  streams and use that name again when printing the buffer.  This could be
 *  useful for generating a C file and also its header file--all from the
 *  same buffer.
 *
 *  If you don't use named rewrite streams, a "default" stream is used.
 *
 *  Terence Parr, parrt@cs.usfca.edu
 *  University of San Francisco
 *  February 2004
 */
public class TokenStreamRewriteEngine implements TokenStream {
	public static final int MIN_TOKEN_INDEX = 0;

	static class RewriteOperation {
		protected int index;
		protected String text;
		protected RewriteOperation(int index, String text) {
			this.index = index;
			this.text = text;
		}
		/** Execute the rewrite operation by possibly adding to the buffer.
		 *  Return the index of the next token to operate on.
		 */
		public int execute(StringBuffer buf) {
			return index;
		}
	}

	static class InsertBeforeOp extends RewriteOperation {
		public InsertBeforeOp(int index, String text) {
			super(index,text);
		}
		public int execute(StringBuffer buf) {
			buf.append(text);
			return index;
		}
	}

	static class ReplaceOp extends RewriteOperation {
		protected int lastIndex;
		public ReplaceOp(int from, int to, String text) {
			super(from,text);
			lastIndex = to;
		}
		public int execute(StringBuffer buf) {
			if ( text!=null ) {
				buf.append(text);
			}
			return lastIndex+1;
		}
	}

	static class DeleteOp extends ReplaceOp {
		public DeleteOp(int from, int to) {
			super(from, to, null);
		}
	}

	public static final String DEFAULT_PROGRAM_NAME = "default";
    public static final int PROGRAM_INIT_SIZE = 100;

	/** Track the incoming list of tokens */
	protected List tokens;

	/** You may have multiple, named streams of rewrite operations.
	 *  I'm calling these things "programs."
	 *  Maps String (name) -> rewrite (List)
	 */
	protected Map programs = null;

	/** Map String (program name) -> Integer index */
	protected Map lastRewriteTokenIndexes = null;

	/** track index of tokens */
	protected int index = MIN_TOKEN_INDEX;

	/** Who do we suck tokens from? */
	protected TokenStream stream;

	/** Which (whitespace) token(s) to throw out */
	protected BitSet discardMask = new BitSet();

	public TokenStreamRewriteEngine(TokenStream upstream) {
		this(upstream,1000);
	}

	public TokenStreamRewriteEngine(TokenStream upstream, int initialSize) {
		stream = upstream;
		tokens = new ArrayList(initialSize);
		programs = new HashMap();
		programs.put(DEFAULT_PROGRAM_NAME,
							   new ArrayList(PROGRAM_INIT_SIZE));
		lastRewriteTokenIndexes = new HashMap();
	}

	public Token nextToken() throws TokenStreamException {
		TokenWithIndex t;
		// suck tokens until end of stream or we find a non-discarded token
		do {
           	t = (TokenWithIndex)stream.nextToken();
			if ( t!=null ) {
				t.setIndex(index);  // what is t's index in list?
				if ( t.getType()!=Token.EOF_TYPE ) {
					tokens.add(t);  // track all tokens except EOF
				}
				index++;			// move to next position
			}
		} while ( t!=null && discardMask.member(t.getType()) );
		return t;
	}

	public void rollback(int instructionIndex) {
		rollback(DEFAULT_PROGRAM_NAME, instructionIndex);
	}

	/** Rollback the instruction stream for a program so that
	 *  the indicated instruction (via instructionIndex) is no
	 *  longer in the stream.  UNTESTED!
	 */
	public void rollback(String programName, int instructionIndex) {
		List is = (List)programs.get(programName);
		if ( is!=null ) {
			programs.put(programName, is.subList(MIN_TOKEN_INDEX,instructionIndex));
		}
	}

	public void deleteProgram() {
		deleteProgram(DEFAULT_PROGRAM_NAME);
	}

	/** Reset the program so that no instructions exist */
	public void deleteProgram(String programName) {
		rollback(programName, MIN_TOKEN_INDEX);
	}

	/** If op.index > lastRewriteTokenIndexes, just add to the end.
	 *  Otherwise, do linear */
	protected void addToSortedRewriteList(RewriteOperation op) {
		addToSortedRewriteList(DEFAULT_PROGRAM_NAME, op);
	}

	protected void addToSortedRewriteList(String programName, RewriteOperation op) {
		List rewrites = getProgram(programName);
		// if at or beyond last op's index, just append
		if ( op.index>=getLastRewriteTokenIndex(programName) ) {
			rewrites.add(op); // append to list of operations
			// record the index of this operation for next time through
			setLastRewriteTokenIndex(programName, op.index);
			return;
		}
		// not after the last one, so must insert to ordered list
		Comparator comparator = new Comparator() {
			public int compare(Object o, Object o1) {
				RewriteOperation a = (RewriteOperation)o;
				RewriteOperation b = (RewriteOperation)o1;
				if ( a.index<b.index ) return -1;
				if ( a.index>b.index ) return 1;
				return 0;
			}
		};
        int pos = Collections.binarySearch(rewrites, op, comparator);
		if ( pos<0 ) {
			rewrites.add(-pos-1, op);
		}
	}

	public void insertAfter(Token t, String text) {
		insertAfter(DEFAULT_PROGRAM_NAME, t, text);
	}

	public void insertAfter(int index, String text) {
		insertAfter(DEFAULT_PROGRAM_NAME, index, text);
	}

	public void insertAfter(String programName, Token t, String text) {
		insertAfter(programName,((TokenWithIndex)t).getIndex(), text); 
	}

	public void insertAfter(String programName, int index, String text) {
		// to insert after, just insert before next index (even if past end)
		insertBefore(programName,index+1, text); 
	}

	public void insertBefore(Token t, String text) {
		insertBefore(DEFAULT_PROGRAM_NAME, t, text);
	}

	public void insertBefore(int index, String text) {
		insertBefore(DEFAULT_PROGRAM_NAME, index, text);
	}

	public void insertBefore(String programName, Token t, String text) {
		insertBefore(programName, ((TokenWithIndex)t).getIndex(), text);
	}

	public void insertBefore(String programName, int index, String text) {
		addToSortedRewriteList(programName, new InsertBeforeOp(index,text));
	}

	public void replace(int index, String text) {
		replace(DEFAULT_PROGRAM_NAME, index, index, text);
	}

	public void replace(int from, int to, String text) {
		replace(DEFAULT_PROGRAM_NAME, from, to, text);
	}

	public void replace(Token indexT, String text) {
		replace(DEFAULT_PROGRAM_NAME, indexT, indexT, text);
	}

	public void replace(Token from, Token to, String text) {
		replace(DEFAULT_PROGRAM_NAME, from, to, text);
	}

	public void replace(String programName, int from, int to, String text) {
		addToSortedRewriteList(new ReplaceOp(from, to, text));
	}

	public void replace(String programName, Token from, Token to, String text) {
		replace(programName,
				((TokenWithIndex)from).getIndex(),
				((TokenWithIndex)to).getIndex(),
				text);
	}

	public void delete(int index) {
		delete(DEFAULT_PROGRAM_NAME, index, index);
	}

	public void delete(int from, int to) {
		delete(DEFAULT_PROGRAM_NAME, from, to);
	}

	public void delete(Token indexT) {
		delete(DEFAULT_PROGRAM_NAME, indexT, indexT);
	}

	public void delete(Token from, Token to) {
		delete(DEFAULT_PROGRAM_NAME, from, to);
	}

	public void delete(String programName, int from, int to) {
		replace(programName,from,to,null);
	}

	public void delete(String programName, Token from, Token to) {
		replace(programName,from,to,null);
	}

	public void discard(int ttype) {
		discardMask.add(ttype);
	}

	public TokenWithIndex getToken(int i) {
		return (TokenWithIndex)tokens.get(i);
	}

	public int getTokenStreamSize() {
		return tokens.size();
	}

	public String toOriginalString() {
		return toOriginalString(MIN_TOKEN_INDEX, getTokenStreamSize()-1);
	}

	public String toOriginalString(int start, int end) {
		StringBuffer buf = new StringBuffer();
		for (int i=start; i>=MIN_TOKEN_INDEX && i<=end && i<tokens.size(); i++) {
			buf.append(getToken(i).getText());
		}
		return buf.toString();
	}

	public String toString() {
		return toString(MIN_TOKEN_INDEX, getTokenStreamSize());
	}

	public String toString(String programName) {
		return toString(programName, MIN_TOKEN_INDEX, getTokenStreamSize());
	}

	public String toString(int start, int end) {
		return toString(DEFAULT_PROGRAM_NAME, start, end);
	}

	public String toString(String programName, int start, int end) {
		List rewrites = (List)programs.get(programName);
		if ( rewrites==null ) {
			return null; // invalid program
		}
		StringBuffer buf = new StringBuffer();

		/** Index of first rewrite we have not done */
		int rewriteOpIndex = 0;

		int tokenCursor=start;
		while ( tokenCursor>=MIN_TOKEN_INDEX &&
				tokenCursor<=end &&
				tokenCursor<tokens.size() )
		{
			if ( rewriteOpIndex<rewrites.size() ) {
				RewriteOperation op =
						(RewriteOperation)rewrites.get(rewriteOpIndex);
				while ( tokenCursor==op.index && rewriteOpIndex<rewrites.size() ) {
					/*
					System.out.println("execute op "+rewriteOpIndex+
									   " (type "+op.getClass().getName()+")"
									   +" at index "+op.index);
					*/
					tokenCursor = op.execute(buf);
					rewriteOpIndex++;
					if ( rewriteOpIndex<rewrites.size() ) {
						op = (RewriteOperation)rewrites.get(rewriteOpIndex);
					}
				}
			}
			if ( tokenCursor<end ) {
				buf.append(getToken(tokenCursor).getText());
				tokenCursor++;
			}
		}
		// now see if there are operations (append) beyond last token index
		for (int opi=rewriteOpIndex; opi<rewrites.size(); opi++) {
			RewriteOperation op =
					(RewriteOperation)rewrites.get(opi);
			op.execute(buf); // must be insertions if after last token
		}

		return buf.toString();
	}

	public String toDebugString() {
		return toDebugString(MIN_TOKEN_INDEX, getTokenStreamSize());
	}

	public String toDebugString(int start, int end) {
		StringBuffer buf = new StringBuffer();
		for (int i=start; i>=MIN_TOKEN_INDEX && i<=end && i<tokens.size(); i++) {
			buf.append(getToken(i));
		}
		return buf.toString();
	}

	public int getLastRewriteTokenIndex() {
		return getLastRewriteTokenIndex(DEFAULT_PROGRAM_NAME);
	}

	protected int getLastRewriteTokenIndex(String programName) {
		Integer I = (Integer)lastRewriteTokenIndexes.get(programName);
		if ( I==null ) {
			return -1;
		}
		return I.intValue();
	}

	protected void setLastRewriteTokenIndex(String programName, int i) {
		lastRewriteTokenIndexes.put(programName, new Integer(i));
	}

	protected List getProgram(String name) {
		List is = (List)programs.get(name);
		if ( is==null ) {
			is = initializeProgram(name);
		}
		return is;
	}

	private List initializeProgram(String name) {
		List is = new ArrayList(PROGRAM_INIT_SIZE);
		programs.put(name, is);
		return is;
	}
}
