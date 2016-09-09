package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.mods.AbstractDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.CodeOrText;
import net.sf.jabref.logic.importer.fileformat.mods.DateDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.DetailDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.ExtentDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.GenreDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.HierarchicalGeographicDefinition;
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
import net.sf.jabref.logic.importer.fileformat.mods.RecordInfoDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.RelatedItemDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.StringPlusLanguage;
import net.sf.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusAuthority;
import net.sf.jabref.logic.importer.fileformat.mods.StringPlusLanguagePlusSupplied;
import net.sf.jabref.logic.importer.fileformat.mods.SubjectDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.TitleInfoDefinition;
import net.sf.jabref.logic.importer.fileformat.mods.UrlDefinition;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.google.common.base.Joiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * Importer for the MODS format.<br>
 * More details about the format can be found here <a href="http://www.loc.gov/standards/mods/">http://www.loc.gov/standards/mods/</a>. <br>
 * The newest xml schema can also be found here <a href="www.loc.gov/standards/mods/mods-schemas.html.">www.loc.gov/standards/mods/mods-schemas.html.</a>.
 *
 */
public class ModsImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(ModsImporter.class);
    private static final String KEYWORD_SEPARATOR = "; ";

    private static final Pattern MODS_PATTERN = Pattern.compile("<mods .*>");


    @Override
    protected boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.lines().anyMatch(line -> MODS_PATTERN.matcher(line).find());
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);

        List<BibEntry> bibItems = new ArrayList<>();

        JAXBContext context;
        try {
            context = JAXBContext.newInstance("net.sf.jabref.logic.importer.fileformat.mods");
            Unmarshaller unmarshaller = context.createUnmarshaller();

            //The mods schema has no @XmlRootElement, so we create the unmarshalledObject as JAXBElement
            //Then we can get from the JAXBElement the ModsDefinition or the ModsColletionDefinition object
            JAXBElement<?> unmarshalledObject = (JAXBElement<?>) unmarshaller.unmarshal(input);

            if (unmarshalledObject.getValue() instanceof ModsCollectionDefinition) {
                ModsCollectionDefinition collection = (ModsCollectionDefinition) unmarshalledObject.getValue();
                List<ModsDefinition> mods = collection.getMods();
                parseModsCollection(bibItems, mods);
            } else if (unmarshalledObject.getValue() instanceof ModsDefinition) {
                ModsDefinition modsDefinition = (ModsDefinition) unmarshalledObject.getValue();
                parseMods(bibItems, modsDefinition);
            }
        } catch (JAXBException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromErrorMessage(e.getLocalizedMessage());
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
        Map<String, String> fields = new HashMap<>();
        if (modsDefinition.getID() != null) {
            entry.setCiteKey(modsDefinition.getID());
        }
        if (modsDefinition.getModsGroup() != null) {
            parseModsGroup(fields, modsDefinition.getModsGroup(), entry);
        }
        entry.setField(fields);
        bibItems.add(entry);
    }

    private void parseModsGroup(Map<String, String> fields, List<Object> modsGroup, BibEntry entry) {
        List<String> keywords = new ArrayList<>();
        List<String> authors = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (Object groupElement : modsGroup) {
            if (groupElement instanceof AbstractDefinition) {
                AbstractDefinition abstractDefinition = (AbstractDefinition) groupElement;
                putIfValueNotNull(fields, FieldName.ABSTRACT, abstractDefinition.getLangValue());
            } else if (groupElement instanceof GenreDefinition) {
                GenreDefinition genre = (GenreDefinition) groupElement;
                if (genre.getValue() != null) {
                    entry.setType(genre.getValue());
                }
            } else if (groupElement instanceof LanguageDefinition) {
                LanguageDefinition language = (LanguageDefinition) groupElement;
                putIfValueNotNull(fields, FieldName.LANGUAGE, language.getLangValue());
            } else if (groupElement instanceof LocationDefinition) {
                LocationDefinition location = (LocationDefinition) groupElement;
                parseLocationAndUrl(fields, location);
            } else if (groupElement instanceof NameDefinition) {
                NameDefinition name = (NameDefinition) groupElement;
                handleAuthorsInNamePart(name, authors, fields);
            } else if (groupElement instanceof OriginInfoDefinition) {
                OriginInfoDefinition originInfo = (OriginInfoDefinition) groupElement;
                parseOriginInfo(fields, originInfo);
            } else if (groupElement instanceof RecordInfoDefinition) {
                RecordInfoDefinition recordInfo = (RecordInfoDefinition) groupElement;
                parseRecordInfo(fields, recordInfo);
            } else if (groupElement instanceof NoteDefinition) {
                NoteDefinition note = (NoteDefinition) groupElement;
                notes.add(note.getValue());
            } else if (groupElement instanceof RelatedItemDefinition) {
                RelatedItemDefinition relatedItem = (RelatedItemDefinition) groupElement;
                List<Object> relatedModsGroup = relatedItem.getModsGroup();
                parseRelatedModsGroup(fields, relatedModsGroup);
            } else if (groupElement instanceof SubjectDefinition) {
                SubjectDefinition subject = (SubjectDefinition) groupElement;
                List<JAXBElement<?>> topicOrGeographicOrTemporal = subject.getTopicOrGeographicOrTemporal();
                for (JAXBElement<?> jaxbElement : topicOrGeographicOrTemporal) {
                    Object value = jaxbElement.getValue();
                    String elementName = jaxbElement.getName().getLocalPart();
                    if (value instanceof HierarchicalGeographicDefinition) {
                        HierarchicalGeographicDefinition hierarchichalGeographic = (HierarchicalGeographicDefinition) value;
                        parseGeographicInformation(fields, hierarchichalGeographic);
                    } else if ((value instanceof StringPlusLanguagePlusAuthority) && "topic".equals(elementName)) {
                        StringPlusLanguagePlusAuthority topic = (StringPlusLanguagePlusAuthority) value;
                        if (topic.getValue() != null) {
                            keywords.add(topic.getValue());
                        }
                    }
                }
            } else if (groupElement instanceof IdentifierDefinition) {
                IdentifierDefinition identifier = (IdentifierDefinition) groupElement;
                String type = identifier.getType();
                if ("citekey".equals(type) && !entry.getCiteKeyOptional().isPresent()) {
                    entry.setCiteKey(identifier.getValue());
                } else if (!"local".equals(type) && !"citekey".equals(type)) {
                    //put all identifiers (doi, issn, isbn,...) except of local and citekey
                    putIfValueNotNull(fields, identifier.getType(), identifier.getValue());
                }
            } else if (groupElement instanceof TitleInfoDefinition) {
                TitleInfoDefinition titleInfo = (TitleInfoDefinition) groupElement;
                putIfValueNotNull(fields, FieldName.TITLE, titleInfo.getTitle());
            }
        }

        //the element subject can appear more than one time, that's why the keywords has to be put here
        //same goes for authors and notes
        putIfListIsNotEmpty(fields, keywords, FieldName.KEYWORDS, KEYWORD_SEPARATOR);
        putIfListIsNotEmpty(fields, authors, FieldName.AUTHOR, " and ");
        putIfListIsNotEmpty(fields, notes, FieldName.NOTE, ", ");

    }

    private void parseGeographicInformation(Map<String, String> fields,
            HierarchicalGeographicDefinition hierarchichalGeographic) {
        List<JAXBElement<? extends StringPlusLanguage>> areaOrContinentOrCountry = hierarchichalGeographic
                .getExtraTerrestrialAreaOrContinentOrCountry();
        for (JAXBElement<? extends StringPlusLanguage> element : areaOrContinentOrCountry) {
            String localName = element.getName().getLocalPart();
            if ("city".equals(localName)) {
                StringPlusLanguage city = element.getValue();
                putIfValueNotNull(fields, "city", city.getValue());
            } else if ("country".equals(localName)) {
                StringPlusLanguage country = element.getValue();
                putIfValueNotNull(fields, "country", country.getValue());
            }
        }
    }

    private void parseLocationAndUrl(Map<String, String> fields, LocationDefinition location) {
        List<String> locations = new ArrayList<>();
        List<PhysicalLocationDefinition> physicalLocations = location.getPhysicalLocation();
        for (PhysicalLocationDefinition physicalLocation : physicalLocations) {
            locations.add(physicalLocation.getValue());
        }
        putIfListIsNotEmpty(fields, locations, FieldName.LOCATION, ", ");

        List<UrlDefinition> urlDefinitions = location.getUrl();
        List<String> urls = new ArrayList<>();
        for (UrlDefinition url : urlDefinitions) {
            urls.add(url.getValue());
        }
        putIfListIsNotEmpty(fields, urls, FieldName.URL, ", ");
    }

    private void parseOriginInfo(Map<String, String> fields, OriginInfoDefinition originInfo) {
        List<JAXBElement<?>> placeOrPublisherOrDateIssued = originInfo.getPlaceOrPublisherOrDateIssued();
        for (JAXBElement<?> jaxbElement : placeOrPublisherOrDateIssued) {
            Object value = jaxbElement.getValue();
            String elementName = jaxbElement.getName().getLocalPart();
            putPlaceOrPublisherOrDate(fields, elementName, value);
        }
    }

    private void parseRecordInfo(Map<String, String> fields, RecordInfoDefinition recordInfo) {
        List<JAXBElement<?>> recordContent = recordInfo.getRecordContentSourceOrRecordCreationDateOrRecordChangeDate();
        for (JAXBElement<?> jaxbElement : recordContent) {
            Object value = jaxbElement.getValue();
            if (value instanceof StringPlusLanguagePlusAuthority) {
                StringPlusLanguagePlusAuthority source = (StringPlusLanguagePlusAuthority) value;
                putIfValueNotNull(fields, "source", source.getValue());
            } else if (value instanceof LanguageDefinition) {
                LanguageDefinition language = (LanguageDefinition) value;
                List<String> languages = new ArrayList<>();
                List<LanguageTermDefinition> languageTerms = language.getLanguageTerm();
                for (LanguageTermDefinition languageTerm : languageTerms) {
                    languages.add(languageTerm.getValue());
                }
                putIfListIsNotEmpty(fields, languages, FieldName.LANGUAGE, ", ");
            }
        }
    }

    private void parseRelatedModsGroup(Map<String, String> fields, List<Object> relatedModsGroup) {
        for (Object groupElement : relatedModsGroup) {
            if (groupElement instanceof PartDefinition) {
                PartDefinition part = (PartDefinition) groupElement;
                List<Object> detailOrExtentOrDate = part.getDetailOrExtentOrDate();
                for (Object object : detailOrExtentOrDate) {
                    if (object instanceof DetailDefinition) {
                        DetailDefinition detail = (DetailDefinition) object;
                        List<JAXBElement<StringPlusLanguage>> numberOrCaptionOrTitle = detail
                                .getNumberOrCaptionOrTitle();

                        //In the for loop should only be the value of the element that belongs to the detail not be null
                        for (JAXBElement<StringPlusLanguage> jaxbElement : numberOrCaptionOrTitle) {
                            StringPlusLanguage value = jaxbElement.getValue();
                            putIfValueNotNull(fields, detail.getType(), value.getValue());
                        }
                    } else if (object instanceof ExtentDefinition) {
                        ExtentDefinition extentDefinition = (ExtentDefinition) object;
                        putPageInformation(extentDefinition, fields);
                    }
                }
            } else if (groupElement instanceof TitleInfoDefinition) {
                TitleInfoDefinition titleInfo = (TitleInfoDefinition) groupElement;
                putIfValueNotNull(fields, FieldName.JOURNAL, titleInfo.getTitle());
            }
        }
    }

    private void putPageInformation(ExtentDefinition extentDefinition, Map<String, String> fields) {
        if ("page".equals(extentDefinition.getUnit())) {
            if (extentDefinition.getTotal() != null) {
                putIfValueNotNull(fields, FieldName.PAGETOTAL, String.valueOf(extentDefinition.getTotal()));
            } else if (extentDefinition.getStart() != null) {
                putIfValueNotNull(fields, FieldName.PAGES, extentDefinition.getStart().getValue());
                if (extentDefinition.getEnd() != null) {
                    String endPage = extentDefinition.getEnd().getValue();
                    //if end appears, then there has to be a start page appeard, so get it and put it together with
                    //the end page
                    String startPage = fields.get(FieldName.PAGES);
                    fields.put(FieldName.PAGES, startPage + "-" + endPage);
                }
            }
        }
    }

    private void putPlaceOrPublisherOrDate(Map<String, String> fields, String elementName, Object value) {
        if (value instanceof IssuanceDefinition) {
            IssuanceDefinition issuance = (IssuanceDefinition) value;
            putIfValueNotNull(fields, "issuance", issuance.value());
        } else if (value instanceof PlaceDefinition) {
            PlaceDefinition place = (PlaceDefinition) value;
            List<String> places = new ArrayList<>();
            List<PlaceTermDefinition> placeTerms = place.getPlaceTerm();
            for (PlaceTermDefinition placeTerm : placeTerms) {
                if ((placeTerm.getValue() != null) && !placeTerm.getType().equals(CodeOrText.CODE)) {
                    places.add(placeTerm.getValue());
                }
            }
            putIfListIsNotEmpty(fields, places, "place", ", ");
        } else if (value instanceof DateDefinition) {
            DateDefinition date = (DateDefinition) value;
            switch (elementName) {
            case "dateIssued":
                if ("yes".equals(date.getKeyDate()) && (date.getValue() != null)) {
                    fields.put(FieldName.YEAR, date.getValue().substring(0, 4));
                }
                putIfValueNotNull(fields, "issued", date.getValue());
                break;
            case "dateCreated":
                //if there was no year in date issued, then take the year from date created
                if ("yes".equals(date.getKeyDate()) || (fields.get(FieldName.YEAR) == null)) {
                    putIfValueNotNull(fields, FieldName.YEAR, date.getValue().substring(0, 4));
                }
                putIfValueNotNull(fields, "created", date.getValue());
                break;
            case "dateCaptured":
                putIfValueNotNull(fields, "captured", date.getValue());
                break;
            case "dateModified":
                putIfValueNotNull(fields, "modified", date.getValue());
                break;
            default:
                break;
            }
        } else if ((value instanceof StringPlusLanguagePlusSupplied) && "publisher".equals(elementName)) {
            StringPlusLanguagePlusSupplied publisher = (StringPlusLanguagePlusSupplied) value;
            putIfValueNotNull(fields, FieldName.PUBLISHER, publisher.getValue());
        } else if ((value instanceof StringPlusLanguagePlusSupplied) && "edition".equals(elementName)) {
            StringPlusLanguagePlusSupplied edition = (StringPlusLanguagePlusSupplied) value;
            putIfValueNotNull(fields, FieldName.EDITION, edition.getValue());
        }
    }

    private void putIfListIsNotEmpty(Map<String, String> fields, List<String> list, String key, String separator) {
        if (!list.isEmpty()) {
            fields.put(key, Joiner.on(separator).join(list));
        }
    }

    private void handleAuthorsInNamePart(NameDefinition name, List<String> authors, Map<String, String> fields) {
        List<JAXBElement<?>> namePartOrDisplayFormOrAffiliation = name.getNamePartOrDisplayFormOrAffiliation();
        List<String> foreName = new ArrayList<>();
        String familyName = "";
        for (JAXBElement<?> element : namePartOrDisplayFormOrAffiliation) {
            Object value = element.getValue();
            String elementName = element.getName().getLocalPart();
            if (value instanceof NamePartDefinition) {
                NamePartDefinition namePart = (NamePartDefinition) value;
                String type = namePart.getType();
                if ((type == null) && (namePart.getValue() != null)) {
                    authors.add(namePart.getValue());
                }
                if ("given".equals(type) && (namePart.getValue() != null)) {
                    foreName.add(namePart.getValue());
                } else if ("family".equals(type) && (namePart.getValue() != null)) {
                    //There should only be one family name for one author
                    familyName = namePart.getValue();
                }
            } else if ((value instanceof StringPlusLanguage) && "affiliation".equals(elementName)) {
                StringPlusLanguage affiliation = (StringPlusLanguage) value;
                putIfValueNotNull(fields, "affiliation", affiliation.getValue());
            }
        }
        if (!foreName.isEmpty() && !"".equals(familyName)) {
            String author = familyName + ", " + Joiner.on(" ").join(foreName);
            authors.add(author);
        } else if (foreName.isEmpty() && !"".equals(familyName)) {
            authors.add(familyName);
        }
    }

    private void putIfValueNotNull(Map<String, String> fields, String modsKey, String value) {
        if (value != null) {
            fields.put(modsKey, value);
        }
    }

    @Override
    public String getFormatName() {
        return "MODS";
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.MODS;
    }

    @Override
    public String getDescription() {
        return "Importer for the MODS format";
    }

}
