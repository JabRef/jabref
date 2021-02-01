package org.jabref.logic.bst;

import java.util.Stack;

import org.jabref.logic.bst.VM.BstEntry;
import org.jabref.logic.bst.VM.BstFunction;

/**
 The |built_in| function {\.{text.prefix\$}} pops the top two literals
 (the integer literal |pop_lit1| and a string literal, in that order).
 It pushes the substring of the (at most) |pop_lit1| consecutive text
 characters starting from the beginning of the string.  This function
 is similar to {\.{substring\$}}, but this one considers an accented
 character (or more precisely, a ``special character''$\!$, even if
 it's missing its matching |right_brace|) to be a single text character
 (rather than however many |ASCII_code| characters it actually
 comprises), and this function doesn't consider braces to be text
 characters; furthermore, this function appends any needed matching
 |right_brace|s.  If any of the types is incorrect, it complains and
 pushes the null string.
 *
 */
public class TextPrefixFunction implements BstFunction {

    private final VM vm;

    public TextPrefixFunction(VM vm) {
        this.vm = vm;
    }

    @Override
    public void execute(BstEntry context) {
        Stack<Object> stack = vm.getStack();

        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation text.prefix$");
        }

        Object o1 = stack.pop();
        if (!(o1 instanceof Integer)) {
            vm.warn("An integer is needed as first parameter to text.prefix$");
            stack.push("");
            return;
        }

        Object o2 = stack.pop();
        if (!(o2 instanceof String)) {
            vm.warn("A string is needed as second parameter to text.prefix$");
            stack.push("");
            return;
        }

        stack.push(BibtexTextPrefix.textPrefix((Integer) o1, (String) o2, vm));
    }
}
