package org.jabref.logic.bst;

import java.util.Locale;
import java.util.Stack;

import org.jabref.logic.bst.BibtexCaseChanger.FORMAT_MODE;
import org.jabref.logic.bst.VM.BstEntry;
import org.jabref.logic.bst.VM.BstFunction;

/**
 * From the Bibtex manual:
 *
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
 *
 * Christopher: I think this should be another grammar! This parser is horrible.
 *
 */
public class ChangeCaseFunction implements BstFunction {

    private final VM vm;


    public ChangeCaseFunction(VM vm) {
        this.vm = vm;
    }

    @Override
    public void execute(BstEntry context) {
        Stack<Object> stack = vm.getStack();

        if (stack.size() < 2) {
            throw new VMException("Not enough operands on stack for operation change.case$");
        }

        Object o1 = stack.pop();
        if (!((o1 instanceof String) && (((String) o1).length() == 1))) {
            throw new VMException("A format string of length 1 is needed for change.case$");
        }

        Object o2 = stack.pop();
        if (!(o2 instanceof String)) {
            throw new VMException("A string is needed as second parameter for change.case$");
        }

        char format = ((String) o1).toLowerCase(Locale.ROOT).charAt(0);
        String s = (String) o2;

        stack.push(BibtexCaseChanger.changeCase(s, FORMAT_MODE.getFormatModeForBSTFormat(format)));
    }

}
