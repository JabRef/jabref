package org.jabref.logic.importer.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MathMLParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MathMLParser.class);
    private static final String XSLT_FILE_PATH = "/xslt/mathml_latex/mmltex.xsl";

    /**
     * Parses the MathML element into its corresponding
     * LaTeX representation, using an XSLT transformation file
     *
     * @param reader the stream reader
     * @return Returns the LaTeX representation
     */
    public static String parse(XMLStreamReader reader) {
        String xmlContent = "";
        String latexResult = "<Unsupported MathML expression>";

        try {
            // extract XML content
            xmlContent = StaxParser.getXMLContent(reader);

            // convert to LaTeX using XSLT file
            Source xmlSource = new StreamSource(new StringReader(xmlContent));

            URL xsltResource = MathMLParser.class.getResource(XSLT_FILE_PATH);
            Source xsltSource = new StreamSource(Objects.requireNonNull(xsltResource).openStream(), xsltResource.toURI().toASCIIString());

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(xsltSource);

            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            transformer.transform(xmlSource, result);

            latexResult = writer.getBuffer().toString();
        } catch (XMLStreamException e) {
            LOGGER.debug("An exception occurred when getting XML content", e);
        } catch (IOException e) {
            LOGGER.debug("An I/O exception occurred", e);
        } catch (URISyntaxException e) {
            LOGGER.debug("XSLT Source URI invalid", e);
        } catch (TransformerException e) {
            LOGGER.debug("An exception occurred during transformation", e);
        }

        return latexResult;
    }
}

