package net.sf.jabref.bst;

import java.util.Stack;

import net.sf.jabref.AuthorList;
import net.sf.jabref.AuthorList.Author;
import net.sf.jabref.bst.VM.BstEntry;
import net.sf.jabref.bst.VM.BstFunction;


/**
 * From Bibtex:
 * 
 * "The |built_in| function {\.{format.name\$}} pops the
 * top three literals (they are a string, an integer, and a string
 * literal, in that order). The last string literal represents a
 * name list (each name corresponding to a person), the integer
 * literal specifies which name to pick from this list, and the
 * first string literal specifies how to format this name, as
 * described in the \BibTeX\ documentation. Finally, this function
 * pushes the formatted name. If any of the types is incorrect, it
 * complains and pushes the null string."
 * 
 * All the pain is encapsulated in BibtexNameFormatter. :-)
 * 
 */
public class FormatNameFunction implements BstFunction {
	
	VM vm;

	public FormatNameFunction(VM vm) {
		this.vm = vm;
	}
	
	public void execute(BstEntry context) {
		Stack<Object> stack = vm.getStack();

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
		
		if (names != null){
			AuthorList a = AuthorList.getAuthorList(names);
			if (name.intValue() > a.size()){
				throw new VMException("Author Out of Bounds. Number " + name + " invalid for " + names);
			}
			Author author = a.getAuthor(name.intValue() - 1);
			
			stack.push(BibtexNameFormatter.formatName(author, format, vm));
		} else {
			stack.push("");
		}
	}
}
