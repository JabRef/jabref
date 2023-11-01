package org.jabref.logic.journals;

import java.util.List;
import java.util.Optional;

import javafx.util.Pair;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.JournalInformationFetcher;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
class JournalInformationFetcherTest {

    private final JournalInformationFetcher fetcher = new JournalInformationFetcher();

    private final JournalInformation journalInformation = new JournalInformation(
            "Annual Review of Biochemistry",
            "Annual Reviews Inc.",
            "",
            "",
            "Biochemistry,  Genetics and Molecular Biology",
            "United States",
            "Biochemistry (Q1)",
            "16801",
            "316",
            "15454509,  00664154",
            List.of(new Pair<>(1999, 50.518), new Pair<>(2000, 39.127), new Pair<>(2001, 39.946), new Pair<>(2002, 35.423), new Pair<>(2003, 35.186), new Pair<>(2004, 34.766), new Pair<>(2005, 32.241), new Pair<>(2006, 35.708), new Pair<>(2007, 34.376), new Pair<>(2008, 35.294), new Pair<>(2009, 31.033), new Pair<>(2010, 30.879), new Pair<>(2011, 30.916), new Pair<>(2012, 27.081), new Pair<>(2013, 27.872), new Pair<>(2014, 23.931), new Pair<>(2015, 24.589), new Pair<>(2016, 20.325), new Pair<>(2017, 18.513), new Pair<>(2018, 17.5), new Pair<>(2019, 17.387), new Pair<>(2020, 16.921), new Pair<>(2021, 13.085), new Pair<>(2022, 11.154)),
            List.of(),
            List.of(new Pair<>(1999, 30.0), new Pair<>(2000, 31.0), new Pair<>(2001, 23.0), new Pair<>(2002, 27.0), new Pair<>(2003, 24.0), new Pair<>(2004, 33.0), new Pair<>(2005, 28.0), new Pair<>(2006, 30.0), new Pair<>(2007, 33.0), new Pair<>(2008, 31.0), new Pair<>(2009, 35.0), new Pair<>(2010, 28.0), new Pair<>(2011, 42.0), new Pair<>(2012, 32.0), new Pair<>(2013, 28.0), new Pair<>(2014, 31.0), new Pair<>(2015, 35.0), new Pair<>(2016, 29.0), new Pair<>(2017, 35.0), new Pair<>(2018, 40.0), new Pair<>(2019, 30.0), new Pair<>(2020, 32.0), new Pair<>(2021, 32.0), new Pair<>(2022, 29.0)),
            List.of(new Pair<>(1999, 79.0), new Pair<>(2000, 84.0), new Pair<>(2001, 88.0), new Pair<>(2002, 84.0), new Pair<>(2003, 81.0), new Pair<>(2004, 74.0), new Pair<>(2005, 84.0), new Pair<>(2006, 85.0), new Pair<>(2007, 91.0), new Pair<>(2008, 91.0), new Pair<>(2009, 94.0), new Pair<>(2010, 99.0), new Pair<>(2011, 94.0), new Pair<>(2012, 105.0), new Pair<>(2013, 102.0), new Pair<>(2014, 102.0), new Pair<>(2015, 91.0), new Pair<>(2016, 94.0), new Pair<>(2017, 95.0), new Pair<>(2018, 99.0), new Pair<>(2019, 104.0), new Pair<>(2020, 105.0), new Pair<>(2021, 102.0), new Pair<>(2022, 94.0)),
            List.of(new Pair<>(1999, 79.0), new Pair<>(2000, 84.0), new Pair<>(2001, 88.0), new Pair<>(2002, 84.0), new Pair<>(2003, 81.0), new Pair<>(2004, 74.0), new Pair<>(2005, 84.0), new Pair<>(2006, 85.0), new Pair<>(2007, 91.0), new Pair<>(2008, 91.0), new Pair<>(2009, 94.0), new Pair<>(2010, 99.0), new Pair<>(2011, 94.0), new Pair<>(2012, 105.0), new Pair<>(2013, 102.0), new Pair<>(2014, 102.0), new Pair<>(2015, 91.0), new Pair<>(2016, 94.0), new Pair<>(2017, 95.0), new Pair<>(2018, 99.0), new Pair<>(2019, 104.0), new Pair<>(2020, 105.0), new Pair<>(2021, 102.0), new Pair<>(2022, 94.0)),
            List.of(new Pair<>(1999, 3477.0), new Pair<>(2000, 3423.0), new Pair<>(2001, 4143.0), new Pair<>(2002, 3586.0), new Pair<>(2003, 3266.0), new Pair<>(2004, 3004.0), new Pair<>(2005, 3064.0), new Pair<>(2006, 3422.0), new Pair<>(2007, 3287.0), new Pair<>(2008, 3194.0), new Pair<>(2009, 3193.0), new Pair<>(2010, 3410.0), new Pair<>(2011, 3445.0), new Pair<>(2012, 3617.0), new Pair<>(2013, 3523.0), new Pair<>(2014, 3000.0), new Pair<>(2015, 2634.0), new Pair<>(2016, 2279.0), new Pair<>(2017, 2218.0), new Pair<>(2018, 2655.0), new Pair<>(2019, 2902.0), new Pair<>(2020, 3350.0), new Pair<>(2021, 2788.0), new Pair<>(2022, 2389.0)),
            List.of(new Pair<>(1999, 36.15), new Pair<>(2000, 43.37), new Pair<>(2001, 32.46), new Pair<>(2002, 37.22), new Pair<>(2003, 39.46), new Pair<>(2004, 33.18), new Pair<>(2005, 34.46), new Pair<>(2006, 39.18), new Pair<>(2007, 33.69), new Pair<>(2008, 31.49), new Pair<>(2009, 32.16), new Pair<>(2010, 31.52), new Pair<>(2011, 36.89), new Pair<>(2012, 30.5), new Pair<>(2013, 29.01), new Pair<>(2014, 32.67), new Pair<>(2015, 23.68), new Pair<>(2016, 21.65), new Pair<>(2017, 22.08), new Pair<>(2018, 28.06), new Pair<>(2019, 27.41), new Pair<>(2020, 22.46), new Pair<>(2021, 26.55), new Pair<>(2022, 17.13)),
            List.of(new Pair<>(1999, 5913.0), new Pair<>(2000, 5859.0), new Pair<>(2001, 4150.0), new Pair<>(2002, 4856.0), new Pair<>(2003, 4453.0), new Pair<>(2004, 5355.0), new Pair<>(2005, 4201.0), new Pair<>(2006, 4354.0), new Pair<>(2007, 4786.0), new Pair<>(2008, 4666.0), new Pair<>(2009, 5344.0), new Pair<>(2010, 4477.0), new Pair<>(2011, 6209.0), new Pair<>(2012, 4863.0), new Pair<>(2013, 4697.0), new Pair<>(2014, 5207.0), new Pair<>(2015, 5186.0), new Pair<>(2016, 4508.0), new Pair<>(2017, 5103.0), new Pair<>(2018, 5630.0), new Pair<>(2019, 4749.0), new Pair<>(2020, 4637.0), new Pair<>(2021, 4872.0), new Pair<>(2022, 4437.0)),
            List.of(new Pair<>(1999, 197.1), new Pair<>(2000, 189.0), new Pair<>(2001, 180.43), new Pair<>(2002, 179.85), new Pair<>(2003, 185.54), new Pair<>(2004, 162.27), new Pair<>(2005, 150.04), new Pair<>(2006, 145.13), new Pair<>(2007, 145.03), new Pair<>(2008, 150.52), new Pair<>(2009, 152.69), new Pair<>(2010, 159.89), new Pair<>(2011, 147.83), new Pair<>(2012, 151.97), new Pair<>(2013, 167.75), new Pair<>(2014, 167.97), new Pair<>(2015, 148.17), new Pair<>(2016, 155.45), new Pair<>(2017, 145.8), new Pair<>(2018, 140.75), new Pair<>(2019, 158.3), new Pair<>(2020, 144.91), new Pair<>(2021, 152.25), new Pair<>(2022, 153.0))
    );

    @Test
    public void getsName() {
        assertEquals("Journal Information", fetcher.getName());
    }

    @Test
    public void getsJournalInfoValidISSN() throws FetcherException {
        assertEquals(Optional.of(journalInformation), fetcher.getJournalInformation("1545-4509", ""));
    }

    @Test
    public void getsJournalInfoUsingName() throws FetcherException {
        assertEquals(Optional.of(journalInformation), fetcher.getJournalInformation("", "Annual Review of Biochemistry"));
    }

    @Test
    public void sameEntryReturnedFromISSNOrName() throws FetcherException {
        assertEquals(fetcher.getJournalInformation("1545-4509", ""), fetcher.getJournalInformation("", "Annual Review of Biochemistry"));
    }

    @Test
    public void getsJournalInfoValidISSNWithoutHyphen() throws FetcherException {
        assertEquals(Optional.of(journalInformation), fetcher.getJournalInformation("15454509", ""));
    }

    @Test
    public void getsJournalInfoNonTrimmedISSN() throws FetcherException {
        assertEquals(Optional.of(journalInformation), fetcher.getJournalInformation(" 1545-4509   ", ""));
    }

    @Test
    public void getJournalInfoExtraSpaceISSN() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("1545 - 4509", ""));
    }

    @Test
    public void getJournalInfoEmptyISSN() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("", ""));
    }

    @Test
    public void getJournalInfoInvalidISSN() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("123-123", ""));
    }

    @Test
    public void getJournalInfoInvalidISSNAndNoName() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("123-123", ""));
    }

    @Test
    public void getJournalInfoNoISSNAndNoName() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("", ""));
    }

    @Test
    public void getJournalInfoNoISSNAndInvalidName() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("", "zzz"));
    }
}
