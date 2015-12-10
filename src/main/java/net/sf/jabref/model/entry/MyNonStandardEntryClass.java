package net.sf.jabref.model.entry;

public class MyNonStandardEntryClass implements MyEntryClass {
    private final String name;


    MyNonStandardEntryClass(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
