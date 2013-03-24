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
package net.sf.jabref.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.TreeSet;

/**
 * Writer that extends OutputStreamWriter, but also checks if the chosen
 * encoding supports all text that is written. Currently only a boolean value is
 * stored to remember whether everything has gone well or not.
 */
public class VerifyingWriter extends OutputStreamWriter {

	CharsetEncoder encoder;
	private boolean couldEncodeAll = true;
	private TreeSet<Character> problemCharacters = new TreeSet<Character>();

	public VerifyingWriter(OutputStream out, String encoding)
			throws UnsupportedEncodingException {
		super(out, encoding);
		encoder = Charset.forName(encoding).newEncoder();
	}

	public void write(String str) throws IOException {
		super.write(str);
		if (!encoder.canEncode(str)) {
			for (int i = 0; i < str.length(); i++) {
				if (!encoder.canEncode(str.charAt(i)))
					problemCharacters.add(new Character(str.charAt(i)));
			}
			couldEncodeAll = false;
		}
	}

	public boolean couldEncodeAll() {
		return couldEncodeAll;
	}

	public String getProblemCharacters() {
		StringBuffer chars = new StringBuffer();
		for (Character ch : problemCharacters) {
			chars.append(ch.charValue());
		}
		return chars.toString();
	}
}
