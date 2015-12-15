package net.sf.jabref.model.entry;

//@formatter:off
public enum MyStandardEntryClass implements MyEntryClass {
    ARTICLE("article"),
    BOOK("book");

    private final String name;


    private MyStandardEntryClass(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
