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
