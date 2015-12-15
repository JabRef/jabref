package net.sf.jabref.model.entry;

import java.util.Map;
import java.util.TreeMap;

public class MyEntryClasses {

    private static Map<String, MyEntryClass> map = new TreeMap<String, MyEntryClass>();


    static {
        for (MyEntryClass myClass : MyStandardEntryClass.values()) {
            map.put(myClass.getName(), myClass);
        }
    }

    public static MyEntryClass getClassFor(String name) {
        MyEntryClass type = map.get(name);
        if (type == null) {
            return new MyNonStandardEntryClass(name);
        } else {
            return type;
        }
    }
}