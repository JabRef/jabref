package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuropePmcFetcher implements IdBasedParserFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EuropePmcFetcher.class);

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        return new URI("https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=" + identifier + "&resultType=core&format=json").toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty()) {
                return List.of();
            }
            return List.of(jsonItemToBibEntry(response));
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            JSONObject result = item.getJSONObject("resultList").getJSONArray("result").getJSONObject(0);

            LOGGER.debug(result.toString(2));

            EntryType entryType = StandardEntryType.Article;
            if (result.has("pubTypeList")) {
                for (Object o : result.getJSONObject("pubTypeList").getJSONArray("pubType")) {
                    if ("letter".equalsIgnoreCase(o.toString())) {
                        entryType = StandardEntryType.Article;
                        break;
                        // TODO: handle other types e.g. books
                    }
                }
            }

            BibEntry entry = new BibEntry(entryType);

            entry.setField(StandardField.TITLE, result.optString("title"));
            entry.setField(StandardField.ABSTRACT, result.optString("abstractText"));

            entry.setField(StandardField.YEAR, result.optString("pubYear"));
            entry.setField(StandardField.VOLUME, result.optString("journalVolume"));
            entry.setField(StandardField.ISSUE, result.optString("journalIssue"));

            String pages = result.optString("pageInfo");
            entry.setField(StandardField.PAGES, pages);

            entry.setField(StandardField.DOI, result.optString("doi"));
            entry.setField(StandardField.PMID, result.optString("pmid"));

            // Handle URL
            if (result.has("pmid")) {
                entry.setField(StandardField.URL, "https://pubmed.ncbi.nlm.nih.gov/" + result.getString("pmid") + "/");
            }

            if (result.has("journalInfo") && result.getJSONObject("journalInfo").has("issn")) {
                entry.setField(StandardField.ISSN, result.getJSONObject("journalInfo").getString("issn"));
            }

            // Handle authors
            if (result.has("authorList") && result.getJSONObject("authorList").has("author")) {
                JSONArray authors = result.getJSONObject("authorList").getJSONArray("author");

                List<Author> authorList = new ArrayList<>();

                for (int i = 0; i < authors.length(); i++) {
                    JSONObject author = authors.getJSONObject(i);

                    String lastName = author.optString("lastName", "");
                    String firstName = author.optString("firstName", "");
                    authorList.add(new Author(firstName, "", "", lastName, ""));

                    entry.setField(StandardField.AUTHOR, AuthorList.of(authorList).getAsLastFirstNamesWithAnd(false));
                }
            }

            if (result.has("pubTypeList") && result.getJSONObject("pubTypeList").has("pubType")) {
                JSONArray pubTypes = result.getJSONObject("pubTypeList").getJSONArray("pubType");
                if (!pubTypes.isEmpty()) {
                    entry.setField(StandardField.PUBSTATE, pubTypes.getString(0));
                }
            }

            if (result.has("pubModel")) {
                Optional.ofNullable(result.optString("pubModel")).ifPresent(pubModel -> entry.setField(StandardField.HOWPUBLISHED, pubModel));
            }
            if (result.has("publicationStatus")) {
                Optional.ofNullable(result.optString("publicationStatus")).ifPresent(pubStatus -> entry.setField(StandardField.PUBSTATE, pubStatus));
            }

            if (result.has("journalInfo")) {
                JSONObject journalInfo = result.getJSONObject("journalInfo");
                Optional.ofNullable(journalInfo.optString("issue")).ifPresent(issue -> entry.setField(StandardField.ISSUE, issue));
                Optional.ofNullable(journalInfo.optString("volume")).ifPresent(volume -> entry.setField(StandardField.VOLUME, volume));
                Optional.of(journalInfo.optInt("yearOfPublication")).ifPresent(year -> entry.setField(StandardField.YEAR, year.toString()));
                Optional.of(journalInfo.optInt("monthOfPublication"))
                        .flatMap(month -> Month.parse(month.toString()))
                        .ifPresent(parsedMonth -> entry.setField(StandardField.MONTH, parsedMonth.getJabRefFormat()));
                if (journalInfo.has("journal")) {
                    JSONObject journal = journalInfo.getJSONObject("journal");
                    Optional.ofNullable(journal.optString("title")).ifPresent(title -> entry.setField(StandardField.JOURNAL, title));
                    Optional.ofNullable(journal.optString("nlmid")).ifPresent(nlmid -> entry.setField(new UnknownField("nlmid"), nlmid));
                    Optional.ofNullable(journal.optString("issn")).ifPresent(issn -> entry.setField(StandardField.ISSN, issn));
                }
            }

            return entry;
        } catch (JSONException e) {
            throw new ParseException("Error parsing EuropePMC response", e);
        }
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()).cleanup(entry);
    }

    @Override
    public String getName() {
        return "Europe/PMCID";
    }
}
