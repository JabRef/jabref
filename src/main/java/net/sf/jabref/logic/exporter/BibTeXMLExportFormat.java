package net.sf.jabref.logic.exporter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.Charset;
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

import net.sf.jabref.logic.importer.fileformat.bibtexml.Article;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Booklet;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Book;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Conference;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Entry;
import net.sf.jabref.logic.importer.fileformat.bibtexml.File;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Inbook;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Incollection;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Inproceedings;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Manual;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Mastersthesis;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Misc;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Phdthesis;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Proceedings;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Techreport;
import net.sf.jabref.logic.importer.fileformat.bibtexml.Unpublished;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BibTeXMLExportFormat extends ExportFormat {

    private static final String BIBTEXML_NAMESPACE_URI = "http://bibtexml.sf.net/";
    private static final Locale ENGLISH = Locale.ENGLISH;
    private static final Log LOGGER = LogFactory.getLog(BibTeXMLExportFormat.class);


    public BibTeXMLExportFormat() {
        super("BibTeXML", "bibtexml", null, null, ".xml");
    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, final String resultFile, final Charset encoding,
            List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }

        File file = new File();
        for (BibEntry bibEntry : entries) {
            Entry entry = new Entry();

            Optional<String> citeKey = bibEntry.getCiteKeyOptional();
            if (citeKey.isPresent()) {
                entry.setId(citeKey.get());
            }

            String type = bibEntry.getType().toLowerCase(ENGLISH);
            switch (type) {
            case "article":
                Article article = new Article();
                parse(article, bibEntry);
                entry.setArticle(article);
                break;
            case "book":
                Book book = new Book();
                parse(book, bibEntry);
                entry.setBook(book);
                break;
            case "booklet":
                Booklet booklet = new Booklet();
                parse(booklet, bibEntry);
                entry.setBooklet(booklet);
                break;
            case "conference":
                Conference conference = new Conference();
                parse(conference, bibEntry);
                entry.setConference(conference);
                break;
            case "inbook":
                Inbook inbook = new Inbook();
                parseInbook(inbook, bibEntry);
                entry.setInbook(inbook);
                break;
            case "incollection":
                Incollection incollection = new Incollection();
                parse(incollection, bibEntry);
                entry.setIncollection(incollection);
                break;
            case "inproceedings":
                Inproceedings inproceedings = new Inproceedings();
                parse(inproceedings, bibEntry);
                entry.setInproceedings(inproceedings);
                break;
            case "mastersthesis":
                Mastersthesis mastersthesis = new Mastersthesis();
                parse(mastersthesis, bibEntry);
                entry.setMastersthesis(mastersthesis);
                break;
            case "manual":
                Manual manual = new Manual();
                parse(manual, bibEntry);
                entry.setManual(manual);
                break;
            case "misc":
                Misc misc = new Misc();
                parse(misc, bibEntry);
                entry.setMisc(misc);
                break;
            case "phdthesis":
                Phdthesis phdthesis = new Phdthesis();
                parse(phdthesis, bibEntry);
                entry.setPhdthesis(phdthesis);
                break;
            case "proceedings":
                Proceedings proceedings = new Proceedings();
                parse(proceedings, bibEntry);
                entry.setProceedings(proceedings);
                break;
            case "techreport":
                Techreport techreport = new Techreport();
                parse(techreport, bibEntry);
                entry.setTechreport(techreport);
                break;
            case "unpublished":
                Unpublished unpublished = new Unpublished();
                parse(unpublished, bibEntry);
                entry.setUnpublished(unpublished);
                break;
            }

            file.getEntry().add(entry);
        }

        try {
            JAXBContext context = JAXBContext.newInstance(File.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            marshaller.marshal(file, new java.io.File(resultFile));
        } catch (JAXBException e) {
            throw new SaveException(e);
        }
    }

    private void parseInbook(Inbook inbook, BibEntry bibEntry) {
        Map<String, String> fieldMap = bibEntry.getFieldMap();
        for (java.util.Map.Entry<String, String> entryField : fieldMap.entrySet()) {
            String value = entryField.getValue();
            String key = entryField.getKey();
            if ("year".equals(key)) {
                XMLGregorianCalendar calendar;
                try {
                    calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(value);

                    JAXBElement<XMLGregorianCalendar> year = new JAXBElement<>(
                            new QName(BIBTEXML_NAMESPACE_URI, key),
                            XMLGregorianCalendar.class, calendar);
                    inbook.getContent().add(year);
                } catch (DatatypeConfigurationException e) {
                    LOGGER.error("A configuration error occured");
                }
            } else if ("number".equals(key)) {
                JAXBElement<BigInteger> number = new JAXBElement<>(new QName(BIBTEXML_NAMESPACE_URI, "number"),
                        BigInteger.class,
                        new BigInteger(value));
                inbook.getContent().add(number);
            } else {
                JAXBElement<String> element = new JAXBElement<>(new QName(BIBTEXML_NAMESPACE_URI, key), String.class,
                        value);
                inbook.getContent().add(element);
            }
        }
    }

    private <T> void parse(T entryType, BibEntry bibEntry) {
        List<Method> declaredSetMethods = getListOfSetMethods(entryType);
        Map<String, String> fieldMap = bibEntry.getFieldMap();
        for (java.util.Map.Entry<String, String> entryField : fieldMap.entrySet()) {
            String value = entryField.getValue();
            String key = entryField.getKey();
            for (Method method : declaredSetMethods) {
                String methodNameWithoutSet = method.getName().replace("set", "").toLowerCase(ENGLISH);
                try {

                    if ("year".equals(key) && key.equals(methodNameWithoutSet)) {
                        try {

                            XMLGregorianCalendar calendar = DatatypeFactory.newInstance()
                                    .newXMLGregorianCalendar(value);
                            method.invoke(entryType, calendar);
                        } catch (DatatypeConfigurationException e) {
                            LOGGER.error("A configuration error occured");
                        }
                        break;
                    } else if ("number".equals(key) && key.equals(methodNameWithoutSet)) {
                        method.invoke(entryType, new BigInteger(value));
                        break;
                    } else if (key.equals(methodNameWithoutSet)) {
                        method.invoke(entryType, value);
                        break;
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error("Could not invoke method", e);
                }
            }
        }

        //        //set the entryType to the entry
        //        List<Method> entryMethods = Arrays.asList(entry.getClass().getDeclaredMethods()).stream()
        //                .filter(method -> method.getName().startsWith("set")).collect(Collectors.toList());
        //        for (Method method : entryMethods) {
        //            String methodWithoutSet = method.getName().replace("set", "");
        //            String simpleClassName = entryType.getClass().getSimpleName().replaceAll("[", "").replaceAll("]", "");
        //            if(methodWithoutSet.equals(simpleClassName)) {
        //                try {
        //                    method.invoke(entry, entryType);
        //                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        //                    // TODO Auto-generated catch block
        //                    e.printStackTrace();
        //                }
        //            }
        //        }

    }

    private <T> List<Method> getListOfSetMethods(T entryType) {
        return Arrays.asList(entryType.getClass().getDeclaredMethods()).stream()
                .filter(method -> method.getName().startsWith("set")).collect(Collectors.toList());
    }

}
