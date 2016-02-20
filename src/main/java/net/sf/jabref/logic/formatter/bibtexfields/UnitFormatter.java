/*  Copyright (C) 2012-2015 JabRef contributors.
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
package net.sf.jabref.logic.formatter.bibtexfields;

import java.util.Arrays;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringLengthComparator;

public class UnitFormatter implements Formatter {

    private static final String[] UNIT_LIST = new String[] {
            "A", // Ampere
            "Ah", // Ampere hours
            "B", // Byte
            "Bq", // Bequerel
            "C", // Coulomb
            "F", // Farad
            "Gy", // Gray
            "H", // Henry
            "Hz", // Hertz
            "J", // Joule
            "K", // Kelvin
            "N", // Newton
            "\\$\\\\Omega\\$", // Ohm
            "Pa", // Pascal
            "S", // Siemens, Samples
            "Sa", // Samples
            "Sv", // Sv
            "T", // Tesla
            "V", // Volt
            "VA", // Volt ampere
            "W", // Watt
            "Wb", // Weber
            "Wh", // Watt hours
            "bar", // bar
            "b", // bit
            "cd", // candela
            "dB", // decibel
            "dBm", // decibel
            "dBc", //decibel
            "eV", // electron volts
            "inch", // inch
            "kat", // katal
            "lm", // lumen
            "lx", // lux
            "m", // meters
            "mol", // mol
            "rad", // radians
            "s", // seconds
            "sr", // steradians
    };

    private static final String[] UNIT_PREFIX_LIST = new String[] {
            "y", // yocto
            "z", // zepto
            "a", // atto
            "f", // femto
            "p", // pico
            "n", // nano
            "\\$\\\\mu\\$", // micro
            "u", // micro
            "m", // milli
            "c", // centi
            "d", // deci
            "", // no prefix
            "da", // deca
            "h", // hekto
            "k", // kilo
            "M", // mega
            "G", // giga
            "T", // tera
            "P", // peta
            "E", // exa
            "Z", // zetta
            "Y", // yotta
    };

    private static final String[] UNIT_COMBINATIONS;

    static {
        int uLLength = UnitFormatter.UNIT_LIST.length;
        int uPLLength = UnitFormatter.UNIT_PREFIX_LIST.length;
        int uCLength = uLLength * uPLLength;
        UNIT_COMBINATIONS = new String[uCLength];
        for (int i = 0; i < uLLength; i++) {
            for (int j = 0; j < uPLLength; j++) {
                UnitFormatter.UNIT_COMBINATIONS[(i * uPLLength) + j] = UnitFormatter.UNIT_PREFIX_LIST[j] + UnitFormatter.UNIT_LIST[i];
            }
        }

    }


    private static String format(String text, String[] listOfWords) {
        Arrays.sort(listOfWords, new StringLengthComparator()); // LengthComparator from CaseKeeper.java

        // Replace the hyphen in 12-bit etc with a non-breaking hyphen, will also avoid bad casing of 12-Bit
        text = text.replaceAll("([0-9,\\.]+)-([Bb][Ii][Tt])", "$1\\\\mbox\\{-\\}$2");

        // Replace the space in 12 bit etc with a non-breaking space, will also avoid bad casing of 12 Bit
        text = text.replaceAll("([0-9,\\.]+) ([Bb][Ii][Tt])", "$1~$2");

        // For each word in the list
        for (String listOfWord : listOfWords) {
            // Add {} if the character before is a space, -, /, (, [, or } or if it is at the start of the string but not if it is followed by a }
            text = text.replaceAll("([0-9])(" + listOfWord + ")", "$1\\{$2\\}"); // Only add brackets to keep case
            text = text.replaceAll("([0-9])-(" + listOfWord + ")", "$1\\\\mbox\\{-\\}\\{$2\\}"); // Replace hyphen with non-break hyphen
            text = text.replaceAll("([0-9]) (" + listOfWord + ")", "$1~\\{$2\\}"); // Replace space with a hard space

        }

        return text;
    }

    @Override
    public String format(String text) {
        if ((text == null) || text.isEmpty()) {
            return text;
        }
        return format(text, UnitFormatter.UNIT_COMBINATIONS);
    }

    @Override
    public String getName() {
        return Localization.lang("UnitFormatter");
    }

    @Override
    public String getKey() {
        return "UnitFormatter";
    }

}
