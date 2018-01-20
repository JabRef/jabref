package org.jabref.logic.exporter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.jabref.logic.importer.fileformat.mods.AbstractDefinition;
import org.jabref.logic.importer.fileformat.mods.CodeOrText;
import org.jabref.logic.importer.fileformat.mods.DateDefinition;
import org.jabref.logic.importer.fileformat.mods.DetailDefinition;
import org.jabref.logic.importer.fileformat.mods.ExtentDefinition;
import org.jabref.logic.importer.fileformat.mods.GenreDefinition;
import org.jabref.logic.importer.fileformat.mods.IdentifierDefinition;
import org.jabref.logic.importer.fileformat.mods.IssuanceDefinition;
import org.jabref.logic.importer.fileformat.mods.LanguageDefinition;
import org.jabref.logic.importer.fileformat.mods.LanguageTermDefinition;
import org.jabref.logic.importer.fileformat.mods.LocationDefinition;
import org.jabref.logic.importer.fileformat.mods.ModsCollectionDefinition;
import org.jabref.logic.importer.fileformat.mods.ModsDefinition;
import org.jabref.logic.importer.fileformat.mods.NameDefinition;
import org.jabref.logic.importer.fileformat.mods.NamePartDefinition;
import org.jabref.logic.importer.fileformat.mods.NoteDefinition;
import org.jabref.logic.importer.fileformat.mods.OriginInfoDefinition;
import org.jabref.logic.importer.fileformat.mods.PartDefinition;
import org.jabref.logic.importer.fileformat.mods.PhysicalLocationDefinition;
import org.jabref.logic.importer.fileformat.mods.PlaceDefinition;
import org.jabref.logic.importer.fileformat.mods.PlaceTermDefinition;
import org.jabref.logic.importer.fileformat.mods.RelatedItemDefinition;
import org.jabref.logic.importer.fileformat.mods.StringPlusLanguage;
import org.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusAuthority;
import org.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusSupplied;
import org.jabref.logic.importer.fileformat.mods.SubjectDefinition;
import org.jabref.logic.importer.fileformat.mods.TitleInfoDefinition;
import org.jabref.logic.importer.fileformat.mods.TypeOfResourceDefinition;
import org.jabref.logic.importer.fileformat.mods.UrlDefinition;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;


/**
 * TemplateExporter for exporting in MODS XML format.
 */
class ModsExporter extends Exporter {

    protected static final String MODS_NAMESPACE_URI = "http://www.loc.gov/mods/v3";
    private static final String MINUS = "-";
    private static final String DOUBLE_MINUS = "--";
    private static final String MODS_SCHEMA_LOCATION = "http://www.loc.gov/standards/mods/v3/mods-3-6.xsd";
    private JAXBContext context;

    public ModsExporter() {
        super("mods", FileType.MODS.getDescription(), FileType.MODS);
    }

    @Override
    public void export(final BibDatabaseContext databaseContext, final Path file, final Charset encoding,
                       List<BibEntry> entries) throws SaveException, IOException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }

        try {
            ModsCollectionDefinition modsCollection = new ModsCollectionDefinition();
            for (BibEntry bibEntry : entries) {
                ModsDefinition mods = new ModsDefinition();
                bibEntry.getCiteKeyOptional().ifPresent(citeKey -> addIdentifier("citekey", citeKey, mods));

                Map<String, String> fieldMap = bibEntry.getFieldMap();
                addGenre(bibEntry, mods);

                OriginInfoDefinition originInfo = new OriginInfoDefinition();
                PartDefinition partDefinition = new PartDefinition();
                RelatedItemDefinition relatedItem = new RelatedItemDefinition();

                for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    switch (key) {

                        case FieldName.AUTHOR:
                            handleAuthors(mods, value);
                            break;
                        case "affiliation":
                            addAffiliation(mods, value);
                            break;
                        case FieldName.ABSTRACT:
                            addAbstract(mods, value);
                            break;
                        case FieldName.TITLE:
                            addTitle(mods, value);
                            break;
                        case FieldName.LANGUAGE:
                            addLanguage(mods, value);
                            break;
                        case FieldName.LOCATION:
                            addLocation(mods, value);
                            break;
                        case FieldName.URL:
                            addUrl(mods, value);
                            break;
                        case FieldName.NOTE:
                            addNote(mods, value);
                            break;
                        case FieldName.KEYWORDS:
                            addKeyWords(mods, value);
                            break;
                        case FieldName.VOLUME:
                            addDetail(FieldName.VOLUME, value, partDefinition);
                            break;
                        case FieldName.ISSUE:
                            addDetail(FieldName.ISSUE, value, partDefinition);
                            break;
                        case FieldName.PAGES:
                            addPages(partDefinition, value);
                            break;
                        case FieldName.URI:
                            addIdentifier(FieldName.URI, value, mods);
                            break;
                        case FieldName.ISBN:
                            addIdentifier(FieldName.ISBN, value, mods);
                            break;
                        case FieldName.ISSN:
                            addIdentifier(FieldName.ISSN, value, mods);
                            break;
                        case FieldName.DOI:
                            addIdentifier(FieldName.DOI, value, mods);
                            break;
                        case FieldName.PMID:
                            addIdentifier(FieldName.PMID, value, mods);
                            break;
                        case FieldName.JOURNAL:
                            addJournal(value, relatedItem);
                            break;
                        default:
                            break;
                    }

                    addOriginInformation(key, value, originInfo);
                }
                mods.getModsGroup().add(originInfo);

                addRelatedAndOriginInfoToModsGroup(relatedItem, partDefinition, mods);
                modsCollection.getMods().add(mods);
            }

            JAXBElement<ModsCollectionDefinition> jaxbElement = new JAXBElement<>(
                    new QName(MODS_NAMESPACE_URI, "modsCollection"), ModsCollectionDefinition.class, modsCollection);

            createMarshallerAndWriteToFile(file, jaxbElement);
        } catch (JAXBException ex) {
            throw new SaveException(ex);
        }
    }

    private void createMarshallerAndWriteToFile(Path file, JAXBElement<ModsCollectionDefinition> jaxbElement)
            throws JAXBException {

        if (context == null) {
            context = JAXBContext.newInstance(ModsCollectionDefinition.class);
        }
        Marshaller marshaller = context.createMarshaller();
        //format the output
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, MODS_SCHEMA_LOCATION);

        // Write to File
        marshaller.marshal(jaxbElement, file.toFile());
    }

    private void addRelatedAndOriginInfoToModsGroup(RelatedItemDefinition relatedItem, PartDefinition partDefinition,
            ModsDefinition mods) {

        relatedItem.getModsGroup().add(partDefinition);
        relatedItem.setAtType("host");
        mods.getModsGroup().add(relatedItem);
        TypeOfResourceDefinition typeOfResource = new TypeOfResourceDefinition();
        typeOfResource.setValue("text");
        mods.getModsGroup().add(typeOfResource);
    }

    private void addGenre(BibEntry bibEntry, ModsDefinition mods) {
        GenreDefinition genre = new GenreDefinition();
        genre.setValue(bibEntry.getType());
        mods.getModsGroup().add(genre);
    }

    private void addAbstract(ModsDefinition mods, String value) {
        AbstractDefinition abstractDefinition = new AbstractDefinition();
        abstractDefinition.setValue(value);
        mods.getModsGroup().add(abstractDefinition);
    }

    private void addTitle(ModsDefinition mods, String value) {
        TitleInfoDefinition titleInfo = new TitleInfoDefinition();
        StringPlusLanguage title = new StringPlusLanguage();
        title.setValue(value);
        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "title"),
                StringPlusLanguage.class, title);
        titleInfo.getTitleOrSubTitleOrPartNumber().add(element);
        mods.getModsGroup().add(titleInfo);
    }

    private void addAffiliation(ModsDefinition mods, String value) {
        NameDefinition nameDefinition = new NameDefinition();
        StringPlusLanguage affiliation = new StringPlusLanguage();
        affiliation.setValue(value);
        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "affiliation"),
                StringPlusLanguage.class, affiliation);
        nameDefinition.getAffiliationOrRoleOrDescription().add(element);
        mods.getModsGroup().add(nameDefinition);
    }

    private void addLocation(ModsDefinition mods, String value) {
        LocationDefinition locationDefinition = new LocationDefinition();
        //There can be more than one location
        String[] locations = value.split(", ");
        for (String location : locations) {
            PhysicalLocationDefinition physicalLocation = new PhysicalLocationDefinition();
            physicalLocation.setValue(location);
            locationDefinition.getPhysicalLocation().add(physicalLocation);
        }
        mods.getModsGroup().add(locationDefinition);
    }

    private void addNote(ModsDefinition mods, String value) {
        String[] notes = value.split(", ");
        for (String note : notes) {
            NoteDefinition noteDefinition = new NoteDefinition();
            noteDefinition.setValue(note);
            mods.getModsGroup().add(noteDefinition);
        }
    }

    private void addUrl(ModsDefinition mods, String value) {
        String[] urls = value.split(", ");
        LocationDefinition location = new LocationDefinition();
        for (String url : urls) {
            UrlDefinition urlDefinition = new UrlDefinition();
            urlDefinition.setValue(url);
            location.getUrl().add(urlDefinition);
            mods.getModsGroup().add(location);
        }
    }

    private void addJournal(String value, RelatedItemDefinition relatedItem) {
        TitleInfoDefinition titleInfo = new TitleInfoDefinition();
        StringPlusLanguage title = new StringPlusLanguage();
        title.setValue(value);
        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "title"),
                StringPlusLanguage.class, title);
        titleInfo.getTitleOrSubTitleOrPartNumber().add(element);
        relatedItem.getModsGroup().add(titleInfo);
    }

    private void addLanguage(ModsDefinition mods, String value) {
        LanguageDefinition language = new LanguageDefinition();
        LanguageTermDefinition languageTerm = new LanguageTermDefinition();
        languageTerm.setValue(value);
        language.getLanguageTerm().add(languageTerm);
        mods.getModsGroup().add(language);
    }

    private void addPages(PartDefinition partDefinition, String value) {
        if (value.contains(DOUBLE_MINUS)) {
            addStartAndEndPage(value, partDefinition, DOUBLE_MINUS);
        } else if (value.contains(MINUS)) {
            addStartAndEndPage(value, partDefinition, MINUS);
        } else {
            BigInteger total = new BigInteger(value);
            ExtentDefinition extent = new ExtentDefinition();
            extent.setTotal(total);
            partDefinition.getDetailOrExtentOrDate().add(extent);
        }
    }

    private void addKeyWords(ModsDefinition mods, String value) {
        String[] keywords = value.split(", ");

        for (String keyword : keywords) {
            SubjectDefinition subject = new SubjectDefinition();
            StringPlusLanguagePlusAuthority topic = new StringPlusLanguagePlusAuthority();
            topic.setValue(keyword);
            JAXBElement<?> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "topic"),
                    StringPlusLanguagePlusAuthority.class, topic);
            subject.getTopicOrGeographicOrTemporal().add(element);
            mods.getModsGroup().add(subject);
        }
    }

    private void handleAuthors(ModsDefinition mods, String value) {
        String[] authors = value.split("and");
        for (String author : authors) {
            NameDefinition name = new NameDefinition();
            name.setAtType("personal");
            NamePartDefinition namePart = new NamePartDefinition();
            if (author.contains(",")) {
                //if author contains ","  then this indicates that the author has a forename and family name
                int commaIndex = author.indexOf(',');
                String familyName = author.substring(0, commaIndex);
                namePart.setAtType("family");
                namePart.setValue(familyName);

                JAXBElement<NamePartDefinition> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "namePart"),
                        NamePartDefinition.class, namePart);
                name.getNamePartOrDisplayFormOrAffiliation().add(element);

                //now take care of the forenames
                String forename = author.substring(commaIndex + 1, author.length());
                String[] forenames = forename.split(" ");
                for (String given : forenames) {
                    if (!given.isEmpty()) {
                        NamePartDefinition namePartDefinition = new NamePartDefinition();
                        namePartDefinition.setAtType("given");
                        namePartDefinition.setValue(given);
                        element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "namePart"), NamePartDefinition.class,
                                namePartDefinition);
                        name.getNamePartOrDisplayFormOrAffiliation().add(element);
                    }
                }
                mods.getModsGroup().add(name);
            } else {
                //no "," indicates that there should only be a family name
                namePart.setAtType("family");
                namePart.setValue(author);
                JAXBElement<NamePartDefinition> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "namePart"),
                        NamePartDefinition.class, namePart);
                name.getNamePartOrDisplayFormOrAffiliation().add(element);
                mods.getModsGroup().add(name);
            }
        }
    }

    private void addIdentifier(String key, String value, ModsDefinition mods) {
        if ("citekey".equals(key)) {
            mods.setID(value);
        }
        IdentifierDefinition identifier = new IdentifierDefinition();
        identifier.setType(key);
        identifier.setValue(value);
        mods.getModsGroup().add(identifier);
    }

    private void addStartAndEndPage(String value, PartDefinition partDefinition, String minus) {
        int minusIndex = value.indexOf(minus);
        String startPage = value.substring(0, minusIndex);
        String endPage = "";
        if (MINUS.equals(minus)) {
            endPage = value.substring(minusIndex + 1);
        } else if (DOUBLE_MINUS.equals(minus)) {
            endPage = value.substring(minusIndex + 2);
        }

        StringPlusLanguage start = new StringPlusLanguage();
        start.setValue(startPage);
        StringPlusLanguage end = new StringPlusLanguage();
        end.setValue(endPage);
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
        JAXBElement<StringPlusLanguage> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "number"),
                StringPlusLanguage.class, detailType);
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
            JAXBElement<StringPlusLanguagePlusSupplied> element = new JAXBElement<>(
                    new QName(MODS_NAMESPACE_URI, "publisher"), StringPlusLanguagePlusSupplied.class, publisher);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        } else if ("issuance".equals(key)) {
            IssuanceDefinition issuance = IssuanceDefinition.fromValue(value);
            JAXBElement<IssuanceDefinition> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "issuance"),
                    IssuanceDefinition.class, issuance);
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
            JAXBElement<PlaceDefinition> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, "place"),
                    PlaceDefinition.class, placeDefinition);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        } else if ("edition".equals(key)) {
            StringPlusLanguagePlusSupplied edition = new StringPlusLanguagePlusSupplied();
            edition.setValue(value);
            JAXBElement<StringPlusLanguagePlusSupplied> element = new JAXBElement<>(
                    new QName(MODS_NAMESPACE_URI, "edition"), StringPlusLanguagePlusSupplied.class, edition);
            originInfo.getPlaceOrPublisherOrDateIssued().add(element);
        }
    }

    private void addDate(String dateName, String value, OriginInfoDefinition originInfo) {
        DateDefinition dateIssued = new DateDefinition();
        dateIssued.setKeyDate("yes");
        dateIssued.setValue(value);
        JAXBElement<DateDefinition> element = new JAXBElement<>(new QName(MODS_NAMESPACE_URI, dateName),
                DateDefinition.class, dateIssued);
        originInfo.getPlaceOrPublisherOrDateIssued().add(element);
    }
}
