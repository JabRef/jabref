package org.jabref.logic.bst;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bst.util.BstCaseChanger;
import org.jabref.logic.bst.util.BstNameFormatter;
import org.jabref.logic.bst.util.BstPurifier;
import org.jabref.logic.bst.util.BstTextPrefixer;
import org.jabref.logic.bst.util.BstWidthCalculator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BstFunctions {
    private static final Logger LOGGER = LoggerFactory.getLogger(BstFunctions.class);
    private static final Pattern ADD_PERIOD_PATTERN = Pattern.compile("([^\\.\\?\\!\\}\\s])(\\}|\\s)*$");

    private final Map<String, String> strings;
    private final Map<String, Integer> integers;
    private final Map<String, BstFunction> functions;
    private final String preamble;

    private final Stack<Object> stack;
    private final StringBuilder bbl;

    private int bstWarning = 0;

    @FunctionalInterface
    public interface BstFunction {

        void execute(BstVMVisitor visitor, ParserRuleContext ctx);

        default void execute(BstVMVisitor visitor, ParserRuleContext ctx, BstEntry bstEntryContext) {
            this.execute(visitor, ctx);
        }
    }

    public BstFunctions(BstVMContext bstVMContext,
                        StringBuilder bbl) {
        this.strings = bstVMContext.strings();
        this.integers = bstVMContext.integers();
        this.functions = bstVMContext.functions();
        this.preamble = Optional.ofNullable(bstVMContext.bibDatabase()).flatMap(BibDatabase::getPreamble).orElse("");
        this.stack = bstVMContext.stack();

        this.bbl = bbl;
    }

    protected Map<String, BstFunction> getBuiltInFunctions() {
        Map<String, BstFunction> builtInFunctions = new HashMap<>();

        builtInFunctions.put(">", this::bstIsGreaterThan);
        builtInFunctions.put("<", this::bstIsLowerThan);
        builtInFunctions.put("=", this::bstEquals);
        builtInFunctions.put("+", this::bstAdd);
        builtInFunctions.put("-", this::bstSubtract);
        builtInFunctions.put("*", this::bstConcat);
        builtInFunctions.put(":=", new BstAssignFunction());
        builtInFunctions.put("add.period$", this::bstAddPeriod);
        builtInFunctions.put("call.type$", new BstCallTypeFunction());
        builtInFunctions.put("change.case$", this::bstChangeCase);
        builtInFunctions.put("chr.to.int$", this::bstChrToInt);
        builtInFunctions.put("cite$", new BstCiteFunction());
        builtInFunctions.put("duplicate$", this::bstDuplicate);
        builtInFunctions.put("empty$", this::bstEmpty);
        builtInFunctions.put("format.name$", this::bstFormatName);
        builtInFunctions.put("if$", this::bstIf);
        builtInFunctions.put("int.to.chr$", this::bstIntToChr);
        builtInFunctions.put("int.to.str$", this::bstIntToStr);
        builtInFunctions.put("missing$", this::bstMissing);
        builtInFunctions.put("newline$", this::bstNewLine);
        builtInFunctions.put("num.names$", this::bstNumNames);
        builtInFunctions.put("pop$", this::bstPop);
        builtInFunctions.put("preamble$", this::bstPreamble);
        builtInFunctions.put("purify$", this::bstPurify);
        builtInFunctions.put("quote$", this::bstQuote);
        builtInFunctions.put("skip$", this::bstSkip);
        builtInFunctions.put("stack$", this::bstStack);
        builtInFunctions.put("substring$", this::bstSubstring);
        builtInFunctions.put("swap$", this::bstSwap);
        builtInFunctions.put("text.length$", this::bstTextLength);
        builtInFunctions.put("text.prefix$", this::bstTextPrefix);
        builtInFunctions.put("top$", this::bstTop);
        builtInFunctions.put("type$", new BstTypeFunction());
        builtInFunctions.put("warning$", this::bstWarning);
        builtInFunctions.put("while$", this::bstWhile);
        builtInFunctions.put("width$", this::bstWidth);
        builtInFunctions.put("write$", this::bstWrite);

        return builtInFunctions;
    }

    /**
     * Pops the top two (integer) literals, compares them, and pushes
     * the integer 1 if the second is greater than the first, 0
     * otherwise.
     */
    private void bstIsGreaterThan(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation > (line %d)".formatted(ctx.start.getLine()));
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new BstVMException("Can only compare two integers with >");
        }

        stack.push(((Integer) o1).compareTo((Integer) o2) > 0 ? BstVM.TRUE : BstVM.FALSE);
    }

    /**
     * Pops the top two (integer) literals, compares them, and pushes
     * the integer 1 if the second is lower than the first, 0
     * otherwise.
     */
    private void bstIsLowerThan(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation <");
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new BstVMException("Can only compare two integers with < (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push(((Integer) o1).compareTo((Integer) o2) < 0 ? BstVM.TRUE : BstVM.FALSE);
    }

    /**
     * Pops the top two (both integer or both string) literals, compares
     * them, and pushes the integer 1 if they're equal, 0 otherwise.
     */
    private void bstEquals(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation = (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();
        Object o2 = stack.pop();

        if ((o1 == null) ^ (o2 == null)) {
            stack.push(BstVM.FALSE);
            return;
        }

        if ((o1 == null) && (o2 == null)) {
            stack.push(BstVM.TRUE);
            return;
        }

        stack.push(o1.equals(o2) ? BstVM.TRUE : BstVM.FALSE);
    }

    /**
     *  Pops the top two (integer) literals and pushes their sum.
     */
    private void bstAdd(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation + (line %d)".formatted(ctx.start.getLine()));
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new BstVMException("Can only compare two integers with + (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push((Integer) o1 + (Integer) o2);
    }

    /**
     * Pops the top two (integer) literals and pushes their difference
     * (the first subtracted from the second).
     */
    private void bstSubtract(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation - (line %d)".formatted(ctx.start.getLine()));
        }
        Object o2 = stack.pop();
        Object o1 = stack.pop();

        if (!((o1 instanceof Integer) && (o2 instanceof Integer))) {
            throw new BstVMException("Can only subtract two integers with - (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push((Integer) o1 - (Integer) o2);
    }

    /**
     * Pops the top two (string) literals, concatenates them (in reverse
     * order, that is, the order in which pushed), and pushes the
     * resulting string.
     */
    private void bstConcat(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation * (line %d)".formatted(ctx.start.getLine()));
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
            throw new BstVMException("Can only concatenate two String with * (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push(o1.toString() + o2);
    }

    /**
     * Pops the top two literals and assigns to the first (which must be
     * a global or entry variable) the value of the second.
     */
    public class BstAssignFunction implements BstFunction {

        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx) {
            this.execute(visitor, ctx, null);
        }

        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx, BstEntry bstEntry) {
            if (stack.size() < 2) {
                throw new BstVMException("Invalid call to operation := (line %d)".formatted(ctx.start.getLine()));
            }
            Object o1 = stack.pop();
            Object o2 = stack.pop();
            doAssign(bstEntry, o1, o2, ctx);
        }

        private boolean doAssign(BstEntry context, Object o1, Object o2, ParserRuleContext ctx) {
            if (!(o1 instanceof BstVMVisitor.Identifier) || !((o2 instanceof String) || (o2 instanceof Integer))) {
                throw new BstVMException("Invalid parameters (line %d)".formatted(ctx.start.getLine()));
            }

            String name = ((BstVMVisitor.Identifier) o1).name();

            // ToDo: use switch statement pattern matching instead (Java 19)
            if (o2 instanceof String) {
                String value = (String) o2;
                if ((context != null) && context.localStrings.containsKey(name)) {
                    context.localStrings.put(name, value);
                    return true;
                }

                if (strings.containsKey(name)) {
                    strings.put(name, value);
                    return true;
                }
            } else {
                Integer value = (Integer) o2;
                if ((context != null) && context.localIntegers.containsKey(name)) {
                    context.localIntegers.put(name, value);
                    return true;
                }

                if (integers.containsKey(name)) {
                    integers.put(name, value);
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Pops the top (string) literal, adds a `.' to it if the last non
     * '}' character isn't a `.', `?', or `!', and pushes this resulting
     * string.
     */
    private void bstAddPeriod(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation add.period$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String s)) {
            throw new BstVMException("Can only add a period to a string for add.period$ (line %d)".formatted(ctx.start.getLine()));
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
    public class BstCallTypeFunction implements BstFunction {
        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx) {
            throw new BstVMException("Call.type$ can only be called from within a context (ITERATE or REVERSE). (line %d)".formatted(ctx.start.getLine()));
        }

        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx, BstEntry bstEntry) {
            if (bstEntry == null) {
                this.execute(visitor, ctx); // Throw error
            } else {
                functions.get(bstEntry.entry.getType().getName()).execute(visitor, ctx, bstEntry);
            }
        }
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
    private void bstChangeCase(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation change.case$ (line %d)".formatted(ctx.start.getLine()));
        }

        Object o1 = stack.pop();
        if (!((o1 instanceof String) && (((String) o1).length() == 1))) {
            throw new BstVMException("A format string of length 1 is needed for change.case$ (line %d)".formatted(ctx.start.getLine()));
        }

        Object o2 = stack.pop();
        if (!(o2 instanceof String)) {
            throw new BstVMException("A string is needed as second parameter for change.case$ (line %d)".formatted(ctx.start.getLine()));
        }

        char format = ((String) o1).toLowerCase(Locale.ROOT).charAt(0);
        String s = (String) o2;

        stack.push(BstCaseChanger.changeCase(s, BstCaseChanger.FORMAT_MODE.getFormatModeForBSTFormat(format)));
    }

    /**
     * Pops the top (string) literal, makes sure it's a single
     * character, converts it to the corresponding ASCII integer, and
     * pushes this integer.
     */
    private void bstChrToInt(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation chr.to.int$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!((o1 instanceof String s) && (((String) o1).length() == 1))) {
            throw new BstVMException("Can only perform chr.to.int$ on string with length 1 (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push((int) s.charAt(0));
    }

    /**
     * Pushes the string that was the \cite-command argument for this
     * entry.
     */
    public class BstCiteFunction implements BstFunction {
        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx) {
            throw new BstVMException("Must have an entry to cite$ (line %d)".formatted(ctx.start.getLine()));
        }

        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx, BstEntry bstEntryContext) {
            if (bstEntryContext == null) {
                execute(visitor, ctx);
                return;
            }

            stack.push(bstEntryContext.entry.getCitationKey().orElse(null));
        }
    }

    /**
     * Pops the top literal from the stack and pushes two copies of it.
     */
    private void bstDuplicate(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation duplicate$ (line %d)".formatted(ctx.start.getLine()));
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
    private void bstEmpty(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation empty$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (o1 == null) {
            stack.push(BstVM.TRUE);
            return;
        }

        if (!(o1 instanceof String s)) {
            throw new BstVMException("Operand does not match function empty$ (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push("".equals(s.trim()) ? BstVM.TRUE : BstVM.FALSE);
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
    private void bstFormatName(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 3) {
            throw new BstVMException("Not enough operands on stack for operation format.name$ (line %d)".formatted(ctx.start.getLine()));
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
                throw new BstVMException("Author Out of Bounds. Number %d invalid for %s (line %d)".formatted(name, names, ctx.start.getLine()));
            }
            Author author = a.getAuthor(name - 1);

            stack.push(BstNameFormatter.formatName(author, format));
        }
    }

    /**
     * Pops the top three literals (they are two function literals and
     * an integer literal, in that order); if the integer is greater
     * than 0, it executes the second literal, else it executes the
     * first.
     */
    private void bstIf(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 3) {
            throw new BstVMException("Not enough operands on stack for if$ (line %d)".formatted(ctx.start.getLine()));
        }

        Object f1 = stack.pop();
        Object f2 = stack.pop();
        Object i = stack.pop();

        if (!((f1 instanceof BstVMVisitor.Identifier) || (f1 instanceof ParseTree))
                && ((f2 instanceof BstVMVisitor.Identifier) || (f2 instanceof ParseTree))
                && (i instanceof Integer)) {
            throw new BstVMException("Expecting two functions and an integer for if$ (line %d)".formatted(ctx.start.getLine()));
        }

        if (((Integer) i) > 0) {
            visitor.visit((ParseTree) f2);
            // new BstVMVisitor.BstVMFunction<>(f2).execute(visitor, ctx);
        } else {
            // new BstVMVisitor.BstVMFunction<>(f1).execute(visitor, ctx);
            visitor.visit((ParseTree) f1);
        }
    }

    /**
     * Pops the top (integer) literal, interpreted as the ASCII integer
     * value of a single character, converts it to the corresponding
     * single-character string, and pushes this string.
     */
    private void bstIntToChr(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation int.to.chr$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof Integer i)) {
            throw new BstVMException("Can only perform operation int.to.chr$ on an Integer (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push(String.valueOf((char) i.intValue()));
    }

    /**
     * Pops the top (integer) literal, converts it to its (unique)
     * string equivalent, and pushes this string.
     */
    private void bstIntToStr(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation int.to.str$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof Integer)) {
            throw new BstVMException("Can only transform an integer to an string using int.to.str$ (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push(o1.toString());
    }

    /**
     * Pops the top literal and pushes the integer 1 if it's a missing
     * field, 0 otherwise.
     */
    private void bstMissing(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation missing$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (o1 == null) {
            stack.push(BstVM.TRUE);
            return;
        }

        if (!(o1 instanceof String)) {
            LOGGER.warn("Not a string or missing field in operation missing$ (line %d)".formatted(ctx.start.getLine()));
            stack.push(BstVM.TRUE);
            return;
        }

        stack.push(BstVM.FALSE);
    }

    /**
     * Writes onto the bbl file what is accumulated in the output buffer.
     * It writes a blank line if and only if the output buffer is empty.
     * Since write$ does reasonable line breaking, you should use this
     * function only when you want a blank line or an explicit line
     * break.
     */
    private void bstNewLine(BstVMVisitor visitor, ParserRuleContext ctx) {
        this.bbl.append('\n');
    }

    /**
     * Pops the top (string) literal and pushes the number of names the
     * string represents one plus the number of occurrences of the
     * substring "and" (ignoring case differences) surrounded by
     * non-null white-space at the top brace level.
     */
    private void bstNumNames(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation num.names$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String s)) {
            throw new BstVMException("Need a string at the top of the stack for num.names$ (line %d)".formatted(ctx.start.getLine()));
        }

        stack.push(AuthorList.parse(s).getNumberOfAuthors());
    }

    /**
     * Pops the top of the stack but doesn't print it; this gets rid of
     * an unwanted stack literal.
     */
    private void bstPop(BstVMVisitor visitor, ParserRuleContext ctx) {
        stack.pop();
    }

    /**
     * The |built_in| function {\.{preamble\$}} pushes onto the stack
     * the concatenation of all the \.{preamble} strings read from the
     * database files. (or the empty string if there were none)
     * '@PREAMBLE' strings are read from the database files.
     */
    private void bstPreamble(BstVMVisitor visitor, ParserRuleContext ctx) {
        stack.push(preamble);
    }

    /**
     * Pops the top (string) literal, removes nonalphanumeric characters
     * except for white-space characters and hyphens and ties (these all get
     * converted to a space), removes certain alphabetic characters
     * contained in the control sequences associated with a \special
     * character", and pushes the resulting string.
     */
    private void bstPurify(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation purify$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String)) {
            LOGGER.warn("A string is needed for purify$");
            stack.push("");
            return;
        }

        stack.push(BstPurifier.purify((String) o1));
    }

    /**
     * Pushes the string consisting of the double-quote character.
     */
    private void bstQuote(BstVMVisitor visitor, ParserRuleContext ctx) {
        stack.push("\"");
    }

    /**
     * Does nothing.
     */
    private void bstSkip(BstVMVisitor visitor, ParserRuleContext ctx) {
        // no-op
    }

    /**
     * Pops and prints the whole stack; it's meant to be used for style
     * designers while debugging.
     */
    private void bstStack(BstVMVisitor visitor, ParserRuleContext ctx) {
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
    private void bstSubstring(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 3) {
            throw new BstVMException("Not enough operands on stack for operation substring$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();
        Object o2 = stack.pop();
        Object o3 = stack.pop();

        if (!((o1 instanceof Integer len) && (o2 instanceof Integer start) && (o3 instanceof String s))) {
            throw new BstVMException("Expecting two integers and a string for substring$ (line %d)".formatted(ctx.start.getLine()));
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
    private void bstSwap(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation swap$ (line %d)".formatted(ctx.start.getLine()));
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
    private void bstTextLength(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation text.length$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String s)) {
            throw new BstVMException("Can only perform operation on a string text.length$ (line %d)".formatted(ctx.start.getLine()));
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
    private void bstTextPrefix(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation text.prefix$ (line %d)".formatted(ctx.start.getLine()));
        }

        Object o1 = stack.pop();
        if (!(o1 instanceof Integer)) {
            LOGGER.warn("An integer is needed as first parameter to text.prefix$ (line {})", ctx.start.getLine());
            stack.push("");
            return;
        }

        Object o2 = stack.pop();
        if (!(o2 instanceof String)) {
            LOGGER.warn("A string is needed as second parameter to text.prefix$ (line {})", ctx.start.getLine());
            stack.push("");
            return;
        }

        stack.push(BstTextPrefixer.textPrefix((Integer) o1, (String) o2));
    }

    /**
     * Pops and prints the top of the stack to the log file. It's useful for debugging.
     */
    private void bstTop(BstVMVisitor visitor, ParserRuleContext ctx) {
        LOGGER.debug("Stack entry {} (line {})", stack.pop(), ctx.start.getLine());
    }

    /**
     * Pushes the current entry's type (book, article, etc.), but pushes
     * the null string if the type is either unknown or undefined.
     */
    public class BstTypeFunction implements BstFunction {
        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx) {
            throw new BstVMException("type$ need a context (line %d)".formatted(ctx.start.getLine()));
        }

        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext ctx, BstEntry bstEntryContext) {
            if (bstEntryContext == null) {
                this.execute(visitor, ctx);
                return;
            }

            stack.push(bstEntryContext.entry.getType().getName());
        }
    }

    /**
     * Pops the top (string) literal and prints it following a warning
     * message. This also increments a count of the number of warning
     * messages issued.
     */
    private void bstWarning(BstVMVisitor visitor, ParserRuleContext ctx) {
        LOGGER.warn("Warning (#{}): {}", bstWarning++, stack.pop());
    }

    /**
     * Pops the top two (function) literals, and keeps executing the
     * second as long as the (integer) literal left on the stack by
     * executing the first is greater than 0.
     */
    private void bstWhile(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.size() < 2) {
            throw new BstVMException("Not enough operands on stack for operation while$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object f2 = stack.pop();
        Object f1 = stack.pop();

        if (!((f1 instanceof BstVMVisitor.Identifier) || (f1 instanceof ParseTree))
                && ((f2 instanceof BstVMVisitor.Identifier) || (f2 instanceof ParseTree))) {
            throw new BstVMException("Expecting two functions for while$ (line %d)".formatted(ctx.start.getLine()));
        }

        do {
            visitor.visit((ParseTree) f1);

            Object i = stack.pop();
            if (!(i instanceof Integer)) {
                throw new BstVMException("First parameter to while has to return an integer but was %s (line %d)"
                        .formatted(i.toString(), ctx.start.getLine()));
            }
            if ((Integer) i <= 0) {
                break;
            }
            visitor.visit((ParseTree) f2);
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
    private void bstWidth(BstVMVisitor visitor, ParserRuleContext ctx) {
        if (stack.isEmpty()) {
            throw new BstVMException("Not enough operands on stack for operation width$ (line %d)".formatted(ctx.start.getLine()));
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String)) {
            LOGGER.warn("A string is needed for width$");
            stack.push(0);
            return;
        }

        stack.push(BstWidthCalculator.width((String) o1));
    }

    /**
     * Pops the top (string) literal and writes it on the output buffer
     * (which will result in stuff being written onto the bbl file when
     * the buffer fills up).
     */
    private void bstWrite(BstVMVisitor visitor, ParserRuleContext ctx) {
        String s = (String) stack.pop();
        bbl.append(s);
    }
}
