package org.jabref.toolkit.converter;

import picocli.CommandLine;

public abstract class CaseInsensitiveEnumConverter<T extends Enum<T>>
        implements CommandLine.ITypeConverter<T> {

    private final Class<T> enumType;

    public CaseInsensitiveEnumConverter(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public T convert(String value) {
        return Enum.valueOf(enumType, value.toUpperCase());
    }
}
