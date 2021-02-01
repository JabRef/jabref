package org.jabref.logic.formatter.bibtexfields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringLengthComparator;

public class UnitsToLatexFormatter extends Formatter {

    private static final List<String> UNIT_LIST = Arrays.asList(
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
            "dBc", // decibel
            "eV", // electron volts
            "inch", // inch
            "kat", // katal
            "lm", // lumen
            "lx", // lux
            "m", // meters
            "mol", // mol
            "rad", // radians
            "s", // seconds
            "sr" // steradians
    );

    private static final List<String> UNIT_PREFIX_LIST = Arrays.asList(
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
            "Y" // yotta
    );

    private final List<String> prefixUnitCombinations;

    public UnitsToLatexFormatter() {
        prefixUnitCombinations = new ArrayList<>(
                UnitsToLatexFormatter.UNIT_LIST.size() * UnitsToLatexFormatter.UNIT_PREFIX_LIST.size());
        for (String unit : UnitsToLatexFormatter.UNIT_LIST) {
            for (String prefix : UnitsToLatexFormatter.UNIT_PREFIX_LIST) {
                prefixUnitCombinations.add(prefix + unit);
            }
        }
        Collections.sort(prefixUnitCombinations, new StringLengthComparator()); // Sort based on string length
    }

    @Override
    public String format(String text) {
        Objects.requireNonNull(text);
        if (text.isEmpty()) {
            return text;
        }

        // Replace the hyphen in 12-bit etc with a non-breaking hyphen, will also avoid bad casing of 12-Bit
        String result = text.replaceAll("([0-9,\\.]+)-([Bb][Ii][Tt])", "$1\\\\mbox\\{-\\}$2");

        // Replace the space in 12 bit etc with a non-breaking space, will also avoid bad casing of 12 Bit
        result = result.replaceAll("([0-9,\\.]+) ([Bb][Ii][Tt])", "$1~$2");

        // For each word in the list
        for (String listOfWord : prefixUnitCombinations) {
            // Add {} if the character before is a space, -, /, (, [, or } or if it is at the start of the string but not if it is followed by a }
            result = result.replaceAll("([0-9])(" + listOfWord + ")", "$1\\{$2\\}"); // Only add brackets to keep case
            result = result.replaceAll("([0-9])-(" + listOfWord + ")", "$1\\\\mbox\\{-\\}\\{$2\\}"); // Replace hyphen with non-break hyphen
            result = result.replaceAll("([0-9]) (" + listOfWord + ")", "$1~\\{$2\\}"); // Replace space with a hard space
        }

        return result;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts units to LaTeX formatting.");
    }

    @Override
    public String getExampleInput() {
        return "1 Hz";
    }

    @Override
    public String getName() {
        return Localization.lang("Units to LaTeX");
    }

    @Override
    public String getKey() {
        return "units_to_latex";
    }
}
