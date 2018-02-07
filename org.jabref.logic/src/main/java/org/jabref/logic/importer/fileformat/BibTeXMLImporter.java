package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.bibtexml.Entry;
import org.jabref.logic.importer.fileformat.bibtexml.File;
import org.jabref.logic.importer.fileformat.bibtexml.Inbook;
import org.jabref.logic.importer.fileformat.bibtexml.Incollection;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the BibTeXML format.
 * <p>
 * check here for details on the format
 * http://bibtexml.sourceforge.net/
 */
public class BibTeXMLImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibTeXMLImporter.class);

    private static final Pattern START_PATTERN = Pattern.compile("<(bibtex:)?file .*");

    private static final List<String> IGNORED_METHODS = Arrays.asList("getClass", "getAnnotate", "getContents",
            "getPrice", "getSize", "getChapter");

    @Override
    public String getName() {
        return "BibTeXML";
    }

    @Override
    public FileType getFileType() {
        return FileType.BIBTEXML;
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
            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.bibtexml");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            File file = (File) unmarshaller.unmarshal(reader);

            List<Entry> entries = file.getEntry();
            Map<String, String> fields = new HashMap<>();

            for (Entry entry : entries) {
                BibEntry bibEntry = new BibEntry();
                if (entry.getArticle() != null) {
                    bibEntry.setType(BibtexEntryTypes.ARTICLE);
                    parse(entry.getArticle(), fields);
                } else if (entry.getBook() != null) {
                    bibEntry.setType(BibtexEntryTypes.BOOK);
                    parse(entry.getBook(), fields);
                } else if (entry.getBooklet() != null) {
                    bibEntry.setType(BibtexEntryTypes.BOOKLET);
                    parse(entry.getBooklet(), fields);
                } else if (entry.getConference() != null) {
                    bibEntry.setType(BibtexEntryTypes.CONFERENCE);
                    parse(entry.getConference(), fields);
                } else if (entry.getInbook() != null) {
                    bibEntry.setType(BibtexEntryTypes.INBOOK);
                    parseInbook(entry.getInbook(), fields);
                } else if (entry.getIncollection() != null) {
                    bibEntry.setType(BibtexEntryTypes.INCOLLECTION);
                    Incollection incollection = entry.getIncollection();
                    if (incollection.getChapter() != null) {
                        fields.put(FieldName.CHAPTER, String.valueOf(incollection.getChapter()));
                    }
                    parse(incollection, fields);
                } else if (entry.getInproceedings() != null) {
                    bibEntry.setType(BibtexEntryTypes.INPROCEEDINGS);
                    parse(entry.getInproceedings(), fields);
                } else if (entry.getManual() != null) {
                    bibEntry.setType(BibtexEntryTypes.MANUAL);
                    parse(entry.getManual(), fields);
                } else if (entry.getMastersthesis() != null) {
                    bibEntry.setType(BibtexEntryTypes.MASTERSTHESIS);
                    parse(entry.getMastersthesis(), fields);
                } else if (entry.getMisc() != null) {
                    bibEntry.setType(BibtexEntryTypes.MISC);
                    parse(entry.getMisc(), fields);
                } else if (entry.getPhdthesis() != null) {
                    bibEntry.setType(BibtexEntryTypes.PHDTHESIS);
                    parse(entry.getPhdthesis(), fields);
                } else if (entry.getProceedings() != null) {
                    bibEntry.setType(BibtexEntryTypes.PROCEEDINGS);
                    parse(entry.getProceedings(), fields);
                } else if (entry.getTechreport() != null) {
                    bibEntry.setType(BibtexEntryTypes.TECHREPORT);
                    parse(entry.getTechreport(), fields);
                } else if (entry.getUnpublished() != null) {
                    bibEntry.setType(BibtexEntryTypes.UNPUBLISHED);
                    parse(entry.getUnpublished(), fields);
                }

                if (entry.getId() != null) {
                    bibEntry.setCiteKey(entry.getId());
                }
                bibEntry.setField(fields);
                bibItems.add(bibEntry);
            }
        } catch (JAXBException e) {
            LOGGER.error("Error with XML parser configuration", e);
            return ParserResult.fromError(e);
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
                } else if (method.getName().startsWith("get")) {
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
                    fields.put(FieldName.NUMBER, String.valueOf(value));
                } else if (FieldName.CHAPTER.equals(localName)) {
                    fields.put(FieldName.CHAPTER, String.valueOf(value));
                }
            } else if (elementValue instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar value = (XMLGregorianCalendar) elementValue;
                if (FieldName.YEAR.equals(localName)) {
                    putYear(fields, value);
                } else {
                    LOGGER.info("Unexpected field was found");
                }
            } else {
                LOGGER.info("Unexpected field was found");
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
