/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.jabref.importer.fileformat.bibtexml.Entry;
import net.sf.jabref.importer.fileformat.bibtexml.File;
import net.sf.jabref.importer.fileformat.bibtexml.Inbook;
import net.sf.jabref.importer.fileformat.bibtexml.Incollection;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Importer for the BibTeXML format.
 * <p>
 * check here for details on the format
 * http://bibtexml.sourceforge.net/
 */
public class BibTeXMLImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(BibTeXMLImporter.class);

    private static final Pattern START_PATTERN = Pattern.compile("<(bibtex:)?file .*");

    private static final List<String> IGNORED_METHODS = Arrays.asList("getClass", "getAnnotate", "getContents",
            "getPrice", "getSize", "getChapter");


    @Override
    public String getFormatName() {
        return "BibTeXML";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList(".xml");
    }

    @Override
    public String getDescription() {
        return "Importer for the BibTeXML format.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // Our strategy is to look for the "<bibtex:file *" line.
        String str;
        while ((str = reader.readLine()) != null) {
            if (START_PATTERN.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        List<BibEntry> bibItems = new ArrayList<>();

        try {
            JAXBContext context = JAXBContext.newInstance("net.sf.jabref.importer.fileformat.bibtexml");
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(reader);

            //go to the root element
            while (!xmlReader.isStartElement()) {
                xmlReader.next();
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            File file = (File) unmarshaller.unmarshal(xmlReader);

            List<Entry> entries = file.getEntry();
            Map<String, String> fields = new HashMap<>();

            for (Entry entry : entries) {
                BibEntry bibEntry = new BibEntry(DEFAULT_BIBTEXENTRY_ID);
                if (entry.getArticle() != null) {
                    bibEntry.setType("article");
                    parse(entry.getArticle(), fields);
                }
                if (entry.getBook() != null) {
                    bibEntry.setType("book");
                    parse(entry.getBook(), fields);
                }
                if (entry.getBooklet() != null) {
                    bibEntry.setType("booklet");
                    parse(entry.getBooklet(), fields);
                }
                if (entry.getConference() != null) {
                    bibEntry.setType("conference");
                    parse(entry.getConference(), fields);
                }
                if (entry.getInbook() != null) {
                    bibEntry.setType("inbook");
                    parseInbook(entry.getInbook(), fields);
                }
                if (entry.getIncollection() != null) {
                    bibEntry.setType("incollection");
                    Incollection incollection = entry.getIncollection();
                    if (incollection.getChapter() != null) {
                        fields.put(FieldName.CHAPTER, String.valueOf(incollection.getChapter()));
                    }
                    parse(incollection, fields);
                }
                if (entry.getInproceedings() != null) {
                    bibEntry.setType("inproceedings");
                    parse(entry.getInproceedings(), fields);
                }
                if (entry.getManual() != null) {
                    bibEntry.setType("manual");
                    parse(entry.getManual(), fields);
                }
                if (entry.getMastersthesis() != null) {
                    bibEntry.setType("mastersthesis");
                    parse(entry.getMastersthesis(), fields);
                }
                if (entry.getMisc() != null) {
                    bibEntry.setType("misc");
                    parse(entry.getMisc(), fields);
                }
                if (entry.getPhdthesis() != null) {
                    bibEntry.setType("phdthesis");
                    parse(entry.getPhdthesis(), fields);
                }
                if (entry.getProceedings() != null) {
                    bibEntry.setType("proceedings");
                    parse(entry.getProceedings(), fields);
                }
                if (entry.getTechreport() != null) {
                    bibEntry.setType("techreport");
                    parse(entry.getTechreport(), fields);
                }
                if (entry.getUnpublished() != null) {
                    bibEntry.setType("unpublished");
                    parse(entry.getUnpublished(), fields);
                }
                if (entry.getId() != null) {
                    bibEntry.setCiteKey(entry.getId());
                }
                bibEntry.setField(fields);
                bibItems.add(bibEntry);
            }
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.error("Error with XML parser configuration", e);
            return ParserResult.fromErrorMessage(e.getLocalizedMessage());
        }
        return new ParserResult(bibItems);
    }

    /**
     * We use a generic method and not work on the real classes, because they all have the same behaviour. They call all get methods
     * that are needed and use the return value. So this will prevent writing similar methods for every type.
     * <p>
     * In this method, all <Code>get</Code> methods that entryType has will be used and their value will be put to fields,
     * if it is not null. So for example if entryType has the method <Code>getAbstract</Code>, then
     * "abstract" will be put as key to fields and the value of <Code>getAbstract</Code> will be put as value to fields.
     * Some <Code>get</Code> methods shouldn't be mapped to fields, so <Code>getClass</Code> for example will be skipped.
     *
     * @param entryType This can be all possible BibTeX types. It contains all fields of the entry and their values.
     * @param fields A map where the name and the value of all fields that the entry contains will be put.
     */
    private <T> void parse(T entryType, Map<String, String> fields) {
        Method[] declaredMethods = entryType.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            try {
                if (method.getName().equals("getYear")) {
                    putYear(fields, (XMLGregorianCalendar) method.invoke(entryType));
                    continue;
                } else if (method.getName().equals("getNumber")) {
                    putNumber(fields, (BigInteger) method.invoke(entryType));
                    continue;
                } else if (isMethodToIgnore(method.getName())) {
                    continue;
                } else if (method.getName().contains("get")) {
                    putIfValueNotNull(fields, method.getName().replace("get", ""), (String) method.invoke(entryType));
                }
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                LOGGER.error("Could not invoke method", e);
            }
        }
    }

    /**
     * Returns whether the value of the given method name should be mapped or whether the method can be ignored.
     *
     * @param methodName The name of the method as String
     * @return true if the method can be ignored, else false
     */
    private boolean isMethodToIgnore(String methodName) {
        return IGNORED_METHODS.contains(methodName);
    }

    /**
     * Inbook needs a special Treatment, because <Code>inbook.getContent()</Code> returns a list of <Code>JAXBElements</Code>.
     * The other types have just <Code>get</Code> methods, which return the values as Strings.
     */
    private void parseInbook(Inbook inbook, Map<String, String> fields) {
        List<JAXBElement<?>> content = inbook.getContent();
        for (JAXBElement<?> element : content) {
            String localName = element.getName().getLocalPart();
            Object elementValue = element.getValue();
            if (elementValue instanceof String) {
                String value = (String) elementValue;
                putIfValueNotNull(fields, localName, value);
            } else if (elementValue instanceof BigInteger) {
                BigInteger value = (BigInteger) elementValue;
                if (FieldName.NUMBER.equals(localName)) {
                    putNumber(fields, value);
                }
                if (FieldName.CHAPTER.equals(localName) && (value != null)) {
                    fields.put(FieldName.CHAPTER, String.valueOf(value));
                }
            } else if (elementValue instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar value = (XMLGregorianCalendar) elementValue;
                if (FieldName.YEAR.equals(localName)) {
                    putYear(fields, value);
                }
            }
        }
    }

    private void putYear(Map<String, String> fields, XMLGregorianCalendar year) {
        if (year != null) {
            fields.put(FieldName.YEAR, String.valueOf(year));
        }
    }

    private void putNumber(Map<String, String> fields, BigInteger number) {
        if (number != null) {
            fields.put(FieldName.NUMBER, String.valueOf(number));
        }
    }

    private void putIfValueNotNull(Map<String, String> fields, String key, String value) {
        if (value != null) {
            fields.put(key, value);
        }
    }
}
