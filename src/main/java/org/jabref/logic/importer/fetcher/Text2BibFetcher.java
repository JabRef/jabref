package org.jabref.logic.importer.fetcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This fetcher parses text format citations using the web page of text2bib (https://text2bib.economics.utoronto.ca/)
 */
public class Text2BibFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Text2BibFetcher.class);
    private final ImportFormatPreferences importFormatPreferences;

    public Text2BibFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        BasicCookieStore cookieStore = new BasicCookieStore();
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(10000);
        requestBuilder.setConnectionRequestTimeout(10000);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        HttpClient client = builder.setDefaultCookieStore(cookieStore).build();

        // get the session ID cookie "T2BSID"
        HttpGet loginPageRequest = new HttpGet("https://text2bib.economics.utoronto.ca/index.php/login/login");
        org.apache.http.HttpResponse loginPageResponse;
        try {
            loginPageResponse = client.execute(loginPageRequest);
        } catch (IOException e) {
            throw new FetcherException("Could not open login page.");
        }
        if (loginPageResponse.getStatusLine().getStatusCode() != 200) {
            LOGGER.error("Could not open login page.");
            throw new FetcherException("Could not open login page.");
        }
        if (LOGGER.isDebugEnabled()) {
            try {
                String loginPageResponseBody = CharStreams.toString(new InputStreamReader(loginPageResponse.getEntity().getContent(), Charsets.UTF_8));
                LOGGER.debug("Received login page", loginPageResponseBody);
            } catch (IOException e) {
            }
        }
        loginPageRequest.reset();

        HttpPost loginRequest = new HttpPost("https://text2bib.economics.utoronto.ca/index.php/login/signIn");
        loginRequest.setHeader("Referer", "https://text2bib.economics.utoronto.ca/index.php/login/login");
        loginRequest.setHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("source", ""));
        form.add(new BasicNameValuePair("username", "koppor"));
        form.add(new BasicNameValuePair("password", "IOAt7cS5zpUXQkXvRmk5"));
        UrlEncodedFormEntity loginRequestEntity = new UrlEncodedFormEntity(form, Consts.UTF_8);
        loginRequest.setEntity(loginRequestEntity);

        org.apache.http.HttpResponse loginResponse;
        try {
            loginResponse = client.execute(loginRequest);
        } catch (IOException e) {
            throw new FetcherException("Could not login.");
        }

        if (loginResponse.getStatusLine().getStatusCode() != 302) {
            LOGGER.error("Could not login.");
            throw new FetcherException("Could not login.");
        }
        loginRequest.reset();

        if (LOGGER.isDebugEnabled()) {
            try {
                HttpGet loggedInRequest = new HttpGet("https://text2bib.economics.utoronto.ca/index.php/index");
                HttpResponse loggedInResponse = client.execute(loggedInRequest);
                String loggedInResponseBody = CharStreams.toString(new InputStreamReader(loggedInResponse.getEntity().getContent(), Charsets.UTF_8));
                LOGGER.debug("Logged in screen: {}", loggedInResponseBody);
            } catch (Exception e) {
            }
        }

        HttpPost conversionRequest = new HttpPost("https://text2bib.economics.utoronto.ca/index.php/index/convert");

        conversionRequest.setHeader("Referer", "https://text2bib.economics.utoronto.ca/index.php/index");
        conversionRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conversionRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:70.0) Gecko/20100101 Firefox/70.0");
        conversionRequest.setHeader("Accept-Language", "de-DE,de;q=0.8,en-US;q=0.5,en;q=0.3/20100101 Firefox/70.0");

        File file = new File("C:/temp/test.txt");

        MultipartEntityBuilder requestBodyMultipartBuilder = MultipartEntityBuilder.create();
        requestBodyMultipartBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        requestBodyMultipartBuilder.addPart("index", new StringBody("0", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("uploadFile", new FileBody(file, ContentType.DEFAULT_TEXT));
        requestBodyMultipartBuilder.addPart("labelStyle", new StringBody("long", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("lineEndings", new StringBody("l", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("charEncoding", new StringBody("utf8leave", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("language", new StringBody("en", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("firstComponent", new StringBody("authors", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("itemSeparator", new StringBody("cr", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("percentComment", new StringBody("0", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("incremental", new StringBody("0", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("citationUserGroupId", new StringBody("0", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("debug", new StringBody("0", ContentType.MULTIPART_FORM_DATA));
        requestBodyMultipartBuilder.addPart("B1", new StringBody("Convert to BibTeX", ContentType.MULTIPART_FORM_DATA));
        HttpEntity entity = requestBodyMultipartBuilder.build();
        conversionRequest.setEntity(entity);
        org.apache.http.HttpResponse conversionResponse;
        try {
            conversionResponse = client.execute(conversionRequest);
        } catch (IOException e) {
            LOGGER.debug("Could not trigger conversion.", e);
            throw new FetcherException("Could not trigger conversion.");
        }

        if (conversionResponse.getStatusLine().getStatusCode() != 200) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Could not convert.");
                try {
                    String conversionResponseBody = CharStreams.toString(new InputStreamReader(conversionResponse.getEntity().getContent(), Charsets.UTF_8));
                    LOGGER.debug("Conversion response body: {}", conversionResponseBody);
                } catch (Exception e) {
                }
            }
            throw new FetcherException("Could not convert.");
        }
        String resultBody;
        try {
            resultBody = CharStreams.toString(new InputStreamReader(conversionResponse.getEntity().getContent(), Charsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Could not get result.");
            throw new FetcherException("Could not get result.");
        }
        conversionRequest.reset();

        LOGGER.debug("Result body {}", resultBody);

        String bibtexUrl = resultBody.replaceAll(".*<a href=\"https://text2bib.economics.utoronto.ca/index.php/index/download/(\\d+).*", "$1");
        if (bibtexUrl.length() > 200) {
            LOGGER.error("Could not determine bibtex url.");
            throw new FetcherException("Could not determine bibtex url.");
        }

        HttpGet bibtexRequest = new HttpGet(bibtexUrl);
        org.apache.http.HttpResponse bibtexResponse;
        try {
            bibtexResponse = client.execute(bibtexRequest);
        } catch (IOException e) {
            LOGGER.error("Could not get bibtex.");
            throw new FetcherException("Could not fetch bibtex.");
        }

        if (bibtexResponse.getStatusLine().getStatusCode() != 200) {
            LOGGER.error("Could not fetch bibtex from {}.", bibtexUrl);
            throw new FetcherException("Could not fetch bibtex.");
        }
        bibtexRequest.reset();

        String bibtexString;
        try {
            bibtexString = CharStreams.toString(new InputStreamReader(bibtexResponse.getEntity().getContent(), Charsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Could not extract bibtex string.");
            throw new FetcherException("Could not extract bibtex string.");
        }

        BibtexParser bibtexParser = new BibtexParser(this.importFormatPreferences, new DummyFileUpdateMonitor());
        List<BibEntry> bibEntries = null;
        try {
            bibEntries = bibtexParser.parseEntries(bibtexString);
        } catch (ParseException e) {
            LOGGER.error("Could parse result", e);
            throw new FetcherException("Could not parse result from online service");
        }
        return bibEntries;
    }

    @Override
    public String getName() {
        return "text2bib";
    }
}
