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
import java.util.Optional;
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
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

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
    public StandardFileType getFileType() {
        return StandardFileType.BIBTEXML;
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
            Map<Field, String> fields = new HashMap<>();

            for (Entry entry : entries) {
                BibEntry bibEntry = new BibEntry();
                if (entry.getArticle() != null) {
                    bibEntry.setType(StandardEntryType.Article);
                    parse(entry.getArticle(), fields);
                } else if (entry.getBook() != null) {
                    bibEntry.setType(StandardEntryType.Book);
                    parse(entry.getBook(), fields);
                } else if (entry.getBooklet() != null) {
                    bibEntry.setType(StandardEntryType.Booklet);
                    parse(entry.getBooklet(), fields);
                } else if (entry.getConference() != null) {
                    bibEntry.setType(StandardEntryType.Conference);
                    parse(entry.getConference(), fields);
                } else if (entry.getInbook() != null) {
                    bibEntry.setType(StandardEntryType.InBook);
                    parseInbook(entry.getInbook(), fields);
                } else if (entry.getIncollection() != null) {
                    bibEntry.setType(StandardEntryType.InCollection);
                    Incollection incollection = entry.getIncollection();
                    if (incollection.getChapter() != null) {
                        fields.put(StandardField.CHAPTER, String.valueOf(incollection.getChapter()));
                    }
                    parse(incollection, fields);
                } else if (entry.getInproceedings() != null) {
                    bibEntry.setType(StandardEntryType.InProceedings);
                    parse(entry.getInproceedings(), fields);
                } else if (entry.getManual() != null) {
                    bibEntry.setType(StandardEntryType.Manual);
                    parse(entry.getManual(), fields);
                } else if (entry.getMastersthesis() != null) {
                    bibEntry.setType(StandardEntryType.MastersThesis);
                    parse(entry.getMastersthesis(), fields);
                } else if (entry.getMisc() != null) {
                    bibEntry.setType(StandardEntryType.Misc);
                    parse(entry.getMisc(), fields);
                } else if (entry.getPhdthesis() != null) {
                    bibEntry.setType(StandardEntryType.PhdThesis);
                    parse(entry.getPhdthesis(), fields);
                } else if (entry.getProceedings() != null) {
                    bibEntry.setType(StandardEntryType.Proceedings);
                    parse(entry.getProceedings(), fields);
                } else if (entry.getTechreport() != null) {
                    bibEntry.setType(StandardEntryType.TechReport);
                    parse(entry.getTechreport(), fields);
                } else if (entry.getUnpublished() != null) {
                    bibEntry.setType(StandardEntryType.Unpublished);
                    parse(entry.getUnpublished(), fields);
                }

                if (entry.getId() != null) {
                    bibEntry.setCitationKey(entry.getId());
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
     * @param fields    A map where the name and the value of all fields that the entry contains will be put.
     */
    private <T> void parse(T entryType, Map<Field, String> fields) {
        Method[] declaredMethods = entryType.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            try {
                if (method.getName().equals("getYear")) {
                    putYear(fields, (XMLGregorianCalendar) method.invoke(entryType));
                    continue;
                } else if (method.getName().equals("getNumber")) {
                    putNumber(fields, (BigInteger) method.invoke(entryType));
                    continue;
                } else if (method.getName().equals("getMonth")) {
                    putMonth(fields, Month.parse((String) method.invoke(entryType)));
                    continue;
                } else if (isMethodToIgnore(method.getName())) {
                    continue;
                } else if (method.getName().startsWith("get")) {
                    putIfValueNotNull(fields, FieldFactory.parseField(method.getName().replace("get", "")), (String) method.invoke(entryType));
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
    private void parseInbook(Inbook inbook, Map<Field, String> fields) {
        List<JAXBElement<?>> content = inbook.getContent();
        for (JAXBElement<?> element : content) {
            Field field = FieldFactory.parseField(element.getName().getLocalPart());
            Object elementValue = element.getValue();
            if (elementValue instanceof String) {
                String value = (String) elementValue;
                if (StandardField.MONTH.equals(field)) {
                    putMonth(fields, Month.parse(value));
                } else {
                    putIfValueNotNull(fields, field, value);
                }
            } else if (elementValue instanceof BigInteger) {
                BigInteger value = (BigInteger) elementValue;
                if (StandardField.NUMBER.equals(field)) {
                    fields.put(StandardField.NUMBER, String.valueOf(value));
                } else if (StandardField.CHAPTER.equals(field)) {
                    fields.put(StandardField.CHAPTER, String.valueOf(value));
                }
            } else if (elementValue instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar value = (XMLGregorianCalendar) elementValue;
                if (StandardField.YEAR.equals(field)) {
                    putYear(fields, value);
                } else {
                    LOGGER.info("Unexpected field was found");
                }
            } else {
                LOGGER.info("Unexpected field was found");
            }
        }
    }

    private void putYear(Map<Field, String> fields, XMLGregorianCalendar year) {
        if (year != null) {
            fields.put(StandardField.YEAR, String.valueOf(year));
        }
    }

    private void putNumber(Map<Field, String> fields, BigInteger number) {
        if (number != null) {
            fields.put(StandardField.NUMBER, String.valueOf(number));
        }
    }

    private void putMonth(Map<Field, String> fields, Optional<Month> month) {
        if (month.isPresent()) {
            fields.put(StandardField.MONTH, month.get().getJabRefFormat());
        }
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null) {
            fields.put(field, value);
        }
    }
}
