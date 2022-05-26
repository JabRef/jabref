package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.bst.util.BibtexCaseChanger;
import org.jabref.logic.bst.util.BibtexNameFormatter;
import org.jabref.logic.bst.util.BibtexPurify;
import org.jabref.logic.bst.util.BibtexTextPrefix;
import org.jabref.logic.bst.util.BibtexWidth;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A BibTeX Virtual machine that can execute .bst files.
 * <p>
 * Documentation can be found in the original bibtex distribution:
 * <p>
 * <a href="https://www.ctan.org/pkg/bibtex">https://www.ctan.org/pkg/bibtex</a>
 */
public class VM {

    public static final Integer FALSE = 0;

    public static final Integer TRUE = 1;

    private static final Pattern ADD_PERIOD_PATTERN = Pattern.compile("([^\\.\\?\\!\\}\\s])(\\}|\\s)*$");

    private static final Logger LOGGER = LoggerFactory.getLogger(VM.class);

    private List<BstEntry> entries;

    private Map<String, String> strings = new HashMap<>();

    private Map<String, Integer> integers = new HashMap<>();

    private Map<String, BstFunction> functions = new HashMap<>();

    private Stack<Object> stack = new Stack<>();

    private final Map<String, BstFunction> buildInFunctions = new HashMap<>(37);

    private Path path;

    private final ParseTree tree;

    private StringBuilder bbl;

    private String preamble = "";

    private int bstWarning = 1;

    public static class ThrowingErrorListener extends BaseErrorListener {

        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    public static class Identifier {

        public final String name;

        public Identifier(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class Variable {

        public final String name;

        public Variable(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @FunctionalInterface
    public interface BstFunction {
        void execute(BstEntry context);
    }

    public VM(Path path) throws RecognitionException, IOException {
        this(CharStreams.fromPath(path));
        this.path = path;
    }

    public VM(String s) throws RecognitionException {
        this(CharStreams.fromString(s));
    }

    private VM(CharStream bst) throws RecognitionException {
        this(VM.charStream2CommonTree(bst));
    }

    private VM(ParseTree tree) {
        this.tree = tree;
        init();
    }

    private static ParseTree charStream2CommonTree(CharStream query) throws RecognitionException {
        BstLexer lexer = new BstLexer(query);
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        BstParser parser = new BstParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancelationException on parse errors
        return parser.start();
    }

    public void init() {
        buildInFunctions.put(">", this::bstIsGreaterThan);
        buildInFunctions.put("<", this::bstIsLowerThan);
        buildInFunctions.put("=", this::bstEquals);
        buildInFunctions.put("+", this::bstAdd);
        buildInFunctions.put("-", this::bstSubtract);
        buildInFunctions.put("*", this::bstConcat);
        buildInFunctions.put(":=", this::bstAssign);
        buildInFunctions.put("add.period$", this::bstAddPeriod);
        buildInFunctions.put("call.type$", this::bstCallType);
        buildInFunctions.put("change.case$", this::bstChangeCase);
        buildInFunctions.put("chr.to.int$", this::bstChrToInt);
        buildInFunctions.put("cite$", this::bstCite);
        buildInFunctions.put("duplicate$", this::bstDuplicate);
        buildInFunctions.put("empty$", this::bstEmpty);
        buildInFunctions.put("format.name$", this::bstFormatName);
        buildInFunctions.put("if$", this::bstIf);
        buildInFunctions.put("int.to.chr$", this::bstIntToChr);
        buildInFunctions.put("int.to.str$", this::bstIntToStr);
        buildInFunctions.put("missing$", this::bstMissing);
        buildInFunctions.put("newline$", this::bstNewLine);
        buildInFunctions.put("num.names$", this::bstNumNames);
        buildInFunctions.put("pop$", this::bstPop);
        buildInFunctions.put("preamble$", this::bstPreamble);
        buildInFunctions.put("purify$", this::bstPurify);
        buildInFunctions.put("quote$", this::bstQuote);
        buildInFunctions.put("skip$", this::bstSkip);
        buildInFunctions.put("stack$", this::bstStack);
        buildInFunctions.put("substring$", this::bstSubstring);
        buildInFunctions.put("swap$", this::bstSwap);
        buildInFunctions.put("text.length$", this::bstTextLength);
        buildInFunctions.put("text.prefix$", this::bstTextPrefix);
        buildInFunctions.put("top$", this::bstTop);
        buildInFunctions.put("type$", this::bstType);
        buildInFunctions.put("warning$", this::bstWarning);
        buildInFunctions.put("while$", this::bstWhile);
        buildInFunctions.put("width$", this::bstWidth);
        buildInFunctions.put("write$", this::bstWrite);
    }

    /**
     * Pops the top two (integer) literals, compares them, and pushes
     * the integer 1 if the second is greater than the first, 0
     * otherwise.
     */
    private void bstIsGreaterThan(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation >");
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new VMException("Can only compare two integers with >");
        }

        stack.push(((Integer) o1).compareTo((Integer) o2) > 0 ? VM.TRUE : VM.FALSE);
    }

    /**
     * Pops the top two (integer) literals, compares them, and pushes
     * the integer 1 if the second is lower than the first, 0
     * otherwise.
     */
    private void bstIsLowerThan(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation <");
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new VMException("Can only compare two integers with <");
        }

        stack.push(((Integer) o1).compareTo((Integer) o2) < 0 ? VM.TRUE : VM.FALSE);
    }

    /**
     * Pops the top two (both integer or both string) literals, compares
     * them, and pushes the integer 1 if they're equal, 0 otherwise.
     */
    private void bstEquals(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation =");
        }
        Object o1 = stack.pop();
        Object o2 = stack.pop();

        if ((o1 == null) ^ (o2 == null)) {
            stack.push(VM.FALSE);
            return;
        }

        if ((o1 == null) && (o2 == null)) {
            stack.push(VM.TRUE);
            return;
        }

        stack.push(o1.equals(o2) ? VM.TRUE : VM.FALSE);
    }

    /**
     *  Pops the top two (integer) literals and pushes their sum.
     */
    private void bstAdd(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation +");
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new VMException("Can only compare two integers with +");
        }

        stack.push((Integer) o1 + (Integer) o2);
    }

    /**
     * Pops the top two (integer) literals and pushes their difference
     * (the first subtracted from the second).
     */
    private void bstSubtract(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation -");
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new VMException("Can only subtract two integers with -");
        }

        stack.push((Integer) o1 - (Integer) o2);
    }

    /**
     * Pops the top two (string) literals, concatenates them (in reverse
     * order, that is, the order in which pushed), and pushes the
     * resulting string.
     */
    private void bstConcat(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation *");
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (o1 == null) {
            o1 = "";
        }
        if (o2 == null) {
            o2 = "";
        }

        if (!((o1 instanceof String) && (o2 instanceof String))) {
            LOGGER.error("o1: {} ({})", o1, o1.getClass());
            LOGGER.error("o2: {} ({})", o2, o2.getClass());
            throw new VMException("Can only concatenate two String with *");
        }

        stack.push(o1.toString() + o2);
    }

    /**
     * Pops the top two literals and assigns to the first (which must be
     * a global or entry variable) the value of the second.
     */
    private void bstAssign(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Invalid call to operation :=");
        }
        Object o1 = stack.pop();
        Object o2 = stack.pop();
        doBstAssign(context, o1, o2);
    }

    private boolean doBstAssign(BstEntry context, Object o1, Object o2) {
        if (!(o1 instanceof VM.Identifier) || !((o2 instanceof String) || (o2 instanceof Integer))) {
            throw new VMException("Invalid parameters");
        }

        String name = ((VM.Identifier) o1).getName();

        if (o2 instanceof String) {
            if ((context != null) && context.localStrings.containsKey(name)) {
                context.localStrings.put(name, (String) o2);
                return true;
            }

            if (strings.containsKey(name)) {
                strings.put(name, (String) o2);
                return true;
            }
            return false;
        }

        if ((context != null) && context.localIntegers.containsKey(name)) {
            context.localIntegers.put(name, (Integer) o2);
            return true;
        }

        if (integers.containsKey(name)) {
            integers.put(name, (Integer) o2);
            return true;
        }
        return false;
    }

    /**
     * Pops the top (string) literal, adds a `.' to it if the last non
     * '}' character isn't a `.', `?', or `!', and pushes this resulting
     * string.
     */
    private void bstAddPeriod(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation add.period$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String s)) {
            throw new VMException("Can only add a period to a string for add.period$");
        }

        Matcher m = ADD_PERIOD_PATTERN.matcher(s);

        if (m.find()) {
            StringBuilder sb = new StringBuilder();
            m.appendReplacement(sb, m.group(1));
            sb.append('.');
            String group2 = m.group(2);
            if (group2 != null) {
                sb.append(m.group(2));
            }
            stack.push(sb.toString());
        } else {
            stack.push(s);
        }
    }

    /**
     * Executes the function whose name is the entry type of an entry.
     * For example if an entry is of type book, this function executes
     * the book function. When given as an argument to the ITERATE
     * command, call.type$ actually produces the output for the entries.
     * For an entry with an unknown type, it executes the function
     * default.type. Thus you should define (before the READ command)
     * one function for each standard entry type as well as a
     * default.type function.
     */
    private void bstCallType(BstEntry context) {
        if (context == null) {
            throw new VMException("Call.type$ can only be called from within a context (ITERATE or REVERSE).");
        }
        VM.this.execute(context.entry.getType().getName(), context);
    }

    /**
     * Pops the top two (string) literals; it changes the case of the second
     * according to the specifications of the first, as follows. (Note: The word
     * `letters' in the next sentence refers only to those at brace-level 0, the
     * top-most brace level; no other characters are changed, except perhaps for
     * \special characters", described in Section 4.) If the first literal is the
     * string `t', it converts to lower case all letters except the very first
     * character in the string, which it leaves alone, and except the first
     * character following any colon and then nonnull white space, which it also
     * leaves alone; if it's the string `l', it converts all letters to lower case;
     * and if it's the string `u', it converts all letters to upper case. It then
     * pushes this resulting string. If either type is incorrect, it complains and
     * pushes the null string; however, if both types are correct but the
     * specification string (i.e., the first string) isn't one of the legal ones, it
     * merely pushes the second back onto the stack, after complaining. (Another
     * note: It ignores case differences in the specification string; for example,
     * the strings t and T are equivalent for the purposes of this built-in
     * function.)
     */
    private void bstChangeCase(BstEntry value) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation change.case$");
        }

        Object o1 = stack.pop();
        if (!((o1 instanceof String) && (((String) o1).length() == 1))) {
            throw new VMException("A format string of length 1 is needed for change.case$");
        }

        Object o2 = stack.pop();
        if (!(o2 instanceof String s)) {
            throw new VMException("A string is needed as second parameter for change.case$");
        }

        char format = ((String) o1).toLowerCase(Locale.ROOT).charAt(0);

        stack.push(BibtexCaseChanger.changeCase(s, BibtexCaseChanger.FORMAT_MODE.getFormatModeForBSTFormat(format)));
    }

    /**
     * Pops the top (string) literal, makes sure it's a single
     * character, converts it to the corresponding ASCII integer, and
     * pushes this integer.
     */
    private void bstChrToInt(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation chr.to.int$");
        }
        Object o1 = stack.pop();

        if (!((o1 instanceof String s) && (((String) o1).length() == 1))) {
            throw new VMException("Can only perform chr.to.int$ on string with length 1");
        }

        stack.push((int) s.charAt(0));
    }

    /**
     * Pushes the string that was the \cite-command argument for this
     * entry.
     */
    private void bstCite(BstEntry context) {
        if (context == null) {
            throw new VMException("Must have an entry to cite$");
        }
        stack.push(context.entry.getCitationKey().orElse(null));
    }

    /**
     * Pops the top literal from the stack and pushes two copies of it.
     */
    private void bstDuplicate(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation duplicate$");
        }
        Object o1 = stack.pop();

        stack.push(o1);
        stack.push(o1);
    }

    /**
     * Pops the top literal and pushes the integer 1 if it's a missing
     * field or a string having no non-white-space characters, 0
     * otherwise.
     */
    private void bstEmpty(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation empty$");
        }
        Object o1 = stack.pop();

        if (o1 == null) {
            stack.push(VM.TRUE);
            return;
        }

        if (!(o1 instanceof String s)) {
            throw new VMException("Operand does not match function empty$");
        }

        stack.push("".equals(s.trim()) ? VM.TRUE : VM.FALSE);
    }

    /**
     * The |built_in| function {\.{format.name\$}} pops the
     * top three literals (they are a string, an integer, and a string
     * literal, in that order). The last string literal represents a
     * name list (each name corresponding to a person), the integer
     * literal specifies which name to pick from this list, and the
     * first string literal specifies how to format this name, as
     * described in the \BibTeX\ documentation. Finally, this function
     * pushes the formatted name. If any of the types is incorrect, it
     * complains and pushes the null string.
     */
    private void bstFormatName(BstEntry context) {
        if (stack.size() < 3) {
            throw new VMException("Not enough operands on stack for operation format.name$");
        }
        Object o1 = stack.pop();
        Object o2 = stack.pop();
        Object o3 = stack.pop();

        if (!(o1 instanceof String) && !(o2 instanceof Integer) && !(o3 instanceof String)) {
            // warning("A string is needed for change.case$");
            stack.push("");
            return;
        }

        String format = (String) o1;
        Integer name = (Integer) o2;
        String names = (String) o3;

        if (names == null) {
            stack.push("");
        } else {
            AuthorList a = AuthorList.parse(names);
            if (name > a.getNumberOfAuthors()) {
                throw new VMException("Author Out of Bounds. Number " + name + " invalid for " + names);
            }
            Author author = a.getAuthor(name - 1);

            stack.push(BibtexNameFormatter.formatName(author, format));
        }
    }

    /**
     * Pops the top three literals (they are two function literals and
     * an integer literal, in that order); if the integer is greater
     * than 0, it executes the second literal, else it executes the
     * first.
     */
    private void bstIf(BstEntry context) {
        if (stack.size() < 3) {
            throw new VMException("Not enough operands on stack for operation =");
        }
        Object f1 = stack.pop();
        Object f2 = stack.pop();
        Object i = stack.pop();

        if (!((f1 instanceof VM.Identifier) || (f1 instanceof Tree))
                && ((f2 instanceof VM.Identifier) || (f2 instanceof Tree)) && (i instanceof Integer)) {
            throw new VMException("Expecting two functions and an integer for if$.");
        }

        if ((Integer) i > 0) {
            VM.this.executeInContext(f2, context);
        } else {
            VM.this.executeInContext(f1, context);
        }
    }

    /**
     * Pops the top (integer) literal, interpreted as the ASCII integer
     * value of a single character, converts it to the corresponding
     * single-character string, and pushes this string.
     */
    private void bstIntToChr(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation int.to.chr$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof Integer i)) {
            throw new VMException("Can only perform operation int.to.chr$ on an Integer");
        }

        stack.push(String.valueOf((char) i.intValue()));
    }

    /**
     * Pops the top (integer) literal, converts it to its (unique)
     * string equivalent, and pushes this string.
     */
    private void bstIntToStr(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation int.to.str$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof Integer)) {
            throw new VMException("Can only transform an integer to an string using int.to.str$");
        }

        stack.push(o1.toString());
    }

    /**
     * Pops the top literal and pushes the integer 1 if it's a missing
     * field, 0 otherwise.
     */
    private void bstMissing(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation missing$");
        }
        Object o1 = stack.pop();

        if (o1 == null) {
            stack.push(VM.TRUE);
            return;
        }

        if (!(o1 instanceof String)) {
            LOGGER.warn("Not a string or missing field in operation missing$");
            stack.push(VM.TRUE);
            return;
        }

        stack.push(VM.FALSE);
    }

    /**
     * Writes onto the bbl file what is accumulated in the output buffer.
     * It writes a blank line if and only if the output buffer is empty.
     * Since write$ does reasonable line breaking, you should use this
     * function only when you want a blank line or an explicit line
     * break.
     */
    private void bstNewLine(BstEntry context) {
        VM.this.bbl.append('\n');
    }

    /**
     * Pops the top (string) literal and pushes the number of names the
     * string represents one plus the number of occurrences of the
     * substring "and" (ignoring case differences) surrounded by
     * non-null white-space at the top brace level.
     */
    private void bstNumNames(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation num.names$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String s)) {
            throw new VMException("Need a string at the top of the stack for num.names$");
        }

        stack.push(AuthorList.parse(s).getNumberOfAuthors());
    }

    /**
     * Pops the top of the stack but doesn't print it; this gets rid of
     * an unwanted stack literal.
     */
    private void bstPop(BstEntry context) {
        stack.pop();
    }

    /**
     * The |built_in| function {\.{preamble\$}} pushes onto the stack
     * the concatenation of all the \.{preamble} strings read from the
     * database files. (or the empty string if there were none)
     * '@PREAMBLE' strings are read from the database files.
     */
    private void bstPreamble(BstEntry context) {
        stack.push(preamble);
    }

    /**
     * Pops the top (string) literal, removes nonalphanumeric characters
     * except for white-space characters and hyphens and ties (these all get
     * converted to a space), removes certain alphabetic characters
     * contained in the control sequences associated with a \special
     * character", and pushes the resulting string.
     */
    private void bstPurify(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation purify$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String)) {
            LOGGER.warn("A string is needed for purify$");
            stack.push("");
            return;
        }

        stack.push(BibtexPurify.purify((String) o1));
    }

    /**
     * Pushes the string consisting of the double-quote character.
     */
    private void bstQuote(BstEntry context) {
        stack.push("\"");
    }

    /**
     * Does nothing.
     */
    private void bstSkip(BstEntry context) {
        // no-op
    }

    /**
     * Pops and prints the whole stack; it's meant to be used for style
     * designers while debugging.
     */
    private void bstStack(BstEntry context) {
        while (!stack.empty()) {
            LOGGER.debug("Stack entry {}", stack.pop());
        }
    }

    /**
     * Pops the top three literals (they are the two integers literals
     * len and start, and a string literal, in that order). It pushes
     * the substring of the (at most) len consecutive characters
     * starting at the startth character (assuming 1-based indexing) if
     * start is positive, and ending at the start-th character
     * (including) from the end if start is negative (where the first
     * character from the end is the last character).
     */
    private void bstSubstring(BstEntry context) {
        if (stack.size() < 3) {
            throw new VMException("Not enough operands on stack for operation substring$");
        }
        Object o1 = stack.pop();
        Object o2 = stack.pop();
        Object o3 = stack.pop();

        if (!((o1 instanceof Integer len) && (o2 instanceof Integer start) && (o3 instanceof String s))) {
            throw new VMException("Expecting two integers and a string for substring$");
        }

        int lenI = len;
        int startI = start;

        if (lenI > (Integer.MAX_VALUE / 2)) {
            lenI = Integer.MAX_VALUE / 2;
        }

        if (startI > (Integer.MAX_VALUE / 2)) {
            startI = Integer.MAX_VALUE / 2;
        }

        if (startI < (Integer.MIN_VALUE / 2)) {
            startI = -Integer.MIN_VALUE / 2;
        }

        if (startI < 0) {
            startI += s.length() + 1;
            startI = Math.max(1, (startI + 1) - lenI);
        }
        stack.push(s.substring(startI - 1, Math.min((startI - 1) + lenI, s.length())));
    }

    /**
     * Swaps the top two literals on the stack. text.length$ Pops the
     * top (string) literal, and pushes the number of text characters
     * it contains, where an accented character (more precisely, a
     * \special character", defined in Section 4) counts as a single
     * text character, even if it's missing its matching right brace,
     * and where braces don't count as text characters.
     */
    private void bstSwap(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation swap$");
        }
        Object f1 = stack.pop();
        Object f2 = stack.pop();

        stack.push(f1);
        stack.push(f2);
    }

    /**
     * text.length$ Pops the top (string) literal, and pushes the number
     * of text characters it contains, where an accented character (more
     * precisely, a "special character", defined in Section 4) counts as
     * a single text character, even if it's missing its matching right
     * brace, and where braces don't count as text characters.
     *
     * From BibTeXing: For the purposes of counting letters in labels,
     * BibTEX considers everything contained inside the braces as a
     * single letter.
     */
    private void bstTextLength(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation text.length$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String s)) {
            throw new VMException("Can only perform operation on a string text.length$");
        }

        char[] c = s.toCharArray();
        int result = 0;
        int i = 0;
        int n = s.length();
        int braceLevel = 0;

        while (i < n) {
            i++;
            if (c[i - 1] == '{') {
                braceLevel++;
                if ((braceLevel == 1) && (i < n)) {
                    if (c[i] == '\\') {
                        i++; // skip over backslash
                        while ((i < n) && (braceLevel > 0)) {
                            if (c[i] == '}') {
                                braceLevel--;
                            } else if (c[i] == '{') {
                                braceLevel++;
                            }
                            i++;
                        }
                        result++;
                    }
                }
            } else if (c[i - 1] == '}') {
                if (braceLevel > 0) {
                    braceLevel--;
                }
            } else {
                result++;
            }
        }
        stack.push(result);
    }

    /**
     * Pops the top two literals (the integer literal len and a string
     * literal, in that order). It pushes the substring of the (at most) len
     * consecutive text characters starting from the beginning of the
     * string. This function is similar to substring$, but this one
     * considers a \special character", even if it's missing its matching
     * right brace, to be a single text character (rather than however many
     * ASCII characters it actually comprises), and this function doesn't
     * consider braces to be text characters; furthermore, this function
     * appends any needed matching right braces.
     */
    private void bstTextPrefix(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation text.prefix$");
        }

        Object o1 = stack.pop();
        if (!(o1 instanceof Integer)) {
            LOGGER.warn("An integer is needed as first parameter to text.prefix$");
            stack.push("");
            return;
        }

        Object o2 = stack.pop();
        if (!(o2 instanceof String)) {
            LOGGER.warn("A string is needed as second parameter to text.prefix$");
            stack.push("");
            return;
        }

        stack.push(BibtexTextPrefix.textPrefix((Integer) o1, (String) o2));
    }

    /**
     * Pops and prints the top of the stack to the log file. It's useful for debugging.
     */
    private void bstTop(BstEntry context) {
        LOGGER.debug("Stack entry {}", stack.pop());
    }

    /**
     * Pushes the current entry's type (book, article, etc.), but pushes
     * the null string if the type is either unknown or undefined.
     */
    private void bstType(BstEntry context) {
        if (context == null) {
            throw new VMException("type$ need a context.");
        }

        stack.push(context.entry.getType().getName());
    }

    /**
     * Pops the top (string) literal and prints it following a warning
     * message. This also increments a count of the number of warning
     * messages issued.
     */
    private void bstWarning(BstEntry context) {
        LOGGER.warn("Warning (#" + (bstWarning++) + "): " + stack.pop());
    }

    /**
     * Pops the top two (function) literals, and keeps executing the
     * second as long as the (integer) literal left on the stack by
     * executing the first is greater than 0.
     */
    private void bstWhile(BstEntry context) {
        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation while$");
        }
        Object f2 = stack.pop();
        Object f1 = stack.pop();

        if (!((f1 instanceof VM.Identifier) || (f1 instanceof Tree))
                && ((f2 instanceof VM.Identifier) || (f2 instanceof Tree))) {
            throw new VMException("Expecting two functions for while$.");
        }

        do {
            executeInContext(f1, context);

            Object i = stack.pop();
            if (!(i instanceof Integer)) {
                throw new VMException("First parameter to while has to return an integer but was " + i);
            }
            if ((Integer) i <= 0) {
                break;
            }
            executeInContext(f2, context);
        } while (true);
    }

    /**
     * The |built_in| function {\.{width\$}} pops the top (string) literal and
     * pushes the integer that represents its width in units specified by the
     * |char_width| array. This function takes the literal literally; that is, it
     * assumes each character in the string is to be printed as is, regardless of
     * whether the character has a special meaning to \TeX, except that special
     * characters (even without their |right_brace|s) are handled specially. If the
     * literal isn't a string, it complains and pushes~0.
     */
    private void bstWidth(BstEntry context) {
        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation width$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String)) {
            LOGGER.warn("A string is needed for width$");
            stack.push(0);
            return;
        }

        stack.push(BibtexWidth.width((String) o1));
    }

    /**
     * Pops the top (string) literal and writes it on the output buffer
     * (which will result in stuff being written onto the bbl file when
     * the buffer fills up).
     */
    private void bstWrite(BstEntry context) {
        String s = (String) stack.pop();
        bbl.append(s);
    }

    public String run(BibDatabase db) {
        preamble = db.getPreamble().orElse("");
        return run(db.getEntries());
    }

    public String run(Collection<BibEntry> bibtex) {
        return this.run(bibtex, null);
    }

    /**
     * Transforms the given list of BibEntries to a rendered list of references using the underlying bst file
     *
     * @param bibEntries  list of entries to convert
     * @param bibDatabase (may be null) the bibDatabase used for resolving strings / crossref
     * @return list of references in plain text form
     */
    public String run(Collection<BibEntry> bibEntries, BibDatabase bibDatabase) {
        Objects.requireNonNull(bibEntries);

        // Reset
        bbl = new StringBuilder();

        strings = new HashMap<>();

        integers = new HashMap<>();
        integers.put("entry.max$", Integer.MAX_VALUE);
        integers.put("global.max$", Integer.MAX_VALUE);

        functions = new HashMap<>();
        functions.putAll(buildInFunctions);

        stack = new Stack<>();

        // Create entries
        entries = new ArrayList<>(bibEntries.size());
        for (BibEntry entry : bibEntries) {
            entries.add(new BstEntry(entry));
        }

        // Go
        for (int i = 0; i < tree.getChildCount(); i++) {
            Tree child = tree.getChild(i);
            switch (child.getType()) {
                case BstParser.STRINGS ->
                        strings(child);
                case BstParser.INTEGERS ->
                        integers(child);
                case BstParser.FUNCTION ->
                        function(child);
                case BstParser.EXECUTE ->
                        execute(child);
                case BstParser.SORT ->
                        sort();
                case BstParser.ITERATE ->
                        iterate(child);
                case BstParser.REVERSE ->
                        reverse(child);
                case BstParser.ENTRY ->
                        entry(child);
                case BstParser.READ ->
                        read(bibDatabase);
                case BstParser.MACRO ->
                        macro(child);
                default ->
                        LOGGER.info("Unknown type: {}", child.getType());
            }
        }

        return bbl.toString();
    }

    /**
     * Dredges up from the database file the field values for each entry in the list. It has no arguments. If a database
     * entry doesn't have a value for a field (and probably no database entry will have a value for every field), that
     * field variable is marked as missing for the entry.
     * <p>
     * We use null for the missing entry designator.
     */
    private void read(BibDatabase bibDatabase) {
        FieldWriter fieldWriter = new FieldWriter(new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences()));
        for (BstEntry e : entries) {
            for (Map.Entry<String, String> mEntry : e.fields.entrySet()) {
                Field field = FieldFactory.parseField(mEntry.getKey());
                String fieldValue = e.entry.getResolvedFieldOrAlias(field, bibDatabase)
                                           .map(content -> {
                                               try {
                                                   String result = fieldWriter.write(field, content);
                                                   if (result.startsWith("{")) {
                                                       // Strip enclosing {} from the output
                                                       return result.substring(1, result.length() - 1);
                                                   }
                                                   if (field == StandardField.MONTH) {
                                                       // We don't have the internal BibTeX strings at hand.
                                                       // We nevertheless want to have the full month name.
                                                       // Thus, we lookup the full month name here.
                                                       return Month.parse(result)
                                                                   .map(Month::getFullName)
                                                                   .orElse(result);
                                                   }
                                                   return result;
                                               } catch (InvalidFieldValueException invalidFieldValueException) {
                                                   // in case there is something wrong with the content, just return the content itself
                                                   return content;
                                               }
                                           })
                                           .orElse(null);
                mEntry.setValue(fieldValue);
            }
        }

        for (BstEntry e : entries) {
            if (!e.fields.containsKey(StandardField.CROSSREF.getName())) {
                e.fields.put(StandardField.CROSSREF.getName(), null);
            }
        }
    }

    /**
     * Defines a string macro. It has two arguments; the first is the macro's name, which is treated like any other
     * variable or function name, and the second is its definition, which must be double-quote-delimited. You must have
     * one for each three-letter month abbreviation; in addition, you should have one for common journal names. The
     * user's database may override any definition you define using this command. If you want to define a string the
     * user can't touch, use the FUNCTION command, which has a compatible syntax.
     */
    private void macro(Tree child) {
        String name = child.getChild(0).getText();
        String replacement = child.getChild(1).getText();
        functions.put(name, new MacroFunction(replacement));
    }

    public class MacroFunction implements BstFunction {

        private final String replacement;

        public MacroFunction(String replacement) {
            this.replacement = replacement;
        }

        @Override
        public void execute(BstEntry context) {
            VM.this.push(replacement);
        }
    }

    /**
     * Declares the fields and entry variables. It has three arguments, each a (possibly empty) list of variable names.
     * The three lists are of: fields, integer entry variables, and string entry variables. There is an additional field
     * that BibTEX automatically declares, crossref, used for cross referencing. And there is an additional string entry
     * variable automatically declared, sort.key$, used by the SORT command. Each of these variables has a value for
     * each entry on the list.
     */
    private void entry(Tree child) {
        // Fields first
        Tree t = child.getChild(0);

        for (int i = 0; i < t.getChildCount(); i++) {
            String name = t.getChild(i).getText();

            for (BstEntry entry : entries) {
                entry.fields.put(name, null);
            }
        }

        // Integers
        t = child.getChild(1);

        for (int i = 0; i < t.getChildCount(); i++) {
            String name = t.getChild(i).getText();

            for (BstEntry entry : entries) {
                entry.localIntegers.put(name, 0);
            }
        }
        // Strings
        t = child.getChild(2);

        for (int i = 0; i < t.getChildCount(); i++) {
            String name = t.getChild(i).getText();
            for (BstEntry entry : entries) {
                entry.localStrings.put(name, null);
            }
        }
        for (BstEntry entry : entries) {
            entry.localStrings.put("sort.key$", null);
        }
    }

    private void reverse(Tree child) {
        BstFunction f = functions.get(child.getChild(0).getText());

        ListIterator<BstEntry> i = entries.listIterator(entries.size());
        while (i.hasPrevious()) {
            f.execute(i.previous());
        }
    }

    private void iterate(Tree child) {
        BstFunction f = functions.get(child.getChild(0).getText());

        for (BstEntry entry : entries) {
            f.execute(entry);
        }
    }

    /**
     * Sorts the entry list using the values of the string entry variable sort.key$. It has no arguments.
     */
    private void sort() {
        entries.sort(Comparator.comparing(o -> (o.localStrings.get("sort.key$"))));
    }

    private void executeInContext(Object o, BstEntry context) {
        if (o instanceof Tree t) {
            new StackFunction(t).execute(context);
        } else if (o instanceof Identifier) {
            execute(((Identifier) o).getName(), context);
        }
    }

    private void execute(Tree child) {
        execute(child.getChild(0).getText(), null);
    }

    public class StackFunction implements BstFunction {

        private final Tree localTree;

        public StackFunction(Tree stack) {
            localTree = stack;
        }

        public Tree getTree() {
            return localTree;
        }

        @Override
        public void execute(BstEntry context) {
            for (int i = 0; i < localTree.getChildCount(); i++) {
                Tree c = localTree.getChild(i);
                try {

                    switch (c.getType()) {
                        case BstParser.STRING -> {
                            String s = c.getText();
                            push(s.substring(1, s.length() - 1));
                        }
                        case BstParser.INTEGER ->
                                push(Integer.parseInt(c.getText().substring(1)));
                        case BstParser.QUOTED ->
                                push(new Identifier(c.getText().substring(1)));
                        case BstParser.STACK ->
                                push(c);
                        default ->
                                VM.this.execute(c.getText(), context);
                    }
                } catch (VMException e) {
                    if (path == null) {
                        LOGGER.error("ERROR " + e.getMessage() + " (" + c.getLine() + ")");
                    } else {
                        LOGGER.error("ERROR " + e.getMessage() + " (" + path + ":"
                                + c.getLine() + ")");
                    }
                    throw e;
                }
            }
        }
    }

    private void push(Tree t) {
        stack.push(t);
    }

    private void execute(String name, BstEntry context) {
        if (context != null) {
            if (context.fields.containsKey(name)) {
                stack.push(context.fields.get(name));
                return;
            }
            if (context.localStrings.containsKey(name)) {
                stack.push(context.localStrings.get(name));
                return;
            }
            if (context.localIntegers.containsKey(name)) {
                stack.push(context.localIntegers.get(name));
                return;
            }
        }
        if (strings.containsKey(name)) {
            stack.push(strings.get(name));
            return;
        }
        if (integers.containsKey(name)) {
            stack.push(integers.get(name));
            return;
        }

        if (functions.containsKey(name)) {
            // OK to have a null context
            functions.get(name).execute(context);
            return;
        }

        throw new VMException("No matching identifier found: " + name);
    }

    private void function(Tree child) {
        String name = child.getChild(0).getText();
        Tree localStack = child.getChild(1);
        functions.put(name, new StackFunction(localStack));
    }

    /**
     * Declares global integer variables. It has one argument, a list of variable names. There are two such
     * automatically-declared variables, entry.max$ and global.max$, used for limiting the lengths of string vari-
     * ables. You may have any number of these commands, but a variable's declaration must precede its use.
     */
    private void integers(Tree child) {
        Tree t = child.getChild(0);

        for (int i = 0; i < t.getChildCount(); i++) {
            String name = t.getChild(i).getText();
            integers.put(name, 0);
        }
    }

    /**
     * Declares global string variables. It has one argument, a list of variable names. You may have any number of these
     * commands, but a variable's declaration must precede its use.
     */
    private void strings(Tree child) {
        Tree t = child.getChild(0);

        for (int i = 0; i < t.getChildCount(); i++) {
            String name = t.getChild(i).getText();
            strings.put(name, null);
        }
    }

    private void push(Integer integer) {
        stack.push(integer);
    }

    private void push(String string) {
        stack.push(string);
    }

    private void push(Identifier identifier) {
        stack.push(identifier);
    }

    public Map<String, String> getStrings() {
        return strings;
    }

    public Map<String, Integer> getIntegers() {
        return integers;
    }

    public List<BstEntry> getEntries() {
        return entries;
    }

    public Map<String, BstFunction> getFunctions() {
        return functions;
    }

    public Stack<Object> getStack() {
        return stack;
    }
}
