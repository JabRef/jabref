package net.sf.jabref.bst;

import java.util.Stack;

import net.sf.jabref.bst.VM.BstEntry;
import net.sf.jabref.bst.VM.BstFunction;



/**
 * The |built_in| function {\.{width\$}} pops the top (string) literal and
 * pushes the integer that represents its width in units specified by the
 * |char_width| array. This function takes the literal literally; that is, it
 * assumes each character in the string is to be printed as is, regardless of
 * whether the character has a special meaning to \TeX, except that special
 * characters (even without their |right_brace|s) are handled specially. If the
 * literal isn't a string, it complains and pushes~0.
 * 
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class WidthFunction implements BstFunction {

	VM vm;

	public WidthFunction(VM vm) {
		this.vm = vm;
	}

	public void execute(BstEntry context) {
		Stack stack = vm.getStack();

		if (stack.size() < 1) {
			throw new VMException("Not enough operands on stack for operation width$");
		}
		Object o1 = stack.pop();

		if (!(o1 instanceof String)) {
			vm.warn("A string is needed for change.case$");
			stack.push(new Integer(0));
			return;
		}
		
		stack.push(new Integer(BibtexWidth.width((String) o1, vm)));
	}
}
