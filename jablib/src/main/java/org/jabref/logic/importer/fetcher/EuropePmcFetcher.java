package org.jabref.logic.importer.fetcher;



import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;

import java.net.URI;

import java.net.URISyntaxException;

import java.net.URL;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

import java.util.List;



import org.jabref.logic.cleanup.FieldFormatterCleanup;

import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;

import org.jabref.logic.importer.IdBasedParserFetcher;

import org.jabref.logic.importer.ParseException;

import org.jabref.logic.importer.Parser;

import org.jabref.logic.importer.SearchBasedParserFetcher;

import org.jabref.logic.importer.util.JsonReader;

import org.jabref.model.entry.BibEntry;

import org.jabref.model.entry.Month;

import org.jabref.model.entry.field.StandardField;

import org.jabref.model.entry.types.EntryType;

import org.jabref.model.entry.types.StandardEntryType;

import org.jabref.model.search.rules.SearchBasedQueryNode;



import kong.unirest.core.json.JSONArray;

import kong.unirest.core.json.JSONException;

import kong.unirest.core.json.JSONObject;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;



public class EuropePmcFetcher implements IdBasedParserFetcher, SearchBasedParserFetcher {

private static final Logger LOGGER = LoggerFactory.getLogger(EuropePmcFetcher.class);

private static final String BASE_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=%s&resultType=core&format=json";



@Override

public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {

try {

String encodedIdentifier = URLEncoder.encode(identifier, StandardCharsets.UTF_8.name());

return new URI(String.format(BASE_URL, encodedIdentifier)).toURL();

} catch (UnsupportedEncodingException e) {

throw new RuntimeException(e);

}

}



// This is the correct method signature the compiler is asking for.

@Override

public URL getURLForQuery(SearchBasedQueryNode query) throws URISyntaxException, MalformedURLException {

try {

String queryString = query.getTree().toString();

String encodedQuery = URLEncoder.encode(queryString, StandardCharsets.UTF_8.name());

return new URI(String.format(BASE_URL, encodedQuery)).toURL();

} catch (UnsupportedEncodingException e) {

throw new RuntimeException(e);

}

}



@Override

public Parser getParser() {

return inputStream -> {

JSONObject response = JsonReader.toJsonObject(inputStream);

List<BibEntry> entries = new ArrayList<>();

if (response.isEmpty() || !response.has("resultList")) {

return List.of();

}

JSONArray results = response.getJSONObject("resultList").getJSONArray("result");

for (int i = 0; i < results.length(); i++) {

JSONObject item = results.getJSONObject(i);

entries.add(jsonItemToBibEntry(item));

}

return entries;

};

}



private BibEntry jsonItemToBibEntry(JSONObject result) throws ParseException {

try {

EntryType entryType = StandardEntryType.Article;

if (result.has("pubTypeList")) {

JSONArray pubTypes = result.getJSONObject("pubTypeList").getJSONArray("pubType");

for (Object type : pubTypes) {

if ("book".equalsIgnoreCase(type.toString())) {

entryType = StandardEntryType.Book;

break;

}

}

}



BibEntry entry = new BibEntry(entryType);

entry.setField(StandardField.TITLE, result.optString("title"));

entry.setField(StandardField.ABSTRACT, result.optString("abstractText"));

entry.setField(StandardField.YEAR, result.optString("pubYear"));



if (result.has("journalInfo")) {

JSONObject journalInfo = result.getJSONObject("journalInfo");

int year = journalInfo.optInt("yearOfPublication");

if (year > 0) {

entry.setField(StandardField.YEAR, String.valueOf(year));

}



int month = journalInfo.optInt("monthOfPublication");

if (month >= 1 && month <= 12) {

Month.of(month)

.ifPresent(parsedMonth -> entry.setField(StandardField.MONTH, parsedMonth.getJabRefFormat()));

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

return "EuropePMC";

}

}
