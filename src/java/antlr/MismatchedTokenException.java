package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;
import antlr.collections.AST;

public class MismatchedTokenException extends RecognitionException {
    // Token names array for formatting
    String[] tokenNames;
    // The token that was encountered
    public Token token;
    // The offending AST node if tree walking
    public AST node;

    String tokenText = null; // taken from node or token object

    // Types of tokens
    public static final int TOKEN = 1;
    public static final int NOT_TOKEN = 2;
    public static final int RANGE = 3;
    public static final int NOT_RANGE = 4;
    public static final int SET = 5;
    public static final int NOT_SET = 6;
    // One of the above
    public int mismatchType;

    // For TOKEN/NOT_TOKEN and RANGE/NOT_RANGE
    public int expecting;

    // For RANGE/NOT_RANGE (expecting is lower bound of range)
    public int upper;

    // For SET/NOT_SET
    public BitSet set;

    /** Looking for AST wildcard, didn't find it */
    public MismatchedTokenException() {
        super("Mismatched Token: expecting any AST node", "<AST>", -1, -1);
    }

    // Expected range / not range
    public MismatchedTokenException(String[] tokenNames_, AST node_, int lower, int upper_, boolean matchNot) {
        super("Mismatched Token", "<AST>", node_==null? -1:node_.getLine(), node_==null? -1:node_.getColumn());
        tokenNames = tokenNames_;
        node = node_;
        if (node_ == null) {
            tokenText = "<empty tree>";
        }
        else {
            tokenText = node_.toString();
        }
        mismatchType = matchNot ? NOT_RANGE : RANGE;
        expecting = lower;
        upper = upper_;
    }

    // Expected token / not token
    public MismatchedTokenException(String[] tokenNames_, AST node_, int expecting_, boolean matchNot) {
		super("Mismatched Token", "<AST>", node_==null? -1:node_.getLine(), node_==null? -1:node_.getColumn());
        tokenNames = tokenNames_;
        node = node_;
        if (node_ == null) {
            tokenText = "<empty tree>";
        }
        else {
            tokenText = node_.toString();
        }
        mismatchType = matchNot ? NOT_TOKEN : TOKEN;
        expecting = expecting_;
    }

    // Expected BitSet / not BitSet
    public MismatchedTokenException(String[] tokenNames_, AST node_, BitSet set_, boolean matchNot) {
		super("Mismatched Token", "<AST>", node_==null? -1:node_.getLine(), node_==null? -1:node_.getColumn());
        tokenNames = tokenNames_;
        node = node_;
        if (node_ == null) {
            tokenText = "<empty tree>";
        }
        else {
            tokenText = node_.toString();
        }
        mismatchType = matchNot ? NOT_SET : SET;
        set = set_;
    }

    // Expected range / not range
    public MismatchedTokenException(String[] tokenNames_, Token token_, int lower, int upper_, boolean matchNot, String fileName_) {
        super("Mismatched Token", fileName_, token_.getLine(), token_.getColumn());
        tokenNames = tokenNames_;
        token = token_;
        tokenText = token_.getText();
        mismatchType = matchNot ? NOT_RANGE : RANGE;
        expecting = lower;
        upper = upper_;
    }

    // Expected token / not token
    public MismatchedTokenException(String[] tokenNames_, Token token_, int expecting_, boolean matchNot, String fileName_) {
        super("Mismatched Token", fileName_, token_.getLine(), token_.getColumn());
        tokenNames = tokenNames_;
        token = token_;
        tokenText = token_.getText();
        mismatchType = matchNot ? NOT_TOKEN : TOKEN;
        expecting = expecting_;
    }

    // Expected BitSet / not BitSet
    public MismatchedTokenException(String[] tokenNames_, Token token_, BitSet set_, boolean matchNot, String fileName_) {
        super("Mismatched Token", fileName_, token_.getLine(), token_.getColumn());
        tokenNames = tokenNames_;
        token = token_;
        tokenText = token_.getText();
        mismatchType = matchNot ? NOT_SET : SET;
        set = set_;
    }

    /**
     * Returns a clean error message (no line number/column information)
     */
    public String getMessage() {
        StringBuffer sb = new StringBuffer();

        switch (mismatchType) {
            case TOKEN:
                sb.append("expecting " + tokenName(expecting) + ", found '" + tokenText + "'");
                break;
            case NOT_TOKEN:
                sb.append("expecting anything but " + tokenName(expecting) + "; got it anyway");
                break;
            case RANGE:
                sb.append("expecting token in range: " + tokenName(expecting) + ".." + tokenName(upper) + ", found '" + tokenText + "'");
                break;
            case NOT_RANGE:
                sb.append("expecting token NOT in range: " + tokenName(expecting) + ".." + tokenName(upper) + ", found '" + tokenText + "'");
                break;
            case SET:
            case NOT_SET:
                sb.append("expecting " + (mismatchType == NOT_SET ? "NOT " : "") + "one of (");
                int[] elems = set.toArray();
                for (int i = 0; i < elems.length; i++) {
                    sb.append(" ");
                    sb.append(tokenName(elems[i]));
                }
                sb.append("), found '" + tokenText + "'");
                break;
            default :
                sb.append(super.getMessage());
                break;
        }

        return sb.toString();
    }

    private String tokenName(int tokenType) {
        if (tokenType == Token.INVALID_TYPE) {
            return "<Set of tokens>";
        }
        else if (tokenType < 0 || tokenType >= tokenNames.length) {
            return "<" + String.valueOf(tokenType) + ">";
        }
        else {
            return tokenNames[tokenType];
        }
    }
}
