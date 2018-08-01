package org.jabref.logic.bst;

import java.util.Stack;

import org.jabref.logic.bst.VM.BstEntry;
import org.jabref.logic.bst.VM.BstFunction;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

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

    private final VM vm;


    public FormatNameFunction(VM vm) {
        this.vm = vm;
    }

    @Override
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

        if (names == null) {
            stack.push("");
        } else {
            AuthorList a = AuthorList.parse(names);
            if (name > a.getNumberOfAuthors()) {
                throw new VMException("Author Out of Bounds. Number " + name + " invalid for " + names);
            }
            Author author = a.getAuthor(name - 1);

            stack.push(BibtexNameFormatter.formatName(author, format, vm));
        }
    }
}
