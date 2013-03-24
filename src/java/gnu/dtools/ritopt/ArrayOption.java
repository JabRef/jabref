package gnu.dtools.ritopt;

import java.util.List;

/**
 * ArrayOption.java
 *
 * Version:
 *    $Id$
 */

/**
 * The principal base class used to register option variables that represent
 * arrays or Collections. Array options are useful for options which represent
 * path lists or file specifications.
 * <p>
 * 
 * The preferred derived sub-class implementation is to provide a constructor
 * with a single array parameter to allow for simple registration. For example,
 * an ArrayOption derived class for int arrays should implement the following
 * constructor and accessor.
 * 
 * <pre>
 *   MyIntArrayOption( int array[] );
 *   int[] getValue();
 * </pre>
 * 
 * Although this has no affect on option processing, following this philosophy
 * for the public interfaces make it easier for the programmer to use your
 * source code.
 * 
 * <hr>
 * 
 * <pre>
 * Copyright (C) Damian Ryan Eads, 2001. All Rights Reserved.
 * 
 * ritopt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * ritopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ritopt; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </pre>
 * 
 * @author Damian Ryan Eads
 */

public abstract class ArrayOption extends Option implements OptionArrayable {

	/**
	 * Builds and initializes ArrayOption class members, and invokes the Option
	 * constructor.
	 */

	public ArrayOption() {
		super();
	}

	/**
	 * Get an ArrayOption in array form. If the option value is an array of
	 * primitive values, references to wrapper objects are returned.
	 * 
	 * @return An array of objects representing the option's value.
	 */

	public abstract Object[] getObjectArray();

	/**
	 * Get a list of objects representing the elements of this array option.
	 * 
	 * @return A list of objects representing the option's value.
	 */

	public List<Object> getObjectList() {
		return java.util.Arrays.asList(getObjectArray());
	}

}
