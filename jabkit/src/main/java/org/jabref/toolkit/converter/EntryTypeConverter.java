package org.jabref.toolkit.converter;

import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.UnknownEntryType;

import picocli.CommandLine;

public class EntryTypeConverter implements CommandLine.ITypeConverter<EntryType> {

    @Override
    public EntryType convert(String value) {
        EntryType entryType = EntryTypeFactory.parse(value);

        if (entryType instanceof UnknownEntryType) {
            throw new CommandLine.TypeConversionException(
                    "Unknown entry type: '" + value + "'. " +
                            "Please use a valid BibTeX entry type (e.g. article, book, inproceedings)."
            );
        }

        return entryType;
    }
}
