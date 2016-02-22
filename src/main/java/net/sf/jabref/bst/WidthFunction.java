/*  Copyright (C) 2003-2015 JabRef contributors.
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
