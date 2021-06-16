package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.mods.AbstractDefinition;
import org.jabref.logic.importer.fileformat.mods.DateDefinition;
import org.jabref.logic.importer.fileformat.mods.DetailDefinition;
import org.jabref.logic.importer.fileformat.mods.ExtentDefinition;
import org.jabref.logic.importer.fileformat.mods.GenreDefinition;
import org.jabref.logic.importer.fileformat.mods.HierarchicalGeographicDefinition;
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
import org.jabref.logic.importer.fileformat.mods.RecordInfoDefinition;
import org.jabref.logic.importer.fileformat.mods.RelatedItemDefinition;
import org.jabref.logic.importer.fileformat.mods.StringPlusLanguage;
import org.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusAuthority;
import org.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusSupplied;
import org.jabref.logic.importer.fileformat.mods.SubjectDefinition;
import org.jabref.logic.importer.fileformat.mods.TitleInfoDefinition;
import org.jabref.logic.importer.fileformat.mods.UrlDefinition;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryTypeFactory;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the MODS format.<br>
 * More details about the format can be found here <a href="http://www.loc.gov/standards/mods/">http://www.loc.gov/standards/mods/</a>. <br>
 * The newest xml schema can also be found here <a href="www.loc.gov/standards/mods/mods-schemas.html.">www.loc.gov/standards/mods/mods-schemas.html.</a>.
 */
public class ModsImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModsImporter.class);
    private static final Pattern MODS_PATTERN = Pattern.compile("<mods .*>");

    private final String keywordSeparator;

    private JAXBContext context;

    public ModsImporter(ImportFormatPreferences importFormatPreferences) {
        keywordSeparator = importFormatPreferences.getKeywordSeparator() + " ";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.lines().anyMatch(line -> MODS_PATTERN.matcher(line).find());
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);

        List<BibEntry> bibItems = new ArrayList<>();

        try {
            if (context == null) {
                context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.mods");
            }
            Unmarshaller unmarshaller = context.createUnmarshaller();

            // The unmarshalled object is a jaxbElement.
            JAXBElement<?> unmarshalledObject = (JAXBElement<?>) unmarshaller.unmarshal(input);

            Optional<ModsCollectionDefinition> collection = getElement(unmarshalledObject.getValue(),
                    ModsCollectionDefinition.class);
            Optional<ModsDefinition> mods = getElement(unmarshalledObject.getValue(), ModsDefinition.class);

            if (collection.isPresent()) {
                List<ModsDefinition> modsDefinitions = collection.get().getMods();
                parseModsCollection(bibItems, modsDefinitions);
            } else if (mods.isPresent()) {
                ModsDefinition modsDefinition = mods.get();
                parseMods(bibItems, modsDefinition);
            } else {
                LOGGER.warn("Not expected root element found");
            }
        } catch (JAXBException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        return new ParserResult(bibItems);
    }

    private void parseModsCollection(List<BibEntry> bibItems, List<ModsDefinition> mods) {
        for (ModsDefinition modsDefinition : mods) {
            parseMods(bibItems, modsDefinition);
        }
    }

    private void parseMods(List<BibEntry> bibItems, ModsDefinition modsDefinition) {
        BibEntry entry = new BibEntry();
        Map<Field, String> fields = new HashMap<>();
        if (modsDefinition.getID() != null) {
            entry.setCitationKey(modsDefinition.getID());
        }
        if (modsDefinition.getModsGroup() != null) {
            parseModsGroup(fields, modsDefinition.getModsGroup(), entry);
        }
        entry.setField(fields);
        bibItems.add(entry);
    }

    private void parseModsGroup(Map<Field, String> fields, List<Object> modsGroup, BibEntry entry) {
        List<String> keywords = new ArrayList<>();
        List<String> authors = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (Object groupElement : modsGroup) {

            // Get the element. Only one of the elements should be not an empty optional.
            Optional<AbstractDefinition> abstractDefinition = getElement(groupElement, AbstractDefinition.class);
            Optional<GenreDefinition> genreDefinition = getElement(groupElement, GenreDefinition.class);
            Optional<LanguageDefinition> languageDefinition = getElement(groupElement, LanguageDefinition.class);
            Optional<LocationDefinition> locationDefinition = getElement(groupElement, LocationDefinition.class);
            Optional<NameDefinition> nameDefinition = getElement(groupElement, NameDefinition.class);
            Optional<OriginInfoDefinition> originInfoDefinition = getElement(groupElement, OriginInfoDefinition.class);
            Optional<RecordInfoDefinition> recordInfoDefinition = getElement(groupElement, RecordInfoDefinition.class);
            Optional<NoteDefinition> noteDefinition = getElement(groupElement, NoteDefinition.class);
            Optional<RelatedItemDefinition> relatedItemDefinition = getElement(groupElement,
                    RelatedItemDefinition.class);
            Optional<SubjectDefinition> subjectDefinition = getElement(groupElement, SubjectDefinition.class);
            Optional<IdentifierDefinition> identifierDefinition = getElement(groupElement, IdentifierDefinition.class);
            Optional<TitleInfoDefinition> titleInfoDefinition = getElement(groupElement, TitleInfoDefinition.class);

            // Now parse the information if the element is present
            abstractDefinition
                    .ifPresent(abstractDef -> putIfValueNotNull(fields, StandardField.ABSTRACT, abstractDef.getValue()));

            genreDefinition.ifPresent(genre -> entry.setType(EntryTypeFactory.parse(genre.getValue())));

            languageDefinition.ifPresent(
                    languageDef -> languageDef.getLanguageTerm().stream().map(LanguageTermDefinition::getValue)
                                              .forEach(language -> putIfValueNotNull(fields, StandardField.LANGUAGE, language)));

            locationDefinition.ifPresent(location -> parseLocationAndUrl(fields, location));

            nameDefinition.ifPresent(name -> handleAuthorsInNamePart(name, authors, fields));

            originInfoDefinition.ifPresent(originInfo -> originInfo
                    .getPlaceOrPublisherOrDateIssued().stream()
                    .forEach(element -> putPlaceOrPublisherOrDate(fields, element.getName().getLocalPart(),
                            element.getValue())));

            recordInfoDefinition.ifPresent(recordInfo -> parseRecordInfo(fields, recordInfo));

            noteDefinition.ifPresent(note -> notes.add(note.getValue()));

            relatedItemDefinition.ifPresent(relatedItem -> parseRelatedModsGroup(fields, relatedItem.getModsGroup()));

            subjectDefinition
                    .ifPresent(subject -> parseTopic(fields, subject.getTopicOrGeographicOrTemporal(), keywords));

            identifierDefinition.ifPresent(identifier -> parseIdentifier(fields, identifier, entry));

            titleInfoDefinition.ifPresent(titleInfo -> parseTitle(fields, titleInfo.getTitleOrSubTitleOrPartNumber()));

        }

        // The element subject can appear more than one time, that's why the keywords has to be put out of the for loop
        putIfListIsNotEmpty(fields, keywords, StandardField.KEYWORDS, this.keywordSeparator);
        // same goes for authors and notes
        putIfListIsNotEmpty(fields, authors, StandardField.AUTHOR, " and ");
        putIfListIsNotEmpty(fields, notes, StandardField.NOTE, ", ");

    }

    private void parseTitle(Map<Field, String> fields, List<Object> titleOrSubTitleOrPartNumber) {
        for (Object object : titleOrSubTitleOrPartNumber) {
            if (object instanceof JAXBElement) {
                @SuppressWarnings("unchecked")
                JAXBElement<StringPlusLanguage> element = (JAXBElement<StringPlusLanguage>) object;
                if ("title".equals(element.getName().getLocalPart())) {
                    StringPlusLanguage title = element.getValue();
                    fields.put(StandardField.TITLE, title.getValue());
                }
            }
        }
    }

    private void parseIdentifier(Map<Field, String> fields, IdentifierDefinition identifier, BibEntry entry) {
        String type = identifier.getType();
        if ("citekey".equals(type) && !entry.getCitationKey().isPresent()) {
            entry.setCitationKey(identifier.getValue());
        } else if (!"local".equals(type) && !"citekey".equals(type)) {
            // put all identifiers (doi, issn, isbn,...) except of local and citekey
            putIfValueNotNull(fields, FieldFactory.parseField(identifier.getType()), identifier.getValue());
        }
    }

    private void parseTopic(Map<Field, String> fields, List<JAXBElement<?>> topicOrGeographicOrTemporal,
                            List<String> keywords) {
        for (JAXBElement<?> jaxbElement : topicOrGeographicOrTemporal) {
            Object value = jaxbElement.getValue();
            String elementName = jaxbElement.getName().getLocalPart();
            if (value instanceof HierarchicalGeographicDefinition) {
                HierarchicalGeographicDefinition hierarchichalGeographic = (HierarchicalGeographicDefinition) value;
                parseGeographicInformation(fields, hierarchichalGeographic);
            } else if ((value instanceof StringPlusLanguagePlusAuthority) && "topic".equals(elementName)) {
                StringPlusLanguagePlusAuthority topic = (StringPlusLanguagePlusAuthority) value;
                keywords.add(topic.getValue().trim());
            }
        }
    }

    /**
     * Returns an Optional which contains an instance of the given class, if the given element can be cast to this class.
     * If the element cannot be cast to the given class, then an empty optional will be returned.
     *
     * @param groupElement The element that should be cast
     * @param clazz        The class to which groupElement should be cast
     * @return An Optional, that contains the groupElement as instance of clazz, if groupElement can be cast to clazz.
     * An empty Optional, if groupElement cannot be cast to clazz
     */
    private <T> Optional<T> getElement(Object groupElement, Class<T> clazz) {
        if (clazz.isAssignableFrom(groupElement.getClass())) {
            return Optional.of(clazz.cast(groupElement));
        }
        return Optional.empty();
    }

    private void parseGeographicInformation(Map<Field, String> fields,
                                            HierarchicalGeographicDefinition hierarchichalGeographic) {
        List<JAXBElement<? extends StringPlusLanguage>> areaOrContinentOrCountry = hierarchichalGeographic
                .getExtraTerrestrialAreaOrContinentOrCountry();
        for (JAXBElement<? extends StringPlusLanguage> element : areaOrContinentOrCountry) {
            String localName = element.getName().getLocalPart();
            if ("city".equals(localName)) {
                StringPlusLanguage city = element.getValue();
                putIfValueNotNull(fields, new UnknownField("city"), city.getValue());
            } else if ("country".equals(localName)) {
                StringPlusLanguage country = element.getValue();
                putIfValueNotNull(fields, new UnknownField("country"), country.getValue());
            }
        }
    }

    private void parseLocationAndUrl(Map<Field, String> fields, LocationDefinition locationDefinition) {
        List<String> locations = locationDefinition.getPhysicalLocation().stream()
                                                   .map(PhysicalLocationDefinition::getValue).collect(Collectors.toList());
        putIfListIsNotEmpty(fields, locations, StandardField.LOCATION, ", ");

        List<String> urls = locationDefinition.getUrl().stream().map(UrlDefinition::getValue)
                                              .collect(Collectors.toList());
        putIfListIsNotEmpty(fields, urls, StandardField.URL, ", ");
    }

    private void parseRecordInfo(Map<Field, String> fields, RecordInfoDefinition recordInfo) {
        List<JAXBElement<?>> recordContent = recordInfo.getRecordContentSourceOrRecordCreationDateOrRecordChangeDate();
        for (JAXBElement<?> jaxbElement : recordContent) {
            Object value = jaxbElement.getValue();
            if (value instanceof StringPlusLanguagePlusAuthority) {
                StringPlusLanguagePlusAuthority source = (StringPlusLanguagePlusAuthority) value;
                putIfValueNotNull(fields, new UnknownField("source"), source.getValue());
            } else if (value instanceof LanguageDefinition) {
                LanguageDefinition language = (LanguageDefinition) value;
                List<LanguageTermDefinition> languageTerms = language.getLanguageTerm();
                List<String> languages = languageTerms.stream().map(LanguageTermDefinition::getValue)
                                                      .collect(Collectors.toList());
                putIfListIsNotEmpty(fields, languages, StandardField.LANGUAGE, ", ");
            }
        }
    }

    /**
     * Puts the Information from the RelatedModsGroup. It has the same elements like the ModsGroup.
     * But Informations like volume, issue and the pages appear here instead of in the ModsGroup.
     * Also if there appears a title field, then this indicates that is the name of journal which the article belongs to.
     */
    private void parseRelatedModsGroup(Map<Field, String> fields, List<Object> relatedModsGroup) {
        for (Object groupElement : relatedModsGroup) {
            if (groupElement instanceof PartDefinition) {
                PartDefinition part = (PartDefinition) groupElement;
                List<Object> detailOrExtentOrDate = part.getDetailOrExtentOrDate();
                for (Object object : detailOrExtentOrDate) {
                    if (object instanceof DetailDefinition) {
                        DetailDefinition detail = (DetailDefinition) object;
                        List<JAXBElement<StringPlusLanguage>> numberOrCaptionOrTitle = detail
                                .getNumberOrCaptionOrTitle();

                        // In the for loop should only be the value of the element that belongs to the detail not be null
                        for (JAXBElement<StringPlusLanguage> jaxbElement : numberOrCaptionOrTitle) {
                            StringPlusLanguage value = jaxbElement.getValue();
                            // put details like volume, issue,...
                            putIfValueNotNull(fields, FieldFactory.parseField(detail.getType()), value.getValue());
                        }
                    } else if (object instanceof ExtentDefinition) {
                        ExtentDefinition extentDefinition = (ExtentDefinition) object;
                        putPageInformation(extentDefinition, fields);
                    }
                }
            } else if (groupElement instanceof TitleInfoDefinition) {
                TitleInfoDefinition titleInfo = (TitleInfoDefinition) groupElement;
                List<Object> titleOrSubTitleOrPartNumber = titleInfo.getTitleOrSubTitleOrPartNumber();
                for (Object object : titleOrSubTitleOrPartNumber) {
                    if (object instanceof JAXBElement) {
                        @SuppressWarnings("unchecked")
                        JAXBElement<StringPlusLanguage> element = (JAXBElement<StringPlusLanguage>) object;
                        if ("title".equals(element.getName().getLocalPart())) {
                            StringPlusLanguage journal = element.getValue();
                            fields.put(StandardField.JOURNAL, journal.getValue());
                        }
                    }
                }
            }
        }
    }

    private void putPageInformation(ExtentDefinition extentDefinition, Map<Field, String> fields) {
        if (extentDefinition.getTotal() != null) {
            putIfValueNotNull(fields, StandardField.PAGES, String.valueOf(extentDefinition.getTotal()));
        } else if (extentDefinition.getStart() != null) {
            putIfValueNotNull(fields, StandardField.PAGES, extentDefinition.getStart().getValue());
            if (extentDefinition.getEnd() != null) {
                String endPage = extentDefinition.getEnd().getValue();
                // if end appears, then there has to be a start page appeared, so get it and put it together with
                // the end page
                String startPage = fields.get(StandardField.PAGES);
                fields.put(StandardField.PAGES, startPage + "-" + endPage);
            }
        }
    }

    private void putPlaceOrPublisherOrDate(Map<Field, String> fields, String elementName, Object object) {
        Optional<IssuanceDefinition> issuanceDefinition = getElement(object, IssuanceDefinition.class);
        Optional<PlaceDefinition> placeDefinition = getElement(object, PlaceDefinition.class);
        Optional<DateDefinition> dateDefinition = getElement(object, DateDefinition.class);
        Optional<StringPlusLanguagePlusSupplied> publisherOrEdition = getElement(object,
                StringPlusLanguagePlusSupplied.class);

        issuanceDefinition.ifPresent(issuance -> putIfValueNotNull(fields, new UnknownField("issuance"), issuance.value()));

        List<String> places = new ArrayList<>();
        placeDefinition
                .ifPresent(place -> place.getPlaceTerm().stream().filter(placeTerm -> placeTerm.getValue() != null)
                                         .map(PlaceTermDefinition::getValue).forEach(element -> places.add(element)));
        putIfListIsNotEmpty(fields, places, StandardField.ADDRESS, ", ");

        dateDefinition.ifPresent(date -> putDate(fields, elementName, date));

        publisherOrEdition.ifPresent(pubOrEd -> putPublisherOrEdition(fields, elementName, pubOrEd));
    }

    private void putPublisherOrEdition(Map<Field, String> fields, String elementName,
                                       StringPlusLanguagePlusSupplied pubOrEd) {
        if ("publisher".equals(elementName)) {
            putIfValueNotNull(fields, StandardField.PUBLISHER, pubOrEd.getValue());
        } else if ("edition".equals(elementName)) {
            putIfValueNotNull(fields, StandardField.EDITION, pubOrEd.getValue());
        }
    }

    private void putDate(Map<Field, String> fields, String elementName, DateDefinition date) {
        if (date.getValue() != null) {
            switch (elementName) {

                case "dateIssued":
                    // The first 4 digits of dateIssued should be the year
                    fields.put(StandardField.YEAR, date.getValue().replaceAll("[^0-9]*", "").replaceAll("\\(\\d?\\d?\\d?\\d?.*\\)", "\1"));
                    break;
                case "dateCreated":
                    // If there was no year in date issued, then take the year from date created
                    fields.computeIfAbsent(StandardField.YEAR, k -> date.getValue().substring(0, 4));
                    fields.put(new UnknownField("created"), date.getValue());
                    break;
                case "dateCaptured":
                    fields.put(new UnknownField("captured"), date.getValue());
                    break;
                case "dateModified":
                    fields.put(new UnknownField("modified"), date.getValue());
                    break;
                default:
                    break;
            }
        }
    }

    private void putIfListIsNotEmpty(Map<Field, String> fields, List<String> list, Field key, String separator) {
        if (!list.isEmpty()) {
            fields.put(key, list.stream().collect(Collectors.joining(separator)));
        }
    }

    private void handleAuthorsInNamePart(NameDefinition name, List<String> authors, Map<Field, String> fields) {
        List<JAXBElement<?>> namePartOrDisplayFormOrAffiliation = name.getNamePartOrDisplayFormOrAffiliation();
        List<String> foreName = new ArrayList<>();
        String familyName = "";
        String author = "";
        for (JAXBElement<?> element : namePartOrDisplayFormOrAffiliation) {
            Object value = element.getValue();
            String elementName = element.getName().getLocalPart();
            if (value instanceof NamePartDefinition) {
                NamePartDefinition namePart = (NamePartDefinition) value;
                String type = namePart.getAtType();
                if ((type == null) && (namePart.getValue() != null)) {
                    String namePartValue = namePart.getValue();
                    namePartValue = namePartValue.replaceAll(",$", "");
                    authors.add(namePartValue);
                } else if ("family".equals(type) && (namePart.getValue() != null)) {
                    // family should come first, so if family appears we can set the author then comes before
                    // we have to check if forename and family name are not empty in case it's the first author
                    if (!foreName.isEmpty() && !familyName.isEmpty()) {
                        // now set and add the old author
                        author = familyName + ", " + Joiner.on(" ").join(foreName);
                        authors.add(author);
                        // remove old forenames
                        foreName.clear();
                    } else if (foreName.isEmpty() && !familyName.isEmpty()) {
                        authors.add(familyName);
                    }
                    familyName = namePart.getValue();
                } else if ("given".equals(type) && (namePart.getValue() != null)) {
                    foreName.add(namePart.getValue());
                }
            } else if ((value instanceof StringPlusLanguage) && "affiliation".equals(elementName)) {
                StringPlusLanguage affiliation = (StringPlusLanguage) value;
                putIfValueNotNull(fields, new UnknownField("affiliation"), affiliation.getValue());
            }
        }

        // last author is not added, so do it here
        if (!foreName.isEmpty() && !familyName.isEmpty()) {
            author = familyName + ", " + Joiner.on(" ").join(foreName);
            authors.add(author.trim());
            foreName.clear();
        } else if (foreName.isEmpty() && !familyName.isEmpty()) {
            authors.add(familyName.trim());
        }
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null) {
            fields.put(field, value);
        }
    }

    @Override
    public String getName() {
        return "MODS";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.XML;
    }

    @Override
    public String getDescription() {
        return "Importer for the MODS format";
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            return importDatabase(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Collections.emptyList();
    }
}
