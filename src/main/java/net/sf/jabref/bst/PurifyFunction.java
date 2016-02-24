/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.bst;

import java.util.Stack;

import net.sf.jabref.bst.VM.BstEntry;
import net.sf.jabref.bst.VM.BstFunction;

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
