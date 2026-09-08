package org.jabref.model.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.value.ObservableValue;

import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.util.Version;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.SaveOrder.OrderType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Guards [MetaData#overwriteWith] against silently dropping a setting: an undone metadata change
/// restores what the copy kept, so a field the copy misses is lost work, not a cosmetic slip.
///
/// The fields are enumerated reflectively, so adding one without extending `overwriteWith` - or
/// without populating it here - fails.
@NullMarked
class MetaDataOverwriteWithTest {

    /// Not contents: the listeners registered on an instance, the binding derived from the group
    /// root, and whether this instance posts at all.
    private static final Set<String> NOT_COPIED = Set.of("eventBus", "groupsRootBinding", "isEventPropagationEnabled");

    @Test
    void overwriteWithCopiesEveryField() throws IllegalAccessException {
        MetaData source = populated();
        MetaData target = new MetaData();

        target.overwriteWith(source);

        for (Field field : copiedFields()) {
            assertEquals(valueOf(field, source), valueOf(field, target),
                    "MetaData.overwriteWith does not copy '%s'; extend it when adding a field".formatted(field.getName()));
        }
    }

    /// Without this, a field added to [MetaData] but not to [#populated] would sit at its default
    /// in both instances and pass the test above while never being copied at all.
    @Test
    void everyCopiedFieldIsPopulated() throws IllegalAccessException {
        MetaData source = populated();
        MetaData untouched = new MetaData();

        List<Field> fields = copiedFields();
        assertFalse(fields.isEmpty(), "reflection found no fields; the test no longer tests anything");

        for (Field field : fields) {
            assertNotEquals(valueOf(field, untouched), valueOf(field, source),
                    "'%s' is left at its default here, so the copy of it is never tested".formatted(field.getName()));
        }
    }

    private static MetaData populated() {
        MetaData metaData = new MetaData();
        metaData.setSaveOrder(new SaveOrder(OrderType.ORIGINAL, List.of()));
        metaData.setGroups(GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ',')));
        metaData.setGroupSearchSyntaxVersion(Version.parse("6.0"));
        metaData.setContainsSearchGroups(true);
        metaData.setGitAutoPull(true);
        metaData.setGitAutoCommit(true);
        metaData.setGitAutoPush(true);
        metaData.setCiteKeyPattern(new CitationKeyPattern("[auth]", CitationKeyPattern.Category.AUTHOR_RELATED),
                Map.of(StandardEntryType.Article, new CitationKeyPattern("[auth][year]", CitationKeyPattern.Category.AUTHOR_RELATED)));
        metaData.setSaveActions(new FieldFormatterCleanupActions(true, List.of()));
        metaData.setMode(BibDatabaseMode.BIBLATEX);
        metaData.setLibraryAbbreviationType(AbbreviationType.DOTLESS);
        metaData.setKeywordSeparator(';');
        metaData.addContentSelector(new ContentSelector(StandardField.AUTHOR, "Einstein"));
        metaData.setLibrarySpecificFileDirectory("/tmp/library");
        metaData.setVersionDBStructure("1");
        metaData.setAiLibraryId("ai-library");
        metaData.markAsProtected();
        metaData.setUserFileDirectory("user-host", "/tmp/user");
        metaData.setLatexFileDirectory("user-host", "/tmp/latex");
        metaData.setEncoding(StandardCharsets.ISO_8859_1);
        metaData.setEncodingExplicitlySupplied(true);
        metaData.putUnknownMetaDataItem("unknown", List.of("value"));
        metaData.setBlgFilePath("user-host", Path.of("/tmp/library.blg"));
        return metaData;
    }

    private static List<Field> copiedFields() {
        List<Field> result = new ArrayList<>();
        for (Field field : MetaData.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !NOT_COPIED.contains(field.getName())) {
                result.add(field);
            }
        }
        return result;
    }

    /// The value as the copy has to reproduce it: a property's content rather than the property.
    private static Object valueOf(Field field, MetaData metaData) throws IllegalAccessException {
        field.setAccessible(true);
        Object value = field.get(metaData);
        return value instanceof ObservableValue<?> observable ? observable.getValue() : value;
    }
}
