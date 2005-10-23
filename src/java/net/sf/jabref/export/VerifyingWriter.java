package net.sf.jabref.export;

import sun.misc.CharacterEncoder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.TreeSet;
import java.util.Iterator;

import net.sf.jabref.Globals;

/**
 * Writer that extends OutputStreamWriter, but also checks if the chosen encoding supports all
 * text that is written. Currently only a boolean value is stored to remember whether everything
 * has gone well or not.
 */
public class VerifyingWriter extends OutputStreamWriter {

    CharsetEncoder encoder;
    private boolean couldEncodeAll = true;
    private TreeSet problemCharacters = new TreeSet();

    public VerifyingWriter(OutputStream out, String encoding) throws UnsupportedEncodingException {
        super(out, encoding);
        encoder = Charset.forName(encoding).newEncoder();
     }

    public void write(String str) throws IOException {
        super.write(str);
        if (!encoder.canEncode(str)) {
            for (int i=0; i<str.length(); i++) {
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
        for (Iterator i=problemCharacters.iterator(); i.hasNext();) {
            chars.append(((Character)i.next()).charValue());
        }
        return chars.toString();
    }
}
