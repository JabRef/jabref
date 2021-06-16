package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameFormatterTest {

    @Test
    public void testFormatStringStringBibtexEntry() {

        NameFormatter l = new NameFormatter();

        assertEquals("Doe", l.format("Joe Doe", "1@*@{ll}"));

        assertEquals("moremoremoremore", l.format("Joe Doe and Mary Jane and Bruce Bar and Arthur Kay",
                "1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{last}@@*@*@more"));

        assertEquals("Doe", l.format("Joe Doe", "1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{last}@@*@*@more"));

        assertEquals("JoeDoe and MaryJ",
                l.format("Joe Doe and Mary Jane", "1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{l}@@*@*@more"));

        assertEquals("Doe, Joe and Jane, M. and Kamp, J.~A.",
                l.format("Joe Doe and Mary Jane and John Arthur van Kamp",
                        "1@*@{ll}, {ff}@@*@1@{ll}, {ff}@2..-1@ and {ll}, {f}."));

        assertEquals("Doe Joe and Jane, M. and Kamp, J.~A.",
                l.format("Joe Doe and Mary Jane and John Arthur van Kamp",
                        "1@*@{ll}, {ff}@@*@1@{ll} {ff}@2..-1@ and {ll}, {f}."));
    }

    @Test
    public void testFormat() {

        NameFormatter a = new NameFormatter();

        // Empty case
        assertEquals("", a.format(""));

        String formatString = "1@1@{vv }{ll}{ ff}@@2@1@{vv }{ll}{ ff}@2@ and {vv }{ll}{, ff}@@*@1@{vv }{ll}{ ff}@2..-2@, {vv }{ll}{, ff}@-1@ and {vv }{ll}{, ff}";

        // Single Names
        assertEquals("Vandekamp Mary~Ann", a.format("Mary Ann Vandekamp", formatString));

        // Two names
        assertEquals("von Neumann John and Black~Brown, Peter",
                a.format("John von Neumann and Black Brown, Peter", formatString));

        // Three names
        assertEquals("von Neumann John, Smith, John and Black~Brown, Peter",
                a.format("von Neumann, John and Smith, John and Black Brown, Peter", formatString));

        assertEquals("von Neumann John, Smith, John and Black~Brown, Peter",
                a.format("John von Neumann and John Smith and Black Brown, Peter", formatString));

        // Four names
        assertEquals("von Neumann John, Smith, John, Vandekamp, Mary~Ann and Black~Brown, Peter", a.format(
                "von Neumann, John and Smith, John and Vandekamp, Mary Ann and Black Brown, Peter", formatString));
    }
}
