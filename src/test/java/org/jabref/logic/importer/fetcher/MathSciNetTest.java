package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class MathSciNetTest {
    MathSciNet fetcher;
    private BibEntry ratiuEntry;

    @BeforeEach
    void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        fetcher = new MathSciNet(importFormatPreferences);

        ratiuEntry = new BibEntry();
        ratiuEntry.setType(StandardEntryType.Article);
        ratiuEntry.setCitationKey("MR3537908");
        ratiuEntry.setField(StandardField.AUTHOR, "Chechkin, Gregory A. and Ratiu, Tudor S. and Romanov, Maxim S. and Samokhin, Vyacheslav N.");
        ratiuEntry.setField(StandardField.TITLE, "Existence and uniqueness theorems for the two-dimensional {E}ricksen-{L}eslie system");
        ratiuEntry.setField(StandardField.JOURNAL, "Journal of Mathematical Fluid Mechanics");
        ratiuEntry.setField(StandardField.VOLUME, "18");
        ratiuEntry.setField(StandardField.YEAR, "2016");
        ratiuEntry.setField(StandardField.NUMBER, "3");
        ratiuEntry.setField(StandardField.PAGES, "571--589");
        ratiuEntry.setField(StandardField.KEYWORDS, "76A15 (35A01 35A02 35K61 82D30)");
        ratiuEntry.setField(StandardField.MR_NUMBER, "3537908");
        ratiuEntry.setField(StandardField.ISSN, "1422-6928, 1422-6952");
        ratiuEntry.setField(StandardField.DOI, "10.1007/s00021-016-0250-0");
    }

    @Test
    void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "existence");
        searchEntry.setField(StandardField.AUTHOR, "Ratiu");
        searchEntry.setField(StandardField.JOURNAL, "fluid");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(Collections.singletonList(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByIdInEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.MR_NUMBER, "3537908");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(Collections.singletonList(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Existence and uniqueness theorems Two-Dimensional Ericksen Leslie System");
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(ratiuEntry, fetchedEntries.get(1));
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByIdFindsEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3537908");
        assertEquals(Optional.of(ratiuEntry), fetchedEntry);
    }

    @Test
    void getParser() throws Exception {
        String json = """
                {
                  "results": [
                    {
                      "mrnumber": 4158623,
                      "titles": {
                        "title": "On the weights of general MDS codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B65",
                        "description": "Bounds on codes"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, Tim L."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2020,
                          "pubYear2": null,
                          "volume": "66",
                          "volume2": null,
                          "volume3": null,
                          "number": "9",
                          "journal": {
                            "id": 2292,
                            "shortTitle": "IEEE Trans. Inform. Theory"
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 889610,
                            "rvrCode": 85231,
                            "name": "Jitman, Somphong"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "5414--5418"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 2
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1109/TIT.2020.2977319",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1109%2FTIT.2020.2977319&rft.aufirst=Tim&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=00189448&rft.title=Institute of Electrical and Electronics Engineers  Transactions on Information Theory&rft.atitle=On the weights of general MDS codes&rft.stitle=IEEE Trans  Inform  Theory&rft.volume=66&rft.date=2020&rft.spage=5414&rft.epage=5418&rft.pages=5414-5418&rft.issue=9&rft.jtitle=Institute of Electrical and Electronics Engineers  Transactions on Information Theory",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 4019905,
                      "titles": {
                        "title": "$n$-dimensional optical orthogonal codes, bounds and optimal constructions",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B65",
                        "description": "Bounds on codes"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2019,
                          "pubYear2": null,
                          "volume": "30",
                          "volume2": null,
                          "volume3": null,
                          "number": "5",
                          "journal": {
                            "id": 4449,
                            "shortTitle": "Appl. Algebra Engrg. Comm. Comput."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 929349,
                            "rvrCode": 128559,
                            "name": "Lee, Nari"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "373--386"
                        },
                        "translatedPaging": null
                      },
                      "counts": null,
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1007/s00200-018-00379-3",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1007%2Fs00200-018-00379-3&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=09381279&rft.title=Applicable Algebra in Engineering, Communication and Computing&rft.atitle=$n$-dimensional optical orthogonal codes, bounds and optimal constructions&rft.stitle=Appl  Algebra Engrg  Comm  Comput &rft.volume=30&rft.date=2019&rft.spage=373&rft.epage=386&rft.pages=373-386&rft.issue=5&rft.jtitle=Applicable Algebra in Engineering, Communication and Computing",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 4014640,
                      "titles": {
                        "title": "A note on full weight spectrum codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B05",
                        "description": "Linear codes (general theory)"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, Tim L."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2019,
                          "pubYear2": null,
                          "volume": "8",
                          "volume2": null,
                          "volume3": null,
                          "number": "3",
                          "journal": {
                            "id": 8218,
                            "shortTitle": "Trans. Comb."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": null,
                      "paging": {
                        "paging": {
                          "text": "15--22"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 6
                      },
                      "itemType": "Summary",
                      "articleUrl": "https://doi.org/10.22108/toc.2019.112621.1584",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.22108%2Ftoc.2019.112621.1584&rft.aufirst=Tim&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=22518657&rft.title=Transactions on Combinatorics&rft.atitle=A note on full weight spectrum codes&rft.stitle=Trans  Comb &rft.volume=8&rft.date=2019&rft.spage=15&rft.epage=22&rft.pages=15-22&rft.issue=3&rft.jtitle=Transactions on Combinatorics",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 3917650,
                      "titles": {
                        "title": "Maximum weight spectrum codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B05",
                        "description": "Linear codes (general theory)"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, Tim"
                        },
                        {
                          "id": 1251963,
                          "name": "Neri, Alessandro"
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2019,
                          "pubYear2": null,
                          "volume": "13",
                          "volume2": null,
                          "volume3": null,
                          "number": "1",
                          "journal": {
                            "id": 6241,
                            "shortTitle": "Adv. Math. Commun."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 685341,
                            "rvrCode": 30916,
                            "name": "Oluwade, Bamidele A."
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "101--119"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 6
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.3934/amc.2019006",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.3934%2Famc.2019006&rft.aufirst=Tim&rft.auinit=T&rft.auinit1=T&rft.auinitm=&rft.aulast=Alderson&rft.genre=article&rft.issn=19305346&rft.title=Advances in Mathematics of Communications&rft.atitle=Maximum weight spectrum codes&rft.stitle=Adv  Math  Commun &rft.volume=13&rft.date=2019&rft.spage=101&rft.epage=119&rft.pages=101-119&rft.issue=1&rft.jtitle=Advances in Mathematics of Communications",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 3897552,
                      "titles": {
                        "title": "How many weights can a linear code have?",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B05",
                        "description": "Linear codes (general theory)"
                      },
                      "authors": [
                        {
                          "id": 863999,
                          "name": "Shi, Minjia"
                        },
                        {
                          "id": 1276838,
                          "name": "Zhu, Hongwei"
                        },
                        {
                          "id": 225546,
                          "name": "Solé, Patrick"
                        },
                        {
                          "id": 50285,
                          "name": "Cohen, Gérard D."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2019,
                          "pubYear2": null,
                          "volume": "87",
                          "volume2": null,
                          "volume3": null,
                          "number": "1",
                          "journal": {
                            "id": 6036,
                            "shortTitle": "Des. Codes Cryptogr."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 249580,
                            "rvrCode": 36256,
                            "name": "Hou, Xiang-dong"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "87--95"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 9
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1007/s10623-018-0488-z",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1007%2Fs10623-018-0488-z&rft.aufirst=Minjia&rft.auinit=M&rft.auinit1=M&rft.auinitm=&rft.aulast=Shi&rft.genre=article&rft.issn=09251022&rft.title=Designs, Codes and Cryptography  An International Journal&rft.atitle=How many weights can a linear code have?&rft.stitle=Des  Codes Cryptogr &rft.volume=87&rft.date=2019&rft.spage=87&rft.epage=95&rft.pages=87-95&rft.issue=1&rft.jtitle=Designs, Codes and Cryptography  An International Journal",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 3809747,
                      "titles": {
                        "title": "3-dimensional optical orthogonal codes with ideal autocorrelation-bounds and optimal constructions",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94A55",
                        "description": "Shift register sequences and sequences over finite alphabets in information and communication theory"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, Tim L."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2018,
                          "pubYear2": null,
                          "volume": "64",
                          "volume2": null,
                          "volume3": null,
                          "number": "6",
                          "journal": {
                            "id": 2292,
                            "shortTitle": "IEEE Trans. Inform. Theory"
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": null,
                      "paging": {
                        "paging": {
                          "text": "4392--4398"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 1
                      },
                      "itemType": "Summary",
                      "articleUrl": "https://doi.org/10.1109/TIT.2017.2717538",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1109%2FTIT.2017.2717538&rft.aufirst=Tim&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=00189448&rft.title=Institute of Electrical and Electronics Engineers  Transactions on Information Theory&rft.atitle=3-dimensional optical orthogonal codes with ideal autocorrelation-bounds and optimal constructions&rft.stitle=IEEE Trans  Inform  Theory&rft.volume=64&rft.date=2018&rft.spage=4392&rft.epage=4398&rft.pages=4392-4398&rft.issue=6&rft.jtitle=Institute of Electrical and Electronics Engineers  Transactions on Information Theory",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 3216214,
                      "titles": {
                        "title": "The partition weight enumerator and bounds on MDS codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B05",
                        "description": "Linear codes (general theory)"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 1068150,
                          "name": "Huntemann, Svenja"
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2014,
                          "pubYear2": null,
                          "volume": "6",
                          "volume2": null,
                          "volume3": null,
                          "number": "1",
                          "journal": {
                            "id": 7425,
                            "shortTitle": "Atl. Electron. J. Math."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": null,
                      "paging": {
                        "paging": {
                          "text": "1--10"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 2
                      },
                      "itemType": "Summary",
                      "articleUrl": null,
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.title=Atlantic Electronic Journal of Mathematics&rft.atitle=The partition weight enumerator and bounds on MDS codes&rft.stitle=Atl  Electron  J  Math &rft.volume=6&rft.date=2014&rft.spage=1&rft.epage=10&rft.pages=1-10&rft.issue=1&rft.jtitle=Atlantic Electronic Journal of Mathematics",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2793231,
                      "titles": {
                        "title": "Spreads, arcs, and multiple wavelength codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B60",
                        "description": "Other types of codes"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, Keith E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2011,
                          "pubYear2": null,
                          "volume": "311",
                          "volume2": null,
                          "volume3": null,
                          "number": "13",
                          "journal": {
                            "id": 643,
                            "shortTitle": "Discrete Math."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 850192,
                            "rvrCode": 71699,
                            "name": "Fan, Cuiling"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "1187--1196"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 6
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1016/j.disc.2010.06.010",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1016%2Fj.disc.2010.06.010&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=0012365X&rft.title=Discrete Mathematics&rft.atitle=Spreads, arcs, and multiple wavelength codes&rft.stitle=Discrete Math &rft.volume=311&rft.date=2011&rft.spage=1187&rft.epage=1196&rft.pages=1187-1196&rft.issue=13&rft.jtitle=Discrete Mathematics",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2772904,
                      "titles": {
                        "title": "Classes of permutation arrays in finite projective spaces",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "51E15",
                        "description": "Finite affine and projective planes (geometric aspects)"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, Keith E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2010,
                          "pubYear2": null,
                          "volume": "1",
                          "volume2": null,
                          "volume3": null,
                          "number": "4",
                          "journal": {
                            "id": 7248,
                            "shortTitle": "Int. J. Inf. Coding Theory"
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 97215,
                            "rvrCode": 6155,
                            "name": "Kallaher, M. J."
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "371--383"
                        },
                        "translatedPaging": null
                      },
                      "counts": null,
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1504/IJICOT.2010.032863",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1504%2FIJICOT.2010.032863&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=17537703&rft.title=International Journal of Information and Coding Theory  IJICOT&rft.atitle=Classes of permutation arrays in finite projective spaces&rft.stitle=Int  J  Inf  Coding Theory&rft.volume=1&rft.date=2010&rft.spage=371&rft.epage=383&rft.pages=371-383&rft.issue=4&rft.jtitle=International Journal of Information and Coding Theory  IJICOT",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2766013,
                      "titles": {
                        "title": "Hyperconics and multiple weight codes for OCDMA",
                        "translatedTitle": null
                      },
                      "entryType": "BC",
                      "primaryClass": {
                        "code": "94B27",
                        "description": "Geometric methods (including applications of algebraic geometry) applied to coding theory"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        }
                      ],
                      "issue": {
                        "issue": null,
                        "translatedIssue": null
                      },
                      "book": {
                        "pubYear": 2010,
                        "publisher": [
                          {
                            "name": "American Mathematical Society",
                            "location": "Providence, RI",
                            "preText": null,
                            "postText": null
                          }
                        ],
                        "isbn": [
                          "978-0-8218-4956-9"
                        ],
                        "series": [
                          {
                            "serId": 1059,
                            "title": "Contemporary Mathematics",
                            "transTitle": null,
                            "volume": "523",
                            "shortTitle": "Contemp. Math."
                          }
                        ]
                      },
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 173380,
                            "rvrCode": 18754,
                            "name": "Tonchev, Vladimir D."
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "67--76"
                        },
                        "translatedPaging": null
                      },
                      "counts": null,
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1090/conm/523/10332",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:book&rft_id=info:doi/10.1090%2Fconm%2F523%2F10332&rft_id=urn:ISBN:978-0-8218-4956-9&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=proceeding&rft.title=Error-correcting codes, finite geometries and cryptography&rft.atitle=Hyperconics and multiple weight codes for OCDMA&rft.stitle=Contemporary Mathematics&rft.volume=523&rft.date=2010&rft.spage=67&rft.epage=76&rft.pages=67-76&rft.isbn=9780821849569",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2742541,
                      "titles": {
                        "title": "Error-correcting codes, finite geometries and cryptography",
                        "translatedTitle": null
                      },
                      "entryType": "BCZ",
                      "primaryClass": {
                        "code": "94Bxx",
                        "description": ""
                      },
                      "authors": [],
                      "issue": {
                        "issue": null,
                        "translatedIssue": null
                      },
                      "book": {
                        "pubYear": 2010,
                        "publisher": [
                          {
                            "name": "American Mathematical Society",
                            "location": "Providence, RI",
                            "preText": null,
                            "postText": null
                          }
                        ],
                        "isbn": [
                          "978-0-8218-4956-9"
                        ],
                        "series": [
                          {
                            "serId": 1059,
                            "title": "Contemporary Mathematics",
                            "transTitle": null,
                            "volume": "523",
                            "shortTitle": "Contemp. Math."
                          }
                        ]
                      },
                      "reviewer": null,
                      "paging": {
                        "paging": {
                          "text": "viii+244 pp."
                        },
                        "translatedPaging": null
                      },
                      "counts": null,
                      "itemType": "Summary",
                      "articleUrl": "https://doi.org/10.1090/conm/523",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:book&rft_id=info:doi/10.1090%2Fconm%2F523&rft_id=urn:ISBN:978-0-8218-4956-9&rft.genre=conference&rft.title=Error-correcting codes, finite geometries and cryptography&rft.stitle=Contemporary Mathematics&rft.volume=523&rft.date=2010&rft.spage=1&rft.spage=244&rft.pages=1-244&rft.isbn=9780821849569",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2553388,
                      "titles": {
                        "title": "2-dimensional optical orthogonal codes from Singer groups",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B60",
                        "description": "Other types of codes"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, Keith E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2009,
                          "pubYear2": null,
                          "volume": "157",
                          "volume2": null,
                          "volume3": null,
                          "number": "14",
                          "journal": {
                            "id": 615,
                            "shortTitle": "Discrete Appl. Math."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": null,
                      "paging": {
                        "paging": {
                          "text": "3008--3019"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 18
                      },
                      "itemType": "Summary",
                      "articleUrl": "https://doi.org/10.1016/j.dam.2009.06.002",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1016%2Fj.dam.2009.06.002&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=0166218X&rft.title=Discrete Applied Mathematics  The Journal of Combinatorial Algorithms, Informatics and Computational Sciences&rft.atitle=2-dimensional optical orthogonal codes from Singer groups&rft.stitle=Discrete Appl  Math &rft.volume=157&rft.date=2009&rft.spage=3008&rft.epage=3019&rft.pages=3008-3019&rft.issue=14&rft.jtitle=Discrete Applied Mathematics  The Journal of Combinatorial Algorithms, Informatics and Computational Sciences",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2529622,
                      "titles": {
                        "title": "On the maximality of linear codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B05",
                        "description": "Linear codes (general theory)"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 615685,
                          "name": "Gács, András"
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2009,
                          "pubYear2": null,
                          "volume": "53",
                          "volume2": null,
                          "volume3": null,
                          "number": "1",
                          "journal": {
                            "id": 6036,
                            "shortTitle": "Des. Codes Cryptogr."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 811978,
                            "rvrCode": 68997,
                            "name": "Pasticci, Fabio"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "59--68"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 3
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1007/s10623-009-9293-z",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1007%2Fs10623-009-9293-z&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=09251022&rft.title=Designs, Codes and Cryptography  An International Journal&rft.atitle=On the maximality of linear codes&rft.stitle=Des  Codes Cryptogr &rft.volume=53&rft.date=2009&rft.spage=59&rft.epage=68&rft.pages=59-68&rft.issue=1&rft.jtitle=Designs, Codes and Cryptography  An International Journal",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2658658,
                      "titles": {
                        "title": "Partitions in finite geometry and related constant composition codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "51E20",
                        "description": "Combinatorial structures in finite projective spaces"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, Tim L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, Keith E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "8",
                          "volume2": null,
                          "volume3": null,
                          "number": null,
                          "journal": {
                            "id": 6154,
                            "shortTitle": "Innov. Incidence Geom."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 811978,
                            "rvrCode": 68997,
                            "name": "Pasticci, Fabio"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "49--71"
                        },
                        "translatedPaging": null
                      },
                      "counts": null,
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.2140/iig.2008.8.49",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.2140%2Fiig.2008.8.49&rft.aufirst=Tim&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=26407337&rft.title=Innovations in Incidence Geometry  Algebraic, Topological and Combinatorial&rft.atitle=Partitions in finite geometry and related constant composition codes&rft.stitle=Innov  Incidence Geom &rft.volume=8&rft.date=2008&rft.spage=49&rft.epage=71&rft.pages=49-71&&rft.jtitle=Innovations in Incidence Geometry  Algebraic, Topological and Combinatorial",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2452815,
                      "titles": {
                        "title": "Geometric constructions of optimal optical orthogonal codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B27",
                        "description": "Geometric methods (including applications of algebraic geometry) applied to coding theory"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, K. E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "2",
                          "volume2": null,
                          "volume3": null,
                          "number": "4",
                          "journal": {
                            "id": 6241,
                            "shortTitle": "Adv. Math. Commun."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 675427,
                            "rvrCode": 35099,
                            "name": "Kim, Jon-Lark"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "451--467"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 15
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.3934/amc.2008.2.451",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.3934%2Famc.2008.2.451&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=19305346&rft.title=Advances in Mathematics of Communications&rft.atitle=Geometric constructions of optimal optical orthogonal codes&rft.stitle=Adv  Math  Commun &rft.volume=2&rft.date=2008&rft.spage=451&rft.epage=467&rft.pages=451-467&rft.issue=4&rft.jtitle=Advances in Mathematics of Communications",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2451027,
                      "titles": {
                        "title": "Families of optimal OOCs with $\\\\lambda=2$",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B25",
                        "description": "Combinatorial codes"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, Tim L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, Keith E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "54",
                          "volume2": null,
                          "volume3": null,
                          "number": "8",
                          "journal": {
                            "id": 2292,
                            "shortTitle": "IEEE Trans. Inform. Theory"
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 334456,
                            "rvrCode": 24644,
                            "name": "Borges-Ayats, Joaquim"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "3722--3724"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 20
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1109/TIT.2008.926394",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1109%2FTIT.2008.926394&rft.aufirst=Tim&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=00189448&rft.title=Institute of Electrical and Electronics Engineers  Transactions on Information Theory&rft.atitle=Families of optimal OOCs with $lambda=2$&rft.stitle=IEEE Trans  Inform  Theory&rft.volume=54&rft.date=2008&rft.spage=3722&rft.epage=3724&rft.pages=3722-3724&rft.issue=8&rft.jtitle=Institute of Electrical and Electronics Engineers  Transactions on Information Theory",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2398834,
                      "titles": {
                        "title": "Codes from cubic curves and their extensions",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B27",
                        "description": "Geometric methods (including applications of algebraic geometry) applied to coding theory"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 42380,
                          "name": "Bruen, A. A."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "15",
                          "volume2": null,
                          "volume3": null,
                          "number": "1",
                          "journal": {
                            "id": 4633,
                            "shortTitle": "Electron. J. Combin."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 272152,
                            "rvrCode": 47895,
                            "name": "Kim, Seon Jeong"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "Research paper 42, 9 pp."
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 1
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.37236/766",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.37236%2F766&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.title=Electronic Journal of Combinatorics&rft.atitle=Codes from cubic curves and their extensions&rft.stitle=Electron  J  Combin &rft.volume=15&rft.date=2008&rft.spage=42&rft.epage=9&rft.pages=42-9&rft.issue=1&rft.jtitle=Electronic Journal of Combinatorics",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2394740,
                      "titles": {
                        "title": "Bruck nets and 2-dimensional codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B05",
                        "description": "Linear codes (general theory)"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "52",
                          "volume2": null,
                          "volume3": null,
                          "number": null,
                          "journal": {
                            "id": 4117,
                            "shortTitle": "Bull. Inst. Combin. Appl."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 721987,
                            "rvrCode": 56185,
                            "name": "Lampe, Lutz"
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "33--44"
                        },
                        "translatedPaging": null
                      },
                      "counts": null,
                      "itemType": "Reviewed",
                      "articleUrl": null,
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=11831278&rft.title=Bulletin of the Institute of Combinatorics and its Applications&rft.atitle=Bruck nets and 2-dimensional codes&rft.stitle=Bull  Inst  Combin  Appl &rft.volume=52&rft.date=2008&rft.spage=33&rft.epage=44&rft.pages=33-44&&rft.jtitle=Bulletin of the Institute of Combinatorics and its Applications",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2389969,
                      "titles": {
                        "title": "Maximal AMDS codes",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B27",
                        "description": "Geometric methods (including applications of algebraic geometry) applied to coding theory"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 42380,
                          "name": "Bruen, A. A."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "19",
                          "volume2": null,
                          "volume3": null,
                          "number": "2",
                          "journal": {
                            "id": 4449,
                            "shortTitle": "Appl. Algebra Engrg. Comm. Comput."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 329025,
                            "rvrCode": 58213,
                            "name": "Daskalov, Rumen N."
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "87--98"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 4
                      },
                      "itemType": "Summary",
                      "articleUrl": "https://doi.org/10.1007/s00200-008-0058-0",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1007%2Fs00200-008-0058-0&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=09381279&rft.title=Applicable Algebra in Engineering, Communication and Computing&rft.atitle=Maximal AMDS codes&rft.stitle=Appl  Algebra Engrg  Comm  Comput &rft.volume=19&rft.date=2008&rft.spage=87&rft.epage=98&rft.pages=87-98&rft.issue=2&rft.jtitle=Applicable Algebra in Engineering, Communication and Computing",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    },
                    {
                      "mrnumber": 2382348,
                      "titles": {
                        "title": "Classes of optical orthogonal codes from arcs in root subspaces",
                        "translatedTitle": null
                      },
                      "entryType": "J",
                      "primaryClass": {
                        "code": "94B27",
                        "description": "Geometric methods (including applications of algebraic geometry) applied to coding theory"
                      },
                      "authors": [
                        {
                          "id": 758603,
                          "name": "Alderson, T. L."
                        },
                        {
                          "id": 692091,
                          "name": "Mellinger, Keith E."
                        }
                      ],
                      "issue": {
                        "issue": {
                          "pubYear": 2008,
                          "pubYear2": null,
                          "volume": "308",
                          "volume2": null,
                          "volume3": null,
                          "number": "7",
                          "journal": {
                            "id": 643,
                            "shortTitle": "Discrete Math."
                          },
                          "volSlash": "N",
                          "isbn": null,
                          "elementOrd": null
                        },
                        "translatedIssue": null
                      },
                      "book": null,
                      "reviewer": {
                        "public": true,
                        "reviewers": [
                          {
                            "authId": 97215,
                            "rvrCode": 6155,
                            "name": "Kallaher, M. J."
                          }
                        ]
                      },
                      "paging": {
                        "paging": {
                          "text": "1093--1101"
                        },
                        "translatedPaging": null
                      },
                      "counts": {
                        "cited": 5
                      },
                      "itemType": "Reviewed",
                      "articleUrl": "https://doi.org/10.1016/j.disc.2007.03.063",
                      "openURL": {
                        "imageLink": "http://www.lib.unb.ca/img/asin/res20x150.gif",
                        "targetLink": "https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1016%2Fj.disc.2007.03.063&rft.aufirst=T.&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=0012365X&rft.title=Discrete Mathematics&rft.atitle=Classes of optical orthogonal codes from arcs in root subspaces&rft.stitle=Discrete Math &rft.volume=308&rft.date=2008&rft.spage=1093&rft.epage=1101&rft.pages=1093-1101&rft.issue=7&rft.jtitle=Discrete Mathematics",
                        "textLink": ""
                      },
                      "prePubl": null,
                      "public": true
                    }
                  ],
                  "total": 31
                }
                """;

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);

        assertEquals(List.of(
                new BibEntry(StandardEntryType.Article)
                        .withField(StandardField.TITLE, "On the weights of general MDS codes")
                        .withField(StandardField.AUTHOR, "Alderson, Tim L.")
                        .withField(StandardField.YEAR, "2020")
                        .withField(StandardField.JOURNAL, "IEEE Trans. Inform. Theory")
                        .withField(StandardField.VOLUME, "66")
                        .withField(StandardField.NUMBER, "9")
                        .withField(StandardField.PAGES, "5414--5418")
                        .withField(StandardField.MR_NUMBER, "4158623")
                        .withField(StandardField.KEYWORDS, "Bounds on codes")
                        .withField(StandardField.DOI, "https://doi.org/10.1109/TIT.2020.2977319")
                        .withField(StandardField.ISSN, "0018-9448")
        ), entries);
    }
}
