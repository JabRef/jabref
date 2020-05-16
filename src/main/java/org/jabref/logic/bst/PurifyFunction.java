package org.jabref.logic.bst;

import java.util.Stack;

import org.jabref.logic.bst.VM.BstEntry;
import org.jabref.logic.bst.VM.BstFunction;

/**
 *
 * The |built_in| function {\.{purify\$}} pops the top (string) literal, removes
 * nonalphanumeric characters except for |white_space| and |sep_char| characters
 * (these get converted to a |space|) and removes certain alphabetic characters
 * contained in the control sequences associated with a special character, and
 * pushes the resulting string. If the literal isn't a string, it complains and
 * pushes the null string.
 *
 */
public class PurifyFunction implements BstFunction {

    private final VM vm;

    public PurifyFunction(VM vm) {
        this.vm = vm;
    }

    @Override
    public void execute(BstEntry context) {
        Stack<Object> stack = vm.getStack();

        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation purify$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String)) {
            vm.warn("A string is needed for purify$");
            stack.push("");
            return;
        }

        stack.push(BibtexPurify.purify((String) o1, vm));
    }
}
