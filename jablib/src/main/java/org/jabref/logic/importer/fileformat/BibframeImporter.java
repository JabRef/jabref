package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Imports a bounded BIBFRAME 2.0 Work and Instance mapping from striped RDF/XML.
// [impl->req~import.bibliographic.xml-formats~1]
@NullMarked
public class BibframeImporter extends Importer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibframeImporter.class);
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String BF = "http://id.loc.gov/ontologies/bibframe/";
    private static final String BFLC = "http://id.loc.gov/ontologies/bflc/";

    @Override
    public String getId() {
        return "bibframe";
    }

    @Override
    public String getName() {
        return "BIBFRAME 2.0 RDF/XML";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the BIBFRAME 2.0 RDF/XML format.");
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.RDF;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return isRecognizedFormat((Reader) input);
    }

    @Override
    public boolean isRecognizedFormat(Reader input) throws IOException {
        try {
            return isBibframe(parse(input));
        } catch (XMLStreamException e) {
            LOGGER.debug("Could not recognize BIBFRAME RDF/XML", e);
            return false;
        }
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        try {
            XmlNode root = parse(input);
            if (!isBibframe(root)) {
                return new ParserResult();
            }
            return new ParserResult(readEntries(root));
        } catch (XMLStreamException e) {
            LOGGER.debug("Could not parse BIBFRAME RDF/XML", e);
            return ParserResult.fromError(e);
        }
    }

    private static XmlNode parse(Reader input) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setXMLResolver((_, _, _, _) -> {
            throw new XMLStreamException("External entities are not allowed in BIBFRAME RDF/XML");
        });

        XMLStreamReader reader = factory.createXMLStreamReader(input);
        try {
            Deque<XmlNode> stack = new ArrayDeque<>();
            Optional<XmlNode> root = Optional.empty();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.DTD) {
                    throw new XMLStreamException("Document types are not allowed in BIBFRAME RDF/XML");
                }
                if (event == XMLStreamConstants.START_ELEMENT) {
                    XmlNode element = new XmlNode(reader);
                    if (stack.isEmpty()) {
                        root = Optional.of(element);
                    } else {
                        stack.getFirst().children.add(element);
                    }
                    stack.push(element);
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (!stack.isEmpty()) {
                        stack.getFirst().text.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    stack.pop();
                }
            }
            return root.orElseThrow(() -> new XMLStreamException("Missing RDF/XML root"));
        } finally {
            reader.close();
        }
    }

    private static boolean isBibframe(XmlNode root) {
        return is(root, RDF, "RDF") && children(root).stream().anyMatch(element -> isType(element, "Instance"));
    }

    private static List<BibEntry> readEntries(XmlNode root) {
        Map<String, XmlNode> resources = new HashMap<>();
        indexResources(root, resources);

        List<BibEntry> entries = new ArrayList<>();
        List<XmlNode> descriptions = children(root);
        for (XmlNode instance : descriptions) {
            if (!isType(instance, "Instance")) {
                continue;
            }
            Optional<XmlNode> work = object(first(instance, BF, "instanceOf"), resources);
            if (work.isEmpty()) {
                work = descriptions.stream().filter(element -> isType(element, "Work"))
                                   .filter(element -> linksTo(element, instance)).findFirst();
            }
            entries.add(readEntry(instance, work, resources));
        }
        return entries;
    }

    private static void indexResources(XmlNode element, Map<String, XmlNode> resources) {
        String uri = element.getAttributeNS(RDF, "about");
        if (!uri.isEmpty()) {
            resources.put(uri, element);
        }
        element.children.forEach(child -> indexResources(child, resources));
    }

    private static boolean linksTo(XmlNode work, XmlNode instance) {
        String uri = instance.getAttributeNS(RDF, "about");
        return !uri.isEmpty() && children(work, BF, "hasInstance").stream()
                                 .anyMatch(link -> uri.equals(link.getAttributeNS(RDF, "resource")));
    }

    private static BibEntry readEntry(XmlNode instance, Optional<XmlNode> work, Map<String, XmlNode> resources) {
        BibEntry entry = new BibEntry();
        work.ifPresent(element -> entry.setType(hasJournalHost(element, resources) ? StandardEntryType.Article : StandardEntryType.Book));

        Optional<XmlNode> title = object(first(instance, BF, "title"), resources)
                .or(() -> work.flatMap(element -> object(first(element, BF, "title"), resources)));
        put(entry, StandardField.TITLE, title.flatMap(element -> value(element, BF, "mainTitle")));
        put(entry, StandardField.SUBTITLE, title.flatMap(element -> value(element, BF, "subtitle")));

        work.ifPresent(element -> {
            readContributions(entry, element, resources);
            readLanguage(entry, element, resources);
            put(entry, StandardField.ABSTRACT,
                    object(first(element, BF, "summary"), resources).flatMap(summary -> value(summary, RDFS, "label")));
            readHost(entry, element, resources);
        });

        for (XmlNode activityProperty : children(instance, BF, "provisionActivity")) {
            object(Optional.of(activityProperty), resources).ifPresent(activity -> {
                putIfAbsent(entry, StandardField.ADDRESS, value(activity, BFLC, "simplePlace"));
                putIfAbsent(entry, StandardField.PUBLISHER, value(activity, BFLC, "simpleAgent"));
                Optional<String> date = value(activity, BFLC, "simpleDate").or(() -> value(activity, BF, "date"));
                date.filter(text -> text.length() >= 4 && text.substring(0, 4).chars().allMatch(Character::isDigit))
                    .ifPresent(text -> putIfAbsent(entry, StandardField.YEAR, Optional.of(text.substring(0, 4))));
            });
        }
        readIdentifiers(entry, instance, resources);
        work.ifPresent(element -> readIdentifiers(entry, element, resources));
        put(entry, StandardField.URL, locator(instance, resources));
        work.ifPresent(element -> putIfAbsent(entry, StandardField.URL, locator(element, resources)));
        return entry;
    }

    private static void readLanguage(BibEntry entry, XmlNode work, Map<String, XmlNode> resources) {
        first(work, BF, "language").ifPresent(property -> {
            Optional<XmlNode> language = object(Optional.of(property), resources);
            language.flatMap(element -> value(element, BF, "code"))
                    .ifPresent(code -> put(entry, StandardField.LANGUAGE, Optional.of(code)));
            String uri = language.map(element -> element.getAttributeNS(RDF, "about"))
                                 .orElseGet(() -> property.getAttributeNS(RDF, "resource"));
            if (entry.getField(StandardField.LANGUAGE).isEmpty() && uri.contains("/")) {
                put(entry, StandardField.LANGUAGE, Optional.of(uri.substring(uri.lastIndexOf('/') + 1)));
            }
        });
    }

    private static boolean hasJournalHost(XmlNode work, Map<String, XmlNode> resources) {
        return children(work, BF, "relation").stream()
                       .map(property -> object(Optional.of(property), resources))
                       .flatMap(Optional::stream)
                       .filter(BibframeImporter::isPartOf)
                       .map(relation -> object(first(relation, BF, "associatedResource"), resources))
                       .flatMap(Optional::stream)
                       .anyMatch(host -> hasIdentifier(host, "Issn", resources));
    }

    private static boolean isPartOf(XmlNode relation) {
        return first(relation, BF, "relationship")
                .map(element -> element.getAttributeNS(RDF, "resource").endsWith("/partof"))
                .orElse(false);
    }

    private static void readHost(BibEntry entry, XmlNode work, Map<String, XmlNode> resources) {
        for (XmlNode property : children(work, BF, "relation")) {
            Optional<XmlNode> relation = object(Optional.of(property), resources).filter(BibframeImporter::isPartOf);
            Optional<XmlNode> host = relation.flatMap(element -> object(first(element, BF, "associatedResource"), resources));
            host.filter(element -> hasIdentifier(element, "Issn", resources)).ifPresent(element -> {
                putIfAbsent(entry, StandardField.JOURNAL,
                        object(first(element, BF, "title"), resources).flatMap(title -> value(title, BF, "mainTitle")));
                readIdentifier(entry, element, resources, "Issn", StandardField.ISSN);
            });
            host.flatMap(element -> object(first(element, BF, "hasInstance"), resources))
                .flatMap(element -> value(element, BF, "part"))
                .filter(part -> part.startsWith("pages:"))
                .ifPresent(part -> putIfAbsent(entry, StandardField.PAGES, Optional.of(part.substring("pages:".length()))));
        }
    }

    private static void readContributions(BibEntry entry, XmlNode work, Map<String, XmlNode> resources) {
        Map<StandardField, List<String>> names = new HashMap<>();
        for (XmlNode property : children(work, BF, "contribution")) {
            object(Optional.of(property), resources).ifPresent(contribution -> {
                Optional<String> role = first(contribution, BF, "role")
                        .map(element -> object(Optional.of(element), resources)
                                .map(resource -> value(resource, BF, "code")
                                        .orElseGet(() -> resource.getAttributeNS(RDF, "about")))
                                .orElseGet(() -> element.getAttributeNS(RDF, "resource")));
                Optional<String> name = object(first(contribution, BF, "agent"), resources)
                        .flatMap(agent -> value(agent, RDFS, "label"));
                Optional<StandardField> field = role.filter(uri -> uri.endsWith("/aut") || "aut".equals(uri)).map(_ -> StandardField.AUTHOR)
                                                    .or(() -> role.filter(uri -> uri.endsWith("/edt") || "edt".equals(uri))
                                                                  .map(_ -> StandardField.EDITOR));
                field.ifPresent(selectedField -> name.ifPresent(text -> names.computeIfAbsent(selectedField, _ -> new ArrayList<>()).add(text)));
            });
        }
        names.forEach((field, values) -> entry.setField(field, values.stream().collect(Collectors.joining(" and "))));
    }

    private static void readIdentifiers(BibEntry entry, XmlNode resource, Map<String, XmlNode> resources) {
        readIdentifier(entry, resource, resources, "Isbn", StandardField.ISBN);
        readIdentifier(entry, resource, resources, "Issn", StandardField.ISSN);
        readIdentifier(entry, resource, resources, "Doi", StandardField.DOI);
    }

    private static void readIdentifier(BibEntry entry, XmlNode resource, Map<String, XmlNode> resources,
                                       String type, StandardField field) {
        children(resource, BF, "identifiedBy").stream()
                .map(property -> object(Optional.of(property), resources))
                .flatMap(Optional::stream)
                .filter(identifier -> isType(identifier, type))
                .map(identifier -> value(identifier, RDF, "value"))
                .flatMap(Optional::stream)
                .findFirst().ifPresent(value -> putIfAbsent(entry, field, Optional.of(value)));
    }

    private static boolean hasIdentifier(XmlNode resource, String type, Map<String, XmlNode> resources) {
        return children(resource, BF, "identifiedBy").stream()
                       .map(property -> object(Optional.of(property), resources))
                       .flatMap(Optional::stream)
                       .anyMatch(identifier -> isType(identifier, type));
    }

    private static Optional<String> locator(XmlNode resource, Map<String, XmlNode> resources) {
        return first(resource, BF, "electronicLocator").flatMap(property -> {
            String uri = property.getAttributeNS(RDF, "resource");
            if (!uri.isBlank()) {
                return Optional.of(uri);
            }
            return object(Optional.of(property), resources).flatMap(element -> value(element, RDF, "value"))
                         .or(() -> Optional.of(property.getTextContent().trim()).filter(text -> !text.isBlank()));
        });
    }

    private static void put(BibEntry entry, StandardField field, Optional<String> value) {
        value.map(String::trim).filter(text -> !text.isBlank()).ifPresent(text -> entry.setField(field, text));
    }

    private static void putIfAbsent(BibEntry entry, StandardField field, Optional<String> value) {
        if (entry.getField(field).isEmpty()) {
            put(entry, field, value);
        }
    }

    private static Optional<String> value(XmlNode parent, String namespace, String localName) {
        return first(parent, namespace, localName).map(element -> element.getTextContent().trim())
                    .filter(text -> !text.isBlank());
    }

    private static Optional<XmlNode> object(Optional<XmlNode> property, Map<String, XmlNode> resources) {
        return property.flatMap(element -> children(element).stream().findFirst()
                .or(() -> Optional.ofNullable(resources.get(element.getAttributeNS(RDF, "resource")))));
    }

    private static boolean isType(XmlNode element, String localName) {
        return is(element, BF, localName) || is(element, RDF, "Description")
                && children(element, RDF, "type").stream()
                           .anyMatch(type -> (BF + localName).equals(type.getAttributeNS(RDF, "resource")));
    }

    private static boolean is(XmlNode element, String namespace, String localName) {
        return namespace.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName());
    }

    private static Optional<XmlNode> first(XmlNode parent, String namespace, String localName) {
        return children(parent, namespace, localName).stream().findFirst();
    }

    private static List<XmlNode> children(XmlNode parent, String namespace, String localName) {
        return children(parent).stream().filter(element -> is(element, namespace, localName)).toList();
    }

    private static List<XmlNode> children(XmlNode parent) {
        return parent.children;
    }

    private static class XmlNode {
        private final String namespace;
        private final String localName;
        private final Map<QName, String> attributes = new HashMap<>();
        private final List<XmlNode> children = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();

        XmlNode(XMLStreamReader reader) {
            namespace = Optional.ofNullable(reader.getNamespaceURI()).orElse("");
            localName = reader.getLocalName();
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attributeNamespace = Optional.ofNullable(reader.getAttributeNamespace(i)).orElse("");
                attributes.put(new QName(attributeNamespace, reader.getAttributeLocalName(i)), reader.getAttributeValue(i));
            }
        }

        String getAttributeNS(String namespace, String localName) {
            return attributes.getOrDefault(new QName(namespace, localName), "");
        }

        String getNamespaceURI() {
            return namespace;
        }

        String getLocalName() {
            return localName;
        }

        String getTextContent() {
            return text.toString();
        }
    }
}
