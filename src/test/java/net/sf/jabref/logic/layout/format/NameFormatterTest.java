package net.sf.jabref.logic.layout.format;

import org.junit.Assert;
import org.junit.Test;

public class NameFormatterTest {

    @Test
    public void testFormatStringStringBibtexEntry() {

        NameFormatter l = new NameFormatter();

        Assert.assertEquals("Doe", l.format("Joe Doe", "1@*@{ll}"));

        Assert.assertEquals("moremoremoremore", l.format("Joe Doe and Mary Jane and Bruce Bar and Arthur Kay",
                "1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{last}@@*@*@more"));

        Assert.assertEquals("Doe", l.format("Joe Doe", "1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{last}@@*@*@more"));

        Assert.assertEquals("JoeDoe and MaryJ",
                l.format("Joe Doe and Mary Jane", "1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{l}@@*@*@more"));

        Assert.assertEquals("Doe, Joe and Jane, M. and Kamp, J.~A.",
                l.format("Joe Doe and Mary Jane and John Arthur van Kamp",
                        "1@*@{ll}, {ff}@@*@1@{ll}, {ff}@2..-1@ and {ll}, {f}."));

        Assert.assertEquals("Doe Joe and Jane, M. and Kamp, J.~A.",
                l.format("Joe Doe and Mary Jane and John Arthur van Kamp",
                        "1@*@{ll}, {ff}@@*@1@{ll} {ff}@2..-1@ and {ll}, {f}."));

    }

    @Test
    public void testFormat() {

        NameFormatter a = new NameFormatter();

        // Empty case
        Assert.assertEquals("", a.format(""));

        String formatString = "1@1@{vv }{ll}{ ff}@@2@1@{vv }{ll}{ ff}@2@ and {vv }{ll}{, ff}@@*@1@{vv }{ll}{ ff}@2..-2@, {vv }{ll}{, ff}@-1@ and {vv }{ll}{, ff}";

        // Single Names
        Assert.assertEquals("Vandekamp Mary~Ann", a.format("Mary Ann Vandekamp", formatString));

        // Two names
        Assert.assertEquals("von Neumann John and Black~Brown, Peter",
                a.format("John von Neumann and Black Brown, Peter", formatString));

        // Three names
        Assert.assertEquals("von Neumann John, Smith, John and Black~Brown, Peter",
                a.format("von Neumann, John and Smith, John and Black Brown, Peter", formatString));

        Assert.assertEquals("von Neumann John, Smith, John and Black~Brown, Peter",
                a.format("John von Neumann and John Smith and Black Brown, Peter", formatString));

        // Four names
        Assert.assertEquals("von Neumann John, Smith, John, Vandekamp, Mary~Ann and Black~Brown, Peter", a.format(
                "von Neumann, John and Smith, John and Vandekamp, Mary Ann and Black Brown, Peter", formatString));
    }

}
