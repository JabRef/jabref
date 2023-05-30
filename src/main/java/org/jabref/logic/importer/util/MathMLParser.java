package org.jabref.logic.importer.util;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

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
    private static final String XSLT_FILE_PATH = "src/main/resources/xslt/mathml_latex/mmltex.xsl";

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
            Source xsltSource = new StreamSource(new File(XSLT_FILE_PATH));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(xsltSource);

            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            transformer.transform(xmlSource, result);

            latexResult = writer.getBuffer().toString();
        } catch (XMLStreamException | TransformerException e) {
            LOGGER.debug("could not convert MathML element into LaTeX", e);
        }

        return latexResult;
    }
}

