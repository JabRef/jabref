package org.jabref.logic.importer.util;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jabref.architecture.AllowedToUseClassGetResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseClassGetResource("to determine the root directory")
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
        String latexResult;

        try (InputStream xsltResource = MathMLParser.class.getResourceAsStream(XSLT_FILE_PATH)) {
            // extract XML content
            xmlContent = StaxParser.getXMLContent(reader);

            // convert to LaTeX using XSLT file
            Source xmlSource = new StreamSource(new StringReader(xmlContent));

            // No SystemId required, because no relative URLs need to be resolved
            Source xsltSource = new StreamSource(xsltResource, MathMLParser.class.getResource(XSLT_FILE_PATH).toExternalForm());

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(xsltSource);

            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            transformer.transform(xmlSource, result);

            latexResult = writer.getBuffer().toString();
        } catch (Exception e) {
            LOGGER.error("Could not transform", e);
            return "<Unsupported MathML expression>";
        }

        return latexResult;
    }
}

