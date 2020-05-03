package org.jabref.logic.bst;

import java.util.Stack;

import org.jabref.logic.bst.VM.BstEntry;
import org.jabref.logic.bst.VM.BstFunction;

/**
 * The |built_in| function {\.{width\$}} pops the top (string) literal and
 * pushes the integer that represents its width in units specified by the
 * |char_width| array. This function takes the literal literally; that is, it
 * assumes each character in the string is to be printed as is, regardless of
 * whether the character has a special meaning to \TeX, except that special
 * characters (even without their |right_brace|s) are handled specially. If the
 * literal isn't a string, it complains and pushes~0.
 *
 */
public class WidthFunction implements BstFunction {

    private final VM vm;

    public WidthFunction(VM vm) {
        this.vm = vm;
    }

    @Override
    public void execute(BstEntry context) {
        Stack<Object> stack = vm.getStack();

        if (stack.isEmpty()) {
            throw new VMException("Not enough operands on stack for operation width$");
        }
        Object o1 = stack.pop();

        if (!(o1 instanceof String)) {
            vm.warn("A string is needed for change.case$");
            stack.push(0);
            return;
        }

        stack.push(BibtexWidth.width((String) o1));
    }
}
