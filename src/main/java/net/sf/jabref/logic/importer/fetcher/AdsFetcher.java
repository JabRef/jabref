package net.sf.jabref.logic.importer.fetcher;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.client.utils.URIBuilder;

/**
 * This class handles accessing and obtaining BibTeX entry
 * from ADS(The NASA Astrophysics Data System).
 * Fetching using DOI(Document Object Identifier) is only supported.
 */
public class AdsFetcher implements IdBasedFetcher {

    private static final String URL_PATTERN = "http://adsabs.harvard.edu/doi/";

    private ImportFormatPreferences preferences;

    public AdsFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "ADS from ADS-DOI";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ADS;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<BibEntry> result = Optional.empty();

        String key = identifier.replaceAll("^(doi:|DOI:)", "");

        try {
            URIBuilder uriBuilder = new URIBuilder(URL_PATTERN + key);
            uriBuilder.addParameter("data_type", "BIBTEX");
            URL url = uriBuilder.build().toURL();

            String bibtexString = Unirest.get(url.toString()).asString().getBody();

            if(bibtexString.contains("@")) {
                bibtexString = bibtexString.substring(bibtexString.indexOf("@"));
                result = BibtexParser.singleFromString(bibtexString, preferences);
            }

            if (result.isPresent()) {
                BibEntry entry = result.get();
                URIBuilder uriBuilderAbstract = new URIBuilder(URL_PATTERN + key);
                uriBuilderAbstract.addParameter("data_type", "XML");
                URL urlAbstract = uriBuilderAbstract.build().toURL();

                String abstractString = Unirest.get(urlAbstract.toString()).asString().getBody();
                InputStream stream = new ByteArrayInputStream(abstractString.getBytes(StandardCharsets.UTF_8));

                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLStreamReader reader = factory.createXMLStreamReader(new BufferedInputStream(stream));
                boolean isAbstract = false;
                StringBuilder abstractSB = new StringBuilder();
                while (reader.hasNext()) {
                    reader.next();
                    if (reader.isStartElement() &&
                            FieldName.ABSTRACT.equals(reader.getLocalName())) {
                        isAbstract = true;
                    }
                    if (isAbstract && reader.isCharacters()) {
                        abstractSB.append(reader.getText());
                    }
                    if (isAbstract && reader.isEndElement()) {
                        isAbstract = false;
                    }
                }
                String abstractText = abstractSB.toString();
                abstractText = abstractText.replace("\n", " ");
                entry.setField(FieldName.ABSTRACT, abstractText);
                result = Optional.of(entry);
            }
        } catch (MalformedURLException | UnirestException | URISyntaxException e) {
            throw new FetcherException("Error fetching ADS", e);
        } catch (XMLStreamException e) {
            throw new FetcherException(Localization.lang("An_error_occurred_while_parsing_abstract"), e);
        }
        return result;
    }

}
