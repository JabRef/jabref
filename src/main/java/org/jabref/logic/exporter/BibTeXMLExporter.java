package org.jabref.logic.exporter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jabref.logic.importer.fileformat.bibtexml.Article;
import org.jabref.logic.importer.fileformat.bibtexml.Book;
import org.jabref.logic.importer.fileformat.bibtexml.Booklet;
import org.jabref.logic.importer.fileformat.bibtexml.Conference;
import org.jabref.logic.importer.fileformat.bibtexml.Entry;
import org.jabref.logic.importer.fileformat.bibtexml.File;
import org.jabref.logic.importer.fileformat.bibtexml.Inbook;
import org.jabref.logic.importer.fileformat.bibtexml.Incollection;
import org.jabref.logic.importer.fileformat.bibtexml.Inproceedings;
import org.jabref.logic.importer.fileformat.bibtexml.Manual;
import org.jabref.logic.importer.fileformat.bibtexml.Mastersthesis;
import org.jabref.logic.importer.fileformat.bibtexml.Misc;
import org.jabref.logic.importer.fileformat.bibtexml.Phdthesis;
import org.jabref.logic.importer.fileformat.bibtexml.Proceedings;
import org.jabref.logic.importer.fileformat.bibtexml.Techreport;
import org.jabref.logic.importer.fileformat.bibtexml.Unpublished;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export format for the BibTeXML format.
 */
public class BibTeXMLExporter extends Exporter {

    private static final String BIBTEXML_NAMESPACE_URI = "http://bibtexml.sf.net/";
    private static final Locale ENGLISH = Locale.ENGLISH;
    private static final Logger LOGGER = LoggerFactory.getLogger(BibTeXMLExporter.class);
    private JAXBContext context;

    public BibTeXMLExporter() {
        super("bibtexml", "BibTexXML", StandardFileType.XML);
    }

    @Override
    public void export(final BibDatabaseContext databaseContext, final Path resultFile, final Charset encoding,
                       List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }

        File file = new File();
        for (BibEntry bibEntry : entries) {
            Entry entry = new Entry();

            bibEntry.getCitationKey().ifPresent(entry::setId);

            EntryType i = bibEntry.getType();
            if (StandardEntryType.Article.equals(i)) {
                parse(new Article(), bibEntry, entry);
            } else if (StandardEntryType.Book.equals(i)) {
                parse(new Book(), bibEntry, entry);
            } else if (StandardEntryType.Booklet.equals(i)) {
                parse(new Booklet(), bibEntry, entry);
            } else if (StandardEntryType.Conference.equals(i)) {
                parse(new Conference(), bibEntry, entry);
            } else if (StandardEntryType.InBook.equals(i)) {
                parseInbook(new Inbook(), bibEntry, entry);
            } else if (StandardEntryType.InCollection.equals(i)) {
                parse(new Incollection(), bibEntry, entry);
            } else if (StandardEntryType.InProceedings.equals(i)) {
                parse(new Inproceedings(), bibEntry, entry);
            } else if (StandardEntryType.MastersThesis.equals(i)) {
                parse(new Mastersthesis(), bibEntry, entry);
            } else if (StandardEntryType.Manual.equals(i)) {
                parse(new Manual(), bibEntry, entry);
            } else if (StandardEntryType.Misc.equals(i)) {
                parse(new Misc(), bibEntry, entry);
            } else if (StandardEntryType.PhdThesis.equals(i)) {
                parse(new Phdthesis(), bibEntry, entry);
            } else if (StandardEntryType.Proceedings.equals(i)) {
                parse(new Proceedings(), bibEntry, entry);
            } else if (StandardEntryType.TechReport.equals(i)) {
                parse(new Techreport(), bibEntry, entry);
            } else if (StandardEntryType.Unpublished.equals(i)) {
                parse(new Unpublished(), bibEntry, entry);
            } else {
                LOGGER.warn("unexpected type appeared");
            }
            file.getEntry().add(entry);
        }
        createMarshallerAndWriteToFile(file, resultFile);
    }

    private void createMarshallerAndWriteToFile(File file, Path resultFile) throws SaveException {
        try {
            if (context == null) {
                context = JAXBContext.newInstance(File.class);
            }
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            marshaller.marshal(file, resultFile.toFile());
        } catch (JAXBException e) {
            throw new SaveException(e);
        }
    }

    /**
     * Contains same logic as the {@link #parse(Object, BibEntry, Entry)} method, but inbook needs a special treatment, because
     * the contents of inbook are stored in a List of JAXBElements. So we first need to create
     * a JAXBElement for every field and then add it to the content list.
     */
    private void parseInbook(Inbook inbook, BibEntry bibEntry, Entry entry) {
        Map<Field, String> fieldMap = bibEntry.getFieldMap();
        for (Map.Entry<Field, String> entryField : fieldMap.entrySet()) {
            String value = entryField.getValue();
            Field key = entryField.getKey();
            if (StandardField.YEAR.equals(key)) {
                XMLGregorianCalendar calendar;
                try {
                    calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(value);

                    JAXBElement<XMLGregorianCalendar> year = new JAXBElement<>(
                            new QName(BIBTEXML_NAMESPACE_URI, "year"), XMLGregorianCalendar.class, calendar);
                    inbook.getContent().add(year);
                } catch (DatatypeConfigurationException e) {
                    LOGGER.error("A configuration error occured");
                }
            } else if (StandardField.NUMBER.equals(key)) {
                JAXBElement<BigInteger> number = new JAXBElement<>(new QName(BIBTEXML_NAMESPACE_URI, "number"),
                        BigInteger.class, new BigInteger(value));
                inbook.getContent().add(number);
            } else if (StandardField.MONTH.equals(key)) {
                Optional<Month> month = bibEntry.getMonth();
                if (month.isPresent()) {
                    JAXBElement<String> element = new JAXBElement<>(new QName(BIBTEXML_NAMESPACE_URI, key.getName()),
                            String.class, month.get().getFullName());
                    inbook.getContent().add(element);
                }
            } else {
                JAXBElement<String> element = new JAXBElement<>(new QName(BIBTEXML_NAMESPACE_URI, key.getName()), String.class,
                        value);
                inbook.getContent().add(element);
            }
        }

        // set the entryType to the entry
        entry.setInbook(inbook);
    }

    /**
     * Generic method that gets an instance of an entry type (article, book, booklet ...). It also
     * gets one bibEntry. Then the method checks all fields of the entry and then for all fields the method
     * uses the set method of the entry type with the fieldname. So for example if a bib entry has the field
     * author and the value for it is "Max Mustermann" and the given type is an article, then this method
     * will invoke <Code>article.setAuthor("Max Mustermann")</Code>. <br>
     * <br>
     * The second part of this method is that the entry type will be set to the entry. So e.g., if the type is
     * article then <Code>entry.setArticle(article)</Code> will be invoked.
     *
     * @param entryType The type parameterized type of the entry.
     * @param bibEntry  The bib entry, which fields will be set to the entryType.
     * @param entry     The bibtexml entry. The entryType will be set to this entry.
     */
    private <T> void parse(T entryType, BibEntry bibEntry, Entry entry) {
        List<Method> declaredSetMethods = getListOfSetMethods(entryType);
        for (Map.Entry<Field, String> entryField : bibEntry.getFieldMap().entrySet()) {
            Field key = entryField.getKey();
            String value = entryField.getValue();
            for (Method method : declaredSetMethods) {
                String methodNameWithoutSet = method.getName().replace("set", "").toLowerCase(ENGLISH);
                if (!methodNameWithoutSet.equals(key.getName())) {
                    continue;
                }

                try {
                    if (StandardField.YEAR.equals(key)) {
                        try {
                            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(value);
                            method.invoke(entryType, calendar);
                        } catch (DatatypeConfigurationException e) {
                            LOGGER.error("A configuration error occured");
                        }
                        break;
                    } else if (StandardField.NUMBER.equals(key)) {
                        try {
                            method.invoke(entryType, new BigInteger(value));
                        } catch (NumberFormatException exception) {
                            LOGGER.warn("The value {} of the 'number' field is not an integer and thus is ignored for the export", value);
                        }
                        break;
                    } else if (StandardField.MONTH.equals(key)) {
                        Optional<Month> month = bibEntry.getMonth();
                        if (month.isPresent()) {
                            method.invoke(entryType, month.get().getFullName());
                        }
                        break;
                    } else {
                        method.invoke(entryType, value);
                        break;
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error("Could not invoke method " + method.getName(), e);
                }
            }

            // set the entryType to the entry
            List<Method> entryMethods = getListOfSetMethods(entry);
            for (Method method : entryMethods) {
                String methodWithoutSet = method.getName().replace("set", "");
                String simpleClassName = entryType.getClass().getSimpleName();

                if (methodWithoutSet.equals(simpleClassName)) {
                    try {
                        method.invoke(entry, entryType);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        LOGGER.warn("Could not set the type to the entry");
                    }
                }
            }
        }
    }

    private <T> List<Method> getListOfSetMethods(T entryType) {
        return Arrays.stream(entryType.getClass().getDeclaredMethods())
                     .filter(method -> method.getName().startsWith("set")).collect(Collectors.toList());
    }
}
