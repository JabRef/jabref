package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.sciteTallies.TalliesResponse;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SciteAiFetcherTest {
    @Test
    void sciteTallyDTO() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", 1);
        jsonObject.put("supporting", 2);
        jsonObject.put("contradicting", 3);
        jsonObject.put("mentioning", 4);
        jsonObject.put("unclassified", 5);
        jsonObject.put("citingPublications", 6);
        jsonObject.put("doi", "test_doi");

        TalliesResponse dto = TalliesResponse.fromJSONObject(jsonObject);

        assertEquals(1, dto.total());
        assertEquals(2, dto.supporting());
        assertEquals(3, dto.contradicting());
        assertEquals(4, dto.mentioning());
        assertEquals(5, dto.unclassified());
        assertEquals(6, dto.citingPublications());
        assertEquals("test_doi", dto.doi());
    }

    @Test
    void fetchTallies() throws FetcherException {
        SciteAiFetcher viewModel = new SciteAiFetcher();
        DOI doi = new DOI("10.1109/ICECS.2010.5724443");
        Optional<DOI> actual = DOI.parse(viewModel.fetchTallies(doi).doi());
        assertEquals(Optional.of(doi), actual);
    }
}
