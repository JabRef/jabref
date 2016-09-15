package net.sf.jabref.logic.exporter;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import net.sf.jabref.logic.importer.fileformat.mods.AbstractDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.CodeOrText;
import net.sf.jabref.logic.importer.fileformat.mods.DateDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.DetailDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.ExtentDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.GenreDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.IdentifierDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.IssuanceDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.LanguageDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.LanguageTermDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.LocationDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.ModsCollectionDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.ModsDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.NameDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.NamePartDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.NoteDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.OriginInfoDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.PartDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.PhysicalLocationDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.PlaceDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.PlaceTermDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.RelatedItemDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.StringPlusLanguage;
import net.sf.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusAuthority;
import net.sf.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusSupplied;
import net.sf.jabref.logic.importer.fileformat.mods.SubjectDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.TitleInfoDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.TypeOfResourceDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.UrlDefinition;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * ExportFormat for exporting in MODS XML format.
 */
class ModsExportFormat extends ExportFormat {

    private static final String KEYWORD_SEPARATOR = ";";


    public ModsExportFormat() {
        super("MODS", "mods", null, null, ".xml");
    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, final String file, final Charset encoding,
            List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }

        try {
            ModsCollectionDefinition modsCollection = new ModsCollectionDefinition();
            for (BibEntry bibEntry : entries) {
                ModsDefinition mods = new ModsDefinition();

                Optional<String> citeKey = bibEntry.getCiteKeyOptional();
                if (citeKey.isPresent()) {
                    mods.setID(citeKey.get());
                    addIdentifier("citekey", citeKey.get(), mods);
                }

                Map<String, String> fieldMap = bibEntry.getFieldMap();
                GenreDefinition genre = new GenreDefinition();
                genre.setValue(bibEntry.getType());
                mods.getModsGroup().add(genre);

                OriginInfoDefinition originInfo = new OriginInfoDefinition();
                PartDefinition partDefinition = new PartDefinition();
                RelatedItemDefinition relatedItem = new RelatedItemDefinition();

                for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (FieldName.AUTHOR.equals(key)) {
                        NameDefinition name = new NameDefinition();
                        name.setAtType("personal");
                        String[] authors = value.split(" and ");
                        for (String author : authors) {
                            NamePartDefinition namePart = new NamePartDefinition();
                            if (author.contains(",")) {
                                //if author contains ","  then this indicates that the author has a forename and family name
                                int commaIndex = author.indexOf(',');
                                String familyName = author.substring(0, commaIndex);
                                namePart.setAtType("family");
                                namePart.setValue(familyName);

                                JAXBElement<NamePartDefinition> element = new JAXBElement<>(new QName("namePart"),
                                        NamePartDefinition.class, namePart);
                                name.getNamePartOrDisplayFormOrAffiliation().add(element);

                                //now take care of the forenames
                                String forename = author.substring(commaIndex + 1, author.length());
                                String[] forenames = forename.split(" ");
                                for (String given : forenames) {
                                    NamePartDefinition namePartDefinition = new NamePartDefinition();
                                    namePartDefinition.setAtType("given");
                                    namePartDefinition.setValue(given);
                                    element = new JAXBElement<>(new QName("", "namePart"), NamePartDefinition.class,
                                            namePartDefinition);
                                    name.getNamePartOrDisplayFormOrAffiliation().add(element);
                                }
                            } else {
                                //no "," indicates that there should only be a family name
                                namePart.setAtType("family");
                                namePart.setValue(author);
                                JAXBElement<NamePartDefinition> element = new JAXBElement<>(new QName("namePart"),
                                        NamePartDefinition.class, namePart);
                                name.getNamePartOrDisplayFormOrAffiliation().add(element);
                            }
                        }
                        mods.getModsGroup().add(name);
                    } else if ("affiliation".equals(key)) {
                        NameDefinition nameDefinition = new NameDefinition();
                        StringPlusLanguage affiliation = new StringPlusLanguage();
                        affiliation.setValue(value);
                        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName("affiliation"),
                                StringPlusLanguage.class, affiliation);
                        nameDefinition.getAffiliationOrRoleOrDescription().add(element);
                        mods.getModsGroup().add(nameDefinition);
                    } else if (FieldName.ABSTRACT.equals(key)) {
                        AbstractDefinition abstractDefinition = new AbstractDefinition();
                        abstractDefinition.setValue(value);
                        mods.getModsGroup().add(abstractDefinition);
                    } else if (FieldName.TITLE.equals(key)) {
                        TitleInfoDefinition titleInfo = new TitleInfoDefinition();
                        StringPlusLanguage title = new StringPlusLanguage();
                        title.setValue(value);
                        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName("title"),
                                StringPlusLanguage.class, title);
                        titleInfo.getTitleOrSubTitleOrPartNumber().add(element);
                    } else if (FieldName.LANGUAGE.equals(key)) {
                        LanguageDefinition language = new LanguageDefinition();
                        LanguageTermDefinition languageTerm = new LanguageTermDefinition();
                        languageTerm.setValue(value);
                        language.getLanguageTerm().add(languageTerm);
                        mods.getModsGroup().add(language);
                    } else if (FieldName.LOCATION.equals(key)) {
                        LocationDefinition locationDefinition = new LocationDefinition();
                        //There can be more than one location
                        String[] locations = value.split(", ");
                        for (String location : locations) {
                            PhysicalLocationDefinition physicalLocation = new PhysicalLocationDefinition();
                            physicalLocation.setValue(location);
                            locationDefinition.getPhysicalLocation().add(physicalLocation);
                        }
                        mods.getModsGroup().add(locationDefinition);
                    } else if (FieldName.URL.equals(key)) {
                        String[] urls = value.split(", ");
                        for (String url : urls) {
                            UrlDefinition urlDefinition = new UrlDefinition();
                            urlDefinition.setValue(url);
                            mods.getModsGroup().add(urlDefinition);
                        }
                    } else if (FieldName.NOTE.equals(key)) {
                        String[] notes = value.split(", ");
                        for (String note : notes) {
                            NoteDefinition noteDefinition = new NoteDefinition();
                            noteDefinition.setValue(note);
                            mods.getModsGroup().add(noteDefinition);
                        }
                    } else if (FieldName.KEYWORDS.equals(key)) {
                        String[] keywords = null;
                        if (value.contains(KEYWORD_SEPARATOR)) {
                            keywords = value.split(KEYWORD_SEPARATOR);
                        } else if (value.contains(", ")) {
                            keywords = value.split(", ");
                        }

                        if (keywords != null) {
                            for (String keyword : keywords) {
                                SubjectDefinition subject = new SubjectDefinition();
                                StringPlusLanguagePlusAuthority topic = new StringPlusLanguagePlusAuthority();
                                topic.setValue(keyword);
                                JAXBElement<?> element = new JAXBElement<>(new QName("topic"),
                                        StringPlusLanguagePlusAuthority.class, topic);
                                subject.getTopicOrGeographicOrTemporal().add(element);
                                mods.getModsGroup().add(subject);
                            }
                        } else {
                            SubjectDefinition subject = new SubjectDefinition();
                            StringPlusLanguagePlusAuthority topic = new StringPlusLanguagePlusAuthority();
                            topic.setValue(value);
                            JAXBElement<?> element = new JAXBElement<>(new QName("topic"),
                                    StringPlusLanguagePlusAuthority.class, topic);
                            subject.getTopicOrGeographicOrTemporal().add(element);
                            mods.getModsGroup().add(subject);
                        }
                    } else if (FieldName.VOLUME.equals(key)) {
                        addDetail(FieldName.VOLUME, value, partDefinition);
                    } else if (FieldName.ISSUE.equals(key)) {
                        addDetail(FieldName.ISSUE, value, partDefinition);
                    } else if (FieldName.PAGES.equals(key)) {
                        if (value.contains("--")) {
                            addStartAndEndPage(value, partDefinition, "--");
                        } else if (value.contains("-")) {
                            addStartAndEndPage(value, partDefinition, "-");
                        } else {
                            BigInteger total = new BigInteger(value);
                            ExtentDefinition extent = new ExtentDefinition();
                            extent.setTotal(total);
                            partDefinition.getDetailOrExtentOrDate().add(extent);
                        }
                    } else if (FieldName.URI.equals(key)) {
                        addIdentifier(FieldName.URI, value, mods);
                    } else if (FieldName.ISBN.equals(key)) {
                        addIdentifier(FieldName.ISBN, value, mods);
                    } else if (FieldName.ISSN.equals(key)) {
                        addIdentifier(FieldName.ISSN, value, mods);
                    } else if (FieldName.DOI.equals(key)) {
                        addIdentifier(FieldName.DOI, value, mods);
                    } else if (FieldName.PMID.equals(key)) {
                        addIdentifier(FieldName.PMID, value, mods);
                    } else if (FieldName.JOURNAL.equals(key)) {
                        TitleInfoDefinition titleInfo = new TitleInfoDefinition();
                        StringPlusLanguage title = new StringPlusLanguage();
                        title.setValue(value);
                        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName("title"),
                                StringPlusLanguage.class, title);
                        titleInfo.getTitleOrSubTitleOrPartNumber().add(element);
                        relatedItem.getModsGroup().add(titleInfo);
                    }

                    addOriginInformation(key, value, originInfo);
                }

                relatedItem.getModsGroup().add(partDefinition);
                relatedItem.setAtType("host");
                mods.getModsGroup().add(relatedItem);
                mods.getModsGroup().add(originInfo);
                TypeOfResourceDefinition typeOfResource = new TypeOfResourceDefinition();
                typeOfResource.setValue("text");
                mods.getModsGroup().add(typeOfResource);
                modsCollection.getMods().add(mods);
            }

            JAXBContext context = JAXBContext.newInstance(ModsCollectionDefinition.class);
            Marshaller marshaller = context.createMarshaller();
            JAXBElement<ModsCollectionDefinition> jaxbElement = new JAXBElement<>(new QName("modsCollection"),
                    ModsCollectionDefinition.class, modsCollection);

            //since it has to be a prefix, use mods everywhere as prefix for elements
            //see also http://www.loc.gov/standards/mods/v3/mods-userguide-intro.html
            NamespacePrefixMapper myPrefixMapper = new NamespacePrefixMapper() {

                @Override
                public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                    if ("http://www.loc.gov/mods/v3".equals(namespaceUri)) {
                        return "mods";
                    } else if ("http://www.w3.org/1999/xlink".equals(namespaceUri)) {
                        return "link";
                    }
                    return "";
                }
            };

            //formate the output
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://www.loc.gov/standards/mods/v3/mods-3-6.xsd");
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", myPrefixMapper);

            // Write to File
            marshaller.marshal(jaxbElement, new File(file));
        } catch (JAXBException ex) {
            throw new SaveException(ex);
        }
    }

    private void addIdentifier(String key, String value, ModsDefinition mods) {
        IdentifierDefinition identifier = new IdentifierDefinition();
        identifier.setType(key);
        identifier.setValue(value);
        mods.getModsGroup().add(identifier);
    }

    private void addStartAndEndPage(String value, PartDefinition partDefinition, String minus) {
        int doubleMinusIndex = value.indexOf(minus);
        String startPage = value.substring(0, doubleMinusIndex);
        String endPage = value.substring(doubleMinusIndex + 1, value.length());

        StringPlusLanguage start = new StringPlusLanguage();
        start.setValue(startPage);
        StringPlusLanguage end = new StringPlusLanguage();
        start.setValue(endPage);
        ExtentDefinition extent = new ExtentDefinition();
        extent.setStart(start);
        extent.setEnd(end);

        partDefinition.getDetailOrExtentOrDate().add(extent);
    }

    private void addDetail(String detailName, String value, PartDefinition partDefinition) {
        DetailDefinition detail = new DetailDefinition();
        StringPlusLanguage detailType = new StringPlusLanguage();
        detailType.setValue(value);
        detail.setType(detailName);
        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName("number"), StringPlusLanguage.class,
                detailType);
        detail.getNumberOrCaptionOrTitle().add(element);
        partDefinition.getDetailOrExtentOrDate().add(detail);
    }

    private void addOriginInformation(String key, String value, OriginInfoDefinition originInfo) {
        if (FieldName.YEAR.equals(key)) {
            addDate("dateIssued", value, originInfo);
        } else if ("created".equals(key)) {
            addDate("dateCreated", value, originInfo);
        } else if ("modified".equals(key)) {
            addDate("dateModified", value, originInfo);
        } else if ("captured".equals(key)) {
            addDate("dateCaptured", value, originInfo);
        } else if (FieldName.PUBLISHER.equals(key)) {
            StringPlusLanguagePlusSupplied publisher = new StringPlusLanguagePlusSupplied();
            publisher.setValue(value);
            JAXBElement<StringPlusLanguagePlusSupplied> element = new JAXBElement<>(new QName("publisher"),
                    StringPlusLanguagePlusSupplied.class, publisher);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        } else if ("issuance".equals(key)) {
            IssuanceDefinition issuance = IssuanceDefinition.fromValue(value);
            JAXBElement<IssuanceDefinition> element = new JAXBElement<>(new QName("issuance"), IssuanceDefinition.class,
                    issuance);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        } else if ("address".equals(key)) {
            PlaceDefinition placeDefinition = new PlaceDefinition();
            //There can be more than one place, so we split to get all places and add them
            String[] places = value.split(", ");
            for (String place : places) {
                PlaceTermDefinition placeTerm = new PlaceTermDefinition();
                //There's no possibility to see from a bib entry whether it is code or text, but since it is in the bib entry
                //we assume that it is text
                placeTerm.setType(CodeOrText.TEXT);
                placeTerm.setValue(place);
                placeDefinition.getPlaceTerm().add(placeTerm);
            }
            JAXBElement<PlaceDefinition> element = new JAXBElement<>(new QName("place"), PlaceDefinition.class,
                    placeDefinition);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        } else if ("edition".equals(key)) {
            StringPlusLanguagePlusSupplied edition = new StringPlusLanguagePlusSupplied();
            edition.setValue(value);
            JAXBElement<StringPlusLanguagePlusSupplied> element = new JAXBElement<>(new QName("edition"),
                    StringPlusLanguagePlusSupplied.class, edition);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        }
    }

    private void addDate(String dateName, String value, OriginInfoDefinition originInfo) {
        DateDefinition dateIssued = new DateDefinition();
        dateIssued.setKeyDate("yes");
        dateIssued.setValue(value);
        JAXBElement<DateDefinition> element = new JAXBElement<>(new QName(dateName), DateDefinition.class, dateIssued);
        originInfo.getPlaceOrPublisherOrDateIssued().add(element);
    }
}
